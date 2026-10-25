package dev.hew7.launcher.data

import android.view.KeyEvent
import java.util.Locale

/** What a remote button can do on Hew7's home screen. */
enum class RemoteAction(val label: String, val short: String = label) {
    DEFAULT("Default", "TV Default"),
    NOTHING("Do Nothing", "Nothing"),
    HEW7_SETTINGS("Hew7 Settings"),
    QUICK_SETTINGS("Quick Settings"),
    MACHINE_ROOM("Machine Room"),
    RUNNING_APPS("Running Apps"),
    INPUTS("Inputs"),
    DOCK("First App in the Dock", "First App"),
    APP_MENU("Menu for the Selected App", "App Menu"),
    SCREENSAVER("Screensaver"),
    OPEN_APP("Open an App"),
}

/** A button's setting: an action, and for OPEN_APP the app to open. */
data class RemoteChoice(val action: RemoteAction, val pkg: String? = null) {
    fun encode() = if (action == RemoteAction.OPEN_APP) "APP:$pkg" else action.name

    companion object {
        fun decode(s: String?): RemoteChoice? = when {
            s == null -> null
            s.startsWith("APP:") -> RemoteChoice(RemoteAction.OPEN_APP, s.removePrefix("APP:"))
            else -> RemoteAction.entries.firstOrNull { it.name == s }?.let { RemoteChoice(it) }
        }
    }
}

/**
 * Remote buttons by the name Android gives them ("MENU", "ALL_APPS", and on TCL TVs extra ones
 * like "TCL_ALL_APP"). Nothing here is tied to one remote: whatever button arrives, Hew7 reads
 * its name from the system, so any TV's remote works.
 */
object RemoteButtons {
    /**
     * The button's name as the system reports it, without the KEYCODE_ prefix. Makers add their own
     * buttons (TCL_ALL_APP...) to the system but not always to its number-to-name lookup; those
     * names are found in the TV's key layout files and turned into numbers by the system itself.
     */
    fun name(keyCode: Int): String {
        val n = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        return if (n.all { it.isDigit() }) makerNames[keyCode] ?: n else n
    }

    private val makerNames: Map<Int, String> by lazy {
        val names = HashMap<Int, String>()
        runCatching {
            val word = Regex("""^\s*key\s+(?:usage\s+)?\S+\s+(\S+)""")
            listOf("/system/usr/keylayout", "/vendor/usr/keylayout", "/odm/usr/keylayout")
                .flatMap { java.io.File(it).listFiles()?.toList().orEmpty() }
                .filter { it.name.endsWith(".kl") }
                .forEach { f ->
                    runCatching {
                        f.forEachLine { line ->
                            val name = word.find(line)?.groupValues?.get(1) ?: return@forEachLine
                            if (name.all { it.isUpperCase() || it.isDigit() || it == '_' } && name.any { it.isLetter() }) {
                                val code = KeyEvent.keyCodeFromString(name)
                                if (code > 0 && code !in names) names[code] = name
                            }
                        }
                    }
                }
        }
        names
    }

    /** Buttons that always keep their normal job: moving around, Back, Home, power and volume. */
    private val fixed = setOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_SLEEP, KeyEvent.KEYCODE_WAKEUP,
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_MUTE, KeyEvent.KEYCODE_UNKNOWN,
    )

    fun isAssignable(keyCode: Int): Boolean {
        if (keyCode in fixed) return false
        val n = name(keyCode)
        return !n.endsWith("TV_ON") && !n.endsWith("TV_OFF") && !n.contains("POWER")
    }

    /** Remote buttons printed with an app's name, and the app each one opens. */
    private val appButtons = mapOf(
        "TCL_NETFLIX" to "com.netflix.ninja",
        "TCL_YOUTUBE" to "com.google.android.youtube.tv",
        "TCL_PRIMEVIDEO" to "com.amazon.amazonvideo.livingroom",
        "TCL_DISNEY" to "com.disney.disneyplus",
        "TCL_HULU" to "com.hulu.livingroomplus",
        "TCL_APPLETV" to "com.apple.atve.androidtv.appletv",
        "TCL_PLUTOTV" to "tv.pluto.android",
        "TCL_STAN" to "au.com.stan.and",
        "TCL_ZEE" to "com.graymatrix.did",
        "TCL_SONYLIV" to "com.sonyliv",
    )

    fun appFor(name: String): String? = appButtons[name]

    /** What a button does when it hasn't been set: the usual job for that kind of button. */
    fun defaultFor(name: String): RemoteAction = when (name) {
        "SETTINGS", "TCL_SET" -> RemoteAction.HEW7_SETTINGS
        "ALL_APPS", "TCL_ALL_APP" -> RemoteAction.DOCK
        "MENU", "TCL_3_LINE_INPUT", "TCL_QUICK_MENU" -> RemoteAction.APP_MENU
        "TV_INPUT" -> RemoteAction.INPUTS
        else -> RemoteAction.DEFAULT
    }

    /** A readable name for a button ("TCL_ALL_APP" -> "Apps", "PROG_RED" -> "Red"). */
    fun label(name: String): String = when (name) {
        "SETTINGS", "TCL_SET" -> "Settings"
        "ALL_APPS", "TCL_ALL_APP" -> "Apps"
        "MENU" -> "Menu"
        "TCL_3_LINE_INPUT" -> "Menu (≡)"
        "TCL_QUICK_MENU" -> "Quick Menu"
        "TV_INPUT" -> "Input"
        "PROG_RED" -> "Red"
        "PROG_GREEN" -> "Green"
        "PROG_YELLOW" -> "Yellow"
        "PROG_BLUE" -> "Blue"
        "TCL_PRIMEVIDEO" -> "Prime Video"
        "TCL_YOUTUBE" -> "YouTube"
        "TCL_EPG", "GUIDE" -> "Guide"
        else -> name.removePrefix("TCL_").split('_').joinToString(" ") { w ->
            w.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }
}
