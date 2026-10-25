package dev.hew7.launcher

import android.view.KeyEvent
import dev.hew7.launcher.data.Prefs
import dev.hew7.launcher.data.RemoteAction
import dev.hew7.launcher.data.RemoteButtons
import dev.hew7.launcher.data.RemoteChoice

/**
 * Where every remote button press on Hew7's home screen goes first, whether it arrives at Hew7's
 * own windows or is caught early by the Hew7 Helper (for buttons the TV would otherwise take,
 * like Settings or Input). Only acts while Hew7's home screen is in front; inside other apps every
 * button keeps its normal job.
 */
object RemoteKeys {
    /** True while Hew7's home screen is in front (set by MainActivity). */
    @Volatile var homeInFront = false

    lateinit var prefs: Prefs

    /** Set while the Remote Buttons page waits for a press; gets the button's name. */
    var learner: ((String) -> Unit)? = null

    /** Runs a button's action on the home screen (set by HomeScreen). */
    var runner: ((RemoteChoice) -> Unit)? = null

    /** Buttons whose press Hew7 used, so their release is swallowed too. */
    private val used = HashSet<Int>()

    /**
     * Opens an app; set by whoever can start activities (the Hew7 Helper, or the home screen).
     * Returns false if the app isn't installed.
     */
    var appOpener: ((String) -> Boolean)? = null

    /** Returns true if Hew7 used the press (it then goes no further). */
    fun onKey(event: KeyEvent): Boolean {
        val code = event.keyCode
        when (event.action) {
            KeyEvent.ACTION_UP -> return used.remove(code)
            KeyEvent.ACTION_DOWN -> Unit
            else -> return code in used
        }
        // App buttons (Netflix, YouTube...) open their app from anywhere, as on Google TV. TCL
        // hands these to Google TV's home screen, which Hew7 replaces, so Hew7 answers them.
        val appPkg = if (learner == null) RemoteButtons.appFor(RemoteButtons.name(code)) else null
        if (appPkg != null && event.repeatCount == 0 && appOpener?.invoke(appPkg) == true) {
            used += code
            return true
        }
        if (!homeInFront || !::prefs.isInitialized || !RemoteButtons.isAssignable(code)) return false
        if (event.repeatCount > 0) return code in used

        val name = RemoteButtons.name(code)
        val seen = prefs.remoteSeen
        if (name !in seen) prefs.remoteSeen = seen + name

        learner?.let {
            it(name)
            used += code
            return true
        }
        val choice = RemoteChoice.decode(prefs.remoteButton(name)) ?: RemoteChoice(RemoteButtons.defaultFor(name))
        if (choice.action == RemoteAction.DEFAULT) return false
        val run = runner ?: return false
        run(choice)
        used += code
        return true
    }
}
