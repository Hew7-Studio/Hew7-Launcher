package dev.hew7.launcher

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * "Hew7 Helper": does the few things Android only lets the TV's own settings app do.
 *
 * - Closing apps for Running Apps: for each app it opens App Info and presses Force Stop, then OK,
 *   the same presses you would make with the remote.
 * - Opening settings pages that have no direct link (Display and Sound, Accounts and Sign-In): it
 *   opens the main settings list and selects that row.
 *
 * While it works, a still picture of the screen as it was covers everything, so the settings
 * pages it is pressing through never show. It acts only when Hew7 asks, and only on the TV's
 * settings app.
 */
class CloseHelperService : AccessibilityService() {

    private enum class Stage { FORCE_STOP, CONFIRM, OPEN_ENTRY }

    private val handler = Handler(Looper.getMainLooper())
    private val queue = ArrayDeque<String>()
    private val closed = ArrayList<String>()
    private var total = 0
    private var current: String? = null
    private var stage = Stage.FORCE_STOP
    private var entryLabels: List<String> = emptyList()
    private var entryPending = false
    private val skip = Runnable { next() }
    private val giveUpEntry = Runnable { finishEntry() }

    private var cover: View? = null
    private var coverText: TextView? = null

    override fun onServiceConnected() {
        instance = this
        AppButtons.listen(this)
    }

