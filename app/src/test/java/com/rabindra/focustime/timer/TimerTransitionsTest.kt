package com.rabindra.focustime.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerTransitionsTest {

    private val config = TimerConfig(
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        longBreakInterval = 4,
    )
    private val now = 1_700_000_000_000L

    @Test
    fun `new timer is idle at full focus duration`() {
        val state = TimerTransitions.initialState(SessionType.FOCUS, config, now)
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(25 * 60_000L, state.remainingMillis)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun `start schedules a wall clock deadline`() {
        val started = TimerTransitions.start(
            TimerTransitions.initialState(SessionType.FOCUS, config, now),
            config,
            now,
        )
        assertEquals(TimerStatus.RUNNING, started.status)
        assertEquals(now + 25 * 60_000L, started.endWallClockMillis)
        assertEquals(25 * 60_000L, TimerTransitions.remainingAt(started, now))
    }

    @Test
    fun `start is idempotent while running`() {
        val started = TimerTransitions.start(
            TimerTransitions.initialState(SessionType.FOCUS, config, now),
            config,
            now,
        )
        val again = TimerTransitions.start(started, config, now + 5_000L)
        assertEquals(started, again)
    }

    @Test
    fun `pause freezes the remaining time`() {
        val started = TimerTransitions.start(
            TimerTransitions.initialState(SessionType.FOCUS, config, now),
            config,
            now,
        )
        val paused = TimerTransitions.pause(started, now + 60_000L)
        assertEquals(TimerStatus.PAUSED, paused.status)
        assertEquals(24 * 60_000L, paused.remainingMillis)
        assertEquals(24 * 60_000L, TimerTransitions.remainingAt(paused, now + 600_000L))
    }

    @Test
    fun `resume continues from the paused remainder`() {
        val started = TimerTransitions.start(
            TimerTransitions.initialState(SessionType.FOCUS, config, now),
            config,
            now,
        )
        val paused = TimerTransitions.pause(started, now + 60_000L)
        val resumed = TimerTransitions.start(paused, config, now + 120_000L)
        assertEquals(TimerStatus.RUNNING, resumed.status)
        assertEquals(now + 120_000L + 24 * 60_000L, resumed.endWallClockMillis)
    }

    @Test
    fun `reset returns the session to idle without losing the cycle`() {
        val state = TimerTransitions.initialState(SessionType.SHORT_BREAK, config, now).copy(
            status = TimerStatus.RUNNING,
            remainingMillis = 60_000L,
            cyclePosition = 2,
            sessionNumber = 3,
        )
        val reset = TimerTransitions.reset(state, config, now + 1_000L)
        assertEquals(TimerStatus.IDLE, reset.status)
        assertEquals(2, reset.cyclePosition)
        assertEquals(3, reset.sessionNumber)
        assertEquals(5 * 60_000L, reset.remainingMillis)
    }

    @Test
    fun `focus completion schedules a short break and keeps the session number`() {
        val focus = TimerTransitions.initialState(SessionType.FOCUS, config, now).copy(
            status = TimerStatus.RUNNING,
            cyclePosition = 0,
            sessionNumber = 1,
        )
        val next = TimerTransitions.advance(focus, config, now, allowAutoStart = false)
        assertEquals(SessionType.SHORT_BREAK, next.sessionType)
        assertEquals(TimerStatus.IDLE, next.status)
        assertEquals(1, next.cyclePosition)
        assertEquals(1, next.sessionNumber)
    }

    @Test
    fun `fourth focus session schedules a long break and resets the cycle`() {
        val focus = TimerTransitions.initialState(SessionType.FOCUS, config, now).copy(
            status = TimerStatus.RUNNING,
            cyclePosition = 3,
            sessionNumber = 4,
        )
        val next = TimerTransitions.advance(focus, config, now, allowAutoStart = false)
        assertEquals(SessionType.LONG_BREAK, next.sessionType)
        assertEquals(0, next.cyclePosition)
        assertEquals(4, next.sessionNumber)
        assertEquals(15 * 60_000L, next.remainingMillis)
    }

    @Test
    fun `long break interval is configurable`() {
        val shortConfig = config.copy(longBreakInterval = 2, shortBreakMinutes = 3)
        val second = TimerTransitions.initialState(SessionType.FOCUS, shortConfig, now).copy(
            status = TimerStatus.RUNNING,
            cyclePosition = 1,
        )
        val next = TimerTransitions.advance(second, shortConfig, now, allowAutoStart = false)
        assertEquals(SessionType.LONG_BREAK, next.sessionType)
        assertEquals(15 * 60_000L, next.remainingMillis)
    }

    @Test
    fun `break completion returns to focus and increments the session number`() {
        val breakState = TimerTransitions.initialState(SessionType.SHORT_BREAK, config, now).copy(
            status = TimerStatus.RUNNING,
            cyclePosition = 2,
            sessionNumber = 3,
        )
        val next = TimerTransitions.advance(breakState, config, now, allowAutoStart = false)
        assertEquals(SessionType.FOCUS, next.sessionType)
        assertEquals(2, next.cyclePosition)
        assertEquals(4, next.sessionNumber)
    }

    @Test
    fun `auto start breaks begins the break immediately`() {
        val autoConfig = config.copy(autoStartBreaks = true)
        val focus = TimerTransitions.initialState(SessionType.FOCUS, autoConfig, now).copy(
            status = TimerStatus.RUNNING,
        )
        val next = TimerTransitions.advance(focus, autoConfig, now + 25 * 60_000L, allowAutoStart = true)
        assertEquals(SessionType.SHORT_BREAK, next.sessionType)
        assertEquals(TimerStatus.RUNNING, next.status)
        assertEquals(5 * 60_000L, next.remainingMillis)
        assertEquals(now + 30 * 60_000L, next.endWallClockMillis)
    }

    @Test
    fun `automatic flow disabled leaves the next session idle`() {
        val manual = config.copy(autoStartBreaks = false, autoStartFocus = false)
        val focus = TimerTransitions.initialState(SessionType.FOCUS, manual, now).copy(
            status = TimerStatus.RUNNING,
        )
        val next = TimerTransitions.advance(focus, manual, now + 25 * 60_000L)
        assertEquals(SessionType.SHORT_BREAK, next.sessionType)
        assertEquals(TimerStatus.IDLE, next.status)
        assertEquals(5 * 60_000L, next.remainingMillis)
        assertEquals(0L, next.endWallClockMillis)
    }

    @Test
    fun `an auto started break begins at its full configured duration`() {
        val autoConfig = config.copy(autoStartBreaks = true)
        val focus = TimerTransitions.initialState(SessionType.FOCUS, autoConfig, now).copy(
            status = TimerStatus.RUNNING,
        )
        val completedAt = now + 25 * 60_000L
        val next = TimerTransitions.advance(focus, autoConfig, completedAt)
        assertEquals(TimerStatus.RUNNING, next.status)
        assertEquals(5 * 60_000L, next.durationMillis)
        assertEquals(5 * 60_000L, next.remainingMillis)
        assertEquals(completedAt + 5 * 60_000L, next.endWallClockMillis)
        assertEquals(0f, next.progress, 0.001f)
    }

    @Test
    fun `auto started focus keeps its configured duration and session number`() {
        val autoConfig = config.copy(autoStartBreaks = true, autoStartFocus = true)
        val breakState = TimerTransitions.initialState(SessionType.SHORT_BREAK, autoConfig, now).copy(
            status = TimerStatus.RUNNING,
            sessionNumber = 1,
            cyclePosition = 1,
        )
        val completedAt = now + 5 * 60_000L
        val focus = TimerTransitions.advance(breakState, autoConfig, completedAt)
        assertEquals(SessionType.FOCUS, focus.sessionType)
        assertEquals(TimerStatus.RUNNING, focus.status)
        assertEquals(25 * 60_000L, focus.durationMillis)
        assertEquals(25 * 60_000L, focus.remainingMillis)
        assertEquals(completedAt + 25 * 60_000L, focus.endWallClockMillis)
        assertEquals(2, focus.sessionNumber)
    }

    @Test
    fun `auto start focus begins the next focus session`() {
        val autoConfig = config.copy(autoStartFocus = true)
        val breakState = TimerTransitions.initialState(SessionType.SHORT_BREAK, autoConfig, now).copy(
            status = TimerStatus.RUNNING,
        )
        val next = TimerTransitions.advance(breakState, autoConfig, now + 5 * 60_000L, allowAutoStart = true)
        assertEquals(SessionType.FOCUS, next.sessionType)
        assertEquals(TimerStatus.RUNNING, next.status)
    }

    @Test
    fun `skip never auto starts even when configured`() {
        val autoConfig = config.copy(autoStartBreaks = true, autoStartFocus = true)
        val focus = TimerTransitions.initialState(SessionType.FOCUS, autoConfig, now).copy(
            status = TimerStatus.RUNNING,
        )
        val skipped = TimerTransitions.skip(focus, autoConfig, now)
        assertEquals(SessionType.SHORT_BREAK, skipped.sessionType)
        assertEquals(TimerStatus.IDLE, skipped.status)
        assertEquals(0L, skipped.endWallClockMillis)
    }

    @Test
    fun `apply config updates an idle duration`() {
        val idle = TimerTransitions.initialState(SessionType.FOCUS, config, now)
        val updated = TimerTransitions.applyConfig(idle, config.copy(focusMinutes = 40), now)
        assertEquals(40 * 60_000L, updated.remainingMillis)
        assertEquals(40 * 60_000L, updated.durationMillis)
    }

    @Test
    fun `apply config does not disturb a running session`() {
        val running = TimerTransitions.start(
            TimerTransitions.initialState(SessionType.FOCUS, config, now),
            config,
            now,
        )
        val updated = TimerTransitions.applyConfig(running, config.copy(focusMinutes = 40), now + 1_000L)
        assertEquals(running, updated)
    }

    @Test
    fun `finished detection only applies to running sessions`() {
        val running = TimerTransitions.start(
            TimerTransitions.initialState(SessionType.FOCUS, config, now),
            config,
            now,
        )
        assertFalse(TimerTransitions.isFinished(running, now + 1_000L))
        assertTrue(TimerTransitions.isFinished(running, now + 25 * 60_000L))
        val paused = TimerTransitions.pause(running, now + 1_000L)
        assertFalse(TimerTransitions.isFinished(paused, now + 10_000_000L))
    }

    @Test
    fun `remaining is clamped to the session duration`() {
        val running = TimerTransitions.start(
            TimerTransitions.initialState(SessionType.FOCUS, config, now),
            config,
            now,
        )
        val rewound = TimerTransitions.remainingAt(running, now - 60_000L)
        assertEquals(25 * 60_000L, rewound)
        val over = TimerTransitions.withRemaining(running, 99_999_999L)
        assertEquals(25 * 60_000L, over.remainingMillis)
    }

    @Test
    fun `a completed focus is followed by a full break without prior elapsed time`() {
        val started = TimerTransitions.start(
            TimerTransitions.initialState(SessionType.FOCUS, config, now),
            config,
            now,
        )
        val nearlyDone = started.copy(remainingMillis = 60_000L)
        val breakState = TimerTransitions.advance(nearlyDone, config, now + 24 * 60_000L, allowAutoStart = false)
        assertEquals(SessionType.SHORT_BREAK, breakState.sessionType)
        assertEquals(5 * 60_000L, breakState.remainingMillis)
        assertEquals(5 * 60_000L, breakState.durationMillis)
        assertEquals(0f, breakState.progress, 0.001f)
        assertEquals(0L, breakState.endWallClockMillis)
    }

    @Test
    fun `a new focus after a break starts at the configured duration`() {
        val breakState = TimerTransitions.initialState(SessionType.SHORT_BREAK, config, now)
            .copy(status = TimerStatus.RUNNING, remainingMillis = 15_000L, cyclePosition = 1)
        val focusState = TimerTransitions.advance(breakState, config, now, allowAutoStart = false)
        assertEquals(SessionType.FOCUS, focusState.sessionType)
        assertEquals(25 * 60_000L, focusState.remainingMillis)
        assertEquals(0f, focusState.progress, 0.001f)

        val started = TimerTransitions.start(focusState, config, now + 1_000L)
        assertEquals(25 * 60_000L, TimerTransitions.remainingAt(started, now + 1_000L))
        assertEquals(TimerStatus.RUNNING, started.status)
        assertEquals(now + 1_000L + 25 * 60_000L, started.endWallClockMillis)
    }

    @Test
    fun `reset restores the newly configured duration and clears elapsed time`() {
        val running = TimerTransitions.start(
            TimerTransitions.initialState(SessionType.FOCUS, config, now),
            config,
            now,
        ).copy(remainingMillis = 3 * 60_000L)
        val reset = TimerTransitions.reset(running, config.copy(focusMinutes = 40), now + 1_000L)
        assertEquals(TimerStatus.IDLE, reset.status)
        assertEquals(40 * 60_000L, reset.durationMillis)
        assertEquals(40 * 60_000L, reset.remainingMillis)
        assertEquals(0L, reset.endWallClockMillis)
        assertEquals(0f, reset.progress, 0.001f)
    }

    @Test
    fun `resetting a focus starts a fresh session and cycle`() {
        val state = TimerTransitions.initialState(SessionType.FOCUS, config, now).copy(
            status = TimerStatus.PAUSED,
            remainingMillis = 2 * 60_000L,
            cyclePosition = 3,
            sessionNumber = 9,
        )
        val reset = TimerTransitions.reset(state, config, now)
        assertEquals(0, reset.cyclePosition)
        assertEquals(1, reset.sessionNumber)
        assertEquals(25 * 60_000L, reset.remainingMillis)
    }

    @Test
    fun `pausing keeps the frozen remainder but never more than a shortened session`() {
        val paused = TimerTransitions.initialState(SessionType.FOCUS, config, now).copy(
            status = TimerStatus.PAUSED,
            remainingMillis = 24 * 60_000L,
        )
        val adjusted = TimerTransitions.applyConfig(paused, config.copy(focusMinutes = 2), now)
        assertEquals(2 * 60_000L, adjusted.durationMillis)
        assertEquals(2 * 60_000L, adjusted.remainingMillis)

        val resumed = TimerTransitions.start(adjusted, config.copy(focusMinutes = 2), now + 1_000L)
        assertEquals(now + 1_000L + 2 * 60_000L, resumed.endWallClockMillis)
    }

    @Test
    fun `elapsed time does not leak into a session started after a reset`() {
        val expired = TimerTransitions.initialState(SessionType.FOCUS, config, now).copy(
            status = TimerStatus.RUNNING,
            remainingMillis = 0L,
            endWallClockMillis = now,
            cyclePosition = 2,
            sessionNumber = 3,
        )
        val skipped = TimerTransitions.skip(expired, config, now)
        assertEquals(5 * 60_000L, skipped.remainingMillis)
        val resetFocus = TimerTransitions.reset(expired, config, now)
        assertEquals(25 * 60_000L, resetFocus.remainingMillis)
        assertEquals(0f, resetFocus.progress, 0.001f)
    }

    @Test
    fun `live progress follows the published remainder, not the persisted one`() {
        val running = TimerTransitions.start(
            TimerTransitions.initialState(SessionType.FOCUS, config, now),
            config,
            now,
        )
        // The persisted state still holds the full duration while the ticker publishes seconds.
        assertEquals(0f, running.progress, 0.001f)
        assertEquals(0.5f, running.progressAt(25 * 60_000L / 2), 0.001f)
        assertEquals(0.96f, running.progressAt(60_000L), 0.001f)
    }

    @Test
    fun `progress grows as the session elapses`() {
        val running = TimerTransitions.start(
            TimerTransitions.initialState(SessionType.FOCUS, config, now),
            config,
            now,
        )
        val halfway = running.copy(remainingMillis = 25 * 60_000L / 2)
        assertEquals(0.5f, halfway.progress, 0.001f)
        assertNotEquals(1f, halfway.progress)
    }
}
