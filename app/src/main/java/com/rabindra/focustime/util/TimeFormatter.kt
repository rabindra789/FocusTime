package com.rabindra.focustime.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ClockText(val time: String, val meridiem: String?)

data class DurationText(val hours: Int, val minutes: Int) {
    val label: String
        get() = when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
}

object TimeFormatter {

    /** 24:32, or 1:04:32 for durations of an hour and more. */
    fun countdown(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0L) + 999L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun clock(time: LocalTime, use24Hour: Boolean): ClockText {
        val pattern = if (use24Hour) "HH:mm" else "h:mm"
        val formatted = time.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
        val meridiem = if (use24Hour) null else {
            if (time.hour < 12) "AM" else "PM"
        }
        return ClockText(formatted, meridiem)
    }

    fun fullDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)).uppercase(locale)

    fun shortDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", locale)).uppercase(locale)

    fun duration(millis: Long): DurationText {
        val totalMinutes = (millis.coerceAtLeast(0L) / 60_000L).toInt()
        return DurationText(hours = totalMinutes / 60, minutes = totalMinutes % 60)
    }

    fun sessionLabel(sessionNumber: Int): String = String.format(Locale.US, "SESSION %02d", sessionNumber)

    fun trackingLabel(text: String): String = text.uppercase(Locale.getDefault())
}
