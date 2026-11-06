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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.tv.material3.Border
import dev.hew7.launcher.data.Sky
import dev.hew7.launcher.data.skyOf
import dev.hew7.launcher.data.temperature
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
    val media by vm.media.collectAsStateWithLifecycle()
    val lastUsed by vm.lastUsed.collectAsStateWithLifecycle()

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

    // Spotlight: the selected app; with nothing selected, whatever is playing or paused right now.
    val nowMs = System.currentTimeMillis()
    val selectedApp = apps.firstOrNull { it.pkg == selectedPkg }
    val spotlight = when {
        selectedApp != null -> spotlightFor(selectedApp, media[selectedApp.pkg], lastUsed[selectedApp.pkg], nowMs)
        else -> media.values
            .filter { it.live && it.title.isNotBlank() && apps.any { a -> a.pkg == it.pkg } }
            .maxWithOrNull(compareBy({ it.playing }, { it.updatedAt }))
            ?.let { m -> spotlightFor(apps.first { it.pkg == m.pkg }, m, lastUsed[m.pkg], nowMs) }
    }
    val showSpotlight = panel == Panel.NONE && spotlight != null

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

            // The picture follows the day: warm around sunrise and sunset, dimmer and warmer at night.
            val ambient by animateColorAsState(ambientTint(now, weather), tween(2_500), label = "ambient")
            Box(Modifier.fillMaxSize().background(ambient))

            // A soft glow in the selected app's colour, behind the Spotlight.
            val glow by animateColorAsState(
                if (showSpotlight) spotlight!!.accent.brighter().copy(alpha = if (c.dark) 0.5f else 0.28f) else Color.Transparent,
                tween(700),
                label = "glow",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        // A wide, soft oval behind Up next (as in the design: 900 × 520 at the left).
                        val center = Offset(size.width * 0.23f, size.height * 0.56f)
                        val r = size.width * 0.36f
                        val brush = Brush.radialGradient(
                            0f to glow, 0.45f to glow.copy(alpha = glow.alpha * 0.55f), 1f to Color.Transparent,
                            center = center, radius = r,
                        )
                        onDrawBehind {
                            scale(scaleX = 1f, scaleY = 0.58f, pivot = center) { drawCircle(brush, r, center) }
                        }
                    },
            )

            // Panels are a glass strip across the top now, so the home screen stays where it is
            // beneath them (only the clock area is covered; Up next steps aside).
            val panelSpace = 0.dp
            // The layout follows the Hew7 2.0 design: clock top left, weather pill top right, Up next
            // above a free-floating dock. The same at every time of day.
            Box(Modifier.fillMaxSize().padding(end = panelSpace)) {
                // A soft darkening (or lightening, for dark ink) behind the text, so its shadow can stay gentle.
                TextScrim(ink)
                // The panels' glass strip covers the top of the screen, so the clock steps out of
                // its way rather than showing through or running underneath it.
                val clockShown by animateFloatAsState(if (panel == Panel.NONE) 1f else 0f, tween(300), label = "clockShown")
                ClockBlock(
                    now = now,
                    greeting = greeting,
                    ink = ink,
                    compact = false,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 54.dp, top = 28.dp).graphicsLayer { alpha = clockShown },
                )
                TopRight(
                    weather = weather,
                    unit = tempUnit,
                    now = now,
                    ink = ink,
                    hidden = panel != Panel.NONE,
                    weatherFocus = inputsFocus,
                    onInputs = openInputs,
                    onSystem = { panel = Panel.SYSTEM },
                    onSettings = { settingsQuick = false; panel = Panel.SETTINGS },
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 48.dp, top = 36.dp),
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                ) {
                val spotAlpha by animateFloatAsState(if (showSpotlight) 1f else 0f, tween(450), label = "spotAlpha")
                Spotlight(
                    item = spotlight,
                    ink = ink,
                    modifier = Modifier.padding(start = 54.dp, bottom = 22.dp).graphicsLayer { alpha = spotAlpha },
                )
                Box {
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
                }
            }

            if (showIntro) {
                BootIntroOverlay(font.family) {
                    BootIntro.markShown(context)
                    vm.showIntro.value = false
                }
            }
        }

        val close = { panel = Panel.NONE; runningFirst = false }
        // Panels are smoky glass with light letters in either theme, from the start (pages pick
        // their colours before they're inside the glass).
        CompositionLocalProvider(LocalLauncherColors provides panelColors(LocalLauncherColors.current)) {
            when (panel) {
                Panel.INPUTS -> InputsPanel(panelAlpha, onClose = close)
                Panel.SYSTEM -> key(runningFirst) { GlassMachineRoomPanel(vm, panelAlpha, onClose = close) }
                Panel.SETTINGS -> key(settingsQuick) { GlassSettingsPanel(vm, panelAlpha, startOnQuick = settingsQuick, onClose = close) }
                Panel.NONE -> Unit
            }
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

/**
 * A wash over the wallpaper that follows the day: warm in the hour around sunrise and sunset,
 * dimmer and a little warmer at night. Clear in the daytime.
 */
private fun ambientTint(now: LocalDateTime, weather: Weather?): Color {
    val t = now.toLocalTime()
    val near = { at: LocalDateTime? -> at != null && kotlin.math.abs(java.time.Duration.between(at.toLocalTime(), t).toMinutes()) <= 45 }
    return when {
        near(weather?.sunset) || near(weather?.sunrise) -> Color(0x26FF8A3D)
        now.hour >= 23 || now.hour < 5 -> Color(0x59120A04)
        weather?.isNightAt(t) ?: (now.hour < 7 || now.hour >= 19) -> Color(0x40000814)
        else -> Color.Transparent
    }
}

/**
 * Soft shade behind the clock (top left) and Up next (bottom left): dark under light text, light
 * under dark text. It lets the text's own shadow stay gentle instead of outlining every letter.
 */
@Composable
private fun TextScrim(ink: WallpaperInk) {
    val tone by animateColorAsState(if (ink.leftDark) Color.White.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.34f), tween(1_200), label = "scrim")
    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val top = Offset(size.width * 0.12f, size.height * 0.2f)
                val bottom = Offset(size.width * 0.18f, size.height * 0.62f)
                val r = size.width * 0.34f
                val a = Brush.radialGradient(0f to tone, 0.55f to tone.copy(alpha = tone.alpha * 0.45f), 1f to Color.Transparent, center = top, radius = r)
                val b = Brush.radialGradient(0f to tone, 0.55f to tone.copy(alpha = tone.alpha * 0.45f), 1f to Color.Transparent, center = bottom, radius = r)
                onDrawBehind {
                    scale(scaleX = 1f, scaleY = 0.62f, pivot = top) { drawCircle(a, r, top) }
                    scale(scaleX = 1f, scaleY = 0.55f, pivot = bottom) { drawCircle(b, r, bottom) }
                }
            },
    )
}

