package com.rabindra.focustime.timer

enum class SessionType(val label: String, val compactLabel: String) {
    FOCUS("Focus session", "FOCUS"),
    SHORT_BREAK("Short break", "SHORT BREAK"),
    LONG_BREAK("Long break", "LONG BREAK");

    val isFocus: Boolean get() = this == FOCUS
}
