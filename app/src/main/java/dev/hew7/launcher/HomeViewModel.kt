package dev.hew7.launcher

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dev.hew7.launcher.data.AppEntry
import dev.hew7.launcher.data.AppRepository
import dev.hew7.launcher.data.GreetingLine
import dev.hew7.launcher.data.GreetingScheduler
import dev.hew7.launcher.data.GreetingStyle
import dev.hew7.launcher.data.Greetings
import dev.hew7.launcher.data.Prefs
import dev.hew7.launcher.data.TempUnit
import dev.hew7.launcher.data.ThemeMode
import dev.hew7.launcher.data.TileStyle
import dev.hew7.launcher.ui.LauncherFont
import dev.hew7.launcher.data.Wallpaper
import dev.hew7.launcher.data.WallpaperRepository
import dev.hew7.launcher.data.WallpaperSource
import dev.hew7.launcher.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val appRepo = AppRepository(app)
    private val weatherRepo = WeatherRepository(prefs)
    private val wallpaperRepo = WallpaperRepository()

    val apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val playStore = MutableStateFlow<AppEntry?>(null)

    val now = MutableStateFlow(LocalDateTime.now())
    val weather = MutableStateFlow(weatherRepo.cached())
    val wallpaper = MutableStateFlow(prefs.currentWallpaper?.let { Wallpaper(it, null) })

    val themeMode = MutableStateFlow(prefs.themeMode)
    val tempUnit = MutableStateFlow(prefs.tempUnit)
    val wallpaperSource = MutableStateFlow(prefs.wallpaperSource)
    val wallpaperMinutes = MutableStateFlow(prefs.wallpaperMinutes)
    val tileStyle = MutableStateFlow(prefs.tileStyle)
    val font = MutableStateFlow(LauncherFont.from(prefs.font))
    val greetingStyle = MutableStateFlow(prefs.greetingStyle)
    private val greetings = GreetingScheduler(app)
    val showAppNames = MutableStateFlow(prefs.showAppNames)

    /** True while the Hew7 intro plays (once per TV start-up). */
    val showIntro = MutableStateFlow(false)
    val panelAlpha = MutableStateFlow(prefs.panelAlpha)
    val dockAlpha = MutableStateFlow(prefs.dockAlpha)

    /** Bumped when Home is pressed while the launcher is already showing. */
    val homePresses = MutableStateFlow(0)

    /**
     * The app Hew7 last opened. When you come back by leaving that app (Back), the dock highlights
     * it; a Home press clears it so the home screen rests with nothing selected.
     */
    val returnTo: StateFlow<String?> get() = _returnTo
    private val _returnTo = MutableStateFlow(prefs.returnTo)

    fun clearReturnTo() = setReturnTo(null)

    private fun setReturnTo(pkg: String?) {
        prefs.returnTo = pkg
        _returnTo.value = pkg
    }

    /** Whether the home screen's window has input focus; focus set before then is lost on TCL TVs. */
    val windowFocused = MutableStateFlow(false)

    /**
     * Set when the Hew7 Helper finishes closing apps, to show Running Apps again; the home screen
     * clears it once handled, so coming back from an app later doesn't reopen the list.
     */
    val openRunningApps = MutableStateFlow(false)

    /** Apps the Hew7 Helper just closed; Running Apps plays their rows away, then clears this. */
    val justClosed = MutableStateFlow<Set<String>>(emptySet())

    /** The Running Apps list as it was before closing, so the closed rows can animate out. */
    var runningBeforeClose: List<dev.hew7.launcher.data.RunningApp>? = null

    val weatherEffects = MutableStateFlow(prefs.weatherEffects)

    /** The TV's screensaver delay in ms (Settings.System.SCREEN_OFF_TIMEOUT). */
    val screensaverDelay = MutableStateFlow(readScreensaverDelay())

    val isDark: StateFlow<Boolean> = combine(themeMode, weather, now) { mode, w, t ->
        when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.AUTO -> w?.isNightAt(t.toLocalTime()) ?: (t.hour < 7 || t.hour >= 19)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** The greeting line; changes every 2 hours or when an event (weather, sunset, return) happens. */
    val greeting: StateFlow<GreetingLine> = combine(now, weather, tempUnit, greetingStyle) { t, w, unit, style ->
        greetings.line(t, w, unit, style)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GreetingLine("", Greetings.simple(LocalDateTime.now())))

    private var wallpaperJob: Job? = null

    init {
        refreshApps()
        viewModelScope.launch {
            while (true) {
                now.value = LocalDateTime.now()
                delay(5_000)
            }
        }
        viewModelScope.launch {
            while (true) {
                weatherRepo.refresh()?.let { weather.value = it }
                delay(30 * 60 * 1000L)
            }
        }
        restartWallpapers()
    }

    fun onPause() {
        prefs.lastSeen = System.currentTimeMillis()
    }

    fun onResume() {
        screensaverDelay.value = readScreensaverDelay()
        prefs.lastSeen.takeIf { it > 0 }?.let { greetings.onReturn(System.currentTimeMillis() - it, LocalDateTime.now()) }
        refreshApps()
        val w = weather.value
        if (w == null || System.currentTimeMillis() - w.fetchedAt > 30 * 60 * 1000L) {
            viewModelScope.launch { weatherRepo.refresh()?.let { weather.value = it } }
        }
    }

    fun onHomePressed() {
        clearReturnTo()
        homePresses.value++
    }

    fun onAppLaunched(pkg: String) = setReturnTo(pkg)

    fun setWeatherEffects(on: Boolean) {
        weatherEffects.value = on
        prefs.weatherEffects = on
    }

    private fun readScreensaverDelay(): Int = runCatching {
        Settings.System.getInt(getApplication<Application>().contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
    }.getOrDefault(600_000)

    /** Sets the TV's screensaver delay. False if Hew7 isn't allowed to change it. */
    fun setScreensaverDelay(ms: Int): Boolean {
        val ctx = getApplication<Application>()
        if (!Settings.System.canWrite(ctx)) return false
        val ok = runCatching { Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, ms) }.getOrDefault(false)
        screensaverDelay.value = readScreensaverDelay()
        return ok
    }

    fun refreshApps() {
        viewModelScope.launch {
            val loaded = appRepo.load()
            playStore.value = loaded.firstOrNull { it.pkg == AppRepository.PLAY_STORE }
            apps.value = AppRepository.ordered(loaded.filter { it.pkg != AppRepository.PLAY_STORE }, prefs.appOrder)
        }
    }

    fun launchIntent(pkg: String) = appRepo.launchIntent(pkg)

    /** Moves an app one place left (-1) or right (+1) and saves the new order. */
    fun moveApp(pkg: String, delta: Int) {
        val list = apps.value.toMutableList()
        val from = list.indexOfFirst { it.pkg == pkg }
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        apps.value = list
        prefs.appOrder = list.map { it.pkg }
    }

    fun setShowAppNames(value: Boolean) {
        showAppNames.value = value
        prefs.showAppNames = value
    }

    fun setPanelAlpha(value: Float) {
        panelAlpha.value = value
        prefs.panelAlpha = value
    }

    fun setDockAlpha(value: Float) {
        dockAlpha.value = value
        prefs.dockAlpha = value
    }

    fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
        prefs.themeMode = mode
    }

    fun setTileStyle(style: TileStyle) {
        tileStyle.value = style
        prefs.tileStyle = style
    }

    fun setGreetingStyle(style: GreetingStyle) {
        greetingStyle.value = style
        prefs.greetingStyle = style
    }

    fun setFont(value: LauncherFont) {
        font.value = value
        prefs.font = value.name
    }

    fun setTempUnit(unit: TempUnit) {
        tempUnit.value = unit
        prefs.tempUnit = unit
    }

    fun setWallpaperSource(source: WallpaperSource) {
        wallpaperSource.value = source
        prefs.wallpaperSource = source
        restartWallpapers()
    }

    fun setWallpaperMinutes(minutes: Int) {
        wallpaperMinutes.value = minutes
        prefs.wallpaperMinutes = minutes
        restartWallpapers()
    }

    /** Cycles through wallpapers, preloading each one so the crossfade never shows a blank frame. */
    private fun restartWallpapers() {
        wallpaperJob?.cancel()
        wallpaperJob = viewModelScope.launch {
            val ctx = getApplication<Application>()
            val loader = SingletonImageLoader.get(ctx)
            var list = emptyList<Wallpaper>()
            var listFetchedAt = 0L
            var index = 0
            while (true) {
                if (list.isEmpty() || System.currentTimeMillis() - listFetchedAt > 6 * 60 * 60 * 1000L) {
                    list = wallpaperRepo.fetch(wallpaperSource.value)
                    listFetchedAt = System.currentTimeMillis()
                    index = list.indexOfFirst { it.url == wallpaper.value?.url }.coerceAtLeast(-1) + 1
                }
                val next = list[index % list.size]
                val loaded = runCatching {
                    loader.execute(dev.hew7.launcher.data.wallpaperRequest(ctx, next.url)) is SuccessResult
                }.getOrDefault(false)
                if (!loaded) {
                    // Usually the internet isn't up yet (just after the TV starts): try again soon,
                    // and fetch a fresh list in case the first fetch failed too.
                    list = emptyList()
                    delay(20_000)
                    continue
                }
                index++
                wallpaper.value = next
                prefs.currentWallpaper = next.url
                delay(wallpaperMinutes.value * 60 * 1000L)
            }
        }
    }
}
