package dev.hew7.launcher.dream

import android.service.dreams.DreamService
import android.text.format.DateFormat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.compose.AsyncImage
import dev.hew7.launcher.data.DeviceInfo
import dev.hew7.launcher.data.GreetingScheduler
import dev.hew7.launcher.data.NetKind
import dev.hew7.launcher.data.NetworkLabel
import dev.hew7.launcher.data.Prefs
import dev.hew7.launcher.data.Sky
import dev.hew7.launcher.data.WallpaperRepository
import dev.hew7.launcher.data.WeatherRepository
import dev.hew7.launcher.ui.LauncherFont
import dev.hew7.launcher.ui.SceneText
import dev.hew7.launcher.ui.WeatherScene
import dev.hew7.launcher.ui.gradeFor
import dev.hew7.launcher.ui.moodFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Screensaver: drifting wallpaper, clock, and an animation that matches the current weather. */
class WeatherDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        window.decorView.setViewTreeLifecycleOwner(this)
        window.decorView.setViewTreeSavedStateRegistryOwner(this)
        setContentView(ComposeView(this).apply { setContent { DreamScreen(onSleep = ::sleep) } })
    }

    /**
     * Sleep After: the TV goes to standby. The Hew7 Helper can do that directly; without it,
     * ending the screensaver on its own lets the TV sleep, as it does after any screensaver.
     */
    private fun sleep() {
        if (!dev.hew7.launcher.CloseHelperService.sleepTv()) finish()
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDetachedFromWindow()
    }
}

/** Reads a system property. Blocking: call from Dispatchers.IO. */
private fun debugProp(name: String): String? {
    return runCatching {
        Runtime.getRuntime().exec(arrayOf("getprop", name)).inputStream.bufferedReader().use { it.readText().trim() }
    }.getOrNull()?.takeIf { it.isNotEmpty() }
}

@Composable
internal fun DreamScreen(onSleep: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    // Calm After: the picture holds still, dims a little, and is redrawn once a minute.
    // Sleep After: the TV goes to standby. Both count from when the screensaver started.
    var calm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val calmMin = prefs.screensaverCalmMinutes
        val sleepMin = prefs.screensaverSleepMinutes
        val started = System.currentTimeMillis()
        if (calmMin > 0) {
            delay(calmMin * 60_000L)
            calm = true
        }
        if (sleepMin > 0) {
            delay((started + sleepMin * 60_000L - System.currentTimeMillis()).coerceAtLeast(0))
            onSleep()
        }
    }
    val weatherRepo = remember { WeatherRepository(prefs) }
    var weather by remember { mutableStateOf(weatherRepo.cached()) }
    var wallpaper by remember { mutableStateOf(prefs.currentWallpaper) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        val w = weather
        if (w == null || System.currentTimeMillis() - w.fetchedAt > 30 * 60 * 1000L) {
            weatherRepo.refresh()?.let { weather = it }
        }
    }
    LaunchedEffect(calm) {
        while (true) {
            now = LocalDateTime.now()
            delay(if (calm) 60_000 else 5_000)
        }
    }
    // A new wallpaper every few minutes; it holds still once calm.
    LaunchedEffect(calm) {
        if (calm) return@LaunchedEffect
        val list = WallpaperRepository().fetch(prefs.wallpaperSource)
        if (list.isEmpty()) return@LaunchedEffect
        var i = list.indexOfFirst { it.url == wallpaper } + 1
        while (true) {
            delay(3 * 60 * 1000L)
            wallpaper = list[i % list.size].url
            i++
        }
    }

    // The slow zoom runs only while not calm; calm keeps the zoom where it is.
    var heldScale by remember { mutableStateOf(1.1f) }
    val scale = if (calm) {
        heldScale
    } else {
        val drift = rememberInfiniteTransition(label = "drift")
        val s by drift.animateFloat(
            1.05f, 1.15f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Reverse), label = "scale",
        )
        SideEffect { heldScale = s }
        s
    }

    // TV and network names; the network is re-checked every minute.
    val tvName = remember { DeviceInfo.tvName(context) }
    var network by remember { mutableStateOf(NetworkLabel(NetKind.OTHER, "")) }
    LaunchedEffect(Unit) {
        while (true) {
            network = withContext(Dispatchers.IO) { DeviceInfo.network(context) }
            delay(60_000)
        }
    }
    val greetings = remember { GreetingScheduler(context) }

    val w = weather
    // Preview hook: `adb shell setprop debug.hew7.sky SNOW` (and debug.hew7.night true/false)
    // forces a weather animation. Only someone with ADB access can set debug.* properties.
    var debugSky by remember { mutableStateOf<Sky?>(null) }
    var debugNight by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            debugSky = debugProp("debug.hew7.sky")?.let { v -> Sky.entries.firstOrNull { it.name == v } }
            debugNight = debugProp("debug.hew7.night")?.toBooleanStrictOrNull()
        }
    }
    val night = debugNight ?: w?.isNightAt(now.toLocalTime()) ?: (now.hour < 7 || now.hour >= 19)
    val is24h = DateFormat.is24HourFormat(context)
    val mood = moodFor(debugSky ?: w?.sky ?: Sky.CLEAR, night)
    // Weather Effects off: plain wallpaper and clock, no colour grade or animation.
    val effects = remember { prefs.weatherEffects }
    val grade = remember(mood, night, effects) { if (effects) ColorFilter.colorMatrix(gradeFor(mood, night)) else null }
    val text = SceneText(
        // Same greeting as the home screen (it changes every 2 hours or on an event).
        greeting = greetings.line(now, w, prefs.tempUnit, prefs.greetingStyle).text,
        time = now.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm")),
        date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
        weather = w?.let { it.temperature(prefs.tempUnit) + "  " + it.description + if (it.city.isNotBlank()) " · ${it.city}" else "" } ?: "",
        tvName = tvName,
        network = network,
    )

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // The same dynamic wallpaper as the home screen, colour-graded to the weather and slowly zooming.
        Crossfade(targetState = wallpaper, animationSpec = tween(2_000), label = "wallpaper") { url ->
            AsyncImage(
                model = url?.let { remember(it) { dev.hew7.launcher.data.wallpaperRequest(context, it) } },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = grade,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
        // Calm: a little dimmer (TVs that set their backlight from the picture use less power).
        if (calm) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        WeatherScene(mood = mood, text = text, font = LauncherFont.from(prefs.font).family, effects = effects, calm = calm)
    }
}
