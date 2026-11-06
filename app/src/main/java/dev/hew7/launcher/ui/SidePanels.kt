package dev.hew7.launcher.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Input
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsInputAntenna
import androidx.compose.material.icons.rounded.SettingsInputHdmi
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.res.stringResource
import dev.hew7.launcher.R
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
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
import dev.hew7.launcher.RemoteKeys
import dev.hew7.launcher.data.DeviceInfo
import dev.hew7.launcher.data.GreetingStyle
import dev.hew7.launcher.data.InputRepository
import dev.hew7.launcher.data.Prefs
import dev.hew7.launcher.data.RemoteAction
import dev.hew7.launcher.data.RemoteButtons
import dev.hew7.launcher.data.RemoteChoice
import dev.hew7.launcher.data.RunningApp
import dev.hew7.launcher.data.RunningApps
import dev.hew7.launcher.data.SourceKind
import dev.hew7.launcher.data.SystemSnapshot
import dev.hew7.launcher.data.SystemStats
import dev.hew7.launcher.data.TempUnit
import dev.hew7.launcher.data.ThemeMode
import dev.hew7.launcher.data.TileStyle
import dev.hew7.launcher.data.TvSource
import dev.hew7.launcher.data.WallpaperSource
import dev.hew7.launcher.dream.ScreensaverActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.tv.material3.Border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset

/** When Left or Right was last pressed in a panel; a blade only switches pages right after one. */
private var lastSideKeyAt = 0L

private val PanelShape = RoundedCornerShape(22.dp)
private val TileShape = RoundedCornerShape(12.dp)
private val BarShape = RoundedCornerShape(3.dp)

/**
 * Hew7 2.0's panel: a frosted glass strip across the top of the screen that grows sideways out of
 * the top-right corner, the way the weather pill opens. Its items flow into columns from left to
 * right (Up and Down within a column, Left and Right between columns), and it widens or narrows
 * smoothly to fit each page. It lives in its own dialog window so focus can't wander to the home
 * screen behind it, and returns to the opening button when it closes. An optional blade sits
 * before or after it, and optional round buttons sit beside the title.
 */
@Composable
fun SidePanel(
    title: String,
    first: FocusRequester,
    alpha: Float,
    onClose: () -> Unit,
    bladeStart: (@Composable () -> Unit)? = null,
    bladeEnd: (@Composable () -> Unit)? = null,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    focusKey: Any = Unit,
    /** Shown before the title, as the weather pill shows its sky. */
    icon: ImageVector? = null,
    /** Small text at the right of the header, like the pill's "Cloudy". */
    detail: String? = null,
    /** Lay the page out as a row of pill cells (like the pill's hours) instead of short columns. */
    cells: Boolean = false,
    content: @Composable () -> Unit,
) {
    // The weather pill's look: smoky glass, a thin white edge, light letters (whatever the theme).
    val theme = LocalLauncherColors.current
    val c = panelColors(theme)
    // A little more solid than the pill, and more so in the light theme (bright daytime photos).
    val glass = Color(0xFF0A0E18).copy(alpha = (0.3f + 0.4f * alpha + if (theme.dark) 0f else 0.12f).coerceIn(0.45f, 0.82f))
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        (LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(0.15f)
        // On opening, on a page change, and whenever the panel's window comes back into focus.
        val windowFocused = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(focusKey, windowFocused) {
            if (!windowFocused) return@LaunchedEffect
            delay(120)
            runCatching { first.requestFocus() }
        }
        Box(
            Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionLeft || e.key == Key.DirectionRight)) {
                        lastSideKeyAt = SystemClock.uptimeMillis()
                    }
                    // Remote buttons (Settings, Menu...) work with a panel open too.
                    RemoteKeys.onKey(e.nativeKeyEvent)
                },
            contentAlignment = Alignment.TopEnd,
        ) {
            AnimatedVisibility(
                visibleState = visible,
                // Grows out sideways from where the weather pill sits, the way the pill opens.
                enter = expandHorizontally(spring(dampingRatio = 0.85f, stiffness = 240f), expandFrom = Alignment.End) + fadeIn(tween(250)),
            ) {
                CompositionLocalProvider(LocalLauncherColors provides c) {
                Row(Modifier.padding(top = 36.dp, end = 48.dp, start = 48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bladeStart?.invoke()
                    Column(
                        Modifier
                            .background(glass, PillShape)
                            .border(1.dp, Color.White.copy(alpha = 0.22f), PillShape)
                            .clip(PillShape)
                            // Widens and narrows smoothly as pages change.
                            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = 260f))
                            .padding(horizontal = 15.dp, vertical = 10.dp),
                    ) {
                        ProvideTextStyle(LocalTextStyle.current.copy(shadow = null, color = c.text)) {
                            // Header, like the pill's top line: icon, a light Outfit title, small text on the right.
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(36.dp)) {
                                icon?.let { Icon(it, contentDescription = null, tint = c.text, modifier = Modifier.size(20.dp)) }
                                Text(
                                    title, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Light, fontFamily = DisplayFont,
                                    modifier = Modifier.padding(start = if (icon != null) 8.dp else 0.dp, end = 28.dp),
                                )
                                detail?.let { Text(it, color = c.subtext, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(end = 12.dp)) }
                                headerActions?.let { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = it) }
                            }
                            // The pill's thin divider, then the page. Padding inside the scrolling
                            // area, so selected cells that grow aren't cut off at its edges.
                            Box(
                                Modifier
                                    .padding(top = 10.dp)
                                    .drawBehind { drawLine(Color.White.copy(alpha = 0.2f), Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
                                    .padding(top = 8.dp)
                                    .heightIn(max = 260.dp)
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                if (cells) {
                                    CompositionLocalProvider(LocalCellRow provides true) { CellRow(Modifier.padding(4.dp)) { content() } }
                                } else {
                                    FlowingColumns(Modifier.padding(4.dp)) { content() }
                                }
                            }
                        }
                    }
                    bladeEnd?.invoke()
                }
                }
            }
        }
    }
}

