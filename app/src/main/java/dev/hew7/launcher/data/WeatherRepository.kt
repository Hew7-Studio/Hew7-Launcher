package dev.hew7.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt

enum class Sky { CLEAR, PARTLY, CLOUDY, FOG, RAIN, SNOW, STORM }

data class Weather(
    val tempC: Double,
    val code: Int,
    val isDay: Boolean,
    val sunrise: LocalDateTime?,
    val sunset: LocalDateTime?,
    val city: String,
    val fetchedAt: Long,
) {
    val sky: Sky
        get() = when (code) {
            0, 1 -> Sky.CLEAR
            2 -> Sky.PARTLY
            3 -> Sky.CLOUDY
            45, 48 -> Sky.FOG
            in 51..67, in 80..82 -> Sky.RAIN
            in 71..77, 85, 86 -> Sky.SNOW
            in 95..99 -> Sky.STORM
            else -> Sky.CLOUDY
        }

    val description: String
        get() = when (sky) {
            Sky.CLEAR -> if (isDay) "Sunny" else "Clear"
            Sky.PARTLY -> "Partly cloudy"
            Sky.CLOUDY -> "Cloudy"
            Sky.FOG -> "Foggy"
            Sky.RAIN -> if (code in 51..57) "Drizzle" else "Rain"
            Sky.SNOW -> "Snow"
            Sky.STORM -> "Thunderstorm"
        }

    val emoji: String
        get() = when (sky) {
            Sky.CLEAR -> if (isDay) "☀️" else "🌙"
            Sky.PARTLY -> if (isDay) "⛅" else "☁️"
            Sky.CLOUDY -> "☁️"
            Sky.FOG -> "🌫️"
            Sky.RAIN -> "🌧️"
            Sky.SNOW -> "❄️"
            Sky.STORM -> "⛈️"
        }

    fun temperature(unit: TempUnit): String = when (unit) {
        TempUnit.CELSIUS -> "${tempC.roundToInt()}°"
        TempUnit.FAHRENHEIT -> "${(tempC * 9 / 5 + 32).roundToInt()}°"
    }

    /** True between sunset and sunrise. Falls back to 7:00–19:00 if times are missing. */
    fun isNightAt(time: LocalTime): Boolean {
        val rise = sunrise?.toLocalTime() ?: LocalTime.of(7, 0)
        val set = sunset?.toLocalTime() ?: LocalTime.of(19, 0)
        return time.isBefore(rise) || !time.isBefore(set)
    }

    fun toJson(): String = JSONObject()
        .put("t", tempC).put("c", code).put("d", isDay)
        .put("sr", sunrise?.toString()).put("ss", sunset?.toString())
        .put("city", city).put("at", fetchedAt)
        .toString()

    companion object {
        fun fromJson(s: String?): Weather? = runCatching {
            val j = JSONObject(s!!)
            Weather(
                tempC = j.getDouble("t"),
                code = j.getInt("c"),
                isDay = j.getBoolean("d"),
                sunrise = j.optString("sr").takeIf { it.isNotEmpty() && it != "null" }?.let(LocalDateTime::parse),
                sunset = j.optString("ss").takeIf { it.isNotEmpty() && it != "null" }?.let(LocalDateTime::parse),
                city = j.optString("city"),
                fetchedAt = j.getLong("at"),
            )
        }.getOrNull()
    }
}

private data class Place(val lat: Double, val lon: Double, val city: String, val fetchedAt: Long)

/** Finds the approximate location from the IP address, then asks Open-Meteo for weather and sun times. */
class WeatherRepository(private val prefs: Prefs) {

    fun cached(): Weather? = Weather.fromJson(prefs.cachedWeather)

    /** Returns fresh weather, or the cached copy if the network fails. */
    suspend fun refresh(): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val place = place()
            val url = "https://api.open-meteo.com/v1/forecast?latitude=${place.lat}&longitude=${place.lon}" +
                "&current=temperature_2m,weather_code,is_day&daily=sunrise,sunset&timezone=auto&forecast_days=1"
            val j = JSONObject(httpGet(url))
            val current = j.getJSONObject("current")
            val daily = j.getJSONObject("daily")
            Weather(
                tempC = current.getDouble("temperature_2m"),
                code = current.getInt("weather_code"),
                isDay = current.getInt("is_day") == 1,
                sunrise = LocalDateTime.parse(daily.getJSONArray("sunrise").getString(0)),
                sunset = LocalDateTime.parse(daily.getJSONArray("sunset").getString(0)),
                city = place.city,
                fetchedAt = System.currentTimeMillis(),
            ).also { prefs.cachedWeather = it.toJson() }
        }.getOrElse { cached() }
    }

    private fun place(): Place {
        val cached = runCatching {
            val j = JSONObject(prefs.cachedPlace!!)
            Place(j.getDouble("lat"), j.getDouble("lon"), j.getString("city"), j.getLong("at"))
        }.getOrNull()
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < 12 * HOUR) return cached

        val fresh = runCatching {
            val j = JSONObject(httpGet("https://ipwho.is/"))
            check(j.optBoolean("success", true))
            Place(j.getDouble("latitude"), j.getDouble("longitude"), j.optString("city"), System.currentTimeMillis())
        }.recoverCatching {
            val j = JSONObject(httpGet("https://ipapi.co/json/"))
            Place(j.getDouble("latitude"), j.getDouble("longitude"), j.optString("city"), System.currentTimeMillis())
        }.getOrNull() ?: return cached ?: error("No location")

        prefs.cachedPlace = JSONObject()
            .put("lat", fresh.lat).put("lon", fresh.lon).put("city", fresh.city).put("at", fresh.fetchedAt)
            .toString()
        return fresh
    }

    companion object {
        const val HOUR = 60 * 60 * 1000L
    }
}