    override fun onDestroy() {
        removeCover()
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    /**
     * Remote buttons, before the TV acts on them. Only used while Hew7's home screen is in front,
     * so buttons the TV takes for itself (Settings, Input, Quick Menu) can do their Hew7 job there.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean = RemoteKeys.onKey(event)

    /** Force-stops these apps one by one behind the cover, then shows Running Apps again. */
    fun close(packages: List<String>) {
        queue.clear()
        queue.addAll(packages)
        closed.clear()
        total = packages.size
        showCover(showProgress = true) { next() }
    }

    /** Opens the main settings list and selects the row with one of these labels. */
    fun openSettingsEntry(labels: List<String>) {
        entryLabels = labels
        entryPending = false
        showCover(showProgress = false) {
            stage = Stage.OPEN_ENTRY
            current = ENTRY
            startActivity(
                Intent().setComponent(ComponentName(SETTINGS_PKG, "$SETTINGS_PKG.MainSettings"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
            handler.postDelayed(giveUpEntry, 4_000)
        }
    }

    private fun next() {
        handler.removeCallbacks(skip)
        val pkg = queue.removeFirstOrNull()
        current = pkg
        if (pkg == null) {
            // Done: back to Hew7's Running Apps list, which plays the closed rows away.
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    .putExtra(MainActivity.EXTRA_OPEN_RUNNING_APPS, true)
                    .putStringArrayListExtra(MainActivity.EXTRA_CLOSED_APPS, ArrayList(closed)),
            )
            // Give Hew7 a moment to draw before lifting the cover.
            handler.postDelayed({ removeCover() }, 900)
            return
        }
        val label = runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)) }.getOrNull() ?: pkg
        coverText?.text = if (total > 1) "Closing $label · ${total - queue.size} of $total" else "Closing $label"
        stage = Stage.FORCE_STOP
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY),
        )
        // If the page never shows what we expect (already stopped, other language), move on.
        handler.postDelayed(skip, 4_000)
    }

    // ---------- Restart and Shut Down ----------

    /** Restarts the TV through Android's own power menu (Restart, then any confirmation). */
    fun restart() {
        showCover(showProgress = true) {
            coverText?.text = "Restarting…"
            performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            pressThrough(
                first = listOf("Restart", "Reboot"),
                confirm = listOf("OK", "Restart", "Reboot"),
                scroll = false,
                failMessage = "This TV's power menu has no Restart option Hew7 can press",
            )
        }
    }

    /** Shuts the TV down through Android's own power menu (Power off, then its confirmation). */
    fun shutDown() {
        showCover(showProgress = true) {
            coverText?.text = "Shutting down…"
            performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            pressThrough(
                first = listOf("Power off", "Shut down", "Turn off", "Power Off", "Shut Down"),
                confirm = listOf("OK", "Power off", "Shut down", "Turn off"),
                scroll = false,
                failMessage = "This TV's power menu has no Shut Down option Hew7 can press",
            )
        }
    }

    /**
     * Presses a row with one of [first]'s labels (scrolling down to it if needed), then a
     * confirmation button with one of [confirm]'s labels. Looks every 350 ms for up to 8 s; the
     * TV restarting or switching off ends it.
     */
    private fun pressThrough(first: List<String>, confirm: List<String>, scroll: Boolean, failMessage: String) {
        val started = android.os.SystemClock.uptimeMillis()
        var pressedBounds: android.graphics.Rect? = null
        lateinit var step: Runnable
        step = Runnable {
            if (android.os.SystemClock.uptimeMillis() - started > 8_000) {
                removeCover()
                if (pressedBounds == null) android.widget.Toast.makeText(this, failMessage, android.widget.Toast.LENGTH_LONG).show()
                return@Runnable
            }
            val roots = (windows.mapNotNull { it.root } + listOfNotNull(rootInActiveWindow)).distinct()
            val wanted = if (pressedBounds == null) first else confirm
            val target = findAny(roots, wanted) { node ->
                // The confirmation can share the row's name ("Restart"): skip the row itself.
                val b = android.graphics.Rect().also { node.getBoundsInScreen(it) }
                pressedBounds == null || b != pressedBounds
            }
            if (target != null) {
                val b = android.graphics.Rect().also { target.getBoundsInScreen(it) }
                target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                handler.postDelayed({
                    target.refresh()
                    target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (pressedBounds == null) {
                        pressedBounds = b
                        handler.postDelayed(step, 700)
                    } else {
                        // Pressed the confirmation: the TV is restarting or turning off. If it
                        // hasn't after a while, lift the cover.
                        handler.postDelayed({ removeCover() }, 10_000)
                    }
                }, 300)
                return@Runnable
            }
            if (scroll && pressedBounds == null) {
                roots.firstNotNullOfOrNull { findScrollable(it) }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }
            handler.postDelayed(step, 350)
        }
        handler.postDelayed(step, 600)
    }

    /** The clickable node (or clickable parent of a node) whose text is one of [labels]. */
    private fun findAny(
        roots: List<AccessibilityNodeInfo>,
        labels: List<String>,
        accept: (AccessibilityNodeInfo) -> Boolean = { true },
    ): AccessibilityNodeInfo? {
        for (root in roots) for (label in labels) {
            for (n in root.findAccessibilityNodeInfosByText(label)) {
                if (!n.text?.toString().equals(label, ignoreCase = true)) continue
                var c: AccessibilityNodeInfo? = n
                while (c != null && !c.isClickable) c = c.parent
                if (c != null && c.isEnabled && accept(c)) return c
            }
        }
        return null
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) node.getChild(i)?.let { c -> findScrollable(c)?.let { return it } }
        return null
    }

    private fun finishEntry() {
        handler.removeCallbacks(giveUpEntry)
        current = null
        // Let the page finish sliding in before lifting the cover.
        handler.postDelayed({ removeCover() }, 700)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = current ?: return
        val root = rootInActiveWindow ?: return
        when (stage) {
            Stage.FORCE_STOP -> {
                val node = find(root, listOf("Force stop")) ?: return
                if (!node.isEnabled) {
                    next() // not running any more
                    return
                }
                if (click(node)) stage = Stage.CONFIRM
            }
            Stage.CONFIRM -> {
                val ok = find(root, listOf("OK")) ?: return
                if (click(ok)) {
                    closed += pkg
                    current = null
                    handler.postDelayed({ next() }, 400)
                }
            }
            Stage.OPEN_ENTRY -> {
                if (entryPending) return
                val label = find(root, entryLabels) ?: return
                var row: AccessibilityNodeInfo? = label
                while (row != null && !row.isClickable) row = row.parent
                row ?: return
                // TV settings lists ignore a click on a row that isn't selected: select it, then
                // click it once the list has moved.
                entryPending = true
                row.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                handler.postDelayed({ clickEntry(row, tries = 4) }, 350)
            }
        }
    }

    /**
     * Clicks the row, then checks it worked: once its page has opened, the main list (and so the
     * row) is gone. Some rows only take the click once the list has finished moving, so retry.
     */
    private fun clickEntry(row: AccessibilityNodeInfo, tries: Int) {
        row.refresh()
        row.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        row.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        handler.postDelayed({
            val stillThere = row.refresh() && row.isVisibleToUser
            if (stillThere && tries > 1) clickEntry(row, tries - 1) else finishEntry()
        }, 500)
    }

    private fun find(root: AccessibilityNodeInfo, texts: List<String>): AccessibilityNodeInfo? =
        texts.firstNotNullOfOrNull { text ->
            root.findAccessibilityNodeInfosByText(text).firstOrNull { it.text?.toString().equals(text, ignoreCase = true) }
        }

    /** Clicks the node or its nearest clickable parent. */
    private fun click(node: AccessibilityNodeInfo): Boolean {
        var n: AccessibilityNodeInfo? = node
        while (n != null && !n.isClickable) n = n.parent
        return n?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    }

    // ---------- the cover ----------

    /** Freezes the screen as it is now, then runs [then] underneath it. */
    private fun showCover(showProgress: Boolean, then: () -> Unit) {
        removeCover()
        var started = false
        val start = {
            if (!started) {
                started = true
                then()
            }
        }
        val add = { shot: Bitmap? ->
            addCover(shot, showProgress)
            // A frame for the cover to appear before anything changes behind it.
            handler.postDelayed(start, 120)
            Unit
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(result: ScreenshotResult) {
                            val bmp = runCatching { Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace) }.getOrNull()
                            result.hardwareBuffer.close()
                            add(bmp)
                        }

                        override fun onFailure(errorCode: Int) {
                            add(null)
                        }
                    },
                )
            }.onFailure { add(null) }
        } else {
            add(null)
        }
    }

    private fun addCover(shot: Bitmap?, showProgress: Boolean) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val dp = { v: Float -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt() }
        val root = FrameLayout(this).apply {
            if (shot != null) {
                addView(ImageView(context).apply {
                    setImageBitmap(shot)
                    scaleType = ImageView.ScaleType.FIT_XY
                })
            } else {
                setBackgroundColor(Color.argb(235, 12, 14, 18))
            }
        }
        if (showProgress) {
            val pill = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16f), dp(10f), dp(18f), dp(10f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(22f).toFloat()
                    setColor(Color.argb(230, 20, 22, 28))
                }
                addView(ProgressBar(context).apply {
                    isIndeterminate = true
                    indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.rgb(138, 180, 248))
                }, LinearLayout.LayoutParams(dp(20f), dp(20f)))
                coverText = TextView(context).apply {
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setPadding(dp(10f), 0, 0, 0)
                    text = "Closing…"
                }
                addView(coverText)
            }
            // Over the bottom of the Running Apps panel, which sits at the right edge.
            root.addView(pill, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                rightMargin = dp(40f)
                bottomMargin = dp(36f)
            })
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        runCatching { wm.addView(root, params) }.onSuccess { cover = root }
    }

    private fun removeCover() {
        val v = cover ?: return
        cover = null
        coverText = null
        runCatching { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v) }
    }

    companion object {
        private const val SETTINGS_PKG = "com.android.tv.settings"
        private const val ENTRY = "entry"

        var instance: CloseHelperService? = null
            private set

        /**
         * Puts the TV into standby, as a press of the remote's power button does (Android's
         * "lock screen" action sends the TV to sleep). False if the helper isn't running.
         */
        fun sleepTv(): Boolean {
            val helper = instance ?: return false
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) return false
            return helper.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        fun isEnabled(context: Context): Boolean {
            val me = ComponentName(context, CloseHelperService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return enabled.split(':').any { it.equals(me, ignoreCase = true) }
        }
    }
}
