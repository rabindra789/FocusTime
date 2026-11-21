package com.rabindra.focustime.util

import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `countdown formats minutes and seconds`() {
        assertEquals("25:00", TimeFormatter.countdown(25 * 60_000L))
        assertEquals("00:00", TimeFormatter.countdown(0L))
        assertEquals("00:01", TimeFormatter.countdown(1L))
        assertEquals("00:02", TimeFormatter.countdown(1_100L))
        assertEquals("24:32", TimeFormatter.countdown(24 * 60_000L + 32_000L))
    }

    @Test
    fun `countdown includes hours for long sessions`() {
        assertEquals("1:04:32", TimeFormatter.countdown(3_872_000L))
        assertEquals("2:00:00", TimeFormatter.countdown(7_200_000L))
    }

    @Test
    fun `negative values are clamped`() {
        assertEquals("00:00", TimeFormatter.countdown(-500L))
    }

    @Test
    fun `twelve hour clock exposes a meridiem`() {
        assertEquals(ClockText("11:04", "AM"), TimeFormatter.clock(LocalTime.of(11, 4), use24Hour = false))
        assertEquals(ClockText("1:05", "PM"), TimeFormatter.clock(LocalTime.of(13, 5), use24Hour = false))
        assertEquals(ClockText("12:00", "AM"), TimeFormatter.clock(LocalTime.MIDNIGHT, use24Hour = false))
    }

    @Test
    fun `twenty four hour clock has no meridiem`() {
        val clock = TimeFormatter.clock(LocalTime.of(21, 7), use24Hour = true)
        assertEquals("21:07", clock.time)
        assertNull(clock.meridiem)
    }

    @Test
    fun `full date is uppercase`() {
        val date = LocalDate.of(2026, 11, 21)
        assertEquals("SATURDAY, 21 NOVEMBER", TimeFormatter.fullDate(date, Locale.ENGLISH))
    }

    @Test
    fun `duration label omits empty parts`() {
        assertEquals("0m", TimeFormatter.duration(0L).label)
        assertEquals("45m", TimeFormatter.duration(45 * 60_000L).label)
        assertEquals("2h", TimeFormatter.duration(120 * 60_000L).label)
        assertEquals("1h 15m", TimeFormatter.duration(75 * 60_000L).label)
    }

    @Test
    fun `session labels are padded`() {
        assertEquals("SESSION 01", TimeFormatter.sessionLabel(1))
        assertEquals("SESSION 12", TimeFormatter.sessionLabel(12))
    }
}
