package dev.hew7.launcher.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Blocking GET that returns the body as text. Call from Dispatchers.IO. */
internal fun httpGet(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 8_000
    conn.readTimeout = 8_000
    conn.setRequestProperty("User-Agent", "Hew7/0.1 (Android TV)")
    try {
        if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode} for $url")
        return conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
}
