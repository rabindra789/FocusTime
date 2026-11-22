package com.rabindra.focustime.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerStateTest {

    private val state = TimerState(
        durationMillis = 100_000L,
        remainingMillis = 100_000L,
    )

    @Test
    fun `progress follows the stored remainder`() {
        assertEquals(0f, state.progress, 0.0001f)
        assertEquals(0.25f, state.copy(remainingMillis = 75_000L).progress, 0.0001f)
        assertEquals(1f, state.copy(remainingMillis = 0L).progress, 0.0001f)
    }

    @Test
    fun `progress clamps remainders outside the session`() {
        assertEquals(1f, state.copy(remainingMillis = -5_000L).progress, 0.0001f)
        assertEquals(0f, state.copy(remainingMillis = 250_000L).progress, 0.0001f)
    }

    @Test
    fun `progress is zero for a zero or negative duration`() {
        assertEquals(0f, state.copy(durationMillis = 0L).progress, 0.0001f)
        assertEquals(0f, state.copy(durationMillis = -1L, remainingMillis = 0L).progress, 0.0001f)
    }

    @Test
    fun `progressAt uses the live remainder not the stored one`() {
        val stored = state.copy(remainingMillis = 100_000L)

        assertEquals(0.5f, stored.progressAt(50_000L), 0.0001f)
        assertEquals(0.96f, stored.progressAt(4_000L), 0.0001f)
        assertEquals(1f, stored.progressAt(0L), 0.0001f)
    }

    @Test
    fun `progressAt clamps beyond the session bounds`() {
        assertEquals(0f, state.progressAt(200_000L), 0.0001f)
        assertEquals(1f, state.progressAt(-1L), 0.0001f)
    }

    @Test
    fun `progressAt is zero for an invalid duration`() {
        assertEquals(0f, state.copy(durationMillis = 0L).progressAt(0L), 0.0001f)
        assertEquals(0f, state.copy(durationMillis = -1L).progressAt(0L), 0.0001f)
    }
}
