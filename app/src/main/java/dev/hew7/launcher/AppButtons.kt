package dev.hew7.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * The remote's app buttons (Netflix, YouTube...). On TCL's Google TV the system sends these to
 * Google TV's home screen to open the app; with Hew7 as the home screen nothing answered, so the
 * buttons did nothing. Hew7 opens the app instead: through the Hew7 Helper when it's on (it sees
 * the button first), and by answering TCL's "start Netflix" message otherwise.
 */
object AppButtons {
    /** Opens an app's TV start screen; false if it isn't installed. */
    fun open(context: Context, pkg: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return false
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        }.isSuccess
    }

    private var registered: BroadcastReceiver? = null

    /** Listens for TCL's Netflix-button message (sent when the Helper didn't catch the button). */
    fun listen(context: Context) {
        if (registered != null) return
        val app = context.applicationContext
        RemoteKeys.appOpener = { open(app, it) }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                open(app, "com.netflix.ninja")
            }
        }
        ContextCompat.registerReceiver(app, receiver, IntentFilter(NETFLIX_KEY), ContextCompat.RECEIVER_EXPORTED)
        registered = receiver
    }

    private const val NETFLIX_KEY = "com.netflix.action.NETFLIX_KEY_START"
}
