package com.rabindra.focustime.util

import java.time.LocalTime

/**
 * International Morse code for digits, plus the timing rules the dial needs.
 *
 * A dot is one unit, a dash three units. Elements inside a digit are separated by one unit,
 * digits by three units, and the hour/minute words by seven units.
 */
object MorseCode {

    const val UNIT_GAP = 1
    const val DIGIT_GAP = 3
    const val WORD_GAP = 7

    private val digits = mapOf(
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

    val letters: Map<Char, String> = mapOf(
        'A' to ".-",
        'M' to "--",
        'P' to ".--.",
    )

    /** ".----" for '1', or an empty string for anything that is not a digit. */
    fun encodeDigit(digit: Char): String = digits[digit] ?: ""

    fun encodeLetter(letter: Char): String = letters[letter.uppercaseChar()] ?: ""

    fun digitsOf(value: Int): List<String> =
        value.toString().padStart(2, '0').map { encodeDigit(it) }

    /** Encodes the clock time exactly as displayed: 24-hour or 12-hour plus its meridiem. */
    fun encodeTime(time: LocalTime, use24Hour: Boolean): MorseTime {
        val hour = if (use24Hour) time.hour else ((time.hour + 11) % 12) + 1
        val meridiem = if (use24Hour) null else if (time.hour < 12) "AM" else "PM"
        return MorseTime(hour = digitsOf(hour), minute = digitsOf(time.minute), meridiem = meridiem)
    }
}

/** Digits are whole words; the hour and minute are separated by a word gap. */
data class MorseTime(
    val hour: List<String>,
    val minute: List<String>,
    val meridiem: String?,
) {
    /** Timing units from the first dot to the last dash, word gap included. */
    val totalUnits: Int
        get() = hour.fold(0) { total, code -> total + unitsOf(code) } +
            MorseCode.DIGIT_GAP * (hour.size - 1) +
            MorseCode.WORD_GAP +
            minute.fold(0) { total, code -> total + unitsOf(code) } +
            MorseCode.DIGIT_GAP * (minute.size - 1)

    companion object {
        fun unitsOf(code: String): Int =
            code.fold(0) { total, element -> total + if (element == '-') 3 else 1 } +
                (code.length - 1) * MorseCode.UNIT_GAP
    }
}
