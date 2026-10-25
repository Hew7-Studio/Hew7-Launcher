package dev.hew7.launcher.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

enum class NetKind { WIFI, WIRED, OTHER, OFFLINE }

data class NetworkLabel(val kind: NetKind, val name: String)

/** The TV's name and current connection, for the screensaver. */
object DeviceInfo {

    /** The name set in the TV's own settings, falling back to its maker and model. */
    fun tvName(context: Context): String =
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)?.takeIf { it.isNotBlank() }
            ?: "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    /** The current network: the Wi-Fi name (needs the location permission granted over ADB), or the connection type. */
    fun network(context: Context): NetworkLabel {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } ?: return NetworkLabel(NetKind.OFFLINE, "Offline")
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkLabel(NetKind.WIRED, "Wired network")
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                @Suppress("DEPRECATION")
                val ssid = context.applicationContext.getSystemService(WifiManager::class.java)?.connectionInfo?.ssid?.trim('"')
                NetworkLabel(NetKind.WIFI, ssid?.takeIf { it.isNotBlank() && it != "<unknown ssid>" } ?: "Wi-Fi")
            }
            else -> NetworkLabel(NetKind.OTHER, "Connected")
        }
    }
}
