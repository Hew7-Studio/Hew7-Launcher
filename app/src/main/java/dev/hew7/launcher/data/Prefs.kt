package dev.hew7.launcher.data

import android.content.Context

enum class ThemeMode(val label: String, val shortLabel: String) {
    AUTO("Auto (Sunrise/Sunset)", "Auto"),
    LIGHT("Light", "Light"),
    DARK("Dark", "Dark"),
}
enum class TempUnit(val label: String) { CELSIUS("°C"), FAHRENHEIT("°F") }
enum class WallpaperSource(val label: String) { BING("Bing Daily"), PHOTOS("Random Photos") }
enum class TileStyle(val label: String) { ICONS("Icons"), BANNERS("Banners") }
enum class GreetingStyle(val label: String) { PLAYFUL("Playful"), SIMPLE("Simple") }

/** Small wrapper over SharedPreferences, shared by the launcher and the screensaver. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("hew7", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = enumOr(sp.getString("theme", null), ThemeMode.AUTO)
        set(v) = putString("theme", v.name)

    var tempUnit: TempUnit
        get() = enumOr(sp.getString("temp_unit", null), TempUnit.CELSIUS)
        set(v) = putString("temp_unit", v.name)

    var wallpaperSource: WallpaperSource
        get() = enumOr(sp.getString("wp_source", null), WallpaperSource.BING)
        set(v) = putString("wp_source", v.name)

    var tileStyle: TileStyle
        get() = enumOr(sp.getString("tile_style", null), TileStyle.ICONS)
        set(v) = putString("tile_style", v.name)

    var greetingStyle: GreetingStyle
        get() = enumOr(sp.getString("greeting", null), GreetingStyle.PLAYFUL)
        set(v) = putString("greeting", v.name)

    /** The app last opened from the dock, kept so Back can highlight it even if Hew7 was closed meanwhile. */
    var returnTo: String?
        get() = sp.getString("return_to", null)
        set(v) = sp.edit().putString("return_to", v).apply()

    /** What a remote button does (a RemoteChoice in text form), by the button's name; null: its default. */
    fun remoteButton(name: String): String? = sp.getString("remote_$name", null)

    fun setRemoteButton(name: String, choice: String?) = putString("remote_$name", choice)

    /** Remote buttons Hew7 has seen pressed, in the order first seen. */
    var remoteSeen: List<String>
        get() = sp.getString("remote_seen", "").orEmpty().split(',').filter { it.isNotBlank() }
        set(v) = putString("remote_seen", v.joinToString(","))

    /** Weather animation on the screensaver (off: plain wallpaper and clock). */
    var weatherEffects: Boolean
        get() = sp.getBoolean("weather_fx", true)
        set(v) = sp.edit().putBoolean("weather_fx", v).apply()

    /** Minutes of screensaver before it goes calm (still picture, no animation); 0 = never. */
    var screensaverCalmMinutes: Int
        get() = sp.getInt("saver_calm_min", 30)
        set(v) = sp.edit().putInt("saver_calm_min", v).apply()

    /** Minutes of screensaver before the TV goes to standby; 0 = never. */
    var screensaverSleepMinutes: Int
        get() = sp.getInt("saver_sleep_min", 120)
        set(v) = sp.edit().putInt("saver_sleep_min", v).apply()

    /** When the home screen was last left, to greet people back after a long break. */
    var lastSeen: Long
        get() = sp.getLong("last_seen", 0L)
        set(v) = sp.edit().putLong("last_seen", v).apply()

    /** Show every app's name under its icon (off: no names, icons centred in the dock). */
    var showAppNames: Boolean
        get() = sp.getBoolean("app_names", false)
        set(v) = sp.edit().putBoolean("app_names", v).apply()

    /** Name of a LauncherFont entry. */
    var font: String?
        get() = sp.getString("font", null)
        set(v) = putString("font", v)

    /** How often the wallpaper changes, in minutes. */
    var wallpaperMinutes: Int
        get() = sp.getInt("wp_minutes", 15)
        set(v) = sp.edit().putInt("wp_minutes", v).apply()

    /** Package names in the order the user arranged them. */
    var appOrder: List<String>
        get() = sp.getString("app_order", "").orEmpty().split(',').filter { it.isNotBlank() }
        set(v) = putString("app_order", v.joinToString(","))

    var cachedPlace: String?
        get() = sp.getString("place", null)
        set(v) = putString("place", v)

    var cachedWeather: String?
        get() = sp.getString("weather", null)
        set(v) = putString("weather", v)

    var currentWallpaper: String?
        get() = sp.getString("wp_current", null)
        set(v) = putString("wp_current", v)

    /** Opacity of the side panels' background (see PanelOpacities). */
    var panelAlpha: Float
        get() = sp.getFloat("panel_alpha", 0.85f)
        set(v) = sp.edit().putFloat("panel_alpha", v).apply()

    /** Opacity of the app dock's glass (see DockOpacities). */
    var dockAlpha: Float
        get() = sp.getFloat("dock_alpha", 0.3f)
        set(v) = sp.edit().putFloat("dock_alpha", v).apply()

    private fun putString(key: String, value: String?) = sp.edit().putString(key, value).apply()

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
