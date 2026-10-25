package dev.hew7.launcher.ui

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.math.abs

private val Bands = listOf(
    Color(0xFFFF4D4D), Color(0xFFFF9F43), Color(0xFFFECA57), Color(0xFF1DD1A1),
    Color(0xFF48DBFB), Color(0xFF54A0FF), Color(0xFFA55EEA),
)

/** Decides whether the Hew7 intro should play: once per TV start-up. */
object BootIntro {
    /** When this boot started, to the nearest minute; changes only when the TV restarts. */
    private fun bootMinute() = (System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 60_000

    fun shouldShow(context: Context): Boolean {
        val last = context.getSharedPreferences("hew7", Context.MODE_PRIVATE).getLong("intro_boot", -1)
        return (abs(bootMinute() - last) > 1).also { Log.i("Hew7", "boot intro: ${if (it) "playing" else "already shown this boot"}") }
    }

    /**
     * Recorded only once the intro has finished: TVs like TCL's may kill and restart the home
     * screen during boot, and the intro should still play on the restart that survives.
     */
    fun markShown(context: Context) {
        Log.i("Hew7", "boot intro: finished")
        context.getSharedPreferences("hew7", Context.MODE_PRIVATE).edit().putLong("intro_boot", bootMinute()).apply()
    }
}

/**
 * About 2.5 s after a restart: the seven bands rise one by one over the horizon line, "Hew7"
 * fades in, then the whole thing fades into the home screen.
 */
@Composable
fun BootIntroOverlay(font: FontFamily, onDone: () -> Unit) {
    val bands = remember { Bands.map { Animatable(0f) } }
    val horizon = remember { Animatable(0f) }
    val words = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            async { horizon.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
            bands.forEachIndexed { i, a ->
                async {
                    delay(150L + i * 110L)
                    a.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
                }
            }
            async {
                delay(900)
                words.animateTo(1f, tween(600, easing = LinearEasing))
            }
        }
        delay(700)
        fade.animateTo(0f, tween(500))
        onDone()
    }

    Box(
        Modifier.fillMaxSize().alpha(fade.value).background(Color(0xFF0B0C10)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(width = 150.dp, height = 84.dp)) {
                val stroke = 7.dp.toPx()
                val cx = size.width / 2
                val base = size.height - 10.dp.toPx()
                val r0 = size.width / 2 - stroke
                val step = (r0 - 14.dp.toPx()) / 6
                bands.forEachIndexed { i, a ->
                    val r = r0 - i * step
                    // Each band sweeps up from the horizon, left to right.
                    drawArc(
                        color = Bands[i],
                        startAngle = 180f,
                        sweepAngle = 180f * a.value,
                        useCenter = false,
                        topLeft = Offset(cx - r, base - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                val barW = size.width * horizon.value
                drawRoundRect(
                    Color.White,
                    topLeft = Offset(cx - barW / 2, base + 4.dp.toPx()),
                    size = Size(barW, 5.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
            Text(
                "Hew7",
                color = Color.White,
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                modifier = Modifier.padding(top = 18.dp).alpha(words.value),
            )
        }
    }
}
