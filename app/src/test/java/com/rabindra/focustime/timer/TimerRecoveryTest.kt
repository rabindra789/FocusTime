package com.rabindra.focustime.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerRecoveryTest {

    private val config = TimerConfig(
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        longBreakInterval = 4,
        autoStartBreaks = true,
        autoStartFocus = true,
    )
    private val now = 1_700_000_000_000L

    private fun runningFocus(endedAt: Long, cycle: Int = 0) = TimerState(
        sessionType = SessionType.FOCUS,
        status = TimerStatus.RUNNING,
        durationMillis = 25 * 60_000L,
        remainingMillis = 25 * 60_000L,
        endWallClockMillis = endedAt,
        cyclePosition = cycle,
        sessionNumber = cycle + 1,
    )

    @Test
    fun `session that ended while the app was away completes exactly once`() {
        val persisted = runningFocus(endedAt = now - 3_600_000L)
        val result = TimerRecovery.reconcile(persisted, config, now)
        assertTrue(result.completedFocus)
        assertEquals(25 * 60_000L, result.completedFocusDurationMillis)
        assertEquals(SessionType.SHORT_BREAK, result.state.sessionType)
        assertEquals(TimerStatus.IDLE, result.state.status)

        val second = TimerRecovery.reconcile(result.state, config, now + 10_000L)
        assertFalse(second.completedFocus)
    }

    @Test
    fun `a long absence does not invent multiple sessions`() {
        val persisted = runningFocus(endedAt = now - 10 * 3_600_000L)
        val result = TimerRecovery.reconcile(persisted, config, now)
        assertTrue(result.completedFocus)
        assertEquals(TimerStatus.IDLE, result.state.status)
        assertEquals(1, result.state.sessionNumber)
        assertEquals(1, result.state.cyclePosition)
    }

    @Test
    fun `expired break does not count as a focus completion`() {
        val persisted = TimerState(
            sessionType = SessionType.SHORT_BREAK,
            status = TimerStatus.RUNNING,
            durationMillis = 5 * 60_000L,
            remainingMillis = 5 * 60_000L,
            endWallClockMillis = now - 60_000L,
            cyclePosition = 2,
            sessionNumber = 3,
        )
        val result = TimerRecovery.reconcile(persisted, config, now)
        assertFalse(result.completedFocus)
        assertEquals(SessionType.FOCUS, result.state.sessionType)
        assertEquals(TimerStatus.IDLE, result.state.status)
        assertEquals(4, result.state.sessionNumber)
    }

    @Test
    fun `running session that has time left resumes as-is`() {
        val persisted = runningFocus(endedAt = now + 10 * 60_000L)
        val result = TimerRecovery.reconcile(persisted, config, now)
        assertFalse(result.completedFocus)
        assertEquals(TimerStatus.RUNNING, result.state.status)
        assertEquals(10 * 60_000L, result.state.endWallClockMillis - now)
    }

    @Test
    fun `paused session keeps its remaining time after a reboot`() {
        val paused = TimerState(
            sessionType = SessionType.FOCUS,
            status = TimerStatus.PAUSED,
            durationMillis = 25 * 60_000L,
            remainingMillis = 7 * 60_000L,
            endWallClockMillis = 0L,
            cyclePosition = 1,
            sessionNumber = 2,
        )
        val result = TimerRecovery.reconcile(paused, config, now)
        assertFalse(result.completedFocus)
        assertEquals(TimerStatus.PAUSED, result.state.status)
        assertEquals(7 * 60_000L, result.state.remainingMillis)
    }

    @Test
    fun `idle session picks up new duration settings`() {
        val idle = TimerState(
            sessionType = SessionType.FOCUS,
            status = TimerStatus.IDLE,
            durationMillis = 25 * 60_000L,
            remainingMillis = 25 * 60_000L,
        )
        val result = TimerRecovery.reconcile(idle, config.copy(focusMinutes = 50), now)
        assertEquals(50 * 60_000L, result.state.remainingMillis)
        assertEquals(TimerStatus.IDLE, result.state.status)
    }

    @Test
    fun `clock moved backwards cannot create time beyond the session length`() {
        val persisted = runningFocus(endedAt = now + 25 * 60_000L)
        val rewound = TimerRecovery.reconcile(persisted, config, now - 3_600_000L)
        assertFalse(rewound.completedFocus)
        assertEquals(TimerStatus.RUNNING, rewound.state.status)
    }

    @Test
    fun `reanchor rebuilds the deadline from the elapsed remainder`() {
        val persisted = runningFocus(endedAt = now + 10 * 60_000L)
        val reanchored = TimerRecovery.reanchor(persisted, 10 * 60_000L, now + 3_600_000L)
        assertEquals(now + 3_600_000L + 10 * 60_000L, reanchored.endWallClockMillis)
        assertEquals(10 * 60_000L, reanchored.remainingMillis)
    }

    @Test
    fun `reanchor ignores paused sessions`() {
        val paused = TimerState(
            sessionType = SessionType.FOCUS,
            status = TimerStatus.PAUSED,
            remainingMillis = 60_000L,
            durationMillis = 25 * 60_000L,
        )
        assertEquals(paused, TimerRecovery.reanchor(paused, 60_000L, now))
    }
}
