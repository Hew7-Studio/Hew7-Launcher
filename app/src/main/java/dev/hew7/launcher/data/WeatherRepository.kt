package dev.hew7.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt

enum class Sky { CLEAR, PARTLY, CLOUDY, FOG, RAIN, SNOW, STORM }

/** Open-Meteo's weather code as a sky. */
fun skyOf(code: Int): Sky = when (code) {
    0, 1 -> Sky.CLEAR
    2 -> Sky.PARTLY
    3 -> Sky.CLOUDY
    45, 48 -> Sky.FOG
    in 51..67, in 80..82 -> Sky.RAIN
    in 71..77, 85, 86 -> Sky.SNOW
    in 95..99 -> Sky.STORM
    else -> Sky.CLOUDY
}

data class HourCast(val time: LocalDateTime, val tempC: Double, val code: Int)

data class DayCast(val code: Int, val maxC: Double, val minC: Double)

fun temperature(c: Double, unit: TempUnit): String = when (unit) {
    TempUnit.CELSIUS -> "${c.roundToInt()}°"
    TempUnit.FAHRENHEIT -> "${(c * 9 / 5 + 32).roundToInt()}°"
}

data class Weather(
    val tempC: Double,
    val code: Int,
    val isDay: Boolean,
    val sunrise: LocalDateTime?,
    val sunset: LocalDateTime?,
    val city: String,
    val fetchedAt: Long,
    /** The next few hours, for the weather pill. */
    val hours: List<HourCast> = emptyList(),
    val tomorrow: DayCast? = null,
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
        .put("h", org.json.JSONArray(hours.map { JSONObject().put("at", it.time.toString()).put("t", it.tempC).put("c", it.code) }))
        .put("tm", tomorrow?.let { JSONObject().put("c", it.code).put("hi", it.maxC).put("lo", it.minC) })
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
                hours = j.optJSONArray("h")?.let { a ->
                    (0 until a.length()).map { a.getJSONObject(it) }.map { HourCast(LocalDateTime.parse(it.getString("at")), it.getDouble("t"), it.getInt("c")) }
                }.orEmpty(),
                tomorrow = j.optJSONObject("tm")?.let { DayCast(it.getInt("c"), it.getDouble("hi"), it.getDouble("lo")) },
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
                "&current=temperature_2m,weather_code,is_day&hourly=temperature_2m,weather_code" +
                "&daily=sunrise,sunset,weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=2"
            val j = JSONObject(httpGet(url))
            val current = j.getJSONObject("current")
            val daily = j.getJSONObject("daily")
            val hourly = j.getJSONObject("hourly")
            val times = hourly.getJSONArray("time")
            val now = LocalDateTime.parse(current.getString("time"))
            val hours = (0 until times.length())
                .map { HourCast(LocalDateTime.parse(times.getString(it)), hourly.getJSONArray("temperature_2m").getDouble(it), hourly.getJSONArray("weather_code").getInt(it)) }
                .filter { it.time.isAfter(now) }
                .take(5)
            val tomorrow = runCatching {
                DayCast(daily.getJSONArray("weather_code").getInt(1), daily.getJSONArray("temperature_2m_max").getDouble(1), daily.getJSONArray("temperature_2m_min").getDouble(1))
            }.getOrNull()
            Weather(
                tempC = current.getDouble("temperature_2m"),
                code = current.getInt("weather_code"),
                isDay = current.getInt("is_day") == 1,
                sunrise = LocalDateTime.parse(daily.getJSONArray("sunrise").getString(0)),
                sunset = LocalDateTime.parse(daily.getJSONArray("sunset").getString(0)),
                city = place.city,
                fetchedAt = System.currentTimeMillis(),
                hours = hours,
                tomorrow = tomorrow,
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
