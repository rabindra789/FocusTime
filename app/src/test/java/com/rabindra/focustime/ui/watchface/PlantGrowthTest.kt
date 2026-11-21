package com.rabindra.focustime.ui.watchface

import com.rabindra.focustime.timer.SessionType
import org.junit.Assert.assertEquals
import org.junit.Test

class PlantGrowthTest {

    @Test
    fun focusGrowsWithElapsedShare() {
        assertEquals(0f, plantGrowth(SessionType.FOCUS, 0f), 0.0001f)
        assertEquals(0.25f, plantGrowth(SessionType.FOCUS, 0.25f), 0.0001f)
        assertEquals(1f, plantGrowth(SessionType.FOCUS, 1f), 0.0001f)
    }

    @Test
    fun focusClampsOutOfRangeProgress() {
        assertEquals(0f, plantGrowth(SessionType.FOCUS, -0.4f), 0.0001f)
        assertEquals(1f, plantGrowth(SessionType.FOCUS, 1.8f), 0.0001f)
    }

    @Test
    fun breaksFoldBackToTheSeedAtAnyPoint() {
        val breakTypes = listOf(SessionType.SHORT_BREAK, SessionType.LONG_BREAK)
        for (breakType in breakTypes) {
            assertEquals(0f, plantGrowth(breakType, 0f), 0.0001f)
            assertEquals(0f, plantGrowth(breakType, 0.4f), 0.0001f)
            assertEquals(0f, plantGrowth(breakType, 1f), 0.0001f)
        }
    }
}
