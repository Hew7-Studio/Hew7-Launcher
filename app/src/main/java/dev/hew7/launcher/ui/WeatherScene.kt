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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.CornerRadius
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
    /** Now playing, shown as a glass card in place of the TV and network names. */
    val card: SceneCard? = null,
    /** The glass weather pill, top right: temperature, sky words and icon. */
    val temp: String = "",
    val cond: String = "",
    val sky: Sky? = null,
    val skyNight: Boolean = false,
    /** Late at night the text turns a warm amber (on dark pictures). */
    val amber: Boolean = false,
    /** The picture is bright behind the clock: dark letters instead of light ones. */
    val darkInk: Boolean = false,
    /** The picture is bright on the right: frosted glass with dark letters for the pill and box. */
    val darkPanels: Boolean = false,
)

/** The screensaver's Now playing card. Rain splashes on it and snow settles on its top edge. */
data class SceneCard(val kicker: String, val title: String, val art: ImageBitmap?, val detail: String = "")

enum class SceneMood { RAIN, STORM, SNOW, SUN, NIGHT, FOG, CLOUDY }

fun moodFor(sky: Sky, night: Boolean): SceneMood = when (sky) {
    Sky.RAIN -> SceneMood.RAIN
    Sky.STORM -> SceneMood.STORM
    Sky.SNOW -> SceneMood.SNOW
    Sky.FOG -> SceneMood.FOG
    Sky.CLOUDY -> SceneMood.CLOUDY
    Sky.CLEAR, Sky.PARTLY -> if (night) SceneMood.NIGHT else SceneMood.SUN
}

