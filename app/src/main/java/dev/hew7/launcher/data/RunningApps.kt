package dev.hew7.launcher.data

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RunningApp(val app: AppEntry, val bytes: Long)

/**
 * Which of the TV's apps are running and how much memory each uses. Android hides other apps'
 * memory from normal apps; this reads the system's own process list (`dumpsys activity
 * processes`), which works once the DUMP permission and usage access have been granted over ADB
 * (`pm grant dev.hew7.launcher android.permission.DUMP`, and on TCL TVs also
 * `appops set dev.hew7.launcher GET_USAGE_STATS allow`). The memory figure is the one the system
 * last measured for each process (`lastPss`). `dumpsys meminfo` would be fresher, but the TV's
 * security policy doesn't let apps reach it.
 */
object RunningApps {
    private const val DUMP = "android.permission.DUMP"
    private val record = Regex("""ProcessRecord\{\w+ \d+:([^/\s}]+)/""")
    private val pss = Regex("""\blastPss=([\d.]+)([KMG]?B)?""")

    fun hasPermission(context: Context) = context.checkSelfPermission(DUMP) == PackageManager.PERMISSION_GRANTED

    /** Launchable apps that are running, biggest first; null if memory can't be read. */
    suspend fun read(context: Context, apps: List<AppEntry>): List<RunningApp>? = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext null
        val text = runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("dumpsys", "activity", "processes"))
            p.inputStream.bufferedReader().use { it.readText() }.also { p.waitFor() }
        }.getOrNull() ?: return@withContext null

        // Each process starts with a "ProcessRecord{… pid:name/uid}" line; its lastPss follows.
        val byPkg = HashMap<String, Long>()
        var pkg: String? = null
        for (l in text.lineSequence()) {
            val r = record.find(l)
            if (r != null) {
                pkg = r.groupValues[1].substringBefore(':')
                continue
            }
            val p = pkg ?: continue
            val m = pss.find(l) ?: continue
            val n = m.groupValues[1].toDoubleOrNull() ?: 0.0
            val bytes = n * when (m.groupValues[2]) {
                "KB" -> 1024.0
                "MB" -> 1_048_576.0
                "GB" -> 1_073_741_824.0
                else -> 1.0
            }
            byPkg[p] = (byPkg[p] ?: 0L) + bytes.toLong()
            pkg = null
        }
        if (byPkg.isEmpty()) return@withContext null
        val known = apps.associateBy { it.pkg }
        // Not Hew7 itself, nor the TV's settings apps: the Hew7 Helper closes apps through them, so
        // they're always back.
        val hidden = setOf(context.packageName, "com.android.tv.settings", "com.tcl.settings")
        byPkg.mapNotNull { (pkg, bytes) -> known[pkg]?.takeIf { pkg !in hidden }?.let { RunningApp(it, bytes) } }
            .sortedByDescending { it.bytes }
    }
}
