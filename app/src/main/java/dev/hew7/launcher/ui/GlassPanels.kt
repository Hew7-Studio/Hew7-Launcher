package dev.hew7.launcher.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.hew7.launcher.CloseHelperService
import dev.hew7.launcher.HomeViewModel
import dev.hew7.launcher.R
import dev.hew7.launcher.RemoteKeys
import dev.hew7.launcher.data.DeviceInfo
import dev.hew7.launcher.data.GreetingStyle
import dev.hew7.launcher.data.Prefs
import dev.hew7.launcher.data.RemoteAction
import dev.hew7.launcher.data.RemoteButtons
import dev.hew7.launcher.data.RemoteChoice
import dev.hew7.launcher.data.RunningApp
import dev.hew7.launcher.data.RunningApps
import dev.hew7.launcher.data.SystemSnapshot
import dev.hew7.launcher.data.SystemStats
import dev.hew7.launcher.data.TempUnit
import dev.hew7.launcher.data.ThemeMode
import dev.hew7.launcher.data.TileStyle
import dev.hew7.launcher.data.WallpaperSource
import dev.hew7.launcher.dream.ScreensaverActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

// Hew7 2.0's Settings and Machine Room: a compact glass panel in the top-right corner, in the
// weather pill's smoky glass, with a soft glow in the colour of what's selected.

private val GlassShape = RoundedCornerShape(18.dp)

/** Where a button is on the screen, so Hew7 Helper can show its progress right on it. */
private class ScreenRect {
    var rect: android.graphics.Rect? = null

    fun anchor(round: Boolean, fill: Color, ink: Color, stroke: Color = Color.Transparent, label: Boolean = false) =
        rect?.let { CloseHelperService.Anchor(it, round, fill.toArgb(), ink.toArgb(), if (stroke == Color.Transparent) 0 else stroke.toArgb(), label) }
}

@Composable
private fun Modifier.onScreen(holder: ScreenRect): Modifier {
    val view = LocalView.current
    return onGloballyPositioned { c ->
        val b = c.boundsInWindow()
        val at = IntArray(2).also { view.getLocationOnScreen(it) }
        holder.rect = android.graphics.Rect(
            at[0] + b.left.roundToInt(), at[1] + b.top.roundToInt(),
            at[0] + b.right.roundToInt(), at[1] + b.bottom.roundToInt(),
        )
    }
}
private val RowShape = RoundedCornerShape(11.dp)

/**
 * The glass panel: opens out sideways from the weather pill's corner, lives in its own dialog
 * window (so focus can't wander behind it), and passes remote buttons on to Hew7 as before.
 */
@Composable
private fun GlassPanel(
    title: String,
    icon: ImageVector,
    alpha: Float,
    accent: Color,
    width: Dp,
    first: FocusRequester,
    onBack: () -> Unit,
    detail: String? = null,
    focusKey: Any = Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = panelColors(LocalLauncherColors.current)
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        (LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(0.15f)
        val windowFocused = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(focusKey, windowFocused) {
            if (!windowFocused) return@LaunchedEffect
            delay(120)
            runCatching { first.requestFocus() }
        }
        Box(
            Modifier.fillMaxSize().onPreviewKeyEvent { e -> RemoteKeys.onKey(e.nativeKeyEvent) },
            contentAlignment = Alignment.TopEnd,
        ) {
            AnimatedVisibility(
                visibleState = visible,
                enter = expandHorizontally(spring(dampingRatio = 0.85f, stiffness = 240f), expandFrom = Alignment.End) + fadeIn(tween(250)),
            ) {
                val glow by animateColorAsState(accent, tween(500), label = "glow")
                CompositionLocalProvider(LocalLauncherColors provides c) {
                    Column(
                        Modifier
                            .padding(top = 24.dp, end = 48.dp)
                            .width(width)
                            .clip(GlassShape)
                            .background(Color(0xFF0A0E18).copy(alpha = (0.35f + 0.4f * alpha).coerceIn(0.5f, 0.8f)))
                            .drawBehind {
                                // A soft glow in the selected colour from the top-left corner, and a
                                // fine highlight along the top edge, so it reads as glass.
                                drawRect(Brush.radialGradient(listOf(glow.copy(alpha = 0.3f), Color.Transparent), center = Offset.Zero, radius = size.width * 0.7f))
                                val inset = 20.dp.toPx()
                                drawLine(
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent), inset, size.width - inset),
                                    Offset(inset, 0.5f), Offset(size.width - inset, 0.5f), 1.dp.toPx(),
                                )
                            }
                            .border(1.dp, Color.White.copy(alpha = 0.22f), GlassShape)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        ProvideTextStyle(LocalTextStyle.current.copy(shadow = null, color = c.text)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(30.dp)) {
                                Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(18.dp))
                                Text(title, color = c.text, fontFamily = DisplayFont, fontWeight = FontWeight.Light, fontSize = 21.sp, modifier = Modifier.padding(start = 8.dp))
                                Spacer(Modifier.weight(1f))
                                if (detail != null) {
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(glow))
                                    Text(detail, color = c.subtext, fontSize = 10.sp, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                            Box(Modifier.padding(top = 8.dp, bottom = 9.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.2f)))
                            content()
                        }
                    }
                }
            }
        }
    }
}