/** How much [gradeFor] brightens or darkens the photo, to judge text colour against the graded picture. */
fun gradeBrightness(mood: SceneMood, night: Boolean): Float {
    val b = when (mood) {
        SceneMood.RAIN -> 0.76f
        SceneMood.STORM -> 0.58f
        SceneMood.SNOW -> 1.0f
        SceneMood.SUN -> 1.05f
        SceneMood.NIGHT -> 0.45f
        SceneMood.FOG -> 1.05f
        SceneMood.CLOUDY -> 0.9f
    }
    return if (night && mood != SceneMood.NIGHT) b * 0.75f else b
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
    sim.weatherIcon = rememberVectorPainter(skyIcon(text.sky ?: Sky.CLOUDY, text.skyNight))
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

private val CardCorner = 18.dp

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
    /** The weather pill's sky icon; set by the composable. */
    var weatherIcon: Painter? = null
    private var iconSpots: List<Pair<Painter, Rect>> = emptyList()
    /** Glass shapes: the weather pill (top right) and the box (bottom right). Weather lands on them. */
    private var panels: List<Rect> = emptyList()
    private var cardArt: Pair<ImageBitmap, Rect>? = null
    /** Text inside the glass shapes, drawn without the big shadow. */
    private var panelTexts: List<Pair<TextLayoutResult, Offset>> = emptyList()
    /**
     * Everything above drawn once into one picture. Each frame only moves this picture (by
     * fractions of a pixel, smoothly filtered), so the drift glides instead of the letters
     * stepping from pixel to pixel.
     */
    private var plate: ImageBitmap? = null
    private var brightBehind = false
    private var glassTone = Color(0x660A0E18)
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
        sec += 6f
    }

    fun draw(scope: DrawScope, measurer: TextMeasurer, text: SceneText, font: FontFamily) = with(scope) {
        if (size.width != w || size.height != h) resize(size)
        ensureText(measurer, text, font)
        // A very slow, smooth drift (about two minutes there and back) so nothing burns into the screen.
        val off = Offset(sin(sec * 0.05f) * 12.dp.toPx(), sin(sec * 0.035f) * 6.dp.toPx())

        if (effects) behindText(off)
        // Shade the lower half (light under dark letters, dark under light ones) so the clock reads.
        drawRect(Brush.verticalGradient(0.5f to Color.Transparent, 1f to (if (brightBehind) Color.White.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.42f))))
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

        // Text colours follow the picture: dark letters where it's bright, light ones where it's
        // dark (warm amber late at night), as on the home screen.
        val darkText = Color(0xFF15161A)
        val ink = when {
            text.darkInk -> darkText
            text.amber -> Color(0xFFF5C98A)
            else -> Color.White
        }
        val sub = when {
            text.darkInk -> darkText.copy(alpha = 0.8f)
            text.amber -> Color(0xFFD9B47E)
            else -> Color(0xFFD6DCEB)
        }
        val panelInk = if (text.darkPanels) darkText else if (text.amber) Color(0xFFF5C98A) else Color.White
        val panelSub = if (text.darkPanels) darkText.copy(alpha = 0.75f) else if (text.amber) Color(0xFFD9B47E) else Color(0xFFD6DCEB)
        brightBehind = text.darkInk
        glassTone = if (text.darkPanels) Color(0x80FFFFFF) else Color(0x660A0E18)
        // A soft, low shadow (as on the home screen), not an outline: dark under light letters,
        // light under dark ones.
        val shadow = when {
            text.darkInk -> Shadow(Color(0x80FFFFFF), Offset(0f, 2.dp.toPx()), 14.dp.toPx())
            mood == SceneMood.FOG -> Shadow(Color(0x99FFFFFF), blurRadius = 20.dp.toPx())
            else -> Shadow(Color(0x73000000), Offset(0f, 2.dp.toPx()), 14.dp.toPx())
        }
        fun style(size: TextUnit, weight: FontWeight, family: FontFamily = font, color: Color = ink) =
            TextStyle(color = color, fontSize = size, fontWeight = weight, fontFamily = family, shadow = shadow)
        val plain = TextStyle(color = panelInk, fontFamily = font)
        val maxText = 300.dp.toPx()
        fun one(s: String, st: TextStyle) =
            measurer.measure(s, st, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, constraints = androidx.compose.ui.unit.Constraints(maxWidth = maxText.toInt()))

        // Bottom left, the same size and order as the home screen's night look: a thin clock,
        // the date, then the greeting.
        val time = measurer.measure(text.time, style(48.sp, FontWeight.ExtraLight, DisplayFont).copy(letterSpacing = (-1.5).sp))
        val date = measurer.measure(text.date, style(13.sp, FontWeight.Normal, color = sub))
        val greeting = measurer.measure(text.greeting, style(17.sp, FontWeight.Light, DisplayFont))
        val margin = 54.dp.toPx()
        val greetingTop = h - 48.dp.toPx() - greeting.size.height
        val dateTop = greetingTop - 6.dp.toPx() - date.size.height
        val timeTop = dateTop - 8.dp.toPx() - time.size.height
        layouts = listOf(
            time to Offset(margin, timeTop),
            date to Offset(margin, dateTop),
            greeting to Offset(margin, greetingTop),
        )

        val spots = ArrayList<Pair<Painter, Rect>>()
        val inside = ArrayList<Pair<TextLayoutResult, Offset>>()
        val shapes = ArrayList<Rect>()
        val pad = 12.dp.toPx()

        // Top right: the glass weather pill, as on the home screen.
        if (text.temp.isNotBlank()) {
            val temp = measurer.measure(text.temp, plain.copy(fontSize = 24.sp, fontWeight = FontWeight.Light, fontFamily = DisplayFont))
            val cond = one(text.cond, plain.copy(fontSize = 12.sp, color = panelSub))
            val icon = 20.dp.toPx()
            val pw = pad + 4.dp.toPx() + icon + 8.dp.toPx() + temp.size.width + 24.dp.toPx() + cond.size.width + pad + 4.dp.toPx()
            val ph = maxOf(temp.size.height.toFloat(), icon) + pad * 1.4f
            val r = Rect(Offset(w - margin - pw, 40.dp.toPx()), Size(pw, ph))
            shapes += r
            val cy = r.top + r.height / 2
            weatherIcon?.let { spots += it to Rect(Offset(r.left + pad + 4.dp.toPx(), cy - icon / 2), Size(icon, icon)) }
            inside += temp to Offset(r.left + pad + 4.dp.toPx() + icon + 8.dp.toPx(), cy - temp.size.height / 2)
            inside += cond to Offset(r.right - pad - 4.dp.toPx() - cond.size.width, cy - cond.size.height / 2)
        }

        // Bottom right: a glass box, with Now playing, or else the TV and network names.
        val card = text.card
        cardArt = null
        if (card != null) {
            val kicker = one(card.kicker.uppercase(), plain.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = panelSub, letterSpacing = 1.sp))
            val title = one(card.title, plain.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
            val detail = one(card.detail, plain.copy(fontSize = 12.sp, color = panelSub))
            val artH = 48.dp.toPx()
            val artW = if (card.art != null) artH * 16f / 9f else 0f
            val gap = if (card.art != null) 12.dp.toPx() else 0f
            val textW = maxOf(kicker.size.width, title.size.width, detail.size.width).toFloat().coerceAtLeast(150.dp.toPx())
            val cw = pad + artW + gap + textW + pad
            val ch = pad + artH + pad
            val r = Rect(Offset(w - margin - cw, h - margin - ch), Size(cw, ch))
            shapes += r
            cardArt = card.art?.let { it to Rect(Offset(r.left + pad, r.top + pad), Size(artW, artH)) }
            val tx = r.left + pad + artW + gap
            val lines = listOf(kicker, title) + if (card.detail.isNotBlank()) listOf(detail) else emptyList()
            val textH = lines.sumOf { it.size.height } + (lines.size - 1) * 2.dp.toPx()
            var ty = r.top + (ch - textH) / 2
            lines.forEach { l -> inside += l to Offset(tx, ty); ty += l.size.height + 2.dp.toPx() }
        } else if (text.tvName.isNotBlank() || text.network.name.isNotBlank()) {
            val tv = one(text.tvName, plain.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium))
            val net = one(text.network.name, plain.copy(fontSize = 13.sp, color = panelSub))
            val icon = tv.size.height * 0.9f
            val gap = 8.dp.toPx()
            val cw = pad + icon + gap + maxOf(tv.size.width, net.size.width) + pad + 4.dp.toPx()
            val ch = pad + tv.size.height + 4.dp.toPx() + net.size.height + pad
            val r = Rect(Offset(w - margin - cw, h - margin - ch), Size(cw, ch))
            shapes += r
            val tx = r.left + pad + icon + gap
            val tvTop = r.top + pad
            val netTop = tvTop + tv.size.height + 4.dp.toPx()
            inside += tv to Offset(tx, tvTop)
            inside += net to Offset(tx, netTop)
            icons?.let { (tvIcon, netIcon) ->
                spots += tvIcon to Rect(Offset(r.left + pad, tvTop + (tv.size.height - icon) / 2), Size(icon, icon))
                spots += netIcon to Rect(Offset(r.left + pad, netTop + (net.size.height - icon) / 2), Size(icon, icon))
            }
        }
        iconSpots = spots
        panelTexts = inside
        panels = shapes

        val colon = text.time.indexOf(':') + 1
        minuteLeft = margin + if (colon in 1 until text.time.length) time.getBoundingBox(colon).left else 0f
        minuteRight = margin + time.size.width
        minuteBottom = timeTop + time.size.height

        plate = ImageBitmap(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)).also { img ->
            CanvasDrawScope().draw(Density(density, fontScale), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(img), Size(w, h)) {
                drawPlate(panelInk)
            }
        }
        computeTops()
        // Drop snow that no longer sits on a surface.
        caps.keys.removeAll { k -> (k % 8192) !in (tops.getOrNull(k / 8192) ?: IntArray(0)) }
    }

    /** Draws the glass shapes, the artwork, the icons and all the text: the picture that drifts. */
    private fun DrawScope.drawPlate(ink: Color) {
        val corner = CornerRadius(CardCorner.toPx())
        panels.forEach { r ->
            drawRoundRect(glassTone, r.topLeft, r.size, corner)
            drawRoundRect(Color.White.copy(alpha = 0.2f), r.topLeft, r.size, corner, style = Stroke(1.dp.toPx()))
        }
        cardArt?.let { (img, a) ->
            val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(a, CornerRadius(10.dp.toPx()))) }
            clipPath(clip) {
                // Cover the 16:9 box, cropping the artwork's centre.
                val k = maxOf(a.width / img.width, a.height / img.height)
                val sw = (a.width / k).toInt().coerceIn(1, img.width)
                val sh = (a.height / k).toInt().coerceIn(1, img.height)
                drawImage(
                    img,
                    srcOffset = androidx.compose.ui.unit.IntOffset((img.width - sw) / 2, (img.height - sh) / 2),
                    srcSize = androidx.compose.ui.unit.IntSize(sw, sh),
                    dstOffset = androidx.compose.ui.unit.IntOffset(a.left.toInt(), a.top.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(a.width.toInt(), a.height.toInt()),
                )
            }
        }
        drawIcons(ink.copy(alpha = 0.92f))
        layouts.forEach { (l, p) -> drawText(l, topLeft = p) }
        panelTexts.forEach { (l, p) -> drawText(l, topLeft = p) }
    }

    /** Finds every upward-facing edge of the text by drawing it (at half size) and scanning the alpha. */
    private fun DrawScope.computeTops() {
        val sw = (w / 2).toInt().coerceAtLeast(1)
        val sh = (h / 2).toInt().coerceAtLeast(1)
        val img = ImageBitmap(sw, sh)
        CanvasDrawScope().draw(Density(density, fontScale), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(img), Size(sw.toFloat(), sh.toFloat())) {
            scale(0.5f, pivot = Offset.Zero) {
                layouts.forEach { (l, p) -> drawText(l, topLeft = p, shadow = Shadow.None) }
                // The glass shapes are solid here, so only their top edges count as surfaces.
                panels.forEach { drawRoundRect(Color.White, it.topLeft, it.size, CornerRadius(CardCorner.toPx())) }
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
        val img = plate ?: return
        drawImage(img, topLeft = Offset.Zero)

        if (effects && mood == SceneMood.SUN) {
            // A warm glint sweeping slowly across the text (the same picture, tinted, through a slit).
            val band = 50 * u
            val gx = (sec * 180 * u) % (w + 600 * u) - 300 * u
            clipRect(gx - band, 0f, gx + band, h) {
                drawImage(img, topLeft = Offset.Zero, alpha = 0.6f, colorFilter = ColorFilter.tint(Color(0xFFFFE2A8), BlendMode.SrcIn))
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
