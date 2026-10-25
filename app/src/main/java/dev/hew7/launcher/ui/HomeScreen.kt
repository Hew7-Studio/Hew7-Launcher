package dev.hew7.launcher.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Input
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import dev.hew7.launcher.RemoteKeys
import dev.hew7.launcher.data.RemoteAction
import dev.hew7.launcher.data.wallpaperRequest
import dev.hew7.launcher.dream.ScreensaverActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.hew7.launcher.HomeViewModel
import dev.hew7.launcher.data.AppEntry
import dev.hew7.launcher.data.GreetingLine
import dev.hew7.launcher.data.TempUnit
import dev.hew7.launcher.data.Weather
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private enum class Panel { NONE, INPUTS, SYSTEM, SETTINGS }

/** Width of a side panel plus its outer margin; Settings also has the Hew7 blade beside it. */
private val PanelSpace = 324.dp
private val BladeSpace = 60.dp

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val context = LocalContext.current
    val dark by vm.isDark.collectAsStateWithLifecycle()
    val font by vm.font.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val playStore by vm.playStore.collectAsStateWithLifecycle()
    val tileStyle by vm.tileStyle.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val wallpaper by vm.wallpaper.collectAsStateWithLifecycle()
    val tempUnit by vm.tempUnit.collectAsStateWithLifecycle()
    val homePresses by vm.homePresses.collectAsStateWithLifecycle()
    val greeting by vm.greeting.collectAsStateWithLifecycle()
    val panelAlpha by vm.panelAlpha.collectAsStateWithLifecycle()
    val dockAlpha by vm.dockAlpha.collectAsStateWithLifecycle()
    val showNames by vm.showAppNames.collectAsStateWithLifecycle()
    val showIntro by vm.showIntro.collectAsStateWithLifecycle()

    var appMenu by remember { mutableStateOf<AppEntry?>(null) }
    var panel by remember { mutableStateOf(Panel.NONE) }
    var movingPkg by remember { mutableStateOf<String?>(null) }

    // Resting state: an invisible focus spot, so nothing on screen is selected. Up from it goes to
    // Inputs, Down to the first app; Back anywhere on the home screen returns to it.
    val restFocus = remember { FocusRequester() }
    val inputsFocus = remember { FocusRequester() }
    var resting by remember { mutableStateOf(false) }
    var dockFocus by remember { mutableStateOf<DockFocus?>(null) }
    var dockFocusId by remember { mutableIntStateOf(0) }
    val openRunning by vm.openRunningApps.collectAsStateWithLifecycle()
    var runningFirst by remember { mutableStateOf(false) }

    BackHandler(enabled = movingPkg != null) { movingPkg = null }
    BackHandler(enabled = movingPkg == null && !resting) { runCatching { restFocus.requestFocus() } }

    // Coming back by leaving an app (Back) highlights that app. Starting up, pressing Home, or
    // returning any other way rests with nothing selected.
    LaunchedEffect(homePresses) {
        delay(120) // let a Home press (onNewIntent) land first
        vm.windowFocused.first { it } // focus given before the window has focus does not stick
        val back = vm.returnTo.value
        vm.clearReturnTo()
        if (back != null && apps.any { it.pkg == back }) {
            dockFocus = DockFocus(back, ++dockFocusId)
            return@LaunchedEffect
        }
        repeat(20) {
            runCatching { restFocus.requestFocus() }
            if (resting) return@LaunchedEffect
            delay(50)
        }
    }

    // Home closes whatever is open. Only a new press counts, not the screen being rebuilt.
    var seenHomePresses by remember { mutableIntStateOf(homePresses) }
    LaunchedEffect(homePresses) {
        if (homePresses != seenHomePresses) {
            seenHomePresses = homePresses
            panel = Panel.NONE
            appMenu = null
            movingPkg = null
        }
    }

    // The Hew7 Helper finished closing apps: show Running Apps again, once.
    LaunchedEffect(openRunning) {
        if (openRunning) {
            vm.openRunningApps.value = false
            runningFirst = true
            panel = Panel.SYSTEM
        }
    }

    // The TV's own inputs menu (TCL's input bar here); Hew7's panel if there is none.
    val openInputs = {
        val tvInputs = Intent("com.android.tv.action.VIEW_INPUTS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = context.packageManager.resolveActivity(tvInputs, 0) != null &&
            runCatching { context.startActivity(tvInputs) }.isSuccess
        if (!opened) panel = Panel.INPUTS
    }

    // Remote buttons (see RemoteKeys). Pressing a panel's button again closes it.
    var selectedPkg by remember { mutableStateOf<String?>(null) }
    var settingsQuick by remember { mutableStateOf(false) }
    val currentApps by rememberUpdatedState(apps)
    DisposableEffect(Unit) {
        RemoteKeys.runner = { choice ->
            fun toggle(p: Panel, quick: Boolean = false, running: Boolean = false) {
                val same = panel == p && (p != Panel.SETTINGS || settingsQuick == quick) && (p != Panel.SYSTEM || runningFirst == running)
                appMenu = null
                if (same) {
                    panel = Panel.NONE
                    runningFirst = false
                } else {
                    settingsQuick = quick
                    runningFirst = running
                    panel = p
                }
            }
            when (choice.action) {
                RemoteAction.HEW7_SETTINGS -> toggle(Panel.SETTINGS)
                RemoteAction.QUICK_SETTINGS -> toggle(Panel.SETTINGS, quick = true)
                RemoteAction.MACHINE_ROOM -> toggle(Panel.SYSTEM)
                RemoteAction.RUNNING_APPS -> toggle(Panel.SYSTEM, running = true)
                RemoteAction.INPUTS -> {
                    panel = Panel.NONE
                    openInputs()
                }
                RemoteAction.DOCK -> {
                    panel = Panel.NONE
                    appMenu = null
                    currentApps.firstOrNull()?.let { dockFocus = DockFocus(it.pkg, ++dockFocusId) }
                }
                // The selected app's menu; with no app selected, Hew7 Settings.
                RemoteAction.APP_MENU -> {
                    val app = currentApps.firstOrNull { it.pkg == selectedPkg }
                    if (panel == Panel.NONE && appMenu == null && app != null) appMenu = app
                    else if (appMenu != null) appMenu = null
                    else toggle(Panel.SETTINGS)
                }
                RemoteAction.SCREENSAVER -> context.startActivity(
                    Intent(context, ScreensaverActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                RemoteAction.OPEN_APP -> choice.pkg?.let { pkg ->
                    vm.launchIntent(pkg)?.let { if (context.startFirst(it)) vm.onAppLaunched(pkg) }
                }
                RemoteAction.DEFAULT, RemoteAction.NOTHING -> Unit
            }
        }
        onDispose { RemoteKeys.runner = null }
    }

    // Text and buttons over the wallpaper pick light or dark ink from what's behind them.
    var ink by remember { mutableStateOf(WallpaperInk.forTheme(dark)) }
    LaunchedEffect(wallpaper?.url, dark) {
        ink = WallpaperInk.measure(context, wallpaper?.url) ?: WallpaperInk.forTheme(dark)
    }

    LauncherTheme(dark, font) {
        val c = LocalLauncherColors.current
        Box(
            Modifier
                .fillMaxSize()
                .background(if (c.dark) Color.Black else Color(0xFFE9EBF0)),
        ) {
            Box(
                Modifier
                    .size(1.dp)
                    .focusRequester(restFocus)
                    .onFocusChanged { resting = it.isFocused }
                    .onPreviewKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (e.key) {
                            Key.DirectionUp -> { runCatching { inputsFocus.requestFocus() }; true }
                            Key.DirectionDown -> {
                                apps.firstOrNull()?.let { dockFocus = DockFocus(it.pkg, ++dockFocusId) }
                                true
                            }
                            Key.DirectionLeft, Key.DirectionRight -> true
                            else -> false
                        }
                    }
                    .focusable(),
            )
            // The wallpaper shows as it is, edge to edge; the text's own shadow keeps it readable.
            // Its own layer, so the dock animating above it doesn't redraw it.
            Box(Modifier.fillMaxSize().graphicsLayer()) { WallpaperLayer(wallpaper?.url) }

            // While a side panel is open, everything reflows into the space to its left.
            val panelSpace by animateDpAsState(
                when (panel) {
                    Panel.NONE -> 0.dp
                    Panel.INPUTS -> PanelSpace
                    // Machine Room's Running Apps page has a blade too; the dock keeps one size for both.
                    Panel.SYSTEM, Panel.SETTINGS -> PanelSpace + BladeSpace
                },
                spring(dampingRatio = 0.85f, stiffness = 300f),
                label = "panelSpace",
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 40.dp, end = 40.dp + panelSpace, top = 24.dp, bottom = 20.dp),
            ) {
                TopBar(
                    now = now,
                    weather = weather,
                    unit = tempUnit,
                    compact = panel != Panel.NONE,
                    greeting = greeting,
                    ink = ink,
                    inputsFocus = inputsFocus,
                    onInputs = openInputs,
                    onSystem = { panel = Panel.SYSTEM },
                    onSettings = { settingsQuick = false; panel = Panel.SETTINGS },
                )
                Spacer(Modifier.weight(1f))
                AppSection(
                    apps = apps,
                    playStore = playStore,
                    tileStyle = tileStyle,
                    dockAlpha = dockAlpha,
                    movingPkg = movingPkg,
                    homePresses = homePresses,
                    focusRequest = dockFocus,
                    showNames = showNames,
                    onLaunch = { pkg ->
                        vm.launchIntent(pkg)?.let { if (context.startFirst(it)) vm.onAppLaunched(pkg) }
                    },
                    onLongPress = { app -> appMenu = app },
                    onMove = vm::moveApp,
                    onMoveDone = { movingPkg = null },
                    onSelected = { selectedPkg = it },
                )
            }

            if (showIntro) {
                BootIntroOverlay(font.family) {
                    BootIntro.markShown(context)
                    vm.showIntro.value = false
                }
            }
        }

        val close = { panel = Panel.NONE; runningFirst = false }
        when (panel) {
            Panel.INPUTS -> InputsPanel(panelAlpha, onClose = close)
            Panel.SYSTEM -> key(runningFirst) { MachineRoomPanel(vm, panelAlpha, startOnRunningApps = runningFirst, onClose = close) }
            Panel.SETTINGS -> key(settingsQuick) { SettingsPanel(vm, panelAlpha, startOnQuick = settingsQuick, onClose = close) }
            Panel.NONE -> Unit
        }

        appMenu?.let { app ->
            AppMenuDialog(
                app = app,
                onInfo = {
                    appMenu = null
                    context.startFirst(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}")))
                },
                onMove = { appMenu = null; movingPkg = app.pkg },
                onUninstall = {
                    appMenu = null
                    context.startFirst(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}")))
                },
                onDismiss = { appMenu = null },
            )
        }
    }
}

