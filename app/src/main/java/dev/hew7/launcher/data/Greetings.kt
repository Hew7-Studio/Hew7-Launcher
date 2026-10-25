package dev.hew7.launcher.data

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.MonthDay
import java.time.format.DateTimeFormatter

/** Greeting lines, grouped by what triggers them. [GreetingScheduler] decides which one shows when. */
object Greetings {

    /** The plain greeting used by the "Simple" style. */
    fun simple(now: LocalDateTime): String = "${partOfDay(now)}, welcome back"

    /**
     * Lines for the regular 2-hour slot: time of day, day of the week and the general weather mood.
     * The plain greeting is deliberately not in here; it belongs to the Simple style only.
     */
    fun moodPool(now: LocalDateTime, weather: Weather?, unit: TempUnit): List<String> {
        val lines = mutableListOf<String>()
        val hour = now.hour
        val time = now.format(DateTimeFormatter.ofPattern("HH:mm"))

        lines += when (hour) {
            in 5..8 -> listOf("Rise and shine. The couch missed you.", "Coffee first, binge later.", "Early start. Respect.")
            in 9..11 -> listOf("Morning. The day is wide open.", "Late breakfast and a good show? Sounds right.")
            in 12..16 -> listOf("Afternoon. Perfect time for just one episode.", "A little afternoon escape, coming up.")
            in 17..20 -> listOf("Evening. Lights low, volume up.", "The day's done. This part is yours.", "Golden hour for the couch.")
            in 21..23 -> listOf("Night owl mode: on.", "Still up? The night is young, and so is your watchlist.")
            else -> listOf("It's $time. One more episode won't hurt. Probably.", "The world's asleep. Perfect time for a film.")
        }

        when (now.dayOfWeek) {
            DayOfWeek.MONDAY -> lines += "Monday? Let's ease into it."
            DayOfWeek.FRIDAY -> if (hour >= 16) lines += "It's Friday. Officially permitted to binge."
            DayOfWeek.SATURDAY -> lines += "Saturday. Zero plans is a plan."
            DayOfWeek.SUNDAY -> lines += "Sunday reset. Nothing on the schedule but this."
            else -> Unit
        }

        if (weather != null) {
            weatherEvent(weather)?.let { lines += it }
            when (weather.sky) {
                Sky.CLOUDY -> lines += "Grey skies, bright screen."
                Sky.CLEAR, Sky.PARTLY -> lines += if (weather.isDay) "Blue skies today. Save the good stuff for later?" else "Clear night out there. Stars outside, stars on screen."
                else -> Unit
            }
            val temp = weather.temperature(unit)
            when {
                weather.tempC >= 26 -> lines += "It's $temp out there. The TV is the cool place to be."
                weather.tempC <= 4 -> lines += "Only $temp outside. Stay in, we've got this."
            }
        }
        return lines.distinct()
    }

    /** Shown straight away when the weather turns to one of these. Null for calm weather. */
    fun weatherEvent(weather: Weather): String? = when (weather.sky) {
        Sky.RAIN -> "Rain outside, cosy inside. Good call."
        Sky.STORM -> "Thunder outside. Surround sound inside."
        Sky.SNOW -> "Snow day energy. Blankets mandatory."
        Sky.FOG -> "Foggy out there. Crystal clear in here."
        else -> null
    }

    fun sunset(minutes: Long) = "Sunset in $minutes minutes. Night mode is warming up."
    fun sunrise(minutes: Long) = "Sunrise in $minutes minutes. Long night?"

    fun away(days: Long) = if (days >= 7) "A whole week! Your shows waited for you." else "Long time no see. Welcome back."

    fun special(day: MonthDay): String? = when (day) {
        MonthDay.of(1, 1) -> "Happy New Year. New year, new watchlist."
        MonthDay.of(2, 14) -> "Happy Valentine's Day. Rom-com night?"
        MonthDay.of(10, 31) -> "Happy Halloween. Something spooky, perhaps?"
        MonthDay.of(12, 24) -> "It's Christmas Eve. Cue the classics."
        MonthDay.of(12, 25) -> "Merry Christmas. Grab the remote before anyone else does."
        MonthDay.of(12, 31) -> "Last night of the year. Make it a good one."
        else -> null
    }

    private fun partOfDay(now: LocalDateTime) = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}
