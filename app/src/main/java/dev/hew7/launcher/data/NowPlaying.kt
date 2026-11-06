package dev.hew7.launcher.data

import android.app.Notification
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import dev.hew7.launcher.NowPlayingListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File

/** What an app is playing, or last played. */
data class MediaInfo(
    val pkg: String,
    val title: String,
    val subtitle: String,
    val art: Bitmap?,
    val playing: Boolean,
    /** The app still has it open, so opening the app resumes it. False: remembered from earlier. */
    val live: Boolean,
    val positionMs: Long,
    /** Wall-clock time [positionMs] was measured; while playing, the position moves on from there. */
    val positionAt: Long,
    val speed: Float,
    val durationMs: Long,
    val updatedAt: Long,
) {
    /** Position now, counting on from the last report while playing. */
    fun positionNow(now: Long = System.currentTimeMillis()): Long {
        val p = if (playing) positionMs + ((now - positionAt) * speed).toLong() else positionMs
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p.coerceAtLeast(0)
    }
}

/**
 * Now playing, gathered from every source Android allows a launcher (all need Notification access,
 * given by the setup script):
 *  1. media sessions: title, artwork and progress from any app that's playing or paused;
 *  2. media notifications: for apps that show a playback notification without sharing a session;
 *  3. history: the last thing each app played, kept on the TV so it still shows after the app closes.
 * Without Notification access everything here is empty and the home screen shows app info instead.
 */
object NowPlaying {
    private val _media = MutableStateFlow<Map<String, MediaInfo>>(emptyMap())
    val media: StateFlow<Map<String, MediaInfo>> = _media

    private val main = Handler(Looper.getMainLooper())
    private fun ui(block: () -> Unit) { main.post(block) }
    private var app: Context? = null
    private var sessions: MediaSessionManager? = null
    private val controllers = HashMap<String, Pair<MediaController, MediaController.Callback>>()
    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { list -> onSessions(list.orEmpty()) }

    private const val KEEP_DAYS = 14L
    private const val ART_PX = 480

    /** Whether Hew7 has Notification access (needed for everything here). */
    fun allowed(context: Context) = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    /** Loads the history and starts following media sessions. Safe to call often. */
    fun start(context: Context) = ui {
        val ctx = context.applicationContext
        if (app == null) {
            app = ctx
            load(ctx)
        }
        if (sessions == null && allowed(ctx)) {
            val msm = ctx.getSystemService(MediaSessionManager::class.java) ?: return@ui
            val component = ComponentName(ctx, NowPlayingListener::class.java)
            runCatching {
                msm.addOnActiveSessionsChangedListener(sessionListener, component, main)
                sessions = msm
                onSessions(msm.getActiveSessions(component))
            }
        } else {
            sessions?.let { msm -> runCatching { onSessions(msm.getActiveSessions(ComponentName(ctx, NowPlayingListener::class.java))) } }
        }
    }

    /** Notification access was taken away: stop following sessions (the history stays). */
    fun stop() = ui {
        runCatching { sessions?.removeOnActiveSessionsChangedListener(sessionListener) }
        sessions = null
        controllers.values.forEach { (c, cb) -> c.unregisterCallback(cb) }
        controllers.clear()
        markEnded { true }
    }

    // ---------- media sessions ----------