/** The clock, the date and the greeting: a thin Outfit clock over the date, with the greeting sliding up beneath. */
@Composable
private fun ClockBlock(
    now: LocalDateTime,
    greeting: GreetingLine,
    ink: WallpaperInk,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val theme = LocalLauncherColors.current
    // The clock block shrinks a little while a side panel needs the room.
    val k by animateFloatAsState(if (compact) 0.8f else 1f, spring(dampingRatio = 0.75f, stiffness = 320f), label = "compact")
    val is24h = DateFormat.is24HourFormat(LocalContext.current)
    val time = now.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm"))
    val c = ink.colors(theme, left = true)
    val darkInk = ink.leftDark
    val clockSize = 78.sp * k

    // Its own layer: the haloed text is costly to draw, so it's only redrawn when it changes.
    Column(modifier.graphicsLayer()) {
        Row(verticalAlignment = Alignment.Bottom) {
            InkText(
                time, c.text, darkInk,
                fontSize = clockSize, fontWeight = FontWeight.ExtraLight, lineHeight = clockSize,
                fontFamily = DisplayFont, letterSpacing = (-1.5).sp,
            )
            if (!is24h) {
                InkText(
                    now.format(DateTimeFormatter.ofPattern("a")),
                    c.subtext,
                    darkInk,
                    fontSize = 16.sp * k,
                    fontFamily = DisplayFont,
                    modifier = Modifier.padding(start = 6.dp, bottom = 10.dp * k),
                )
            }
        }
        InkText(
            now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            c.subtext, darkInk, fontSize = 13.sp * k,
            modifier = Modifier.padding(top = 8.dp * k),
        )
        Spacer(Modifier.height(6.dp * k))
        AnimatedGreeting(greeting, 21.sp * k, c.text, darkInk)
    }
}

/**
 * Top right: the glass weather pill, which opens to the next few hours when selected (Up from
 * rest). Inputs, Machine Room and Settings sit to its left and show only while you're up here.
 */
