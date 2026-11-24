package dev.hew7.launcher.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import dev.hew7.launcher.CloseHelperService
import kotlinx.coroutines.delay

// Everything the screens use to reach Hew7 Helper (CloseHelperService): opening the TV's settings
// pages through it, and showing whether it is running, with a way to turn it on.

private val DialogShape = RoundedCornerShape(22.dp)

// ---------- the TV's settings pages ----------

/** Starts the first intent that opens; falls back to the TV's main settings. */
internal fun Context.openSetting(vararg intents: Intent) = startFirst(*intents, Intent(Settings.ACTION_SETTINGS))

internal fun component(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))

/**
 * Opens a page of the TV's settings that has no direct link, by its row label in the main list.
 * The Hew7 Helper selects the row out of sight; without it, the main settings list opens.
 */
internal fun Context.openSettingsEntry(vararg labels: String, at: CloseHelperService.Anchor? = null) {
    val helper = CloseHelperService.instance
    if (helper != null && CloseHelperService.isEnabled(this)) {
        helper.openSettingsEntry(labels.toList(), at)
    } else {
        explainHelper()
        openSetting(component("com.android.tv.settings", "com.android.tv.settings.MainSettings"))
    }
}

internal fun Context.openAccessibilitySettings() = openSetting(
    component("com.android.tv.settings", "com.android.tv.settings.oemlink.AccessibilitySettingsActivity"),
    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
)

// ---------- Hew7 Helper status, shown wherever a feature needs it ----------

/** Running; switched on but stopped (Android stops it whenever Hew7 is updated); or switched off. */
internal enum class HelperState { RUNNING, STOPPED, OFF }

internal fun helperState(context: Context) = when {
    !CloseHelperService.isEnabled(context) -> HelperState.OFF
    CloseHelperService.instance == null -> HelperState.STOPPED
    else -> HelperState.RUNNING
}

/** Checked again whenever the panel comes back into focus, e.g. after a visit to Accessibility. */
@Composable
internal fun rememberHelperState(): HelperState {
    val context = LocalContext.current
    val focused = LocalWindowInfo.current.isWindowFocused
    return remember(focused) { helperState(context) }
}

/** For a tap on a feature that needs the helper while it isn't running. */
internal fun Context.explainHelper() {
    val text = when (helperState(this)) {
        HelperState.STOPPED -> "Hew7 Helper has stopped. Choose Restart Hew7 Helper on this page, then switch it off and on."
        else -> "This needs Hew7 Helper. Choose Turn On Hew7 Helper on this page."
    }
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

/**
 * When the helper isn't running: a button to fix it and step-by-step instructions, naming the
 * features on this page that need it. Shows nothing while the helper is running.
 */
@Composable
internal fun HelperNotice(needs: String) {
    val context = LocalContext.current
    val c = LocalLauncherColors.current
    val state = rememberHelperState()
    if (state == HelperState.RUNNING) return
    var consent by remember { mutableStateOf(false) }
    MenuItem(
        Icons.Rounded.Accessibility,
        if (state == HelperState.STOPPED) "Restart Hew7 Helper" else "Turn On Hew7 Helper",
        // Switching it back on after an update needs no new consent; the first time does.
        { if (state == HelperState.STOPPED) context.openAccessibilitySettings() else consent = true },
    )
    Text(
        if (state == HelperState.STOPPED) {
            "Hew7 Helper has stopped (this happens whenever Hew7 is updated). $needs need it.\n" +
                "To fix: choose Restart Hew7 Helper above, then in Accessibility select Hew7 Helper and switch it Off, then On again."
        } else {
            "$needs need Hew7 Helper, which is off.\n" +
                "To turn it on: choose Turn On Hew7 Helper above and Agree, then in Accessibility select Hew7 Helper and switch it On."
        },
        color = c.subtext,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 10.dp),
    )
    if (consent) HelperConsentDialog(onAgree = { consent = false; context.openAccessibilitySettings() }, onDecline = { consent = false })
}

/**
 * Says plainly what Hew7 Helper does before sending you to switch it on (Google Play asks apps
 * that use the Accessibility API for this). Nothing changes unless you choose Agree.
 */
@Composable
internal fun HelperConsentDialog(onAgree: () -> Unit, onDecline: () -> Unit) {
    val c = LocalLauncherColors.current
    val agree = remember { FocusRequester() }
    Dialog(onDismissRequest = onDecline, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) {
            delay(150)
            runCatching { agree.requestFocus() }
        }
        Column(
            Modifier
                .width(520.dp)
                .background(c.glassStrong, DialogShape)
                .padding(24.dp),
        ) {
            ProvideTextStyle(LocalTextStyle.current.copy(shadow = null)) {
                Text("Turn On Hew7 Helper?", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Hew7 Helper uses Android's Accessibility service so Hew7 can do things only the TV's own system can:",
                    color = c.text,
                    fontSize = 13.sp,
                )
                listOf(
                    "Close apps from Running Apps, by pressing Force Stop for you",
                    "Open Display and Sound and Accounts directly",
                    "Restart or shut down the TV from Quick Settings",
                    "Make your remote's Settings, Input, Menu and app buttons work with Hew7",
                ).forEach {
                    // Bullet in its own column, so a wrapped line lines up with the text above it.
                    Row(Modifier.padding(start = 6.dp, top = 4.dp)) {
                        Text("•", color = c.text, fontSize = 13.sp, modifier = Modifier.width(16.dp))
                        Text(it, color = c.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "It acts only when you ask. It reads only the TV's settings screens and power menu while doing these jobs, " +
                        "and which remote button you press. It doesn't read other apps' screens, and nothing is collected, stored or sent anywhere.",
                    color = c.subtext,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuItem(Icons.Rounded.Check, "Agree", onAgree, modifier = Modifier.weight(1f).focusRequester(agree))
                    MenuItem(Icons.Rounded.Close, "Not Now", onDecline, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