/** Light letters for the panels' smoky glass, in either theme. */
internal fun panelColors(theme: LauncherColors) = theme.copy(
    text = Color(0xFFF5F5F7),
    subtext = Color(0xBFF5F5F7),
    glass = Color.White.copy(alpha = 0.1f),
    focusFill = Color(0xFFF5F5F7),
    onFocusFill = Color(0xFF15161A),
    // dark stays the theme's, so the glass can be a touch more solid in the daytime.
)

/** The weather pill's shape, shared by the panels and their blades. */
private val PillShape = RoundedCornerShape(18.dp)
private val ColumnWidth = 250.dp
private val ColumnGap = 18.dp

/**
 * Lays its items side by side, like the open weather pill's hours. Items that aren't cells (a
 * note, the Helper's notice) get a column's width and sit in line with the cells.
 */
@Composable
private fun CellRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val gap = 6.dp.roundToPx()
        val maxW = ColumnWidth.roundToPx()
        val maxH = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
        val placeables = measurables.map { it.measure(Constraints(maxWidth = maxW, maxHeight = maxH)) }
        val h = placeables.maxOfOrNull { it.height } ?: 0
        var x = 0
        val xs = placeables.map { p -> x.also { x += p.width + if (p.width > 1) gap else 0 } }
        layout((x - gap).coerceAtLeast(0), h) {
            placeables.forEachIndexed { i, p -> p.place(xs[i], (h - p.height) / 2) }
        }
    }
}

/**
 * Lays its items top to bottom in columns of [ColumnWidth]; when a column is full, the next item
 * starts a new column to the right. So a page's list reads across the strip, not down the screen.
 */
@Composable
private fun FlowingColumns(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val cw = ColumnWidth.roundToPx()
        val gap = ColumnGap.roundToPx()
        val maxH = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
        val placeables = measurables.map { it.measure(Constraints(maxWidth = cw, maxHeight = maxH)) }
        val spots = ArrayList<IntOffset>(placeables.size)
        var col = 0
        var y = 0
        var tallest = 0
        placeables.forEach { p ->
            if (y > 0 && y + p.height > maxH) {
                col++
                y = 0
            }
            spots += IntOffset(col * (cw + gap), y)
            y += p.height
            tallest = maxOf(tallest, y)
        }
        layout((col + 1) * cw + col * gap, tallest) {
            placeables.forEachIndexed { i, p -> p.place(spots[i]) }
        }
    }
}

/**
 * Invisible focus anchor so a panel opens with nothing highlighted. Once focus moves off it, it
 * steps aside, so Up from the first row goes straight to the header buttons.
 */
@Composable
private fun Anchor(first: FocusRequester) {
    var held by remember { mutableStateOf(false) }
    var spent by remember { mutableStateOf(false) }
    // Coming back to the panel (from a settings page or the Hew7 Helper) rests on it again.
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocused) {
        if (!windowFocused) {
            held = false
            spent = false
        }
    }
    Box(
        Modifier
            .size(1.dp)
            .focusRequester(first)
            .onFocusChanged {
                if (it.isFocused) held = true else if (held) spent = true
            }
            .focusProperties { canFocus = !spent }
            .focusable(),
    )
}

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

// ---------- Inputs (fallback when the TV has no inputs menu of its own) ----------

@Composable
fun InputsPanel(alpha: Float, onClose: () -> Unit) {
    val c = LocalLauncherColors.current
    val context = LocalContext.current
    val repo = remember { InputRepository(context) }
    val sources = remember { repo.list() }
    val first = remember { FocusRequester() }

    SidePanel("Inputs", first, alpha, onClose, icon = Icons.Rounded.Input, cells = true) {
        if (sources.isEmpty()) {
            Anchor(first)
            Text("This TV didn't report any inputs.", color = c.subtext, fontSize = 14.sp)
        }
        sources.forEachIndexed { i, source ->
            MenuItem(
                icon = iconFor(source),
                text = source.label,
                value = if (source.active) "Device On" else null,
                onClick = {
                    onClose()
                    if (!repo.switchTo(source)) Toast.makeText(context, "The TV didn't accept the input switch", Toast.LENGTH_SHORT).show()
                },
                modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
            )
        }
    }
}

private fun iconFor(source: TvSource): ImageVector = when (source.kind) {
    SourceKind.TUNER -> Icons.Rounded.SettingsInputAntenna
    SourceKind.HDMI -> Icons.Rounded.SettingsInputHdmi
    SourceKind.AV, SourceKind.COMPONENT -> Icons.Rounded.Cable
    SourceKind.OTHER -> Icons.Rounded.Input
}

// ---------- Machine Room ----------

