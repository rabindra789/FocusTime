package com.rabindra.focustime.ui.watchface

import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.watchface.WatchFace
import org.junit.Assert.assertEquals
import org.junit.Test

class ForestGrowthTest {

    @Test
    fun focusStartsBareAndEndsFull() {
        assertEquals(0f, forestGrowth(SessionType.FOCUS, 0f), 0.0001f)
        assertEquals(0.5f, forestGrowth(SessionType.FOCUS, 0.5f), 0.0001f)
        assertEquals(1f, forestGrowth(SessionType.FOCUS, 1f), 0.0001f)
    }

    @Test
    fun breaksStartFullAndClearGradually() {
        for (breakType in listOf(SessionType.SHORT_BREAK, SessionType.LONG_BREAK)) {
            assertEquals(1f, forestGrowth(breakType, 0f), 0.0001f)
            assertEquals(0.5f, forestGrowth(breakType, 0.5f), 0.0001f)
            assertEquals(0f, forestGrowth(breakType, 1f), 0.0001f)
        }
    }

    @Test
    fun progressOutsideZeroToOneIsClamped() {
        assertEquals(0f, forestGrowth(SessionType.FOCUS, -2f), 0.0001f)
        assertEquals(1f, forestGrowth(SessionType.FOCUS, 3f), 0.0001f)
        assertEquals(1f, forestGrowth(SessionType.SHORT_BREAK, -1f), 0.0001f)
        assertEquals(0f, forestGrowth(SessionType.SHORT_BREAK, 2f), 0.0001f)
    }

    @Test
    fun forestIsTheFifthFaceAndItsStoredIdIsStable() {
        assertEquals(5, WatchFace.entries.size)
        assertEquals(WatchFace.FOREST, WatchFace.fromId("forest"))
        assertEquals("forest", WatchFace.FOREST.id)
        assertEquals(WatchFace.FOREST, WatchFace.MORSE_CODE.next())
        assertEquals(WatchFace.DESK_CLOCK, WatchFace.FOREST.next())
        assertEquals(WatchFace.MORSE_CODE, WatchFace.FOREST.previous())
    }
}
