package dev.hew7.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Finds Google TV's quick settings panel. It lives inside the stock home screen app, whose
 * activity names differ between versions, so we look for an exported activity that sounds like it.
 */
object GoogleTvShortcuts {
    private val hosts = listOf(
        "com.google.android.apps.tv.launcherx",
        "com.google.android.tvlauncher",
        "com.android.tv.settings",
    )
    private val names = Regex("quick.?settings|dashboard|settingspanel|sidepanel", RegexOption.IGNORE_CASE)

    /** Google TV opens its quick settings ("dashboard") panel for this standard action. */
    private const val OPEN_PANEL = "android.app.action.OPEN_NOTIFICATION_HANDLER_PANEL"

    fun quickSettingsIntent(context: Context): Intent {
        val pm = context.packageManager
        val panel = Intent(OPEN_PANEL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (pm.resolveActivity(panel, 0) != null) return panel
        for (pkg in hosts) {
            val activities = runCatching {
                pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES).activities
            }.getOrNull() ?: continue
            val match = activities.firstOrNull { it.exported && it.enabled && names.containsMatchIn(it.name) }
            if (match != null) {
                return Intent().setComponent(ComponentName(pkg, match.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
