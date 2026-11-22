package com.rabindra.focustime.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `session that ended while the app was away starts its break exactly once`() {
        val persisted = runningFocus(endedAt = now - 3_600_000L)
        val result = TimerRecovery.reconcile(persisted, config, now)
        assertTrue(result.completedFocus)
        assertEquals(25 * 60_000L, result.completedFocusDurationMillis)
        assertEquals(SessionType.SHORT_BREAK, result.state.sessionType)
        assertEquals(TimerStatus.RUNNING, result.state.status)
        assertEquals(SessionType.FOCUS, result.completedSession)
        assertTrue(result.startedSession)
        // The recovered break starts now, at its configured length, not at the stale deadline.
        assertEquals(5 * 60_000L, result.state.remainingMillis)
        assertEquals(5 * 60_000L, result.state.durationMillis)
        assertEquals(now + 5 * 60_000L, result.state.endWallClockMillis)

        val second = TimerRecovery.reconcile(result.state, config, now + 10_000L)
        assertFalse(second.completedFocus)
        assertNull(second.completedSession)
        assertEquals(TimerStatus.RUNNING, second.state.status)
    }

    @Test
    fun `a long absence does not invent multiple sessions`() {
        val persisted = runningFocus(endedAt = now - 10 * 3_600_000L)
        val result = TimerRecovery.reconcile(persisted, config, now)
        assertTrue(result.completedFocus)
        assertEquals(SessionType.SHORT_BREAK, result.state.sessionType)
        assertEquals(1, result.state.sessionNumber)
        assertEquals(1, result.state.cyclePosition)
    }

    @Test
    fun `expired break starts the next focus and does not count as a focus completion`() {
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
        assertEquals(SessionType.SHORT_BREAK, result.completedSession)
        assertEquals(SessionType.FOCUS, result.state.sessionType)
        assertEquals(TimerStatus.RUNNING, result.state.status)
        assertEquals(4, result.state.sessionNumber)
        assertEquals(25 * 60_000L, result.state.remainingMillis)
    }

    @Test
    fun `disabling automatic flow leaves the next session idle`() {
        val manual = config.copy(autoStartBreaks = false, autoStartFocus = false)
        val result = TimerRecovery.reconcile(runningFocus(endedAt = now - 60_000L), manual, now)
        assertTrue(result.completedFocus)
        assertEquals(SessionType.SHORT_BREAK, result.state.sessionType)
        assertEquals(TimerStatus.IDLE, result.state.status)
        assertFalse(result.startedSession)
        assertEquals(0L, result.state.endWallClockMillis)
    }

    @Test
    fun `recovery can be told not to auto start even when flow is enabled`() {
        val result = TimerRecovery.reconcile(
            runningFocus(endedAt = now - 60_000L),
            config,
            now,
            allowAutoStart = false,
        )
        assertEquals(TimerStatus.IDLE, result.state.status)
        assertEquals(SessionType.FOCUS, result.completedSession)
    }

    @Test
    fun `recovered long break respects the configured interval`() {
        val persisted = runningFocus(endedAt = now - 60_000L, cycle = 3)
        val result = TimerRecovery.reconcile(persisted, config, now)
        assertEquals(SessionType.LONG_BREAK, result.state.sessionType)
        assertEquals(TimerStatus.RUNNING, result.state.status)
        assertEquals(15 * 60_000L, result.state.remainingMillis)
        assertEquals(0, result.state.cyclePosition)
    }

    @Test
    fun `running session that has time left resumes as-is`() {
        val persisted = runningFocus(endedAt = now + 10 * 60_000L)
        val result = TimerRecovery.reconcile(persisted, config, now)
        assertFalse(result.completedFocus)
        assertNull(result.completedSession)
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
