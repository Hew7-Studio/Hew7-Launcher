package dev.hew7.launcher.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.io.File
import java.net.Inet4Address

data class CpuInfo(
    val chip: String,
    val cores: Int,
    /** Average current clock across cores, if the TV exposes it. */
    val currentMhz: Int?,
    val maxMhz: Int?,
    /** System-wide load. Most TVs block this for apps, so it is usually null. */
    val usagePercent: Int?,
    val temperatureC: Float?,
)

data class NetworkInfo(
    val type: String,
    val wifiName: String?,
    val signalPercent: Int?,
    val linkMbps: Int?,
    val ip: String?,
    val downBytesPerSec: Long,
    val upBytesPerSec: Long,
)

data class SystemSnapshot(
    val model: String,
    val android: String,
    val uptimeMillis: Long,
    val cpu: CpuInfo,
    val ramUsed: Long,
    val ramTotal: Long,
    val storageUsed: Long,
    val storageTotal: Long,
    val network: NetworkInfo,
)

/**
 * Reads device stats for the System resources panel. Call [read] about once a second;
 * network speed and CPU load are measured as the change since the previous call.
 */
class SystemStats(private val context: Context) {
    private val am = context.getSystemService(ActivityManager::class.java)
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    private var lastRx = TrafficStats.getTotalRxBytes()
    private var lastTx = TrafficStats.getTotalTxBytes()
    private var lastAt = SystemClock.elapsedRealtime()
    private var lastCpu: LongArray? = readProcStat()

    fun read(): SystemSnapshot {
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val stat = StatFs(Environment.getDataDirectory().path)
        return SystemSnapshot(
            model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            android = "Android ${Build.VERSION.RELEASE}",
            uptimeMillis = SystemClock.elapsedRealtime(),
            cpu = cpu(),
            ramUsed = mem.totalMem - mem.availMem,
            ramTotal = mem.totalMem,
            storageUsed = stat.totalBytes - stat.availableBytes,
            storageTotal = stat.totalBytes,
            network = network(),
        )
    }

    private fun cpu(): CpuInfo {
        val cores = Runtime.getRuntime().availableProcessors()
        val cur = (0 until cores).mapNotNull { readInt("/sys/devices/system/cpu/cpu$it/cpufreq/scaling_cur_freq") }
        val max = (0 until cores).mapNotNull { readInt("/sys/devices/system/cpu/cpu$it/cpufreq/cpuinfo_max_freq") }

        val now = readProcStat()
        val prev = lastCpu
        lastCpu = now
        val usage = if (now != null && prev != null) {
            val total = now[0] - prev[0]
            val idle = now[1] - prev[1]
            if (total > 0) (100 * (total - idle) / total).toInt().coerceIn(0, 100) else null
        } else null

        val chip = if (Build.VERSION.SDK_INT >= 31 && Build.SOC_MODEL != Build.UNKNOWN) {
            "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}"
        } else {
            Build.HARDWARE
        }
        return CpuInfo(
            chip = chip,
            cores = cores,
            currentMhz = cur.takeIf { it.isNotEmpty() }?.average()?.div(1000)?.toInt(),
            maxMhz = max.maxOrNull()?.div(1000),
            usagePercent = usage,
            temperatureC = cpuTemperature(),
        )
    }

    /** Returns [total, idle] jiffies, or null when /proc/stat is blocked (most Android 8+ devices). */
    private fun readProcStat(): LongArray? = runCatching {
        val parts = File("/proc/stat").bufferedReader().use { it.readLine() }.trim().split(Regex("\\s+")).drop(1).map { it.toLong() }
        longArrayOf(parts.sum(), parts[3] + parts.getOrElse(4) { 0 })
    }.getOrNull()

    /** Set once the thermal folder turns out to be blocked, so we stop retrying every second. */
    private var thermalBlocked = false

    private fun cpuTemperature(): Float? {
        if (thermalBlocked) return null
        val zones = File("/sys/class/thermal").listFiles { f -> f.name.startsWith("thermal_zone") }
        if (zones.isNullOrEmpty()) {
            thermalBlocked = true
            return null
        }
        val readings = zones.mapNotNull { zone ->
            val type = runCatching { File(zone, "type").readText().trim().lowercase() }.getOrNull() ?: return@mapNotNull null
            val raw = readInt(File(zone, "temp").path) ?: return@mapNotNull null
            val c = if (raw > 1000) raw / 1000f else raw.toFloat()
            if (c !in 10f..120f) return@mapNotNull null
            type to c
        }
        return (readings.firstOrNull { (t, _) -> "cpu" in t || "soc" in t } ?: readings.firstOrNull())?.second
    }

    private fun network(): NetworkInfo {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val at = SystemClock.elapsedRealtime()
        val secs = ((at - lastAt) / 1000.0).coerceAtLeast(0.5)
        val down = ((rx - lastRx) / secs).toLong().coerceAtLeast(0)
        val up = ((tx - lastTx) / secs).toLong().coerceAtLeast(0)
        lastRx = rx; lastTx = tx; lastAt = at

        val active = cm.activeNetwork
        val caps = active?.let { cm.getNetworkCapabilities(it) }
        val ip = active?.let { cm.getLinkProperties(it) }?.linkAddresses
            ?.map { it.address }?.firstOrNull { it is Inet4Address }?.hostAddress

        return when {
            caps == null -> NetworkInfo("Not connected", null, null, null, null, down, up)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkInfo("Ethernet", null, null, null, ip, down, up)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                @Suppress("DEPRECATION")
                val info = context.applicationContext.getSystemService(WifiManager::class.java)?.connectionInfo
                @Suppress("DEPRECATION")
                val ssid = info?.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != WifiManager.UNKNOWN_SSID.trim('<', '>') && it != "<unknown ssid>" }
                NetworkInfo(
                    type = "Wi-Fi",
                    wifiName = ssid,
                    signalPercent = info?.rssi?.let { ((it + 100) * 2).coerceIn(0, 100) },
                    linkMbps = info?.linkSpeed?.takeIf { it > 0 },
                    ip = ip,
                    downBytesPerSec = down,
                    upBytesPerSec = up,
                )
            }
            else -> NetworkInfo("Connected", null, null, null, ip, down, up)
        }
    }

    /**
     * Asks Android to stop the background processes of every launchable app. Returns the bytes of
     * RAM that became available. Android 14+ only lets apps stop their own processes, so there it
     * frees little or nothing.
     */
    fun closeBackgroundApps(): Long {
        val before = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.availMem
        val pm = context.packageManager
        val pkgs = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER).flatMap { cat ->
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0).map { it.activityInfo.packageName }
        }.toSet() - context.packageName
        pkgs.forEach { runCatching { am.killBackgroundProcesses(it) } }
        Thread.sleep(800)
        val after = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.availMem
        return (after - before).coerceAtLeast(0)
    }

    private fun readInt(path: String): Int? = runCatching { File(path).readText().trim().toInt() }.getOrNull()
}
