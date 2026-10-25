package dev.hew7.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import dev.hew7.launcher.data.NetKind
import dev.hew7.launcher.data.NetworkLabel
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hew7.launcher.data.Sky
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import android.graphics.Paint as NativePaint

/** Text shown on the screensaver. The weather uses its exact shape: snow settles on it, rain splashes off it. */
data class SceneText(
    val greeting: String,
    val time: String,
    val date: String,
    val weather: String,
    val tvName: String,
    val network: NetworkLabel,
)

enum class SceneMood { RAIN, STORM, SNOW, SUN, NIGHT, FOG, CLOUDY }

fun moodFor(sky: Sky, night: Boolean): SceneMood = when (sky) {
    Sky.RAIN -> SceneMood.RAIN
    Sky.STORM -> SceneMood.STORM
    Sky.SNOW -> SceneMood.SNOW
    Sky.FOG -> SceneMood.FOG
    Sky.CLOUDY -> SceneMood.CLOUDY
    Sky.CLEAR, Sky.PARTLY -> if (night) SceneMood.NIGHT else SceneMood.SUN
}

/** Colour grade for the wallpaper photo so it looks like the weather outside. */
fun gradeFor(mood: SceneMood, night: Boolean): ColorMatrix {
    // saturation, brightness, red / blue shift
    val (sat, bright, shift) = when (mood) {
        SceneMood.RAIN -> Triple(0.7f, 0.76f, -8f)
        SceneMood.STORM -> Triple(0.55f, 0.58f, -6f)
        SceneMood.SNOW -> Triple(0.6f, 1.0f, -6f)
        SceneMood.SUN -> Triple(1.2f, 1.05f, 14f)
        SceneMood.NIGHT -> Triple(0.6f, 0.45f, -12f)
        SceneMood.FOG -> Triple(0.55f, 0.95f, 0f)
        SceneMood.CLOUDY -> Triple(0.8f, 0.9f, -4f)
    }
    val b = if (night && mood != SceneMood.NIGHT) bright * 0.75f else bright
    val lift = if (mood == SceneMood.FOG) 22f else 0f // washes out contrast
    return ColorMatrix().apply {
        setToSaturation(sat)
        timesAssign(
            ColorMatrix(
                floatArrayOf(
                    b, 0f, 0f, 0f, shift + lift,
                    0f, b, 0f, 0f, lift,
                    0f, 0f, b, 0f, -shift + lift,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }
}

/**
 * Everything drawn over the screensaver wallpaper: the weather as seen through a window (rain in
 * depth, drops on the glass, frost, a snowy sill, sun rays, stars, fog) plus the time and info text,
 * which the weather lands on.
 */
@Composable
fun WeatherScene(
    mood: SceneMood,
    text: SceneText,
    font: FontFamily,
    modifier: Modifier = Modifier,
    effects: Boolean = true,
    /** Calm: no weather, and the scene is drawn once a minute instead of every frame. */
    calm: Boolean = false,
) {
    val measurer = rememberTextMeasurer()
    val sim = remember(mood, effects, calm) { WeatherSim(mood, effects && !calm) }
    val tvIcon = rememberVectorPainter(Icons.Rounded.Tv)
    // The network icon follows the connection type.
    val netIcon = rememberVectorPainter(
        when (text.network.kind) {
            NetKind.WIFI -> Icons.Rounded.Wifi
            NetKind.WIRED -> Icons.Rounded.SettingsEthernet
            NetKind.OTHER -> Icons.Rounded.Public
            NetKind.OFFLINE -> Icons.Rounded.WifiOff
        },
    )
    sim.icons = tvIcon to netIcon
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sim) {
        if (calm) {
            // Once a minute: the clock changes, and the text shifts a little so nothing burns in.
            while (true) {
                sim.nudge()
                tick++
                delay(60_000)
            }
        } else {
            while (true) withFrameMillis { ms ->
                sim.advance(ms)
                tick = ms
            }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        tick // read so the canvas redraws every frame
        sim.draw(this, measurer, text, font)
    }
}

/** Positions for a field of particles, plus a per-particle random "seed" in s. */
private class Field(val n: Int) {
    val x = FloatArray(n)
    val y = FloatArray(n)
    val s = FloatArray(n)
}

private class GlassDrop(var x: Float, var y: Float, val r: Float) {
    var v = 0f
    val trail = ArrayDeque<Offset>()
}

/** With [effects] off, only the text is drawn: no weather, no glint or flash. */
private class WeatherSim(private val mood: SceneMood, private val effects: Boolean) {
    private val rnd = Random(System.nanoTime())
    private val wet = mood == SceneMood.RAIN || mood == SceneMood.STORM

    private var w = 0f
    private var h = 0f
    private var u = 1f // 1 unit = 1px on a 960-wide screen
    private var lastMs = 0L
    private var dt = 1f // frames at 60 fps since the last draw
    private var sec = 0f
    private var flash = 0f
    private var sill = 0f

    private val rain = arrayOf(Field(80), Field(55), Field(28))
    private val snow = arrayOf(Field(90), Field(60), Field(12))
    private val stars = Field(110)
    private val motes = Field(40)
    private val clouds = Field(7)
    private val ripples = ArrayList<FloatArray>() // x, y, r, max
    private val sparks = ArrayList<FloatArray>() // x, y, vx, vy, life, runs down
    private val falling = ArrayList<FloatArray>() // x, y, vx, vy
    private val glass = ArrayList<GlassDrop>()
    private var shoot: FloatArray? = null // x, y, length

    // Text and the surfaces it offers (in text coordinates, before the drift offset).
    private var textKey: Any? = null
    private var lastTime: String? = null
    private var layouts: List<Pair<TextLayoutResult, Offset>> = emptyList()

    /** TV and network icons, drawn beside their lines; set by the composable each recomposition. */
    var icons: Pair<Painter, Painter>? = null
    private var iconSpots: List<Pair<Painter, Rect>> = emptyList()
    private var tops: Array<IntArray> = emptyArray()
    private var minuteLeft = 0f
    private var minuteRight = 0f
    private var minuteBottom = 0f
    private val caps = HashMap<Int, Float>() // column * 8192 + y -> snow height

    private val linePaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply { strokeCap = NativePaint.Cap.ROUND }

    fun advance(ms: Long) {
        if (lastMs == 0L) lastMs = ms
        dt = ((ms - lastMs) / 16.67f).coerceIn(0.2f, 3f)
        lastMs = ms
        sec += dt / 60f
        if (flash > 0) flash = (flash - 0.05f * dt).coerceAtLeast(0f)
        if (mood == SceneMood.STORM && rnd.nextFloat() < 0.004f * dt) flash = 1f
    }

    /** Calm mode's once-a-minute step: moves the drift along so the text shifts a few pixels. */
    fun nudge() {
        sec += 4f
    }

    fun draw(scope: DrawScope, measurer: TextMeasurer, text: SceneText, font: FontFamily) = with(scope) {
        if (size.width != w || size.height != h) resize(size)
        ensureText(measurer, text, font)
        // Slow drift so nothing burns into the screen.
        val off = Offset(sin(sec * 0.12f) * 16.dp.toPx(), sin(sec * 0.12f) * 8.dp.toPx())

        if (effects) behindText(off)
        drawRect(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.48f)))
        translate(off.x, off.y) { textLayer() }
        if (effects) inFront()
        if (effects && flash > 0) drawRect(Color.White.copy(alpha = flash * 0.35f), blendMode = BlendMode.Screen)
    }

    // ---------- setup ----------

    private fun resize(size: Size) {
        w = size.width
        h = size.height
        u = w / 960f
        fun seed(f: Field, yMax: Float = h) = repeat(f.n) { f.x[it] = rnd.nextFloat() * w; f.y[it] = rnd.nextFloat() * yMax; f.s[it] = rnd.nextFloat() }
        rain.forEach { seed(it) }
        snow.forEach { seed(it) }
        seed(stars, h * 0.55f)
        seed(motes)
        seed(clouds, h * 0.5f)
        textKey = null
        caps.clear()
    }

    private fun DrawScope.ensureText(measurer: TextMeasurer, text: SceneText, font: FontFamily) {
        val key = Triple(text, font, w)
        if (key == textKey) return
        textKey = key

        // When the minute changes, the snow on the old minute digits tumbles off.
        if (lastTime != null && lastTime != text.time) {
            val it = caps.entries.iterator()
            while (it.hasNext()) {
                val (k, height) = it.next()
                val col = k / 8192
                val y = k % 8192
                if (col >= minuteLeft && col <= minuteRight && y < minuteBottom) {
                    var j = 0f
                    while (j < height) {
                        falling += floatArrayOf(col.toFloat(), y - j, (rnd.nextFloat() - 0.5f) * 0.6f * u, rnd.nextFloat() * 0.5f * u)
                        j += 3f * u
                    }
                    it.remove()
                }
            }
        }
        lastTime = text.time

        val shadow = when (mood) {
            SceneMood.NIGHT -> Shadow(Color(0xCCAAC8FF), blurRadius = 18.dp.toPx())
            SceneMood.FOG -> Shadow(Color(0xBFFFFFFF), blurRadius = 26.dp.toPx())
            else -> Shadow(Color(0x80000000), Offset(0f, 2.dp.toPx()), 12.dp.toPx())
        }
        fun style(size: TextUnit, weight: FontWeight) =
            TextStyle(color = Color.White, fontSize = size, fontWeight = weight, fontFamily = font, shadow = shadow)

        val greeting = measurer.measure(text.greeting, style(22.sp, FontWeight.Bold))
        val time = measurer.measure(text.time, style(96.sp, FontWeight.Light))
        val date = measurer.measure(text.date, style(22.sp, FontWeight.Normal))
        val weather = measurer.measure(text.weather, style(24.sp, FontWeight.Bold))
        val tv = measurer.measure(text.tvName, style(18.sp, FontWeight.Normal))
        val net = measurer.measure(text.network.name, style(18.sp, FontWeight.Normal))

        // Left block, bottom up: weather, date, time, and the greeting on top like a headline.
        val margin = 56.dp.toPx()
        val weatherTop = h - margin - weather.size.height
        val dateTop = weatherTop - 2.dp.toPx() - date.size.height
        val timeTop = dateTop - time.size.height + 10.dp.toPx()
        val greetingTop = timeTop - greeting.size.height + 12.dp.toPx()
        // Right block: TV name above the network, each with its icon in front.
        val iconSize = tv.size.height * 0.85f
        val iconGap = 8.dp.toPx()
        val netTop = h - margin - net.size.height
        val tvTop = netTop - 4.dp.toPx() - tv.size.height
        val tvX = w - margin - tv.size.width
        val netX = w - margin - net.size.width
        layouts = listOf(
            greeting to Offset(margin, greetingTop),
            time to Offset(margin, timeTop),
            date to Offset(margin, dateTop),
            weather to Offset(margin, weatherTop),
            tv to Offset(tvX, tvTop),
            net to Offset(netX, netTop),
        )
        val iconInset = (tv.size.height - iconSize) / 2
        iconSpots = icons?.let { (tvIcon, netIcon) ->
            listOf(
                tvIcon to Rect(Offset(tvX - iconGap - iconSize, tvTop + iconInset), Size(iconSize, iconSize)),
                netIcon to Rect(Offset(netX - iconGap - iconSize, netTop + iconInset), Size(iconSize, iconSize)),
            )
        } ?: emptyList()
        val colon = text.time.indexOf(':') + 1
        minuteLeft = margin + if (colon in 1 until text.time.length) time.getBoundingBox(colon).left else 0f
        minuteRight = margin + time.size.width
        minuteBottom = timeTop + time.size.height

        computeTops()
        // Drop snow that no longer sits on a surface.
        caps.keys.removeAll { k -> (k % 8192) !in (tops.getOrNull(k / 8192) ?: IntArray(0)) }
    }

    /** Finds every upward-facing edge of the text by drawing it (at half size) and scanning the alpha. */
    private fun DrawScope.computeTops() {
        val sw = (w / 2).toInt().coerceAtLeast(1)
        val sh = (h / 2).toInt().coerceAtLeast(1)
        val img = ImageBitmap(sw, sh)
        CanvasDrawScope().draw(Density(density, fontScale), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(img), Size(sw.toFloat(), sh.toFloat())) {
            scale(0.5f, pivot = Offset.Zero) {
                layouts.forEach { (l, p) -> drawText(l, topLeft = p, shadow = Shadow.None) }
                drawIcons(Color.White)
            }
        }
        val px = IntArray(sw * sh)
        img.readPixels(px)
        val cols = w.toInt()
        tops = Array(cols) { IntArray(0) }
        val found = ArrayList<Int>()
        for (cx in 0 until sw) {
            found.clear()
            var prev = 0
            for (cy in 0 until sh) {
                val a = px[cy * sw + cx] ushr 24
                if (a >= 120 && prev < 120) found += cy * 2
                prev = a
            }
            val arr = found.toIntArray()
            if (cx * 2 < cols) tops[cx * 2] = arr
            if (cx * 2 + 1 < cols) tops[cx * 2 + 1] = arr
        }
    }

    /** First text surface in this column below y, or -1. */
    private fun hitTop(col: Int, y: Float): Int {
        val c = tops.getOrNull(col) ?: return -1
        for (t in c) if (t > y) return t
        return -1
    }

    // ---------- layers ----------

    private fun DrawScope.behindText(off: Offset) {
        when (mood) {
            SceneMood.NIGHT -> nightSky()
            SceneMood.SUN -> sunRays()
            SceneMood.FOG -> fogBanks()
            SceneMood.CLOUDY -> cloudDrift()
            else -> Unit
        }
        if (wet) {
            rainLayers(off)
            groundRipples()
        }
        if (mood == SceneMood.SNOW) snowBehind(off)
    }

    private fun DrawScope.drawIcons(color: Color) {
        iconSpots.forEach { (painter, r) ->
            translate(r.left, r.top) { with(painter) { draw(r.size, colorFilter = ColorFilter.tint(color)) } }
        }
    }

    private fun DrawScope.textLayer() {
        layouts.forEach { (l, p) -> drawText(l, topLeft = p) }
        drawIcons(Color.White.copy(alpha = 0.9f))

        if (effects && mood == SceneMood.SUN) {
            // A warm glint sweeping slowly across the text.
            val band = 50 * u
            val gx = (sec * 180 * u) % (w + 600 * u) - 300 * u
            clipRect(gx - band, 0f, gx + band, h) {
                layouts.forEach { (l, p) -> drawText(l, color = Color(0xFFFFE2A8), topLeft = p, alpha = 0.7f, shadow = Shadow.None) }
            }
        }

        if (wet) {
            val it = sparks.iterator()
            while (it.hasNext()) {
                val s = it.next()
                s[0] += s[2] * dt
                s[1] += s[3] * dt
                if (s[5] == 0f) s[3] += 0.3f * u * dt
                s[4] -= dt
                drawRect(Color(0xD9E6F0FF), Offset(s[0], s[1]), Size(1.5f * u, (if (s[5] == 1f) 3f else 1.5f) * u))
                if (s[4] <= 0) it.remove()
            }
        }

        if (mood == SceneMood.SNOW) {
            if (caps.isNotEmpty()) {
                val pts = FloatArray(caps.size * 4)
                var k = 0
                for ((key, height) in caps) {
                    val col = (key / 8192).toFloat()
                    val y = (key % 8192).toFloat()
                    pts[k++] = col; pts[k++] = y; pts[k++] = col; pts[k++] = y - height
                }
                linePaint.color = 0xF7FCFDFF.toInt()
                linePaint.strokeWidth = 1.8f * u
                drawContext.canvas.nativeCanvas.drawLines(pts, 0, k, linePaint)
            }
            val it = falling.iterator()
            while (it.hasNext()) {
                val f = it.next()
                f[3] += 0.12f * u * dt
                f[0] += f[2] * dt
                f[1] += f[3] * dt
                drawRect(Color(0xF2FCFDFF), Offset(f[0], f[1]), Size(1.8f * u, 1.8f * u))
                if (f[1] > h) it.remove()
            }
        }
    }

    private fun DrawScope.inFront() {
        if (mood == SceneMood.SNOW) {
            // Big, soft flakes right in front of the "window".
            val f = snow[2]
            for (i in 0 until f.n) {
                f.y[i] += 2.2f * (0.7f + f.s[i] * 0.6f) * u * dt
                f.x[i] += sin(sec * 1f + f.s[i] * 9) * 1.2f * u * dt
                if (f.y[i] > h + 20 * u) { f.y[i] = -20 * u; f.x[i] = rnd.nextFloat() * w }
                drawCircle(Color.White.copy(alpha = 0.22f), 9 * (0.7f + f.s[i] * 0.6f) * u, Offset(f.x[i], f.y[i]))
            }
            // Frost creeping in from the corners.
            for (c in listOf(Offset.Zero, Offset(w, 0f), Offset(0f, h), Offset(w, h))) {
                drawRect(Brush.radialGradient(listOf(Color(0x5CEBF5FF), Color(0x00EBF5FF)), center = c, radius = 250 * u))
            }
            // Snow building up along the bottom, like a windowsill.
            sill = (sill + 0.02f * u * dt).coerceAtMost(16 * u)
            val path = Path().apply {
                moveTo(0f, h)
                for (i in 0..48) lineTo(i * w / 48, h - sill * (0.7f + 0.3f * sin(i * 1.7f)))
                lineTo(w, h)
                close()
            }
            drawPath(path, Color(0xEBFAFCFF))
        }

        if (wet) glassDrops()

        if (mood == SceneMood.SUN) {
            // Soft lens flare.
            drawCircle(Color(0x1AFFE6B4), 40 * u, Offset(w * 0.78f, h * 0.2f))
            drawCircle(Color(0x21FFE6B4), 18 * u, Offset(w * 0.62f, h * 0.42f))
            drawCircle(Color(0x0DFFE6B4), 60 * u, Offset(w * 0.48f, h * 0.6f))
        }
    }

    // ---------- effects ----------

    private fun DrawScope.rainLayers(off: Offset) {
        val heavy = if (mood == SceneMood.STORM) 1.6f else 1f
        val alphas = floatArrayOf(0.16f, 0.28f, 0.42f)
        val widths = floatArrayOf(0.8f, 1.3f, 2.2f)
        val speeds = floatArrayOf(7f, 11f, 17f)
        val lens = floatArrayOf(8f, 14f, 26f)
        for (d in 0..2) {
            val f = rain[d]
            val sp = speeds[d] * heavy * u * dt
            val len = lens[d] * u
            val pts = FloatArray(f.n * 4)
            var k = 0
            for (i in 0 until f.n) {
                val ny = f.y[i] + sp
                if (d == 2) {
                    // The nearest drops can land on the text and splash.
                    val tx = f.x[i] - off.x
                    val hy = hitTop(tx.toInt(), f.y[i] - off.y)
                    if (hy >= 0 && hy + off.y >= f.y[i] && hy + off.y < ny) {
                        repeat(3) { sparks += floatArrayOf(tx, hy.toFloat(), (rnd.nextFloat() - 0.5f) * 2.2f * u, (-1f - rnd.nextFloat() * 1.6f) * u, 12f, 0f) }
                        if (rnd.nextFloat() < 0.1f) sparks += floatArrayOf(tx, hy.toFloat(), 0f, 0.6f * u, 60f, 1f)
                        f.y[i] = -30 * u
                        f.x[i] = rnd.nextFloat() * w
                        continue
                    }
                }
                f.y[i] = ny
                f.x[i] += sp * 0.12f
                if (f.y[i] > h) { f.y[i] = -30 * u; f.x[i] = rnd.nextFloat() * w }
                pts[k++] = f.x[i]; pts[k++] = f.y[i]; pts[k++] = f.x[i] + len * 0.12f; pts[k++] = f.y[i] + len
            }
            linePaint.color = Color(0xFFD2E1FF).copy(alpha = alphas[d]).toArgb()
            linePaint.strokeWidth = widths[d] * u
            drawContext.canvas.nativeCanvas.drawLines(pts, 0, k, linePaint)
        }
    }

    private fun DrawScope.groundRipples() {
        // Splashes on the "ground": bigger lower down, so they read in perspective.
        val expected = (if (mood == SceneMood.STORM) 5f else 3f) * dt
        val count = expected.toInt() + if (rnd.nextFloat() < expected % 1f) 1 else 0
        repeat(count) {
            val y = h * 0.62f + rnd.nextFloat() * h * 0.36f
            ripples += floatArrayOf(rnd.nextFloat() * w, y, 0f, (2f + (y - h * 0.6f) / (h * 0.4f) * 7f) * u)
        }
        val it = ripples.iterator()
        while (it.hasNext()) {
            val r = it.next()
            r[2] += 0.5f * u * dt
            drawOval(
                Color(0x73DCE6FF).copy(alpha = 0.45f * (1 - r[2] / r[3]).coerceIn(0f, 1f)),
                Offset(r[0] - r[2] * 1.6f, r[1] - r[2] * 0.5f),
                Size(r[2] * 3.2f, r[2]),
                style = Stroke(1f * u),
            )
            if (r[2] >= r[3]) it.remove()
        }
    }

    private fun DrawScope.glassDrops() {
        if (glass.size < 32 && rnd.nextFloat() < 0.22f * dt) glass += GlassDrop(rnd.nextFloat() * w, rnd.nextFloat() * h, (3 + rnd.nextFloat() * 9) * u)
        val it = glass.iterator()
        while (it.hasNext()) {
            val d = it.next()
            if (d.r > 7 * u && d.v == 0f && rnd.nextFloat() < 0.01f * dt) d.v = (1.5f + rnd.nextFloat() * 2f) * u
            if (d.v > 0) {
                d.trail.addLast(Offset(d.x, d.y))
                if (d.trail.size > 40) d.trail.removeFirst()
                d.y += d.v * dt
                d.x += (rnd.nextFloat() - 0.5f) * 0.8f * u
            }
            if (d.trail.size > 1) {
                val p = Path().apply {
                    moveTo(d.trail.first().x, d.trail.first().y)
                    d.trail.forEach { lineTo(it.x, it.y) }
                }
                drawPath(p, Color.White.copy(alpha = 0.12f), style = Stroke(d.r * 0.6f, cap = StrokeCap.Round))
            }
            drawCircle(Color.White.copy(alpha = 0.13f), d.r, Offset(d.x, d.y))
            drawCircle(Color.White.copy(alpha = 0.5f), d.r * 0.28f, Offset(d.x - d.r * 0.35f, d.y - d.r * 0.35f))
            if (d.y > h + 20 * u) it.remove()
        }
    }

    private fun DrawScope.snowBehind(off: Offset) {
        val speeds = floatArrayOf(0.5f, 1f)
        val sway = floatArrayOf(0.3f, 0.6f)
        val radii = floatArrayOf(1.4f, 2.4f)
        val alphas = floatArrayOf(0.6f, 0.85f)
        val maxCap = 9 * u
        for (d in 0..1) {
            val f = snow[d]
            for (i in 0 until f.n) {
                f.y[i] += speeds[d] * (0.7f + f.s[i] * 0.6f) * u * dt
                f.x[i] += sin(sec * 1f + f.s[i] * 9) * sway[d] * u * dt
                if (d == 1) {
                    // Mid-depth flakes settle on the text and build little snow caps.
                    val lx = (f.x[i] - off.x).toInt()
                    val ly = f.y[i] - off.y
                    val hy = hitTop(lx, ly - 2 * u)
                    if (hy >= 0) {
                        val key = lx * 8192 + hy
                        val cur = caps[key] ?: 0f
                        if (ly >= hy - cur - 1) {
                            if (cur < maxCap) {
                                caps[key] = cur + 1.4f * u
                                // Spread a little to the neighbouring columns on the same edge.
                                val spread = (2 * u).toInt().coerceAtLeast(1)
                                for (dx in -spread..spread) {
                                    if (dx == 0 || hy !in (tops.getOrNull(lx + dx) ?: IntArray(0))) continue
                                    val k2 = (lx + dx) * 8192 + hy
                                    caps[k2] = maxOf(caps[k2] ?: 0f, cur * (1f - abs(dx) / (spread + 1f)))
                                }
                            }
                            f.y[i] = -10 * u
                            f.x[i] = rnd.nextFloat() * w
                            continue
                        }
                    }
                }
                if (f.y[i] > h + 20 * u) { f.y[i] = -20 * u; f.x[i] = rnd.nextFloat() * w }
                drawCircle(Color.White.copy(alpha = alphas[d]), radii[d] * (0.7f + f.s[i] * 0.6f) * u, Offset(f.x[i], f.y[i]))
            }
        }
    }

    private fun DrawScope.nightSky() {
        for (i in 0 until stars.n) {
            val a = 0.2f + 0.7f * abs(sin(sec * 1.2f + stars.s[i] * 20))
            drawCircle(Color.White.copy(alpha = a), (0.6f + stars.s[i] * 1.4f) * u, Offset(stars.x[i], stars.y[i]))
        }
        if (shoot == null && rnd.nextFloat() < 0.004f * dt) shoot = floatArrayOf(rnd.nextFloat() * w * 0.7f + w * 0.2f, rnd.nextFloat() * h * 0.25f, 0f)
        shoot?.let { s ->
            s[2] += 14 * u * dt
            val a = 1 - s[2] / (400 * u)
            if (a <= 0) {
                shoot = null
            } else {
                drawLine(
                    Color.White.copy(alpha = a),
                    Offset(s[0] - s[2], s[1] + s[2] * 0.35f),
                    Offset(s[0] - s[2] + 60 * u, s[1] + s[2] * 0.35f - 21 * u),
                    strokeWidth = 1.6f * u,
                )
            }
        }
    }

    private fun DrawScope.sunRays() {
        val pivot = Offset(w * 0.9f, -20 * u)
        val ray = Path().apply {
            moveTo(pivot.x, pivot.y)
            lineTo(pivot.x - 40 * u, pivot.y + 800 * u)
            lineTo(pivot.x + 40 * u, pivot.y + 800 * u)
            close()
        }
        val brush = Brush.linearGradient(
            listOf(Color(0x2BFFF0C8), Color(0x00FFF0C8)),
            start = pivot,
            end = Offset(pivot.x - 200 * u, pivot.y + 700 * u),
        )
        var angle = 0f
        for (i in 0 until 9) {
            angle += 7.4f + sin(sec * 0.2f + i) * 0.12f
            rotate(angle, pivot) { drawPath(ray, brush) }
        }
        for (i in 0 until motes.n) {
            motes.x[i] += sin(sec * 0.5f + motes.s[i] * 7) * 0.3f * u * dt
            motes.y[i] -= (0.15f + motes.s[i] * 0.2f) * u * dt
            if (motes.y[i] < 0) motes.y[i] = h
            val a = 0.25f + 0.35f * sin(sec * 1.5f + motes.s[i] * 9).let { it * it }
            drawCircle(Color(0xFFFFF0D2).copy(alpha = a), (1 + motes.s[i] * 1.8f) * u, Offset(motes.x[i], motes.y[i]))
        }
    }

    private fun DrawScope.fogBanks() {
        for (i in 0 until 5) {
            val y = h * (0.35f + i * 0.13f)
            val band = 70 * u
            val shift = (sec * (12f + i * 7f) * u) % (w * 2) - w
            val brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to Color(0xFFF0F2F5).copy(alpha = 0.15f + i * 0.04f),
                1f to Color.Transparent,
                startY = y - band,
                endY = y + band,
            )
            drawRect(brush, Offset(shift, y - band), Size(w * 2, band * 2))
            drawRect(brush, Offset(shift - w * 2, y - band), Size(w * 2, band * 2))
        }
    }

    private fun DrawScope.cloudDrift() {
        for (i in 0 until clouds.n) {
            val r = minOf(w, h) * (0.25f + clouds.s[i] * 0.3f)
            val x = ((clouds.s[i] + sec * (0.004f + clouds.s[i] * 0.006f)) % 1f) * (w + r * 2) - r
            val c = Offset(x, clouds.y[i])
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent), center = c, radius = r), r, c)
        }
    }
}