@Composable
private fun TopRight(
    weather: Weather?,
    unit: TempUnit,
    now: LocalDateTime,
    ink: WallpaperInk,
    /** A side panel is open: the buttons fold away (they stay focusable so Back returns to them). */
    hidden: Boolean,
    weatherFocus: FocusRequester,
    onInputs: () -> Unit,
    onSystem: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalLauncherColors.current
    var inGroup by remember { mutableStateOf(false) }
    val showButtons by animateFloatAsState(if (inGroup && !hidden) 1f else 0f, spring(dampingRatio = 0.75f, stiffness = 320f), label = "buttons")
    val pillShown by animateFloatAsState(if (hidden) 0f else 1f, tween(300), label = "pill")
    Row(
        modifier.onFocusChanged { inGroup = it.hasFocus }.graphicsLayer { alpha = pillShown },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(top = 6.dp)
                // The spring overshoots slightly past 0 and 1, so clamp before using it as a size.
                .graphicsLayer { alpha = showButtons.coerceIn(0f, 1f) }
                .layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout((p.width * showButtons.coerceIn(0f, 1f)).roundToInt(), p.height) { p.place(0, 0) }
                },
        ) {
            CompositionLocalProvider(LocalLauncherColors provides ink.colors(theme, left = false)) {
                CircleButton(Icons.Rounded.Input, "Inputs", ink.rightDark, onInputs)
                CircleButton(Icons.Rounded.Memory, "Machine Room", ink.rightDark, onSystem)
                CircleButton(Icons.Rounded.Settings, "Settings", ink.rightDark, onSettings)
            }
        }
        WeatherPill(weather, unit, now, ink, Modifier.focusRequester(weatherFocus))
    }
}

/** The glass weather pill: temperature and sky; selected, it opens to the next 5 hours and tomorrow. */
@Composable
private fun WeatherPill(weather: Weather?, unit: TempUnit, now: LocalDateTime, ink: WallpaperInk, modifier: Modifier = Modifier) {
    val theme = LocalLauncherColors.current
    val c = ink.colors(theme, left = false)
    var open by remember { mutableStateOf(false) }
    // Smoky glass under light text, frosted glass under dark text.
    val glass by animateColorAsState(if (ink.rightDark) Color(0x73FFFFFF) else Color(0x660A0E18), tween(900), label = "pillGlass")
    Surface(
        onClick = { open = !open },
        modifier = modifier.onFocusChanged { open = it.isFocused }.animateContentSize(spring(dampingRatio = 0.85f, stiffness = 380f)),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = glass,
            contentColor = c.text,
            focusedContainerColor = glass,
            focusedContentColor = c.text,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), shape = RoundedCornerShape(18.dp)),
            focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = RoundedCornerShape(18.dp)),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 10.dp).width(if (open) 300.dp else 150.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(skyIcon(weather), contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    weather?.temperature(unit) ?: "--°",
                    fontFamily = DisplayFont, fontWeight = FontWeight.Light, fontSize = 22.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(weather?.description ?: "", fontSize = 11.sp, color = c.subtext, maxLines = 1)
            }
            if (open && weather != null) {
                val hours = weather.hours.filter { it.time.isAfter(now.minusMinutes(30)) }.take(5)
                if (hours.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp).background(Color.White.copy(alpha = 0.2f)).height(1.dp),
                    ) {}
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        hours.forEachIndexed { i, h ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (i == 0) "Now" else h.time.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h a")), fontSize = 10.sp, color = c.subtext)
                                Icon(skyIcon(skyOf(h.code), night = h.time.hour < 7 || h.time.hour >= 19), contentDescription = null, modifier = Modifier.padding(top = 4.dp).size(16.dp))
                                Text(temperature(h.tempC, unit), fontFamily = DisplayFont, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
                weather.tomorrow?.let { t ->
                    Text(
                        "Tomorrow: ${skyWords(skyOf(t.code))}, ${temperature(t.maxC, unit)} / ${temperature(t.minC, unit)}",
                        fontSize = 10.sp, color = c.subtext, modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }
}

private fun skyIcon(weather: Weather?): ImageVector =
    if (weather == null) Icons.Rounded.Cloud else skyIcon(weather.sky, night = !weather.isDay)

internal fun skyIcon(sky: Sky, night: Boolean): ImageVector = when (sky) {
    Sky.CLEAR -> if (night) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny
    Sky.PARTLY -> if (night) Icons.Rounded.NightsStay else Icons.Rounded.WbCloudy
    Sky.CLOUDY -> Icons.Rounded.Cloud
    Sky.FOG -> Icons.Rounded.Dehaze
    Sky.RAIN -> Icons.Rounded.Grain
    Sky.SNOW -> Icons.Rounded.AcUnit
    Sky.STORM -> Icons.Rounded.Thunderstorm
}

private fun skyWords(sky: Sky): String = when (sky) {
    Sky.CLEAR -> "sunny"
    Sky.PARTLY -> "sunny spells"
    Sky.CLOUDY -> "cloudy"
    Sky.FOG -> "foggy"
    Sky.RAIN -> "rain"
    Sky.SNOW -> "snow"
    Sky.STORM -> "thunderstorms"
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
        InkText(line.text, color, darkInk, fontSize = fontSize, fontWeight = FontWeight.Light, fontFamily = DisplayFont)
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
