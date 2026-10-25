package dev.hew7.launcher.dream

import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * The screensaver started on demand (Back three times on the home screen), showing the same scene
 * as the system screensaver. Any button press closes it.
 */
class ScreensaverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Sleep After: standby through the Hew7 Helper; without it, back to the home screen, where
        // the TV's own timers take over.
        setContent { DreamScreen(onSleep = { if (!dev.hew7.launcher.CloseHelperService.sleepTv()) finish() }) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private val openedAt = SystemClock.uptimeMillis()
    private var sawPress = false

    // Wake on a full press made while the screensaver is showing. Some TVs act on Back when it goes
    // down, so the release of the press that opened us arrives here; ignore releases we never saw
    // pressed, and anything in the first half-second.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (SystemClock.uptimeMillis() - openedAt < 500) return true
        when (event.action) {
            KeyEvent.ACTION_DOWN -> sawPress = true
            KeyEvent.ACTION_UP -> if (sawPress) finish()
        }
        return true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_UP) finish()
        return true
    }
}
