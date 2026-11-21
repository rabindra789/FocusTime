package com.rabindra.focustime.util

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MorseCodeTest {

    @Test
    fun `every digit uses the international five element code`() {
        val expected = mapOf(
            '0' to "-----",
            '1' to ".----",
            '2' to "..---",
            '3' to "...--",
            '4' to "....-",
            '5' to ".....",
            '6' to "-....",
            '7' to "--...",
            '8' to "---..",
            '9' to "----.",
        )
        expected.forEach { (digit, code) ->
            assertEquals(code, MorseCode.encodeDigit(digit))
        }
    }

    @Test
    fun `non digit characters encode to nothing`() {
        assertEquals("", MorseCode.encodeDigit(':'))
        assertEquals("", MorseCode.encodeDigit('x'))
    }

    @Test
    fun `time is encoded digit by digit in 24 hour form`() {
        val morse = MorseCode.encodeTime(LocalTime.of(14, 5), use24Hour = true)
        assertEquals(listOf(".----", "....-"), morse.hour)
        assertEquals(listOf("-----", "....."), morse.minute)
        assertNull(morse.meridiem)
    }

    @Test
    fun `twelve hour time uses the displayed hour and meridiem`() {
        val pm = MorseCode.encodeTime(LocalTime.of(14, 5), use24Hour = false)
        assertEquals(listOf("-----", "..---"), pm.hour)
        assertEquals("PM", pm.meridiem)

        val am = MorseCode.encodeTime(LocalTime.of(9, 30), use24Hour = false)
        assertEquals(listOf("-----", "----."), am.hour)
        assertEquals("AM", am.meridiem)
    }

    @Test
    fun `midnight and noon are twelve o'clock`() {
        assertEquals(listOf(".----", "..---"), MorseCode.encodeTime(LocalTime.MIDNIGHT, false).hour)
        assertEquals("AM", MorseCode.encodeTime(LocalTime.MIDNIGHT, false).meridiem)
        assertEquals(listOf(".----", "..---"), MorseCode.encodeTime(LocalTime.NOON, false).hour)
        assertEquals("PM", MorseCode.encodeTime(LocalTime.NOON, false).meridiem)
    }

    @Test
    fun `timing counts dots as one unit and dashes as three`() {
        // Widths plus one unit between elements: 5 dots + 4 gaps, 5 dashes + 4 gaps.
        assertEquals(9, MorseTime.unitsOf("....."))
        assertEquals(19, MorseTime.unitsOf("-----"))
        assertEquals(17, MorseTime.unitsOf(".----"))
    }

    @Test
    fun `hour and minute are separated by a word gap`() {
        val morse = MorseCode.encodeTime(LocalTime.of(14, 5), use24Hour = true)
        // 17 + gap 3 + 11 (hour) + word 7 + 19 + gap 3 + 9 (minute)
        assertEquals(69, morse.totalUnits)
    }

    @Test
    fun `single digit values are padded for consistent width`() {
        assertEquals(listOf("-----", "-----"), MorseCode.digitsOf(0))
        assertEquals(listOf("-----", "--..."), MorseCode.digitsOf(7))
    }

    @Test
    fun `meridiem letters are available in morse`() {
        assertEquals(".-", MorseCode.encodeLetter('A'))
        assertEquals("--", MorseCode.encodeLetter('m'))
        assertEquals(".--.", MorseCode.encodeLetter('P'))
    }
}
