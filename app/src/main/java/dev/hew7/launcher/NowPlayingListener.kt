package dev.hew7.launcher

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.hew7.launcher.data.NowPlaying

/**
 * Notification access, used only for Now playing: it lets Hew7 read media sessions (what's playing
 * or paused in each app) and media notifications. Other notifications are ignored, and nothing
 * leaves the TV. Given by the setup script; can be switched off in the TV's settings.
 */
class NowPlayingListener : NotificationListenerService() {
    override fun onListenerConnected() {
        NowPlaying.start(this)
        runCatching { activeNotifications }.getOrNull()?.forEach { NowPlaying.onNotification(this, it) }
    }

    override fun onListenerDisconnected() = NowPlaying.stop()

    override fun onNotificationPosted(sbn: StatusBarNotification) = NowPlaying.onNotification(this, sbn)

    override fun onNotificationRemoved(sbn: StatusBarNotification) = NowPlaying.onNotificationRemoved(sbn)
}
