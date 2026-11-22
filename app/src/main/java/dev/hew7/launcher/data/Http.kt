package dev.hew7.launcher.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal const val MAX_BODY_BYTES = 1024 * 1024

/** Blocking GET that returns the body as text. Call from Dispatchers.IO. */
internal fun httpGet(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 8_000
    conn.readTimeout = 8_000
    conn.setRequestProperty("User-Agent", "Hew7/0.1 (Android TV)")
    try {
        if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode} for $url")
        // Every reply Hew7 reads is a few KB of JSON; cap it so a broken or hostile server can't fill memory.
        conn.inputStream.use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                if (out.size() > MAX_BODY_BYTES) throw IOException("Reply over $MAX_BODY_BYTES bytes from $url")
            }
            return out.toString(Charsets.UTF_8.name())
        }
    } finally {
        conn.disconnect()
    }
}
