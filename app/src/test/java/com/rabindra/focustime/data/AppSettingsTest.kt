package com.rabindra.focustime.data

import com.rabindra.focustime.timer.TimerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `default settings map to the default timer config`() {
        assertEquals(TimerConfig(), AppSettings().toTimerConfig())
    }

    @Test
    fun `every timer field is carried over`() {
        val settings = AppSettings(
            focusMinutes = 40,
            shortBreakMinutes = 8,
            longBreakMinutes = 24,
            longBreakInterval = 3,
            autoStartBreaks = false,
            autoStartFocus = true,
        )

        assertEquals(
            TimerConfig(
                focusMinutes = 40,
                shortBreakMinutes = 8,
                longBreakMinutes = 24,
                longBreakInterval = 3,
                autoStartBreaks = false,
                autoStartFocus = true,
            ),
            settings.toTimerConfig(),
        )
    }

    @Test
    fun `invalid stored durations are sanitised before reaching the timer`() {
        val settings = AppSettings(
            focusMinutes = 0,
            shortBreakMinutes = 999,
            longBreakMinutes = -10,
            longBreakInterval = 99,
            autoStartBreaks = false,
            autoStartFocus = true,
        )

        val config = settings.toTimerConfig()
        assertEquals(TimerConfig.MIN_MINUTES, config.focusMinutes)
        assertEquals(TimerConfig.MAX_MINUTES, config.shortBreakMinutes)
        assertEquals(TimerConfig.MIN_MINUTES, config.longBreakMinutes)
        assertEquals(TimerConfig.MAX_INTERVAL, config.longBreakInterval)
        assertFalse(config.autoStartBreaks)
        assertTrue(config.autoStartFocus)
    }
}
