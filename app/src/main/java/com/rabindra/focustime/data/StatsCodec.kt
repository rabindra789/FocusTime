package com.rabindra.focustime.data

import java.time.LocalDate

data class DayStats(
    val focusSessions: Int = 0,
    val focusMillis: Long = 0L,
)

/**
 * Encodes daily statistics as newline separated `date,sessions,millis` rows.
 * Unparsable rows are dropped instead of failing the whole read.
 */
object StatsCodec {

    fun encode(days: Map<LocalDate, DayStats>): String = days.entries
        .sortedBy { it.key }
        .joinToString("\n") { (date, stats) ->
            "${date}\u0001${stats.focusSessions}\u0001${stats.focusMillis}"
        }

    fun decode(raw: String?): Map<LocalDate, DayStats> {
        if (raw.isNullOrBlank()) return emptyMap()
        val result = LinkedHashMap<LocalDate, DayStats>()
        raw.split('\n').forEach { row ->
            val parts = row.split('\u0001')
            if (parts.size != 3) return@forEach
            val date = runCatching { LocalDate.parse(parts[0].trim()) }.getOrNull() ?: return@forEach
            val sessions = parts[1].trim().toIntOrNull() ?: return@forEach
            val millis = parts[2].trim().toLongOrNull() ?: return@forEach
            if (sessions < 0 || millis < 0) return@forEach
            result[date] = DayStats(sessions, millis)
        }
        return result
    }

    fun addSession(
        days: Map<LocalDate, DayStats>,
        date: LocalDate,
        focusMillis: Long,
    ): Map<LocalDate, DayStats> {
        val current = days[date] ?: DayStats()
        return days + (date to DayStats(
            focusSessions = current.focusSessions + 1,
            focusMillis = current.focusMillis + focusMillis.coerceAtLeast(0L),
        ))
    }

    fun totalSessions(days: Map<LocalDate, DayStats>): Int = days.values.sumOf { it.focusSessions }

    fun totalMillis(days: Map<LocalDate, DayStats>): Long = days.values.sumOf { it.focusMillis }

    /** Focus millis over today plus the six preceding days. */
    fun weeklyMillis(days: Map<LocalDate, DayStats>, today: LocalDate): Long {
        var sum = 0L
        for (offset in 0L..6L) {
            sum += days[today.minusDays(offset)]?.focusMillis ?: 0L
        }
        return sum
    }
}
