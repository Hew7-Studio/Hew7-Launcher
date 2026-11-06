package dev.hew7.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.hew7.launcher.data.AppEntry
import dev.hew7.launcher.data.MediaInfo
import kotlin.math.roundToInt

/** What Up next shows for one app: its Now playing, or when it was last used. */
@Immutable
data class SpotlightItem(
    val pkg: String,
    val kicker: String,
    val title: String,
    val subtitle: String,
    /** 0–1, or null without progress. */
    val progress: Float?,
    /** The white pill: what OK on the app does. */
    val action: String,
    /** The glass pill beside it, or null. */
    val second: String?,
    val accent: Color,
    val live: Boolean,
)

/** "just now", "5 min ago", "2 h ago", "yesterday", "3 days ago". */
fun ago(time: Long, now: Long = System.currentTimeMillis()): String {
    val min = ((now - time) / 60_000).coerceAtLeast(0)
    return when {
        min < 2 -> "just now"
        min < 60 -> "$min min ago"
        min < 24 * 60 -> "${min / 60} h ago"
        min < 48 * 60 -> "yesterday"
        else -> "${min / (24 * 60)} days ago"
    }
}

private fun left(ms: Long): String {
    val min = (ms / 60_000.0).roundToInt()
    return if (min >= 60) "${min / 60} h ${min % 60} min left" else "$min min left"
}

/**
 * Builds Up next for an app: what it's playing (live media sessions first, then its media
 * notification), else what it last played, else when it was last opened.
 */
fun spotlightFor(app: AppEntry, media: MediaInfo?, lastUsed: Long?, now: Long): SpotlightItem {
    val accent = Color(app.accent)
    if (media != null && media.title.isNotBlank()) {
        val dur = media.durationMs
        val pos = media.positionNow(now)
        val kicker = when {
            media.live && media.playing -> "Playing on ${app.label}"
            media.live -> "Up next on ${app.label}"
            else -> "Last played on ${app.label} · ${ago(media.updatedAt, now)}"
        }
        val sub = listOf(media.subtitle, if (dur > 0 && pos in 1 until dur) left(dur - pos) else "")
            .filter { it.isNotBlank() }.joinToString(" · ")
        return SpotlightItem(
            pkg = app.pkg,
            kicker = kicker,
            title = media.title,
            subtitle = sub,
            progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else null,
            action = if (media.playing) "Back to it" else "Resume",
            second = "Open app",
            accent = accent,
            live = media.live,
        )
    }
    return SpotlightItem(
        pkg = app.pkg,
        kicker = lastUsed?.let { "Last opened ${ago(it, now)}" } ?: "Your apps",
        title = app.label,
        subtitle = "",
        progress = null,
        action = "Open ${app.label}",
        second = null,
        accent = accent,
        live = false,
    )
}

/**
 * Up next, as in the Hew7 2.0 design: a coloured dot and a small kicker, a large title, the
 * details, a progress bar, and the pills saying what OK does. It changes with a soft slide as
 * you move along the dock. Text uses the wallpaper ink so it reads on any picture.
 */
@Composable
fun Spotlight(item: SpotlightItem?, ink: WallpaperInk, modifier: Modifier = Modifier) {
    val theme = LocalLauncherColors.current
    val c = ink.colors(theme, left = true)
    val darkInk = ink.leftDark
    AnimatedContent(
        targetState = item,
        contentKey = { it?.pkg to it?.title },
        transitionSpec = {
            (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 260f)) { it / 6 } + fadeIn(tween(350))) togetherWith
                fadeOut(tween(200))
        },
        label = "spotlight",
        modifier = modifier,
    ) { s ->
        Column(Modifier.widthIn(max = 470.dp).height(SpotlightHeight), verticalArrangement = Arrangement.Bottom) {
            if (s == null) return@Column
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(s.accent.brighter()))
                InkText(
                    s.kicker.uppercase(), c.subtext, darkInk,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp,
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
            InkText(
                s.title, c.text, darkInk,
                fontSize = 33.sp, fontWeight = FontWeight.Medium, lineHeight = 36.sp, fontFamily = DisplayFont,
                modifier = Modifier.padding(top = 9.dp),
            )
            if (s.subtitle.isNotBlank()) {
                InkText(s.subtitle, c.subtext, darkInk, fontSize = 13.5.sp, modifier = Modifier.padding(top = 6.dp))
            }
            s.progress?.let { p ->
                Box(Modifier.padding(top = 15.dp).width(240.dp).height(3.dp).clip(CircleShape).background(c.text.copy(alpha = 0.25f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(p).background(c.text))
                }
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.height(36.dp).background(if (darkInk) Color(0xFF13253A) else Color.White, RoundedCornerShape(50)).padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(s.action, color = if (darkInk) Color.White else Color(0xFF0B1624), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                s.second?.let { label ->
                    Box(
                        Modifier
                            .height(36.dp)
                            .background(c.text.copy(alpha = 0.08f), RoundedCornerShape(50))
                            .border(1.dp, c.text.copy(alpha = 0.3f), RoundedCornerShape(50))
                            .padding(horizontal = 17.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
    }
}

val SpotlightHeight = 150.dp

/** A little lighter and more vivid, for the dot and the glow. */
fun Color.brighter(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(android.graphics.Color.argb(255, (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()), hsv)
    hsv[1] = (hsv[1] * 1.1f).coerceAtMost(1f)
    hsv[2] = (hsv[2] * 1.25f).coerceIn(0.55f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
