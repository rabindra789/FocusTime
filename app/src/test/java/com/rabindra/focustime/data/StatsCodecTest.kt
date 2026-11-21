package com.rabindra.focustime.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsCodecTest {

    private val today = LocalDate.of(2026, 11, 21)

    @Test
    fun `round trips daily stats`() {
        val days = mapOf(
            today to DayStats(focusSessions = 3, focusMillis = 4_500_000L),
            today.minusDays(1) to DayStats(focusSessions = 1, focusMillis = 1_500_000L),
        )
        val decoded = StatsCodec.decode(StatsCodec.encode(days))
        assertEquals(days, decoded)
    }

    @Test
    fun `decoding an empty value yields no days`() {
        assertTrue(StatsCodec.decode(null).isEmpty())
        assertTrue(StatsCodec.decode("").isEmpty())
    }

    @Test
    fun `malformed rows are skipped without losing valid rows`() {
        val raw = listOf(
            "$today\u00013\u00014500000",
            "not-a-date\u00011\u00011000",
            "${today.minusDays(1)}\u0001x\u00011000",
            "${today.minusDays(2)}\u00012\u0001-5",
            "missing,fields",
        ).joinToString("\n")
        val decoded = StatsCodec.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals(DayStats(3, 4_500_000L), decoded[today])
    }

    @Test
    fun `adding a session accumulates sessions and focus time`() {
        var days = StatsCodec.addSession(emptyMap(), today, 25 * 60_000L)
        days = StatsCodec.addSession(days, today, 25 * 60_000L)
        assertEquals(2, days[today]?.focusSessions)
        assertEquals(50 * 60_000L, days[today]?.focusMillis)
    }

    @Test
    fun `weekly window covers today and the six preceding days`() {
        val days = mapOf(
            today to DayStats(1, 60_000L),
            today.minusDays(6) to DayStats(1, 120_000L),
            today.minusDays(7) to DayStats(5, 999_000L),
        )
        assertEquals(180_000L, StatsCodec.weeklyMillis(days, today))
    }

    @Test
    fun `totals sum every stored day`() {
        val days = mapOf(
            today to DayStats(2, 3_000_000L),
            today.minusDays(30) to DayStats(1, 1_500_000L),
        )
        assertEquals(3, StatsCodec.totalSessions(days))
        assertEquals(4_500_000L, StatsCodec.totalMillis(days))
    }
}
