package dev.hew7.launcher.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.Text
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Which ink reads best over the wallpaper: dark ink where the picture behind is bright, light ink
 * where it's dark. Measured separately for the clock corner (top left) and the buttons (top right),
 * since one photo can be bright on one side and dark on the other.
 */
@Immutable
data class WallpaperInk(val leftDark: Boolean, val rightDark: Boolean) {

    /** The theme's colours with text and button glass swapped to this ink. */
    fun colors(theme: LauncherColors, left: Boolean): LauncherColors {
        val dark = if (left) leftDark else rightDark
        return theme.copy(
            text = if (dark) DarkInk else LightInk,
            subtext = (if (dark) DarkInk else LightInk).copy(alpha = 0.85f),
            glass = if (dark) Color(0x99FFFFFF) else Color(0x66000000),
        )
    }

    companion object {
        private val DarkInk = Color(0xFF15161A)
        private val LightInk = Color(0xFFF5F5F7)

        fun forTheme(dark: Boolean) = WallpaperInk(leftDark = !dark, rightDark = !dark)

        /** Reads a small copy of the wallpaper (from Coil's cache) and measures both corners. */
        suspend fun measure(context: Context, url: String?): WallpaperInk? {
            if (url == null) return null
            val bmp = withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context).data(url).size(Size(192, 108)).allowHardware(false).build()
                (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
            } ?: return null
            return withContext(Dispatchers.Default) {
                WallpaperInk(
                    // The clock, date and greeting.
                    leftDark = luminance(bmp, 0.02f, 0.03f, 0.45f, 0.28f) > BRIGHT,
                    // Inputs, Machine Room and Settings.
                    rightDark = luminance(bmp, 0.74f, 0.02f, 0.98f, 0.14f) > BRIGHT,
                )
            }
        }

        private const val BRIGHT = 0.58f

        /**
         * Average brightness (0–1) of a part of the screen, given as fractions of the screen. The
         * wallpaper is centre-cropped to 16:9 on screen, so the same crop is applied here.
         */
        private fun luminance(bmp: Bitmap, x0: Float, y0: Float, x1: Float, y1: Float): Float {
            val w = bmp.width.toFloat()
            val h = bmp.height.toFloat()
            val screen = 16f / 9f
            val visW = if (w / h > screen) h * screen else w
            val visH = if (w / h > screen) h else w / screen
            val offX = (w - visW) / 2f
            val offY = (h - visH) / 2f
            val left = (offX + x0 * visW).toInt().coerceIn(0, bmp.width - 1)
            val right = (offX + x1 * visW).toInt().coerceIn(left + 1, bmp.width)
            val top = (offY + y0 * visH).toInt().coerceIn(0, bmp.height - 1)
            val bottom = (offY + y1 * visH).toInt().coerceIn(top + 1, bmp.height)
            var sum = 0.0
            var n = 0
            for (y in top until bottom) {
                for (x in left until right) {
                    val p = bmp.getPixel(x, y)
                    val r = (p shr 16 and 0xFF) / 255.0
                    val g = (p shr 8 and 0xFF) / 255.0
                    val b = (p and 0xFF) / 255.0
                    sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
                    n++
                }
            }
            return if (n == 0) 0.5f else (sum / n).toFloat()
        }
    }
}

/**
 * Text that stays readable on any picture: a halo of the opposite ink is drawn around the letters
 * themselves (a soft-edged outline), so nothing is boxed in behind it. Blurs stay tiny: on TV
 * GPUs a wide text glow is cut off at the text's bounds and shows up as a box.
 */
@Composable
fun InkText(
    text: String,
    color: Color,
    darkInk: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
    val halo = if (darkInk) Color.White else Color.Black
    val strokePx = with(LocalDensity.current) {
        val size = if (fontSize == TextUnit.Unspecified) LocalTextStyle.current.fontSize else fontSize
        (size.toPx() * 0.12f).coerceIn(2f, 6f)
    }
    val base = LocalTextStyle.current.merge(TextStyle(fontSize = fontSize, fontWeight = fontWeight, lineHeight = lineHeight))
    Box(modifier) {
        // Halo: a wide, faint outline under a narrower, stronger one gives a soft edge without blur.
        Text(
            text,
            maxLines = 1,
            style = base.copy(
                color = halo.copy(alpha = 0.22f),
                drawStyle = Stroke(width = strokePx * 1.9f, join = StrokeJoin.Round),
                shadow = null,
            ),
        )
        Text(
            text,
            maxLines = 1,
            style = base.copy(
                color = halo.copy(alpha = if (darkInk) 0.6f else 0.5f),
                drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round),
                shadow = null,
            ),
        )
        Text(
            text,
            maxLines = 1,
            style = base.copy(color = color, shadow = Shadow(halo.copy(alpha = 0.4f), Offset(0f, 1f), blurRadius = 2f)),
        )
    }
}