@Composable
fun MachineRoomPanel(vm: HomeViewModel, alpha: Float, startOnRunningApps: Boolean, onClose: () -> Unit) {
    val c = LocalLauncherColors.current
    val context = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val stats = remember { SystemStats(context) }
    var snap by remember { mutableStateOf<SystemSnapshot?>(null) }
    var running by remember { mutableStateOf<List<RunningApp>?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var showRunning by remember { mutableStateOf(startOnRunningApps) }
    val first = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        while (true) {
            snap = withContext(Dispatchers.IO) { stats.read() }
            delay(1_000)
        }
    }
    LaunchedEffect(refresh, apps) {
        while (true) {
            running = RunningApps.read(context, apps)
            delay(5_000)
        }
    }

    if (showRunning) {
        RunningAppsPage(vm, running, alpha, onBack = { showRunning = false; refresh++ }, onClose = onClose)
        return
    }

    val s = snap
    SidePanel(
        "Machine Room", first, alpha, onClose,
        icon = Icons.Rounded.Memory,
        detail = s?.let { "${it.model} · up ${duration(it.uptimeMillis)}" },
        cells = true,
    ) {
        // Never recomposes away, so focus survives the data arriving.
        Anchor(first)

        if (s == null) {
            Text("Reading…", color = c.subtext, fontSize = 13.sp)
        } else {
            // One figure per cell, like the weather pill's hours.
            val clock = s.cpu.currentMhz?.let { "%.1f GHz".format(Locale.US, it / 1000f) }
            StatCell(Icons.Rounded.Memory, "CPU", s.cpu.usagePercent?.let { "$it%" } ?: "${s.cpu.cores} cores", s.cpu.usagePercent?.let { it / 100f })
            if (clock != null) StatCell(Icons.Rounded.Speed, "Speed", clock)
            s.cpu.temperatureC?.let { StatCell(Icons.Rounded.Thermostat, "Temperature", "${it.toInt()}°C") }
            StatCell(Icons.Rounded.Memory, "RAM", "${gbShort(s.ramUsed)} / ${gb(s.ramTotal)}", s.ramUsed.toFloat() / s.ramTotal)
            StatCell(Icons.Rounded.SdStorage, "Storage", "${gbShort(s.storageUsed)} / ${gb(s.storageTotal)}", s.storageUsed.toFloat() / s.storageTotal)
            val n = s.network
            StatCell(if (n.type == "Wi-Fi") Icons.Rounded.Wifi else Icons.Rounded.SettingsEthernet, n.type, n.signalPercent?.let { "$it%" } ?: n.linkMbps?.let { "$it Mbps" } ?: "")
            StatCell(Icons.Rounded.ArrowDownward, "Down", speed(n.downBytesPerSec))
            StatCell(Icons.Rounded.ArrowUpward, "Up", speed(n.upBytesPerSec))
        }

        val r = running
        MenuItem(
            icon = Icons.Rounded.Apps,
            text = "Running Apps",
            value = when {
                r == null -> null
                r.isEmpty() -> "None"
                else -> "${r.size} · ${mb(r.sumOf { it.bytes })}"
            },
            onClick = { showRunning = true },
        )
    }
}

/**
 * The apps that are running, each with its memory and a bar, and a way to close it. Closing uses
 * the Hew7 Helper (an accessibility service the user switches on once) to press Force Stop for
 * them out of sight; when it hands back, the closed rows slide away. Without the helper, a single
 * app's App Info page opens instead.
 */