/**
 * Full-screen wallpaper that crossfades when it changes. It stays still between changes:
 * a constantly moving full-screen image costs too much on TV chips (the screensaver still drifts).
 */
@Composable
private fun WallpaperLayer(url: String?) {
    val context = LocalContext.current
    Crossfade(targetState = url, animationSpec = tween(1_500), label = "wallpaper") { target ->
        if (target != null) {
            AsyncImage(
                model = remember(target) { wallpaperRequest(context, target) },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Clock, date, weather and greeting on the left; Inputs, System and Settings buttons on the right. */
@Composable
private fun TopBar(
    now: LocalDateTime,
    weather: Weather?,
    unit: TempUnit,
    compact: Boolean,
    greeting: GreetingLine,
    ink: WallpaperInk,
    inputsFocus: FocusRequester,
    onInputs: () -> Unit,
    onSystem: () -> Unit,
    onSettings: () -> Unit,
) {
    val theme = LocalLauncherColors.current
    val squeeze = spring<Float>(dampingRatio = 0.75f, stiffness = 320f)
    // The clock block shrinks a little while a side panel needs the room.
    val k by animateFloatAsState(if (compact) 0.82f else 1f, squeeze, label = "compact")
    // The buttons fold away while a panel is open. They stay in the layout (just zero width and
    // invisible) so the button you pressed keeps focus and Back returns to it.
    val shown by animateFloatAsState(if (compact) 0f else 1f, squeeze, label = "buttons")
    val is24h = DateFormat.is24HourFormat(LocalContext.current)
    val time = now.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm"))

    // Its own layer: the haloed text is costly to draw, so it's only redrawn when it changes.
    Row(Modifier.fillMaxWidth().graphicsLayer(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            val c = ink.colors(theme, left = true)
            val darkInk = ink.leftDark
            Row(verticalAlignment = Alignment.Bottom) {
                InkText(time, c.text, darkInk, fontSize = 46.sp * k, fontWeight = FontWeight.Light, lineHeight = 50.sp * k)
                if (!is24h) {
                    InkText(
                        now.format(DateTimeFormatter.ofPattern("a")),
                        c.subtext,
                        darkInk,
                        fontSize = 15.sp * k,
                        modifier = Modifier.padding(start = 5.dp, bottom = 8.dp * k),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                InkText(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), c.subtext, darkInk, fontSize = 14.sp * k)
                if (weather != null) {
                    Text("   ${weather.emoji}  ", fontSize = 14.sp * k)
                    InkText(weather.temperature(unit), c.text, darkInk, fontSize = 14.sp * k, fontWeight = FontWeight.Bold)
                    InkText(
                        "  ${weather.description}" + if (weather.city.isNotBlank()) " · ${weather.city}" else "",
                        c.subtext,
                        darkInk,
                        fontSize = 14.sp * k,
                    )
                }
            }
            Spacer(Modifier.height(6.dp * k))
            AnimatedGreeting(greeting, 19.sp * k, c.text, darkInk)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                // The spring overshoots slightly past 0 and 1, so clamp before using it as a size.
                .graphicsLayer { alpha = shown.coerceIn(0f, 1f) }
                .layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout((p.width * shown.coerceIn(0f, 1f)).roundToInt(), p.height) { p.place(0, 0) }
                },
        ) {
            CompositionLocalProvider(LocalLauncherColors provides ink.colors(theme, left = false)) {
                CircleButton(Icons.Rounded.Input, "Inputs", ink.rightDark, onInputs, Modifier.focusRequester(inputsFocus))
                CircleButton(Icons.Rounded.Memory, "Machine Room", ink.rightDark, onSystem)
                CircleButton(Icons.Rounded.Settings, "Settings", ink.rightDark, onSettings)
            }
        }
    }
}

/**
 * The greeting line chosen by GreetingScheduler. A new line slides up into place only when the
 * line itself changes (every 2 hours, or on an event); live text like sunset minutes updates in place.
 */
@Composable
private fun AnimatedGreeting(greeting: GreetingLine, fontSize: TextUnit, color: Color, darkInk: Boolean) {
    AnimatedContent(
        targetState = greeting,
        contentKey = { it.key },
        transitionSpec = {
            (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 200f)) { it } + fadeIn(tween(600))) togetherWith
                (slideOutVertically(tween(400)) { -it } + fadeOut(tween(300)))
        },
        label = "greeting",
    ) { line ->
        InkText(line.text, color, darkInk, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CircleButton(icon: ImageVector, label: String, darkInk: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalLauncherColors.current
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.14f else 1f, spring(dampingRatio = 0.5f, stiffness = 420f), label = "scale")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = modifier
                .size(40.dp)
                .onFocusChanged { focused = it.isFocused }
                .graphicsLayer { scaleX = scale; scaleY = scale },
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = c.glass,
                contentColor = c.text,
                focusedContainerColor = c.focusFill,
                focusedContentColor = c.onFocusFill,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.align(Alignment.Center).size(21.dp))
        }
        InkText(
            label,
            c.text,
            darkInk,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp).alpha(if (focused) 1f else 0f),
        )
    }
}

/** Starts the first intent that some app can handle. */
internal fun Context.startFirst(vararg intents: Intent): Boolean {
    for (intent in intents) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        } catch (e: ActivityNotFoundException) {
            continue
        } catch (e: SecurityException) {
            continue
        }
    }
    Toast.makeText(this, "Couldn't open that on this TV", Toast.LENGTH_SHORT).show()
    return false
}
