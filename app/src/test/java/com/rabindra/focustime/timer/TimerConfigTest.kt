package com.rabindra.focustime.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerConfigTest {

    private val config = TimerConfig(
        focusMinutes = 50,
        shortBreakMinutes = 10,
        longBreakMinutes = 20,
        longBreakInterval = 6,
        autoStartBreaks = false,
        autoStartFocus = true,
    )

    @Test
    fun `duration uses the matching session length`() {
        assertEquals(50 * 60_000L, config.durationMillis(SessionType.FOCUS))
        assertEquals(10 * 60_000L, config.durationMillis(SessionType.SHORT_BREAK))
        assertEquals(20 * 60_000L, config.durationMillis(SessionType.LONG_BREAK))
    }

    @Test
    fun `duration clamps minutes to the supported range`() {
        assertEquals(1 * 60_000L, TimerConfig(focusMinutes = 0).durationMillis(SessionType.FOCUS))
        assertEquals(1 * 60_000L, TimerConfig(longBreakMinutes = -5).durationMillis(SessionType.LONG_BREAK))
        assertEquals(
            TimerConfig.MAX_MINUTES * 60_000L,
            TimerConfig(shortBreakMinutes = 999).durationMillis(SessionType.SHORT_BREAK),
        )
    }

    @Test
    fun `sanitized clamps every duration and the interval`() {
        val sanitized = TimerConfig(
            focusMinutes = 0,
            shortBreakMinutes = 999,
            longBreakMinutes = -2,
            longBreakInterval = 0,
        ).sanitized()

        assertEquals(TimerConfig.MIN_MINUTES, sanitized.focusMinutes)
        assertEquals(TimerConfig.MAX_MINUTES, sanitized.shortBreakMinutes)
        assertEquals(TimerConfig.MIN_MINUTES, sanitized.longBreakMinutes)
        assertEquals(TimerConfig.MIN_INTERVAL, sanitized.longBreakInterval)
    }

    @Test
    fun `sanitized keeps valid values and the automation flags`() {
        assertEquals(config, config.sanitized())
        assertFalse(config.sanitized().autoStartBreaks)
        assertTrue(config.sanitized().autoStartFocus)

        val upperBounds = TimerConfig(
            focusMinutes = TimerConfig.MAX_MINUTES,
            shortBreakMinutes = TimerConfig.MAX_MINUTES,
            longBreakMinutes = TimerConfig.MAX_MINUTES,
            longBreakInterval = TimerConfig.MAX_INTERVAL,
        )
        assertEquals(upperBounds, upperBounds.sanitized())
    }
}
