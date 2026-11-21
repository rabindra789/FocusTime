package com.rabindra.focustime.timer

/**
 * Logical timer state. [endWallClockMillis] is the wall-clock deadline used for persistence
 * and recovery. In-process countdown accuracy is derived from elapsed realtime in the controller.
 */
data class TimerState(
    val sessionType: SessionType = SessionType.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val durationMillis: Long = 25 * 60_000L,
    val remainingMillis: Long = 25 * 60_000L,
    val endWallClockMillis: Long = 0L,
    val cyclePosition: Int = 0,
    val sessionNumber: Int = 1,
    val updatedAtMillis: Long = 0L,
) {
    val progress: Float
        get() = if (durationMillis <= 0L) 0f
        else (1f - remainingMillis.toFloat() / durationMillis).coerceIn(0f, 1f)

    /**
     * Live progress for a running session. The published [remainingMillis] flow ticks every
     * second while [remainingMillis] in the persisted state only changes on state transitions,
     * so screens must use this instead of [progress] to draw a moving indicator.
     */
    fun progressAt(remainingMillis: Long): Float = if (durationMillis <= 0L) {
        0f
    } else {
        (1f - remainingMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
    }
}