// ---------- Settings ----------

/** One setting in a category. */
private sealed interface SRow {
    val key: String
    val icon: ImageVector
    val name: String
}

/** Left and Right step through [options] one by one; each applies as you land on it. */
private class StepRow(
    override val key: String,
    override val icon: ImageVector,
    override val name: String,
    val options: List<String>,
    val index: Int,
    val onPick: (Int) -> Unit,
    /** Each option shown in its own typeface (Font). */
    val faces: List<FontFamily>? = null,
    /** Power: stepping only chooses; OK does it. */
    val confirm: ((Int) -> Unit)? = null,
    /** Icons for [confirm] rows' buttons. */
    val buttonIcons: List<ImageVector>? = null,
    /** Where each button is on screen (for the progress). */
    val buttonRects: List<ScreenRect>? = null,
) : SRow

/** Left and Right move it in 5% steps. */
private class SlideRow(
    override val key: String,
    override val icon: ImageVector,
    override val name: String,
    val value: Float,
    val range: ClosedFloatingPointRange<Float>,
    val lo: String,
    val hi: String,
    val onChange: (Float) -> Unit,
) : SRow

/** OK opens something (a TV settings page, the screensaver, a note). */
private class LinkRow(
    override val key: String,
    override val icon: ImageVector,
    override val name: String,
    val value: String,
    /** Where its pill is on screen, for links Hew7 Helper opens (progress shows on the pill). */
    val rect: ScreenRect? = null,
    val onOpen: () -> Unit,
) : SRow

private class Category(val label: String, val blurb: String, val icon: ImageVector, val color: Color, val tint: Color, val rows: List<SRow>)

/**
 * Settings: categories down the left, each in its own colour; the selected category's settings on
 * the right, each stepped through with Left and Right. Back goes from a setting to its category,
 * and from the categories closes the panel.
 */
