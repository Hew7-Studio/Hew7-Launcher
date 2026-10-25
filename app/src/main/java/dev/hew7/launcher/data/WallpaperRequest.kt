package dev.hew7.launcher.data

import android.content.Context
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import kotlin.math.max
import kotlin.math.min

/**
 * A wallpaper decoded at the screen's own size. The photos are often 4K; on a 1080p TV a full
 * decode holds four times the memory for pixels the screen can't show. Every place that loads a
 * wallpaper uses this, so they all share one copy in the image cache.
 */
fun wallpaperRequest(context: Context, url: String): ImageRequest {
    val m = context.resources.displayMetrics
    return ImageRequest.Builder(context)
        .data(url)
        .size(Size(max(m.widthPixels, m.heightPixels), min(m.widthPixels, m.heightPixels)))
        .precision(Precision.INEXACT)
        .memoryCacheKey("wallpaper:$url")
        .build()
}
