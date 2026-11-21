package com.rabindra.focustime.timer

data class TimerConfig(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakInterval: Int = 4,
    val autoStartBreaks: Boolean = false,
    val autoStartFocus: Boolean = false,
) {
    fun durationMillis(type: SessionType): Long = when (type) {
        SessionType.FOCUS -> focusMinutes
        SessionType.SHORT_BREAK -> shortBreakMinutes
        SessionType.LONG_BREAK -> longBreakMinutes
    }.coerceIn(MIN_MINUTES, MAX_MINUTES) * 60_000L

    fun sanitized(): TimerConfig = copy(
        focusMinutes = focusMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES),
        shortBreakMinutes = shortBreakMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES),
        longBreakMinutes = longBreakMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES),
        longBreakInterval = longBreakInterval.coerceIn(MIN_INTERVAL, MAX_INTERVAL),
    )

    companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 180
        const val MIN_INTERVAL = 2
        const val MAX_INTERVAL = 8
    }
}
