package dev.hew7.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate

data class Wallpaper(val url: String, val caption: String?)

/** Pulls a rotating set of wallpapers from the internet. */
class WallpaperRepository {

    suspend fun fetch(source: WallpaperSource): List<Wallpaper> = withContext(Dispatchers.IO) {
        when (source) {
            WallpaperSource.BING -> runCatching { bing() }.getOrElse { photos() }
            WallpaperSource.PHOTOS -> photos()
        }.ifEmpty { photos() }
    }

    /** Bing's last 8 "image of the day" photos, at UHD (3840×2160). */
    private fun bing(): List<Wallpaper> {
        val j = JSONObject(httpGet("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=en-US"))
        val images = j.getJSONArray("images")
        return (0 until images.length()).map { i ->
            val img = images.getJSONObject(i)
            Wallpaper(
                url = "https://www.bing.com" + img.getString("urlbase") + "_UHD.jpg",
                caption = img.optString("title").ifBlank { null },
            )
        }
    }

    /** Random landscape-sized photos from Picsum, reshuffled daily. */
    private fun photos(): List<Wallpaper> {
        val day = LocalDate.now().dayOfYear
        return (0 until 12).map { Wallpaper("https://picsum.photos/seed/hew7${day}x$it/3840/2160", null) }
    }
}