@Composable
private fun RunningAppsPage(vm: HomeViewModel, fresh: List<RunningApp>?, alpha: Float, onBack: () -> Unit, onClose: () -> Unit) {
    val c = LocalLauncherColors.current
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    val helperOn = rememberHelperState() == HelperState.RUNNING
    val justClosed by vm.justClosed.collectAsStateWithLifecycle()
    // Right after closing, show the list as it was so the closed rows can play away.
    val before = remember { vm.runningBeforeClose.takeIf { justClosed.isNotEmpty() } }
    var settled by remember { mutableStateOf(before == null) }
    LaunchedEffect(Unit) {
        if (!settled) {
            delay(1_100)
            vm.justClosed.value = emptySet()
            vm.runningBeforeClose = null
            settled = true
        }
    }
    val running = if (settled) fresh else before

    fun close(pkgs: List<String>) {
        val helper = CloseHelperService.instance
        if (helperOn && helper != null) {
            vm.runningBeforeClose = running
            helper.close(pkgs)
        } else {
            // Without the helper, say what to do; for one app, its App Info page still lets you
            // press Force Stop yourself.
            context.explainHelper()
            if (pkgs.size == 1) context.startFirst(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${pkgs[0]}")))
        }
    }

    SidePanel(
        "Running Apps",
        first,
        alpha,
        onClose = onBack,
        bladeStart = { Blade(Icons.Rounded.ChevronLeft, "Machine Room", alpha, onEnter = onBack) },
        icon = Icons.Rounded.Apps,
    ) {
        Anchor(first)
        HelperNotice("Closing apps and Close All Apps")
        when {
            running == null -> {
                Text(
                    if (RunningApps.hasPermission(context)) "Reading…" else "Hew7 needs the memory permission (granted over ADB) to list running apps.",
                    color = c.subtext,
                    fontSize = 13.sp,
                )
            }
            running.isEmpty() -> Text("No apps are running right now.", color = c.subtext, fontSize = 13.sp)
            else -> {
                val total = running.sumOf { it.bytes }
                Text("Using ${mb(total)}", color = c.subtext, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                val biggest = running.maxOf { it.bytes }.coerceAtLeast(1)
                running.forEach { app ->
                    key(app.app.pkg) {
                        val visible = remember { MutableTransitionState(true) }
                        LaunchedEffect(justClosed) {
                            if (app.app.pkg in justClosed) {
                                delay(250)
                                visible.targetState = false
                            }
                        }
                        AnimatedVisibility(
                            visibleState = visible,
                            exit = slideOutHorizontally(tween(420)) { it } + fadeOut(tween(380)) + shrinkVertically(tween(320, delayMillis = 300)),
                        ) {
                            RunningRow(app, biggest) { close(listOf(app.app.pkg)) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                MenuItem(Icons.Rounded.RocketLaunch, "Close All Apps", { close(running.map { it.app.pkg }) })
            }
        }
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
                .background(c.glassStrong, PanelShape)
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

@Composable
private fun RunningRow(app: RunningApp, biggest: Long, onClose: () -> Unit) {
    val c = LocalLauncherColors.current
    var focused by remember { mutableStateOf(false) }
    val mbUsed = app.bytes / 1_048_576
    val barColor = when {
        mbUsed >= 200 -> Color(0xFFF28B82)
        mbUsed >= 100 -> Color(0xFFFDD663)
        else -> Color(0xFF8AB4F8)
    }
    Surface(
        onClick = onClose,
        modifier = Modifier.fillMaxWidth().height(50.dp).padding(vertical = 2.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(TileShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = c.text,
            focusedContainerColor = c.focusFill,
            focusedContentColor = c.onFocusFill,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(app.app.icon, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Row {
                    FittedText(app.app.label, focused, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(mb(app.bytes), fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                }
                Box(
                    Modifier.padding(top = 4.dp).fillMaxWidth().height(4.dp)
                        .background((if (focused) c.onFocusFill else c.text).copy(alpha = 0.15f), BarShape),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(app.bytes.toFloat() / biggest).background(barColor, BarShape))
                }
            }
            Icon(Icons.Rounded.Close, contentDescription = "Close ${app.app.label}", modifier = Modifier.size(18.dp))
        }
    }
}

// ---------- Hew7 Settings and Quick Settings ----------

private enum class Page(val title: String) {
    HEW7("Hew7 Settings"),
    QUICK("Quick Settings"),
    THEME("Theme"),
    FONT("Font"),
    GREETINGS("Greetings"),
    TILES("App Tiles"),
    NAMES("App Names"),
    WALLPAPERS("Wallpapers"),
    TEMPERATURE("Temperature"),
    TRANSPARENCY("Transparency"),
    SCREENSAVER("Screensaver"),
    REMOTE("Remote Buttons"),
    REMOTE_BUTTON(""), // titled with the button's name
    REMOTE_APP("Open an App"),
    POWER("Power"),
    ABOUT("About Hew7"),
}

internal val WallpaperIntervals = listOf(5, 15, 60, 360)
internal val ScreensaverDelays = listOf(5, 10, 15, 30, 60).map { it * 60_000 } + Int.MAX_VALUE
internal val CalmAfter = listOf(15, 30, 60, 0)
internal val SleepAfter = listOf(60, 120, 240, 0)

internal fun minutesLabel(m: Int) = if (m < 60) "$m Minutes" else if (m == 60) "1 Hour" else "${m / 60} Hours"
internal fun delayLabel(ms: Int) = if (ms == Int.MAX_VALUE) "Never" else minutesLabel(ms / 60_000)

/**
 * Hew7 Settings first, with a slim Quick Settings blade on the right. Every Hew7 tile opens its
 * own pop-down page; Back returns to the tiles.
 */
@Composable
fun SettingsPanel(vm: HomeViewModel, alpha: Float, startOnQuick: Boolean = false, onClose: () -> Unit) {
    val context = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val prefs = remember { Prefs(context) }
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
    var page by remember { mutableStateOf(if (startOnQuick) Page.QUICK else Page.HEW7) }
    var calmMinutes by remember { mutableStateOf(prefs.screensaverCalmMinutes) }
    var sleepMinutes by remember { mutableStateOf(prefs.screensaverSleepMinutes) }
    val first = remember { FocusRequester() }
    // Remote Buttons: the button being set up, whether a press is awaited, and a counter that
    // re-reads the saved choices after a change.
    var button by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var remoteVersion by remember { mutableStateOf(0) }
    val helperOn = rememberHelperState() == HelperState.RUNNING
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    DisposableEffect(listening) {
        if (listening) {
            RemoteKeys.learner = { name ->
                listening = false
                button = name
                page = Page.REMOTE_BUTTON
            }
        }
        onDispose { RemoteKeys.learner = null }
    }
    fun choiceFor(name: String) = RemoteChoice.decode(prefs.remoteButton(name)) ?: RemoteChoice(RemoteAction.DEFAULT)
    fun choiceLabel(name: String, choice: RemoteChoice): String = when (choice.action) {
        RemoteAction.DEFAULT -> RemoteButtons.defaultFor(name).let { d ->
            if (d == RemoteAction.DEFAULT) "Default (What the TV Does)" else "Default (${d.label})"
        }
        RemoteAction.OPEN_APP -> "Open " + (apps.firstOrNull { it.pkg == choice.pkg }?.label ?: "App")
        else -> choice.action.label
    }
    fun setChoice(choice: RemoteChoice) {
        prefs.setRemoteButton(button, if (choice.action == RemoteAction.DEFAULT) null else choice.encode())
        remoteVersion++
    }
    val network = remember { DeviceInfo.network(context).name }
    // Back from a pop-down selects the tile that opened it; arriving any other way selects nothing.
    val tileFocus = remember { Page.entries.associateWith { FocusRequester() } }
    var openedFrom by remember { mutableStateOf<Page?>(null) }
    fun open(p: Page) {
        openedFrom = p
        page = p
    }
    fun toHew7(fromTile: Boolean) {
        if (!fromTile) openedFrom = null
        page = Page.HEW7
    }

    val back = {
        when {
            listening -> listening = false
            page == Page.HEW7 -> onClose()
            page == Page.QUICK -> toHew7(fromTile = false)
            page == Page.REMOTE_BUTTON -> page = Page.REMOTE
            page == Page.REMOTE_APP -> page = Page.REMOTE_BUTTON
            page == Page.POWER -> page = Page.QUICK
            else -> toHew7(fromTile = true)
        }
    }

    SidePanel(
        title = if (page == Page.REMOTE_BUTTON) RemoteButtons.label(button) else page.title,
        first = openedFrom?.takeIf { page == Page.HEW7 }?.let { tileFocus.getValue(it) } ?: first,
        alpha = alpha,
        onClose = back,
        focusKey = page,
        icon = when (page) {
            Page.QUICK -> Icons.Rounded.Tune
            Page.POWER -> Icons.Rounded.PowerSettingsNew
            Page.REMOTE, Page.REMOTE_BUTTON, Page.REMOTE_APP -> Icons.Rounded.SettingsRemote
            else -> Icons.Rounded.Settings
        },
        // Pages of choices and tiles read across like the pill's hours; text and lists stay in columns.
        cells = page in setOf(
            Page.HEW7, Page.QUICK, Page.THEME, Page.FONT, Page.GREETINGS, Page.TILES, Page.NAMES,
            Page.TEMPERATURE, Page.POWER, Page.REMOTE_BUTTON, Page.REMOTE_APP,
        ),
        bladeStart = when (page) {
            Page.QUICK -> ({ Blade(Icons.Rounded.ChevronLeft, "Hew7 Settings", alpha) { toHew7(fromTile = false) } })
            Page.HEW7 -> null
            Page.POWER -> ({ Blade(Icons.Rounded.ChevronLeft, "Quick Settings", alpha) { page = Page.QUICK } })
            else -> ({ Blade(Icons.Rounded.ChevronLeft, "Hew7 Settings", alpha) { toHew7(fromTile = true) } })
        },
        bladeEnd = if (page == Page.HEW7) {
            { Blade(Icons.Rounded.Tune, "Quick Settings", alpha) { page = Page.QUICK } }
        } else null,
        headerActions = if (page == Page.QUICK) {
            {
                HeaderButton(Icons.Rounded.Settings, "All Settings") { context.openSetting() }
                HeaderButton(Icons.Rounded.Person, "Accounts") {
                    context.openSettingsEntry("Accounts and sign-in", "Accounts & sign-in", "Accounts and Sign-In")
                }
                HeaderButton(Icons.Rounded.PowerSettingsNew, "Power") { page = Page.POWER }
            }
        } else null,
    ) {
        key(page) { Anchor(first) }
        when (page) {
            Page.HEW7 -> TileGrid(
                listOf(
                    TileSpec(Icons.Rounded.Palette, "Theme", theme.shortLabel, focus = tileFocus[Page.THEME]) { open(Page.THEME) },
                    TileSpec(Icons.Rounded.TextFields, "Font", font.label, focus = tileFocus[Page.FONT]) { open(Page.FONT) },
                    TileSpec(Icons.Rounded.EmojiEmotions, "Greetings", greeting.label, focus = tileFocus[Page.GREETINGS]) { open(Page.GREETINGS) },
                    TileSpec(Icons.Rounded.GridView, "App Tiles", tiles.label, focus = tileFocus[Page.TILES]) { open(Page.TILES) },
                    TileSpec(Icons.Rounded.Label, "App Names", if (showNames) "On" else "Off", focus = tileFocus[Page.NAMES]) { open(Page.NAMES) },
                    TileSpec(Icons.Rounded.Wallpaper, "Wallpapers", "${source.label} · ${shortMinutes(minutes)}", focus = tileFocus[Page.WALLPAPERS]) { open(Page.WALLPAPERS) },
                    TileSpec(Icons.Rounded.Thermostat, "Temperature", unit.label, focus = tileFocus[Page.TEMPERATURE]) { open(Page.TEMPERATURE) },
                    TileSpec(Icons.Rounded.Contrast, "Transparency", "Panels ${pct(panelAlpha, PanelRange)} · Dock ${pct(dockAlpha, DockRange)}", focus = tileFocus[Page.TRANSPARENCY]) { open(Page.TRANSPARENCY) },
                    TileSpec(Icons.Rounded.Bedtime, "Screensaver", if (weatherFx) "Weather Effects On" else "Weather Effects Off", focus = tileFocus[Page.SCREENSAVER]) { open(Page.SCREENSAVER) },
                    TileSpec(
                        Icons.Rounded.SettingsRemote,
                        "Remote Buttons",
                        prefs.remoteSeen.size.let { if (it == 0) "Set Up" else "$it Buttons" },
                        focus = tileFocus[Page.REMOTE],
                    ) { open(Page.REMOTE) },
                    TileSpec(Icons.Rounded.Info, "About Hew7", "Version $versionName", focus = tileFocus[Page.ABOUT]) { open(Page.ABOUT) },
                ),
            )
            Page.ABOUT -> {
                val c = LocalLauncherColors.current
                Text("Hew7 $versionName", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.copyright), color = c.subtext, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                Section("Privacy")
                ReadingBlock(
                    "Hew7 has no account, ads or analytics, and keeps your settings on this TV. To show the weather it looks up " +
                        "your approximate location from your internet address (ipwho.is, ipapi.co) and asks Open-Meteo for the forecast. " +
                        "Wallpapers come from Bing or Picsum. Nothing else leaves the TV.",
                )
                Section("Credits")
                listOf(
                    "Weather data by Open-Meteo.com (CC BY 4.0)",
                    "Wallpapers: Bing daily images © their owners; Picsum photos via Unsplash",
                    "Fonts: DM Sans, Inter, Lexend, Manrope, Nunito, Outfit, Poppins, Quicksand (SIL Open Font License 1.1)",
                    "Built with AndroidX, Jetpack Compose, Kotlin, Coil and OkHttp (Apache License 2.0)",
                    "Google TV and Android TV are trademarks of Google LLC. Hew7 isn't affiliated with or endorsed by Google, TCL, Netflix or other app makers.",
                ).forEach { ReadingBlock(it) }
            }
            Page.REMOTE -> {
                val c = LocalLauncherColors.current
                remoteVersion // re-read after changes
                if (listening) {
                    MenuItem(Icons.Rounded.SettingsRemote, "Press a Button on Your Remote…", { listening = false })
                    Text("Back to cancel.", color = c.subtext, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                } else {
                    MenuItem(Icons.Rounded.Add, "Add a Button", { listening = true })
                }
                // Buttons first seen under a bare number get their proper name once it's known.
                val seen = remember(remoteVersion) {
                    val raw = prefs.remoteSeen
                    raw.map { n -> if (n.all { it.isDigit() }) RemoteButtons.name(n.toInt()) else n }.distinct()
                        .also { if (it != raw) prefs.remoteSeen = it }
                }
                if (seen.isNotEmpty()) Section("Your Remote")
                seen.forEach { name ->
                    val choice = choiceFor(name)
                    MenuItem(
                        icon = null,
                        text = RemoteButtons.label(name),
                        value = when (choice.action) {
                            RemoteAction.DEFAULT -> RemoteButtons.defaultFor(name).short
                            RemoteAction.OPEN_APP -> apps.firstOrNull { it.pkg == choice.pkg }?.label ?: "App"
                            else -> choice.action.short
                        },
                        onClick = {
                            button = name
                            page = Page.REMOTE_BUTTON
                        },
                    )
                }
                Text(
                    "Buttons work on Hew7's home screen; in other apps they keep their usual job.",
                    color = c.subtext,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 6.dp),
                )
                HelperNotice("The Settings, Input and Menu buttons (which the TV keeps for itself) and app buttons inside other apps")
            }
            Page.REMOTE_BUTTON -> {
                remoteVersion
                val current = choiceFor(button)
                RemoteAction.entries.filter { it != RemoteAction.OPEN_APP }.forEach { a ->
                    MenuItem(
                        icon = if (current.action == a) Icons.Rounded.Check else null,
                        text = choiceLabel(button, RemoteChoice(a)),
                        onClick = { setChoice(RemoteChoice(a)) },
                    )
                }
                MenuItem(
                    icon = if (current.action == RemoteAction.OPEN_APP) Icons.Rounded.Check else null,
                    text = if (current.action == RemoteAction.OPEN_APP) choiceLabel(button, current) else "Open an App…",
                    onClick = { page = Page.REMOTE_APP },
                )
            }
            Page.POWER -> {
                val c = LocalLauncherColors.current
                // Restart and Shut Down need the Hew7 Helper running.
                val helper = CloseHelperService.instance.takeIf { helperOn }
                HelperNotice("Restart and Shut Down")
                MenuItem(Icons.Rounded.RestartAlt, "Restart", {
                    if (helper != null) {
                        onClose()
                        helper.restart()
                    } else {
                        context.explainHelper()
                    }
                })
                MenuItem(Icons.Rounded.PowerSettingsNew, "Shut Down", {
                    if (helper != null) {
                        onClose()
                        helper.shutDown()
                    } else {
                        context.explainHelper()
                    }
                })
                if (helper != null) {
                    Text(
                        "Restart starts the TV again. Shut Down turns it off completely; switch it on with the remote's power button.",
                        color = c.subtext,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    )
                }
            }
            Page.REMOTE_APP -> {
                val current = choiceFor(button)
                apps.forEach { app ->
                    MenuItem(
                        icon = if (current.action == RemoteAction.OPEN_APP && current.pkg == app.pkg) Icons.Rounded.Check else null,
                        text = app.label,
                        onClick = {
                            setChoice(RemoteChoice(RemoteAction.OPEN_APP, app.pkg))
                            page = Page.REMOTE_BUTTON
                        },
                    )
                }
            }
            Page.QUICK -> {
                HelperNotice("Display and Sound, Accounts and Power (Restart, Shut Down)")
                QuickTiles(context, network)
            }
            Page.THEME -> Options(ThemeMode.entries, theme, { it.label }) { vm.setThemeMode(it) }
            Page.FONT -> LauncherFont.entries.forEach { f ->
                MenuItem(
                    icon = if (f == font) Icons.Rounded.Check else null,
                    text = f.label,
                    onClick = { vm.setFont(f) },
                    textStyle = TextStyle(fontFamily = f.family, fontSize = 16.sp),
                )
            }
            Page.GREETINGS -> Options(GreetingStyle.entries, greeting, { it.label }) { vm.setGreetingStyle(it) }
            Page.TILES -> Options(TileStyle.entries, tiles, { it.label }) { vm.setTileStyle(it) }
            Page.NAMES -> Options(listOf(true, false), showNames, { if (it) "On" else "Off" }) { vm.setShowAppNames(it) }
            Page.TEMPERATURE -> Options(TempUnit.entries, unit, { if (it == TempUnit.CELSIUS) "Celsius (°C)" else "Fahrenheit (°F)" }) { vm.setTempUnit(it) }
            Page.WALLPAPERS -> {
                Section("Source")
                Options(WallpaperSource.entries, source, { it.label }) { vm.setWallpaperSource(it) }
                Section("Change Every")
                Options(WallpaperIntervals, minutes, { minutesLabel(it) }) { vm.setWallpaperMinutes(it) }
            }
            Page.TRANSPARENCY -> {
                SliderRow("Panels", panelAlpha, PanelRange) { vm.setPanelAlpha(it) }
                SliderRow("App Dock", dockAlpha, DockRange) { vm.setDockAlpha(it) }
                Text("◀ ▶ to adjust", color = LocalLauncherColors.current.subtext, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
            }
            Page.SCREENSAVER -> {
                MenuItem(Icons.Rounded.PlayArrow, "Start Now", {
                    onClose()
                    context.startActivity(Intent(context, ScreensaverActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                })
                Section("Weather Effects")
                Options(listOf(true, false), weatherFx, { if (it) "On" else "Off" }) { vm.setWeatherEffects(it) }
                Section("Start After")
                Options(ScreensaverDelays, ScreensaverDelays.minByOrNull { kotlin.math.abs(it.toLong() - saverDelay) } ?: saverDelay, { delayLabel(it) }) {
                    if (!vm.setScreensaverDelay(it)) {
                        Toast.makeText(context, "Hew7 isn't allowed to change the screensaver timer yet", Toast.LENGTH_SHORT).show()
                    }
                }
                // Energy saving for a screensaver left running.
                Section("Calm After")
                Options(CalmAfter, calmMinutes, { if (it == 0) "Never" else minutesLabel(it) }) {
                    prefs.screensaverCalmMinutes = it
                    calmMinutes = it
                }
                Section("Sleep After")
                Options(SleepAfter, sleepMinutes, { if (it == 0) "Never" else minutesLabel(it) }) {
                    prefs.screensaverSleepMinutes = it
                    sleepMinutes = it
                }
                Text(
                    "Calm: the picture holds still and dims, and the clock updates once a minute. " +
                        "Sleep: the TV goes to standby.",
                    color = LocalLauncherColors.current.subtext,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
                )
                HelperNotice("Sleep After")
            }
        }
    }
}

/** Quick Settings' tiles: the TV's own settings pages. */
@Composable
private fun QuickTiles(context: Context, network: String) {
    TileGrid(
        listOf(
            // The TV's own Display and Sound page, which has no direct link (see openSettingsEntry).
            TileSpec(Icons.Rounded.Tv, "Display and Sound", wide = true) {
                context.openSettingsEntry("Display and sound", "Display & sound", "Display and Sound")
            },
            TileSpec(Icons.Rounded.Wifi, "Network", network) { context.openSetting(Intent(Settings.ACTION_WIFI_SETTINGS)) },
            TileSpec(Icons.Rounded.Bluetooth, "Bluetooth and Remotes") {
                context.openSetting(
                    component("com.android.tv.settings", "com.tcl.android.tv.settings.accessories.AccessoriesActivity"),
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                )
            },
            TileSpec(Icons.Rounded.Accessibility, "Accessibility") {
                context.openSetting(
                    component("com.android.tv.settings", "com.android.tv.settings.oemlink.AccessibilitySettingsActivity"),
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                )
            },
            TileSpec(Icons.Rounded.Apps, "Apps") { context.openSetting(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)) },
        ),
    )
}

/**
 * A paragraph you can move onto with the remote. On a TV the panel only scrolls by moving the
 * selection, so pages of plain text (About Hew7) need something selectable to step through.
 */
@Composable
private fun ReadingBlock(text: String) {
    val c = LocalLauncherColors.current
    var focused by remember { mutableStateOf(false) }
    Text(
        text,
        color = if (focused) c.text else c.subtext,
        fontSize = 11.sp,
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
            .background(if (focused) c.text.copy(alpha = 0.08f) else Color.Transparent, TileShape)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

/** A list of choices with a tick beside the current one. */
@Composable
private fun <T> Options(values: List<T>, current: T, label: (T) -> String, onPick: (T) -> Unit) {
    values.forEach { v ->
        MenuItem(icon = if (v == current) Icons.Rounded.Check else null, text = label(v), onClick = { onPick(v) })
    }
}

/** How solid panels and the dock can be; each slider shows its range as 0–100%. */
internal val PanelRange = 0.4f..1f
internal val DockRange = 0f..0.8f

private fun fractionOf(v: Float, range: ClosedFloatingPointRange<Float>) =
    ((v - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

private fun pct(v: Float, range: ClosedFloatingPointRange<Float>) = "${(fractionOf(v, range) * 100).roundToInt()}%"
private fun shortMinutes(m: Int) = if (m < 60) "$m Min" else "${m / 60} H"

/** A horizontal slider for the remote: Up/Down choose it, Left/Right move it in 5% steps. */
@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    val c = LocalLauncherColors.current
    var focused by remember { mutableStateOf(false) }
    val fraction = fractionOf(value, range)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(if (focused) c.text.copy(alpha = 0.1f) else Color.Transparent, TileShape)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val step = when (e.key) {
                    Key.DirectionLeft -> -0.05f
                    Key.DirectionRight -> 0.05f
                    else -> return@onPreviewKeyEvent false
                }
                // Steps of 5% of the slider, landing on whole percentages.
                val next = ((fraction + step) * 20).roundToInt().coerceIn(0, 20) / 20f
                onChange(range.start + next * (range.endInclusive - range.start))
                true
            }
            .focusable()
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Row {
            Text(label, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(pct(value, range), color = c.subtext, fontSize = 12.sp)
        }
        Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(if (focused) 8.dp else 6.dp).background(c.text.copy(alpha = 0.15f), BarShape)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(Color(0xFF8AB4F8), BarShape))
        }
    }
}

/** Round button for the panel's title row (Quick Settings: All Settings and Accounts). */
@Composable
private fun HeaderButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = LocalLauncherColors.current
    var focused by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(30.dp).onFocusChanged { focused = it.isFocused },
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = c.text.copy(alpha = 0.1f),
                contentColor = c.text,
                focusedContainerColor = c.focusFill,
                focusedContentColor = c.onFocusFill,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.align(Alignment.Center).size(16.dp))
        }
        Text(label, color = c.text, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp).alpha(if (focused) 1f else 0f))
    }
}

private class TileSpec(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onLongClick: (() -> Unit)? = null,
    val focus: FocusRequester? = null,
    /** Takes a whole row of the grid. */
    val wide: Boolean = false,
    val onClick: () -> Unit,
)

/** Settings tiles, one per line of a column; the strip's columns lay them out side by side. */
@Composable
private fun TileGrid(tiles: List<TileSpec>) {
    val cells = LocalCellRow.current
    tiles.forEach { Tile(it, if (cells) Modifier else Modifier.fillMaxWidth().padding(vertical = 4.dp)) }
}

@Composable
private fun Tile(spec: TileSpec, modifier: Modifier = Modifier) {
    if (LocalCellRow.current) {
        PillCell(
            spec.icon, spec.title, spec.subtitle, spec.onClick,
            modifier = spec.focus?.let { Modifier.focusRequester(it) } ?: Modifier,
            onLongClick = spec.onLongClick,
        )
        return
    }
    val c = LocalLauncherColors.current
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = spec.onClick,
        onLongClick = spec.onLongClick,
        modifier = modifier
            .height(54.dp)
            .then(spec.focus?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(TileShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = c.text.copy(alpha = 0.07f),
            contentColor = c.text,
            focusedContainerColor = c.focusFill,
            focusedContentColor = c.onFocusFill,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(spec.icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Column(Modifier.padding(start = 8.dp)) {
                FittedText(spec.title, focused, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                spec.subtitle?.let { FittedText(it, focused, fontSize = 10.5.sp, modifier = Modifier.padding(top = 1.dp)) }
            }
        }
    }
}

/**
 * Slim strip beside a panel. Moving onto it switches pages straight away, so it works like a
 * second blade: Right from Hew7 Settings opens Quick Settings, Left goes back.
 */
@Composable
private fun Blade(icon: ImageVector, label: String, alpha: Float, onEnter: () -> Unit) {
    val c = LocalLauncherColors.current
    // A panel opening or coming back into focus (after the Hew7 Helper or a settings page) can
    // hand focus to the blade by itself; only a Left/Right press moving onto it switches pages.
    Surface(
        onClick = onEnter,
        modifier = Modifier.width(40.dp).height(CellHeight + 64.dp).onFocusChanged {
            if (it.isFocused && SystemClock.uptimeMillis() - lastSideKeyAt < 400) onEnter()
        },
        shape = ClickableSurfaceDefaults.shape(PillShape),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), shape = PillShape),
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF0A0E18).copy(alpha = (0.25f + 0.35f * alpha).coerceIn(0.3f, 0.65f)),
            contentColor = c.subtext,
            focusedContainerColor = c.focusFill,
            focusedContentColor = c.onFocusFill,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            if (icon != Icons.Rounded.ChevronLeft) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.padding(top = 6.dp).size(16.dp))
            }
        }
    }
}

// ---------- small pieces ----------

/**
 * One line of text that stays inside its tile: cut off with "…" normally, and scrolling as
 * running text while its tile is selected, so the full label can still be read.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FittedText(
    text: String,
    selected: Boolean,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = if (selected) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = if (selected) modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 600) else modifier,
    )
}

/** A Machine Room figure in a pill cell, with a slim bar under it when it's a share of something. */
@Composable
private fun StatCell(icon: ImageVector, label: String, value: String, fraction: Float? = null) {
    val c = LocalLauncherColors.current
    PillCellContent(icon, label, value, focused = false, extra = fraction?.let { f ->
        {
            Box(Modifier.padding(top = 2.dp).width(56.dp).height(3.dp).background(c.text.copy(alpha = 0.2f), BarShape)) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(f.coerceIn(0f, 1f))
                        .background(if (f > 0.85f) Color(0xFFF5B14A) else c.text, BarShape),
                )
            }
        }
    })
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        color = LocalLauncherColors.current.text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    val c = LocalLauncherColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.subtext, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun UsageBar(fraction: Float) {
    val f = fraction.coerceIn(0f, 1f)
    val color = when {
        f > 0.9f -> Color(0xFFF28B82)
        f > 0.75f -> Color(0xFFFDD663)
        else -> Color(0xFF8AB4F8)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 6.dp)
            .height(6.dp)
            .background(LocalLauncherColors.current.text.copy(alpha = 0.15f), BarShape),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(f).background(color, BarShape))
    }
}

@Composable
private fun Speed(icon: ImageVector, bytesPerSec: Long) {
    val c = LocalLauncherColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(16.dp))
        Text(speed(bytesPerSec), color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
    }
}

internal fun gb(bytes: Long) = "%.1f GB".format(Locale.US, bytes / 1_073_741_824.0)
/** The number only ("1.6"), for "1.6 / 2.0 GB". */
internal fun gbShort(bytes: Long) = "%.1f".format(Locale.US, bytes / 1_073_741_824.0)
internal fun mb(bytes: Long) = "${bytes / 1_048_576} MB"

internal fun speed(bps: Long): String = when {
    bps >= 1_048_576 -> "%.1f MB/s".format(Locale.US, bps / 1_048_576.0)
    bps >= 1024 -> "${bps / 1024} KB/s"
    else -> "$bps B/s"
}

internal fun duration(ms: Long): String {
    val mins = ms / 60_000
    val h = mins / 60
    val d = h / 24
    return when {
        d > 0 -> "${d}d ${h % 24}h"
        h > 0 -> "${h}h ${mins % 60}m"
        else -> "${mins}m"
    }
}
