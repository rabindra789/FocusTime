package com.rabindra.focustime.ui.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClockGesturesTest {

    private val minimum = 56f

    @Test
    fun shortDragsAreNotSwipes() {
        assertNull(swipeDirectionFor(10f, 0f, minimum))
        assertNull(swipeDirectionFor(0f, -40f, minimum))
        assertNull(swipeDirectionFor(0f, 0f, minimum))
    }

    @Test
    fun horizontalDragsKeepTheirDirection() {
        assertEquals(SwipeDirection.RIGHT, swipeDirectionFor(140f, 12f, minimum))
        assertEquals(SwipeDirection.LEFT, swipeDirectionFor(-140f, -12f, minimum))
    }

    @Test
    fun verticalDragsKeepTheirDirection() {
        assertEquals(SwipeDirection.UP, swipeDirectionFor(8f, -150f, minimum))
        assertEquals(SwipeDirection.DOWN, swipeDirectionFor(-8f, 150f, minimum))
    }

    @Test
    fun dominantAxisWinsOnDiagonals() {
        assertEquals(SwipeDirection.LEFT, swipeDirectionFor(-120f, 60f, minimum))
        assertEquals(SwipeDirection.DOWN, swipeDirectionFor(60f, 120f, minimum))
    }

    @Test
    fun exactlyAtTheThresholdStillCounts() {
        assertEquals(SwipeDirection.RIGHT, swipeDirectionFor(minimum, 0f, minimum))
        assertEquals(SwipeDirection.UP, swipeDirectionFor(0f, -minimum, minimum))
    }
}
