package dev.hew7.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import dev.hew7.launcher.data.Prefs
import dev.hew7.launcher.dream.ScreensaverActivity
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.hew7.launcher.ui.BootIntro
import dev.hew7.launcher.ui.HomeScreen

class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = vm.refreshApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RemoteKeys.prefs = Prefs(this)
        AppButtons.listen(this)
        if (BootIntro.shouldShow(this)) vm.showIntro.value = true
        setContent { HomeScreen(vm) }

        // A home screen never closes on Back; the Compose screen handles Back for its own panels first.
        // With nothing left to close, three quick presses start the screensaver.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            private val presses = ArrayDeque<Long>()
            override fun handleOnBackPressed() {
                val now = SystemClock.uptimeMillis()
                presses.addLast(now)
                while (presses.isNotEmpty() && now - presses.first() > 1_500) presses.removeFirst()
                if (presses.size >= 3) {
                    presses.clear()
                    startActivity(Intent(this@MainActivity, ScreensaverActivity::class.java))
                }
            }
        })

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    // Don't rebuild the window (recreate) when coming back from an app: on TCL TVs, rebuilding
    // while the switch back is under way leaves the other app's picture stacked over Hew7.

    override fun onResume() {
        super.onResume()
        // TCL's start-up splash leaves the home screen marked see-through, so the TV keeps the app
        // underneath "visible" and can end up drawing that app's picture over Hew7. Hew7 is opaque.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) setTranslucent(false)
        RemoteKeys.homeInFront = true
        vm.onResume()
    }

    override fun onPause() {
        RemoteKeys.homeInFront = false
        vm.onPause()
        super.onPause()
    }

    // Remote buttons with a job on the home screen (Settings, Apps, Menu...); see RemoteKeys.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        RemoteKeys.onKey(event) || super.dispatchKeyEvent(event)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        vm.windowFocused.value = hasFocus
        // TVs have no system bars; this keeps it full-screen on boxes or tablets that do.
        if (hasFocus) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.onHomePressed()
        if (intent.getBooleanExtra(EXTRA_OPEN_RUNNING_APPS, false)) {
            vm.clearReturnTo()
            vm.justClosed.value = intent.getStringArrayListExtra(EXTRA_CLOSED_APPS).orEmpty().toSet()
            vm.openRunningApps.value = true
        }
    }

    companion object {
        const val EXTRA_OPEN_RUNNING_APPS = "dev.hew7.launcher.OPEN_RUNNING_APPS"
        const val EXTRA_CLOSED_APPS = "dev.hew7.launcher.CLOSED_APPS"
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }
}
