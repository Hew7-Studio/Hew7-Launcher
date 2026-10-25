package dev.hew7.launcher.data

import android.content.Context
import java.time.Duration
import java.time.LocalDateTime
import java.time.MonthDay
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/** A greeting plus a key that changes only when the line itself should change (for the slide animation). */
data class GreetingLine(val key: String, val text: String)

/**
 * Decides which greeting shows when:
 * - a new mood line every 2 hours (on even hours), stable across restarts;
 * - events (weather turning, sunset/sunrise within 30 minutes, back after days away) show straight
 *   away, once, until the next 2-hour slot;
 * - a special day leads the first slot it's seen in.
 */
class GreetingScheduler(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("greetings", Context.MODE_PRIVATE)

    /** Call when the home screen comes back after being away. */
    fun onReturn(awayMillis: Long, now: LocalDateTime) {
        val days = awayMillis / DAY
        if (days >= 2) setEvent("away", Greetings.away(days), slotEnd(now))
    }

    fun line(now: LocalDateTime, weather: Weather?, unit: TempUnit, style: GreetingStyle): GreetingLine {
        if (style == GreetingStyle.SIMPLE) return GreetingLine("simple-${now.hour / 2}", Greetings.simple(now))

        val today = now.toLocalDate().toString()
        val end = slotEnd(now)

        // Special day: leads the first slot of the day it's seen in.
        Greetings.special(MonthDay.from(now))?.let { text ->
            if (sp.getString("special_day", null) != today) {
                sp.edit().putString("special_day", today).apply()
                setEvent("special", text, end)
            }
        }

        if (weather != null) {
            // Weather turning to rain, storm, snow or fog.
            val last = sp.getString("last_sky", null)
            if (last != weather.sky.name) {
                sp.edit().putString("last_sky", weather.sky.name).apply()
                if (last != null) Greetings.weatherEvent(weather)?.let { setEvent("weather-${weather.sky}", it, end) }
            }
            // Sunset / sunrise coming up: shown once a day, until it happens.
            weather.sunset?.let { set -> sunEvent("sunset", set, now, today) }
            weather.sunrise?.let { rise -> sunEvent("sunrise", rise, now, today) }
        }

        // An event that's still running wins over the regular slot.
        if (sp.getLong("event_until", 0) > now.millis()) {
            val kind = sp.getString("event_kind", "") ?: ""
            // A weather line ends as soon as the weather changes (no "rain outside" on a clear night).
            if (kind.startsWith("weather-") && weather != null && kind != "weather-${weather.sky}") {
                sp.edit().putLong("event_until", 0).apply()
                return line(now, weather, unit, style)
            }
            val text = when (kind) {
                // Keep the minutes live while the line stays put.
                "sunset" -> weather?.sunset?.let { Greetings.sunset(Duration.between(now, it).toMinutes().coerceAtLeast(1)) }
                "sunrise" -> weather?.sunrise?.let { Greetings.sunrise(Duration.between(now, it).toMinutes().coerceAtLeast(1)) }
                else -> null
            } ?: sp.getString("event_text", null)
            if (!text.isNullOrBlank()) return GreetingLine("event-$kind-${sp.getLong("event_until", 0)}", text)
        }

        // Regular 2-hour mood line, picked the same way every time for this slot.
        val slot = now.toLocalDate().toEpochDay() * 12 + now.hour / 2
        val pool = Greetings.moodPool(now, weather, unit)
        return GreetingLine("slot-$slot", pool[Random(slot).nextInt(pool.size)])
    }

    private fun sunEvent(kind: String, at: LocalDateTime, now: LocalDateTime, today: String) {
        val mins = Duration.between(now, at).toMinutes()
        if (mins in 1..30 && sp.getString("${kind}_day", null) != today) {
            sp.edit().putString("${kind}_day", today).apply()
            setEvent(kind, "", minOf(at.millis(), slotEnd(now)))
        }
    }

    private fun setEvent(kind: String, text: String, until: Long) {
        sp.edit().putString("event_kind", kind).putString("event_text", text).putLong("event_until", until).apply()
    }

    /** Start of the next even hour. */
    private fun slotEnd(now: LocalDateTime): Long =
        now.truncatedTo(ChronoUnit.HOURS).withHour(now.hour - now.hour % 2).plusHours(2).millis()

    private fun LocalDateTime.millis() = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
