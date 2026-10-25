package dev.hew7.launcher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/** Colors for the "glass over wallpaper" look, separate from the Material scheme. */
@Immutable
data class LauncherColors(
    val text: Color,
    val subtext: Color,
    val glass: Color,
    val glassStrong: Color,
    val scrim: Color,
    val focusFill: Color,
    val onFocusFill: Color,
    val dark: Boolean,
)

val LocalLauncherColors = staticCompositionLocalOf {
    LauncherColors(Color.White, Color.White, Color.Black, Color.Black, Color.Black, Color.White, Color.Black, true)
}

@Composable
fun LauncherTheme(dark: Boolean, font: LauncherFont, content: @Composable () -> Unit) {
    val anim = tween<Color>(1200)
    val text by animateColorAsState(if (dark) Color(0xFFF5F5F7) else Color(0xFF15161A), anim, label = "text")
    val subtext by animateColorAsState(if (dark) Color(0xCCF5F5F7) else Color(0xCC15161A), anim, label = "subtext")
    val glass by animateColorAsState(if (dark) Color(0x59000000) else Color(0x8CFFFFFF), anim, label = "glass")
    val glassStrong by animateColorAsState(if (dark) Color(0xE61C1D22) else Color(0xF2F6F7FA), anim, label = "glassStrong")
    val scrim by animateColorAsState(if (dark) Color(0xB3000000) else Color(0x80FFFFFF), anim, label = "scrim")

    val colors = LauncherColors(
        text = text,
        subtext = subtext,
        glass = glass,
        glassStrong = glassStrong,
        scrim = scrim,
        focusFill = if (dark) Color(0xFFF5F5F7) else Color(0xFF15161A),
        onFocusFill = if (dark) Color(0xFF15161A) else Color(0xFFF5F5F7),
        dark = dark,
    )
    val scheme = if (dark) {
        darkColorScheme(primary = Color(0xFF8AB4F8), surface = Color(0xFF1C1D22), onSurface = Color(0xFFF5F5F7))
    } else {
        lightColorScheme(primary = Color(0xFF1A73E8), surface = Color(0xFFF6F7FA), onSurface = Color(0xFF15161A))
    }
    MaterialTheme(colorScheme = scheme) {
        // A soft drop shadow lifts the text off the wallpaper so it reads as sitting on top of the
        // screen, in both themes (lighter and cooler in light mode so dark text doesn't look smudged).
        val shadow = if (dark) {
            Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 4f), blurRadius = 18f)
        } else {
            Shadow(Color(0xFF142032).copy(alpha = 0.32f), Offset(0f, 4f), blurRadius = 16f)
        }
        ProvideTextStyle(LocalTextStyle.current.copy(fontFamily = font.family, shadow = shadow)) {
            CompositionLocalProvider(LocalLauncherColors provides colors, content = content)
        }
    }
}