    private fun onSessions(list: List<MediaController>) {
        val ours = app?.packageName
        val current = list.filter { it.packageName != ours }.associateBy { it.packageName }
        // Sessions that closed: forget the controller, keep what they played as history.
        controllers.keys.filter { it !in current }.forEach { pkg ->
            controllers.remove(pkg)?.let { (c, cb) -> c.unregisterCallback(cb) }
        }
        markEnded { it !in current && !notified.contains(it) }
        for ((pkg, c) in current) {
            val known = controllers[pkg]
            if (known != null && known.first.sessionToken == c.sessionToken) continue
            known?.let { (old, cb) -> old.unregisterCallback(cb) }
            val cb = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = fromSession(c)
                override fun onPlaybackStateChanged(state: PlaybackState?) = fromSession(c)
                override fun onSessionDestroyed() = markEnded { it == pkg }
            }
            c.registerCallback(cb, main)
            controllers[pkg] = c to cb
            fromSession(c)
        }
    }

    private fun fromSession(c: MediaController) {
        val md = c.metadata
        val title = md?.text(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: md?.text(MediaMetadata.METADATA_KEY_TITLE)
        val prev = _media.value[c.packageName]
        if (title.isNullOrBlank() && prev == null) return
        val state = c.playbackState
        val playing = state?.state == PlaybackState.STATE_PLAYING || state?.state == PlaybackState.STATE_BUFFERING
        val now = System.currentTimeMillis()
        val position = state?.position?.takeIf { it >= 0 } ?: 0L
        // The state's time is on the uptime clock; turn it into wall-clock time.
        val at = state?.lastPositionUpdateTime?.takeIf { it > 0 }?.let { now - (SystemClock.elapsedRealtime() - it) } ?: now
        val sameItem = prev != null && (title.isNullOrBlank() || title == prev.title)
        put(
            MediaInfo(
                pkg = c.packageName,
                title = if (title.isNullOrBlank()) prev!!.title else title,
                subtitle = md?.text(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                    ?: md?.text(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: md?.text(MediaMetadata.METADATA_KEY_ALBUM)
                    ?: (if (sameItem) prev!!.subtitle else ""),
                art = md?.art() ?: (if (sameItem) prev!!.art else null),
                playing = playing,
                live = true,
                positionMs = position,
                positionAt = at,
                speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
                durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0 } ?: 0L,
                updatedAt = if (playing || !sameItem) now else prev!!.updatedAt,
            ),
        )
    }

    private fun MediaMetadata.text(key: String) = getText(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private fun MediaMetadata.art(): Bitmap? =
        (getBitmap(MediaMetadata.METADATA_KEY_ART) ?: getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON))?.shrunk()

    // ---------- media notifications ----------

    /** Apps whose media notification is showing (some apps never share a session). */
    private val notified = HashSet<String>()

    fun onNotification(context: Context, sbn: StatusBarNotification) = ui {
        if (sbn.packageName == context.packageName) return@ui
        val n = sbn.notification ?: return@ui
        val ex = n.extras ?: return@ui
        val media = ex.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
            n.category == Notification.CATEGORY_TRANSPORT ||
            ex.getString(Notification.EXTRA_TEMPLATE).orEmpty().contains("MediaStyle")
        if (!media) return@ui
        notified += sbn.packageName
        // A live session says more (progress, playing or paused); the notification only fills gaps.
        if (controllers.containsKey(sbn.packageName) && _media.value[sbn.packageName]?.live == true) return@ui
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        if (title.isNullOrBlank()) return@ui
        val prev = _media.value[sbn.packageName]
        val art = runCatching {
            n.getLargeIcon()?.loadDrawable(context)?.toBitmap()?.shrunk()
                ?: (ex.get(Notification.EXTRA_LARGE_ICON) as? Bitmap)?.shrunk()
        }.getOrNull()
        val now = System.currentTimeMillis()
        put(
            MediaInfo(
                pkg = sbn.packageName,
                title = title,
                subtitle = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty(),
                art = art ?: prev?.takeIf { it.title == title }?.art,
                playing = prev?.playing ?: false,
                live = true,
                positionMs = 0,
                positionAt = now,
                speed = 1f,
                durationMs = 0,
                updatedAt = now,
            ),
        )
    }

    fun onNotificationRemoved(sbn: StatusBarNotification) = ui {
        if (notified.remove(sbn.packageName) && !controllers.containsKey(sbn.packageName)) markEnded { it == sbn.packageName }
    }

    // ---------- history ----------

    private fun markEnded(which: (String) -> Boolean) {
        val map = _media.value
        val changed = map.mapValues { (pkg, m) -> if (m.live && which(pkg)) m.copy(live = false, playing = false, positionMs = m.positionNow(), positionAt = System.currentTimeMillis()) else m }
        if (changed != map) {
            _media.value = changed
            changed.values.filter { it != map[it.pkg] }.forEach { save(it, artChanged = false) }
        }
    }

    private fun put(m: MediaInfo) {
        val prev = _media.value[m.pkg]
        _media.value = _media.value + (m.pkg to m)
        save(m, artChanged = m.art != null && m.art !== prev?.art)
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("hew7_media", Context.MODE_PRIVATE)
    private fun artFile(ctx: Context, pkg: String) = File(File(ctx.filesDir, "media").apply { mkdirs() }, "$pkg.jpg")

    private fun save(m: MediaInfo, artChanged: Boolean) {
        val ctx = app ?: return
        val json = JSONObject()
            .put("t", m.title).put("s", m.subtitle)
            .put("p", m.positionNow()).put("d", m.durationMs).put("u", m.updatedAt)
        prefs(ctx).edit().putString(m.pkg, json.toString()).apply()
        if (artChanged) {
            val art = m.art ?: return
            Thread {
                runCatching { artFile(ctx, m.pkg).outputStream().use { art.compress(Bitmap.CompressFormat.JPEG, 85, it) } }
            }.start()
        }
    }

    private fun load(ctx: Context) {
        val cutoff = System.currentTimeMillis() - KEEP_DAYS * 24 * 60 * 60 * 1000L
        val sp = prefs(ctx)
        val out = HashMap<String, MediaInfo>()
        for ((pkg, v) in sp.all) {
            val j = runCatching { JSONObject(v as String) }.getOrNull() ?: continue
            val updated = j.optLong("u")
            if (updated < cutoff) {
                sp.edit().remove(pkg).apply()
                artFile(ctx, pkg).delete()
                continue
            }
            val art = artFile(ctx, pkg).takeIf { it.exists() }?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() }
            out[pkg] = MediaInfo(pkg, j.optString("t"), j.optString("s"), art, playing = false, live = false,
                positionMs = j.optLong("p"), positionAt = updated, speed = 1f, durationMs = j.optLong("d"), updatedAt = updated)
        }
        _media.value = out + _media.value
    }

    private fun Bitmap.shrunk(): Bitmap {
        if (width <= ART_PX && height <= ART_PX) return this
        val k = ART_PX.toFloat() / maxOf(width, height)
        return scale((width * k).toInt().coerceAtLeast(1), (height * k).toInt().coerceAtLeast(1))
    }

    // ---------- every other app ----------

    /** When each app was last used (Usage access, given by the setup script); empty without it. */
    fun lastUsed(context: Context): Map<String, Long> = runCatching {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val now = System.currentTimeMillis()
        usm.queryAndAggregateUsageStats(now - 60L * 24 * 60 * 60 * 1000, now)
            .mapValues { it.value.lastTimeUsed }
            .filterValues { it > 0 }
    }.getOrDefault(emptyMap())
}
