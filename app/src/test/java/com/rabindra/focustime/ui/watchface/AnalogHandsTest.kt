package com.rabindra.focustime.ui.watchface

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalogHandsTest {

    private fun assertAngles(
        time: LocalTime,
        hour: Float,
        minute: Float,
        second: Float,
    ) {
        val angles = analogHandAngles(time)
        assertEquals("hour", hour, angles.hour, 0.001f)
        assertEquals("minute", minute, angles.minute, 0.001f)
        assertEquals("second", second, angles.second, 0.001f)
    }

    @Test
    fun midnightPointsEveryHandAtTwelve() {
        assertAngles(LocalTime.MIDNIGHT, 0f, 0f, 0f)
    }

    @Test
    fun threeOClockPointsTheHourHandAtThree() {
        assertAngles(LocalTime.of(3, 0), 90f, 0f, 0f)
    }

    @Test
    fun halfPastThreeAdvancesBothHands() {
        assertAngles(LocalTime.of(3, 30), 105f, 180f, 0f)
    }

    @Test
    fun quarterToOneCarriesMinutesIntoTheHourHand() {
        assertAngles(LocalTime.of(12, 45, 30), 22.75f, 273f, 180f)
    }

    @Test
    fun afternoonHoursUseATwelveHourDial() {
        val morning = analogHandAngles(LocalTime.of(3, 30, 15))
        val afternoon = analogHandAngles(LocalTime.of(15, 30, 15))
        assertEquals(morning, afternoon)
    }

    @Test
    fun secondsCreepTheMinuteHandForward() {
        assertAngles(LocalTime.of(10, 9, 30), 304.75f, 57f, 180f)
    }
}
