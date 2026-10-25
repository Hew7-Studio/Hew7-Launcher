package dev.hew7.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val pkg: String,
    val label: String,
    /** 16:9 TV banner, if the app ships one. */
    val banner: ImageBitmap?,
    val icon: ImageBitmap,
    val isSystem: Boolean,
    /** Default position group: lower comes first (see AppRepository.rankOf). */
    val rank: Int,
)

class AppRepository(private val context: Context) {
    private val pm = context.packageManager

    /** Every app that can be opened from a TV home screen, plus phone-style apps without a TV entry. */
    suspend fun load(): List<AppEntry> = withContext(Dispatchers.IO) {
        val seen = LinkedHashMap<String, AppEntry>()
        // Render at the screen's real pixel density (tile sizes in dp, see AppTile) so icons stay sharp on 4K UIs.
        val density = context.resources.displayMetrics.density
        val iconPx = (96 * density).toInt().coerceIn(128, 384)
        val bannerW = (186 * density).toInt().coerceIn(320, 744)
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val ai = ri.activityInfo
                val pkg = ai.packageName
                if (pkg == context.packageName || pkg in seen) continue
                val banner = ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm)
                seen[pkg] = AppEntry(
                    pkg = pkg,
                    label = ri.loadLabel(pm).toString(),
                    banner = banner?.render(bannerW, bannerW * 9 / 16),
                    icon = ri.loadIcon(pm).toSquareIcon(iconPx),
                    isSystem = ai.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    rank = rankOf(pkg, ai.applicationInfo),
                )
            }
        }
        seen.values.toList()
    }

    fun launchIntent(pkg: String): Intent? =
        (pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg))
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun Drawable.render(w: Int, h: Int): ImageBitmap = toBitmap(w, h).asImageBitmap()

    /**
     * Renders an icon as a full-bleed square so the UI can clip it to a rounded square.
     * Adaptive icons are drawn layer by layer without the system mask; legacy icons sit on white.
     */
    private fun Drawable.toSquareIcon(size: Int): ImageBitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (this is AdaptiveIconDrawable) {
            // Adaptive layers are 108dp with the visible 72dp in the middle, so overdraw by 25% per side.
            val pad = size / 4
            listOfNotNull(background, foreground).forEach { layer ->
                layer.setBounds(-pad, -pad, size + pad, size + pad)
                layer.draw(canvas)
            }
        } else {
            canvas.drawColor(Color.WHITE)
            val inset = (size * 0.14f).toInt()
            setBounds(inset, inset, size - inset, size - inset)
            draw(canvas)
        }
        return bmp.asImageBitmap()
    }

    companion object {
        const val PLAY_STORE = "com.android.vending"

        /** Always first, in this order. */
        private val FEATURED = listOf(
            "com.netflix.ninja",
            "com.amazon.amazonvideo.livingroom",
            "com.google.android.youtube.tv",
        )

        /** Streaming and TV apps that don't always declare themselves as video apps. */
        private val STREAMING = setOf(
            "com.disney.disneyplus", "air.ITVMobilePlayer", "com.channel4.ondemand", "com.channel5.my5",
            "com.bskyb.nowtv.beta", "com.cbs.ca", "com.cbs.ott", "com.tubitv", "in.startv.hotstar",
            "in.startv.hotstartvonly", "tv.wuaki.apptv", "com.plexapp.android", "com.google.android.youtube.tvmusic",
            "com.google.android.videos", "com.tcl.tv", "com.ibopro.ultra", "bbc.iplayer.android",
            "com.apple.atve.androidtv.appletv", "com.spotify.tv.android", "tv.pluto.android", "tv.twitch.android.app",
        )

        /**
         * Default grouping for the dock: Netflix, Prime Video, YouTube; then streaming and TV apps;
         * then games; then everything else.
         */
        fun rankOf(pkg: String, ai: ApplicationInfo): Int {
            FEATURED.indexOf(pkg).takeIf { it >= 0 }?.let { return it }
            return when {
                pkg in STREAMING || ai.category == ApplicationInfo.CATEGORY_VIDEO || ai.category == ApplicationInfo.CATEGORY_AUDIO -> 10
                ai.category == ApplicationInfo.CATEGORY_GAME || "games" in pkg -> 20
                else -> 30
            }
        }

        /** Applies the saved order; apps not in it follow by group, then alphabetically. */
        fun ordered(apps: List<AppEntry>, order: List<String>): List<AppEntry> {
            val byPkg = apps.associateBy { it.pkg }
            val known = order.mapNotNull { byPkg[it] }
            val rest = apps.filter { it.pkg !in order }.sortedWith(compareBy({ it.rank }, { it.label.lowercase() }))
            return known + rest
        }
    }
}
