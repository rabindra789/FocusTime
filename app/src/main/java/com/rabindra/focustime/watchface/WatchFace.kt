package com.rabindra.focustime.watchface

/** Landscape desk-clock faces. The id is what DataStore persists, so it must stay stable. */
enum class WatchFace(val id: String, val label: String) {
    DESK_CLOCK("desk_clock", "Desk clock"),
    CIRCULAR_POMODORO("circular_pomodoro", "Circular"),
    GROWING_PLANT("growing_plant", "Growing plant"),
    MORSE_CODE("morse_code", "Morse code"),
    FOREST("forest", "Forest"),
    ;

    fun next(): WatchFace = entries[(ordinal + 1) % entries.size]

    fun previous(): WatchFace = entries[(ordinal - 1 + entries.size) % entries.size]

    companion object {
        fun fromId(value: String?): WatchFace =
            entries.firstOrNull { it.id == value } ?: DESK_CLOCK
    }
}