@Composable
fun GlassSettingsPanel(vm: HomeViewModel, alpha: Float, startOnQuick: Boolean = false, onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val apps by vm.apps.collectAsStateWithLifecycle()
    val theme by vm.themeMode.collectAsStateWithLifecycle()
    val unit by vm.tempUnit.collectAsStateWithLifecycle()
    val source by vm.wallpaperSource.collectAsStateWithLifecycle()
    val minutes by vm.wallpaperMinutes.collectAsStateWithLifecycle()
    val tiles by vm.tileStyle.collectAsStateWithLifecycle()
    val font by vm.font.collectAsStateWithLifecycle()
    val greeting by vm.greetingStyle.collectAsStateWithLifecycle()
    val panelAlpha by vm.panelAlpha.collectAsStateWithLifecycle()
    val dockAlpha by vm.dockAlpha.collectAsStateWithLifecycle()
    val showNames by vm.showAppNames.collectAsStateWithLifecycle()
    val weatherFx by vm.weatherEffects.collectAsStateWithLifecycle()
    val saverDelay by vm.screensaverDelay.collectAsStateWithLifecycle()
    var calmMinutes by remember { mutableStateOf(prefs.screensaverCalmMinutes) }
    var sleepMinutes by remember { mutableStateOf(prefs.screensaverSleepMinutes) }
    var remoteVersion by remember { mutableIntStateOf(0) }
    var listening by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val helper = rememberHelperState()
    val network = remember { DeviceInfo.network(context).name }
    val versionName = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }

    DisposableEffect(listening) {
        if (listening) {
            RemoteKeys.learner = { _ ->
                listening = false
                remoteVersion++
            }
        }
        onDispose { RemoteKeys.learner = null }
    }

    val powerRects = remember { List(3) { ScreenRect() } }
    val displayRect = remember { ScreenRect() }
    val accountsRect = remember { ScreenRect() }
    fun linkAnchor(r: ScreenRect) = r.anchor(round = false, fill = Color(0xFF2A303C), ink = Color.White, stroke = Color.White.copy(alpha = 0.5f))
    fun power(i: Int) {
        val h = CloseHelperService.instance.takeIf { helper == HelperState.RUNNING }
        // Restart and Shut down show their progress on the button itself.
        val at = powerRects[i].anchor(round = false, fill = Color(0xFFF5B14A), ink = Color.White, stroke = Color.White.copy(alpha = 0.9f))
        when (i) {
            0 -> { onClose(); if (!CloseHelperService.sleepTv()) context.explainHelper() }
            1 -> if (h != null) h.restart(at) else context.explainHelper()
            else -> if (h != null) h.shutDown(at) else context.explainHelper()
        }
    }

    // The remote's buttons and every job each can have: Hew7's actions, then opening any app.
    remoteVersion
    val jobs = remember(apps) {
        RemoteAction.entries.filter { it != RemoteAction.OPEN_APP }.map { RemoteChoice(it) } +
            apps.map { RemoteChoice(RemoteAction.OPEN_APP, it.pkg) }
    }
    fun jobLabel(name: String, ch: RemoteChoice) = when (ch.action) {
        RemoteAction.DEFAULT -> RemoteButtons.defaultFor(name).let { d -> if (d == RemoteAction.DEFAULT) "Default (TV)" else "Default (${d.short})" }
        RemoteAction.OPEN_APP -> "Open " + (apps.firstOrNull { it.pkg == ch.pkg }?.label ?: "app")
        else -> ch.action.label
    }
    val seen = prefs.remoteSeen.map { n -> if (n.all { it.isDigit() }) RemoteButtons.name(n.toInt()) else n }.distinct()

    val cats = listOf(
        Category(
            "Appearance", "How Hew7 looks", Icons.Rounded.AutoAwesome, Color(0xFFC58AF9), Color(0xFF7B4BC4),
            listOf(
                StepRow("theme", Icons.Rounded.Palette, "Theme", ThemeMode.entries.map { it.label }, ThemeMode.entries.indexOf(theme), { vm.setThemeMode(ThemeMode.entries[it]) }),
                StepRow("font", Icons.Rounded.TextFields, "Font", LauncherFont.entries.map { it.label }, LauncherFont.entries.indexOf(font), { vm.setFont(LauncherFont.entries[it]) }, faces = LauncherFont.entries.map { it.family }),
                StepRow("greet", Icons.Rounded.EmojiEmotions, "Greetings", GreetingStyle.entries.map { it.label }, GreetingStyle.entries.indexOf(greeting), { vm.setGreetingStyle(GreetingStyle.entries[it]) }),
                StepRow("temp", Icons.Rounded.Thermostat, "Temperature", listOf("Celsius (°C)", "Fahrenheit (°F)"), TempUnit.entries.indexOf(unit), { vm.setTempUnit(TempUnit.entries[it]) }),
                StepRow("wall", Icons.Rounded.Image, "Wallpapers", WallpaperSource.entries.map { it.label }, WallpaperSource.entries.indexOf(source), { vm.setWallpaperSource(WallpaperSource.entries[it]) }),
                StepRow("every", Icons.Rounded.Schedule, "Change every", WallpaperIntervals.map { minutesLabel(it) }, WallpaperIntervals.indexOf(minutes).coerceAtLeast(0), { vm.setWallpaperMinutes(WallpaperIntervals[it]) }),
                SlideRow("panels", Icons.Rounded.Contrast, "Panel glass", panelAlpha, PanelRange, "Clear", "Solid") { vm.setPanelAlpha(it) },
            ),
        ),
        Category(
            "Dock", "Your apps", Icons.Rounded.GridView, Color(0xFF8AB4F8), Color(0xFF2F6FCF),
            listOf(
                StepRow("tiles", Icons.Rounded.GridView, "App tiles", TileStyle.entries.map { it.label }, TileStyle.entries.indexOf(tiles), { vm.setTileStyle(TileStyle.entries[it]) }),
                StepRow("names", Icons.Rounded.Label, "App names", listOf("Off", "On"), if (showNames) 1 else 0, { vm.setShowAppNames(it == 1) }),
                SlideRow("dock", Icons.Rounded.Layers, "Dock glass", dockAlpha, DockRange, "None", "Solid") { vm.setDockAlpha(it) },
            ),
        ),
        Category(
            "Remote", "Settings, Input and Menu use Hew7 Helper", Icons.Rounded.SettingsRemote, Color(0xFFF28BB5), Color(0xFFB8487A),
            seen.map { name ->
                val current = RemoteChoice.decode(prefs.remoteButton(name)) ?: RemoteChoice(RemoteAction.DEFAULT)
                StepRow(
                    "b_$name", Icons.Rounded.RadioButtonChecked, RemoteButtons.label(name),
                    jobs.map { jobLabel(name, it) }, jobs.indexOf(current).coerceAtLeast(0),
                    { i ->
                        val ch = jobs[i]
                        prefs.setRemoteButton(name, if (ch.action == RemoteAction.DEFAULT) null else ch.encode())
                        remoteVersion++
                    },
                )
            } + LinkRow("add", Icons.Rounded.Add, "Add a button", if (listening) "Press a button…" else "Press one") { listening = !listening },
        ),
        Category(
            "Screensaver", "When the TV rests", Icons.Rounded.Bedtime, Color(0xFF7FD1AE), Color(0xFF2B8A6A),
            listOf(
                LinkRow("start", Icons.Rounded.PlayArrow, "Start now", "Preview") {
                    onClose()
                    context.startActivity(Intent(context, ScreensaverActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                StepRow("fx", Icons.Rounded.Cloud, "Weather effects", listOf("On", "Off"), if (weatherFx) 0 else 1, { vm.setWeatherEffects(it == 0) }),
                StepRow(
                    "after", Icons.Rounded.Timer, "Start after", ScreensaverDelays.map { delayLabel(it) },
                    ScreensaverDelays.indices.minByOrNull { kotlin.math.abs(ScreensaverDelays[it].toLong() - saverDelay) } ?: 0,
                    { if (!vm.setScreensaverDelay(ScreensaverDelays[it])) note = "Hew7 isn't allowed to change the screensaver timer yet." },
                ),
                StepRow("calm", Icons.Rounded.Spa, "Calm after", CalmAfter.map { if (it == 0) "Never" else minutesLabel(it) }, CalmAfter.indexOf(calmMinutes).coerceAtLeast(0), {
                    prefs.screensaverCalmMinutes = CalmAfter[it]; calmMinutes = CalmAfter[it]
                }),
                StepRow("sleep", Icons.Rounded.Bedtime, "Sleep after", SleepAfter.map { if (it == 0) "Never" else minutesLabel(it) }, SleepAfter.indexOf(sleepMinutes).coerceAtLeast(0), {
                    prefs.screensaverSleepMinutes = SleepAfter[it]; sleepMinutes = SleepAfter[it]
                }),
            ),
        ),
        Category(
            "Quick Settings", "The TV's own settings", Icons.Rounded.Tv, Color(0xFFF5B14A), Color(0xFFB9781C),
            listOf(
                LinkRow("display", Icons.Rounded.Tv, "Display & sound", "Open", displayRect) {
                    context.openSettingsEntry("Display and sound", "Display & sound", "Display and Sound", at = linkAnchor(displayRect))
                },
                LinkRow("net", Icons.Rounded.Wifi, "Network", network.ifBlank { "Open" }) { context.openSetting(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                LinkRow("bt", Icons.Rounded.Bluetooth, "Bluetooth & remotes", "Open") {
                    context.openSetting(component("com.android.tv.settings", "com.tcl.android.tv.settings.accessories.AccessoriesActivity"), Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                },
                LinkRow("acc", Icons.Rounded.Accessibility, "Accessibility", "Open") { context.openAccessibilitySettings() },
                LinkRow("apps", Icons.Rounded.Apps, "Apps", "Open") { context.openSetting(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)) },
                LinkRow("all", Icons.Rounded.Settings, "All settings", "Open") { context.openSetting() },
                LinkRow("acct", Icons.Rounded.Person, "Accounts", "Open", accountsRect) {
                    context.openSettingsEntry("Accounts and sign-in", "Accounts & sign-in", "Accounts and Sign-In", at = linkAnchor(accountsRect))
                },
                StepRow("power", Icons.Rounded.PowerSettingsNew, "Power", listOf("Sleep", "Restart", "Shut down"), 0, {}, confirm = { power(it) },
                    buttonIcons = listOf(Icons.Rounded.Bedtime, Icons.Rounded.RestartAlt, Icons.Rounded.PowerSettingsNew), buttonRects = powerRects),
            ),
        ),
        Category(
            "About", "Hew7 $versionName", Icons.Rounded.Info, Color(0xFF9AA4B2), Color(0xFF5B6576),
            listOf(
                LinkRow("ver", Icons.Rounded.Info, "Version", versionName) { note = context.getString(R.string.copyright) },
                LinkRow("priv", Icons.Rounded.Shield, "Privacy", "Read") {
                    note = "No account, ads or analytics; your settings stay on this TV. The weather uses your approximate location from your " +
                        "internet address (ipwho.is, ipapi.co) and Open-Meteo. Wallpapers come from Bing or Picsum. Now playing stays on the TV."
                },
                LinkRow("cred", Icons.Rounded.Favorite, "Credits", "Read") {
                    note = "Weather by Open-Meteo.com (CC BY 4.0). Fonts under the SIL Open Font License. Built with AndroidX, Jetpack Compose, Kotlin, Coil and OkHttp."
                },
            ),
        ),
    )

    var cat by remember { mutableIntStateOf(if (startOnQuick) 4 else 0) }
    val catFocus = remember { List(6) { FocusRequester() } }
    val firstRow = remember { FocusRequester() }
    var inRows by remember { mutableStateOf(false) }
    val current = cats[cat]

    GlassPanel(
        title = "Settings",
        icon = Icons.Rounded.Settings,
        alpha = alpha,
        accent = current.color,
        width = 610.dp,
        first = catFocus[cat],
        detail = current.label,
        onBack = {
            when {
                listening -> listening = false
                inRows -> runCatching { catFocus[cat].requestFocus() }
                else -> onClose()
            }
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Categories: moving onto one shows its settings; Right goes into them.
            Column(Modifier.width(128.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                cats.forEachIndexed { i, k ->
                    CategoryItem(
                        k, selected = i == cat,
                        modifier = Modifier
                            .focusRequester(catFocus[i])
                            .onFocusChanged { if (it.isFocused) { cat = i; note = null } }
                            .onPreviewKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) {
                                    runCatching { firstRow.requestFocus() }; true
                                } else false
                            },
                        onClick = { runCatching { firstRow.requestFocus() } },
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(max = 262.dp)
                    .onFocusChanged { inRows = it.hasFocus }
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)) {
                    Text(current.label, fontFamily = DisplayFont, fontWeight = FontWeight.Light, fontSize = 17.sp)
                    Text(current.blurb, color = LocalLauncherColors.current.subtext, fontSize = 10.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp))
                }
                if (current.label in setOf("Remote", "Quick Settings", "Screensaver")) {
                    HelperNotice(
                        when (current.label) {
                            "Remote" -> "The Settings, Input and Menu buttons, and app buttons inside other apps,"
                            "Quick Settings" -> "Display and Sound, Accounts and Power (Restart, Shut Down)"
                            else -> "Sleep After"
                        },
                    )
                }
                // Up and Down stay inside the category: stop at the first and last setting rather
                // than jumping to another category (Up still reaches the Hew7 Helper notice).
                val noticeShown = helper != HelperState.RUNNING && current.label in setOf("Remote", "Quick Settings", "Screensaver")
                val last = current.rows.lastIndex
                current.rows.forEachIndexed { i, r ->
                    val edges = Modifier.onPreviewKeyEvent { e ->
                        e.type == KeyEventType.KeyDown &&
                            ((i == last && e.key == Key.DirectionDown) || (i == 0 && !noticeShown && e.key == Key.DirectionUp))
                    }
                    SettingRow(r, current, edges.then(if (i == 0) Modifier.focusRequester(firstRow) else Modifier))
                }
                note?.let {
                    Text(
                        it, fontSize = 11.sp, color = LocalLauncherColors.current.text.copy(alpha = 0.88f),
                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth().clip(RowShape).background(Color.White.copy(alpha = 0.07f)).padding(10.dp),
                    )
                }
            }
        }
        Text(
            "▲ ▼ choose · ◀ ▶ step through options one by one · OK opens · Back to the categories",
            fontSize = 9.sp, color = LocalLauncherColors.current.subtext.copy(alpha = 0.75f), modifier = Modifier.padding(top = 9.dp),
        )
    }
}

@Composable
private fun CategoryItem(k: Category, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val fill by animateColorAsState(if (selected) k.color else Color.Transparent, tween(250), label = "ring")
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(34.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RowShape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, contentColor = Color.White, focusedContainerColor = Color.Transparent, focusedContentColor = Color.White),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .background(
                    if (selected) Brush.horizontalGradient(listOf(k.color.copy(alpha = 0.3f), k.color.copy(alpha = 0.08f))) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                    RowShape,
                )
                .border(1.5.dp, if (focused) Color.White.copy(alpha = 0.9f) else if (selected) k.color.copy(alpha = 0.6f) else Color.Transparent, RowShape)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(19.dp).clip(CircleShape).background(fill).border(2.5.dp, k.color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(k.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
            Text(k.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** One setting: icon tile, name, and its control (◀ value ▶, a slider, or an Open link). */
@Composable
private fun SettingRow(r: SRow, k: Category, modifier: Modifier) {
    val c = LocalLauncherColors.current
    var focused by remember { mutableStateOf(false) }
    // Power keeps its own choice until OK.
    var powerPick by remember { mutableIntStateOf(0) }
    val step = r as? StepRow
    val index = if (step?.confirm != null) powerPick else step?.index ?: 0
    Surface(
        onClick = {
            when (r) {
                is LinkRow -> r.onOpen()
                is StepRow -> r.confirm?.invoke(powerPick) ?: r.onPick((r.index + 1) % r.options.size)
                is SlideRow -> Unit
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val d = when (e.key) {
                    Key.DirectionLeft -> -1
                    Key.DirectionRight -> 1
                    else -> return@onPreviewKeyEvent false
                }
                when (r) {
                    is StepRow -> {
                        val n = r.options.size
                        if (r.confirm != null) {
                            // Buttons: no wrapping; Left on the first goes back to the categories.
                            val next = powerPick + d
                            if (next < 0) return@onPreviewKeyEvent false
                            powerPick = next.coerceAtMost(n - 1)
                        } else {
                            r.onPick((r.index + d + n) % n)
                        }
                        true
                    }
                    is SlideRow -> {
                        val span = r.range.endInclusive - r.range.start
                        val f = ((r.value - r.range.start) / span).coerceIn(0f, 1f)
                        val next = ((f + d * 0.05f) * 20).roundToInt().coerceIn(0, 20) / 20f
                        r.onChange(r.range.start + next * span)
                        true
                    }
                    // Left from a link goes back to the categories (normal focus); Right does nothing.
                    is LinkRow -> d > 0
                }
            },
        shape = ClickableSurfaceDefaults.shape(RowShape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, contentColor = c.text, focusedContainerColor = Color.Transparent, focusedContentColor = c.text),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .background(
                    if (focused) Brush.horizontalGradient(listOf(k.color.copy(alpha = 0.24f), Color.White.copy(alpha = 0.07f))) else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.035f), Color.White.copy(alpha = 0.035f))),
                    RowShape,
                )
                .border(1.5.dp, if (focused) Color.White.copy(alpha = 0.85f) else Color.Transparent, RowShape)
                .padding(start = 6.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(Brush.linearGradient(listOf(k.color, k.tint))),
                contentAlignment = Alignment.Center,
            ) { Icon(r.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp)) }
            Text(r.name, fontFamily = DisplayFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(start = 9.dp).weight(1f))
            when (r) {
                is StepRow -> if (r.confirm != null) {
                    ActionButtons(r.options, r.buttonIcons, r.buttonRects, index, focused, k.color)
                } else {
                    Stepper(r.options[index], index, r.options.size, r.faces?.getOrNull(index), focused, k.color)
                }
                is SlideRow -> Slider(r, focused, k.color)
                is LinkRow -> Row(
                    Modifier.then(r.rect?.let { Modifier.onScreen(it) } ?: Modifier).height(24.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape).background(Color.White.copy(alpha = 0.05f)).padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(r.value, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/** Separate buttons (Power): the highlighted one fills with the category colour; OK runs it. */
@Composable
private fun ActionButtons(labels: List<String>, icons: List<ImageVector>?, rects: List<ScreenRect>?, index: Int, focused: Boolean, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            val on = focused && i == index
            val fill by animateColorAsState(if (on) color else Color.White.copy(alpha = 0.06f), tween(180), label = "btn")
            Row(
                Modifier
                    .then(rects?.getOrNull(i)?.let { Modifier.onScreen(it) } ?: Modifier)
                    .height(24.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .border(1.dp, if (on) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icons?.getOrNull(i)?.let { Icon(it, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp).padding(end = 1.dp)) }
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

/** ◀ value ▶ in a small dark capsule, with dots for where you are among the options. */
@Composable
private fun Stepper(value: String, index: Int, count: Int, face: FontFamily?, focused: Boolean, color: Color) {
    val arrow = if (focused) color else Color.White.copy(alpha = 0.08f)
    Row(
        Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.2f)).border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape).padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(arrow), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Column(Modifier.width(150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = face, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (count <= 13) {
                Row(Modifier.padding(top = 2.dp).height(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(count) { j ->
                        Box(Modifier.width(if (j == index) 10.dp else 3.dp).fillMaxHeight().clip(CircleShape).background(Color.White.copy(alpha = if (j == index) 0.95f else 0.35f)))
                    }
                }
            } else {
                Text("${index + 1} / $count", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }
        Box(Modifier.size(20.dp).clip(CircleShape).background(arrow), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun Slider(r: SlideRow, focused: Boolean, color: Color) {
    val f = ((r.value - r.range.start) / (r.range.endInclusive - r.range.start)).coerceIn(0f, 1f)
    val shown by animateFloatAsState(f, tween(200), label = "slider")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("−", fontSize = 14.sp, color = if (focused) color else Color.White.copy(alpha = 0.5f))
        Column(Modifier.width(118.dp).padding(horizontal = 6.dp)) {
            Row {
                Text(r.lo, fontSize = 8.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.weight(1f))
                Text("${(f * 100).roundToInt()}%", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(r.hi, fontSize = 8.sp, color = Color.White.copy(alpha = 0.7f))
            }
            Box(Modifier.padding(top = 3.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(shown).background(color))
            }
        }
        Text("+", fontSize = 14.sp, color = if (focused) color else Color.White.copy(alpha = 0.5f))
    }
}

// ---------- Machine Room ----------

/**
 * Machine Room: four live gauges (CPU, Memory, Storage, Network) and the running apps right under
 * them. It opens on the first running app, so closing one is a single OK.
 */
@Composable
fun GlassMachineRoomPanel(vm: HomeViewModel, alpha: Float, onClose: () -> Unit) {
    val context = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val stats = remember { SystemStats(context) }
    var snap by remember { mutableStateOf<SystemSnapshot?>(null) }
    var running by remember { mutableStateOf<List<RunningApp>?>(null) }
    val helperOn = rememberHelperState() == HelperState.RUNNING
    val first = remember { FocusRequester() }
    val anchor = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        while (true) {
            snap = withContext(Dispatchers.IO) { stats.read() }
            delay(1_000)
        }
    }
    LaunchedEffect(apps) {
        while (true) {
            running = RunningApps.read(context, apps)
            delay(5_000)
        }
    }
    val closeAllRect = remember { ScreenRect() }
    fun close(pkgs: List<String>, at: CloseHelperService.Anchor?) {
        val helper = CloseHelperService.instance
        if (helperOn && helper != null) {
            vm.runningBeforeClose = running
            helper.close(pkgs, at)
        } else {
            context.explainHelper()
            if (pkgs.size == 1) context.startFirst(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${pkgs[0]}")))
        }
    }

    val s = snap
    val r = running
    GlassPanel(
        title = "Machine Room",
        icon = Icons.Rounded.Memory,
        alpha = alpha,
        accent = Color(0xFF8AB4F8),
        width = 680.dp,
        first = if (r.isNullOrEmpty()) anchor else first,
        focusKey = r?.isNotEmpty() == true,
        detail = s?.let { "${it.model} · Android ${it.android.removePrefix("Android ")} · up ${duration(it.uptimeMillis)}" },
        onBack = onClose,
    ) {
        Box(Modifier.size(1.dp).focusRequester(anchor).focusable())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cpu = s?.cpu?.usagePercent
            Gauge("CPU", cpu?.let { "$it%" } ?: "${s?.cpu?.cores ?: "–"} cores", s?.cpu?.currentMhz?.let { "%.1f GHz".format(Locale.US, it / 1000f) } ?: "", (cpu ?: 0) / 100f, Color(0xFF8AB4F8), Modifier.weight(1f))
            Gauge("Memory", s?.let { gb(it.ramUsed) } ?: "–", s?.let { "of ${gb(it.ramTotal)}" } ?: "", s?.let { it.ramUsed.toFloat() / it.ramTotal } ?: 0f, Color(0xFFF5B14A), Modifier.weight(1f))
            Gauge("Storage", s?.let { gb(it.storageUsed) } ?: "–", s?.let { "of ${gb(it.storageTotal)}" } ?: "", s?.let { it.storageUsed.toFloat() / it.storageTotal } ?: 0f, Color(0xFF7FD1AE), Modifier.weight(1f))
            val n = s?.network
            Gauge(
                "Network", n?.let { speed(it.downBytesPerSec) } ?: "–",
                n?.let { "↑ ${speed(it.upBytesPerSec)}" + (it.signalPercent?.let { p -> " · ${it.type} $p%" } ?: "") } ?: "",
                ((n?.downBytesPerSec ?: 0L) / 3_000_000f).coerceIn(0f, 1f), Color(0xFFC58AF9), Modifier.weight(1f),
            )
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) {
            Text("RUNNING APPS", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = LocalLauncherColors.current.subtext)
            if (!r.isNullOrEmpty()) Text("${r.size} apps · ${mb(r.sumOf { it.bytes })}", fontSize = 10.sp, color = LocalLauncherColors.current.subtext, modifier = Modifier.padding(start = 8.dp))
        }
        HelperNotice("Closing apps and Close All Apps")
        when {
            r == null -> Text(
                if (RunningApps.hasPermission(context)) "Reading…" else "Hew7 needs the memory permission (granted over ADB) to list running apps.",
                fontSize = 11.sp, color = LocalLauncherColors.current.subtext,
            )
            r.isEmpty() -> Text("No apps are running. Everything's closed.", fontSize = 11.sp, color = LocalLauncherColors.current.subtext)
            else -> {
                val biggest = r.maxOf { it.bytes }.coerceAtLeast(1)
                Column(Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    r.chunked(2).forEachIndexed { row, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEachIndexed { i, app ->
                                AppRunningRow(
                                    app, biggest, { at -> close(listOf(app.app.pkg), at) },
                                    Modifier.weight(1f).then(if (row == 0 && i == 0) Modifier.focusRequester(first) else Modifier),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 9.dp)) {
                    Surface(
                        onClick = { close(r.map { it.app.pkg }, closeAllRect.anchor(round = false, fill = Color.White, ink = Color(0xFF0B1624), stroke = Color(0xFF8AB4F8), label = true)) },
                        modifier = Modifier.onScreen(closeAllRect).height(30.dp),
                        shape = ClickableSurfaceDefaults.shape(CircleShape),
                        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFFF5F5F7), contentColor = Color(0xFF0B1624), focusedContainerColor = Color.White, focusedContentColor = Color(0xFF0B1624)),
                        border = ClickableSurfaceDefaults.border(focusedBorder = androidx.tv.material3.Border(androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF8AB4F8)), shape = CircleShape)),
                    ) {
                        Row(Modifier.fillMaxHeight().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = Color(0xFF0B1624), modifier = Modifier.size(14.dp))
                            Text("Close all apps", color = Color(0xFF0B1624), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    Text("OK on an app closes it", fontSize = 9.sp, color = LocalLauncherColors.current.subtext, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}

/** A gauge card: a ring filled to [fraction], with a label, a big figure and a small one. */
@Composable
private fun Gauge(label: String, big: String, small: String, fraction: Float, color: Color, modifier: Modifier) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(800), label = "gauge")
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.06f)).padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(40.dp)) {
            val w = 4.5.dp.toPx()
            val inset = w / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
            drawArc(Color.White.copy(alpha = 0.14f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            drawArc(color, -90f, 360f * f, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        Column(Modifier.padding(start = 8.dp)) {
            Text(label.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = Color.White.copy(alpha = 0.72f))
            Text(big, fontFamily = DisplayFont, fontWeight = FontWeight.Light, fontSize = 16.sp, maxLines = 1)
            Text(small, fontSize = 8.sp, color = Color.White.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AppRunningRow(app: RunningApp, biggest: Long, onClose: (CloseHelperService.Anchor?) -> Unit, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    val xRect = remember { ScreenRect() }
    Surface(
        onClick = { onClose(xRect.anchor(round = true, fill = Color(0xFF323A4A), ink = Color(0xFF8AB4F8))) },
        modifier = modifier.height(42.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RowShape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, contentColor = Color.White, focusedContainerColor = Color.Transparent, focusedContentColor = Color.White),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .background(if (focused) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f), RowShape)
                .border(1.5.dp, if (focused) Color.White.copy(alpha = 0.9f) else Color.Transparent, RowShape)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(app.app.icon, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Row {
                    Text(app.app.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(mb(app.bytes), fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(start = 6.dp))
                }
                Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(app.bytes.toFloat() / biggest).background(Color(0xFF8AB4F8)))
                }
            }
            Box(Modifier.onScreen(xRect).size(24.dp).clip(CircleShape).background(if (focused) Color.White.copy(alpha = 0.22f) else Color.Transparent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, contentDescription = "Close ${app.app.label}", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}
