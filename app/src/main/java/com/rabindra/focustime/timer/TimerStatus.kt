package com.rabindra.focustime.timer

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    ;

    val isRunning: Boolean get() = this == RUNNING
}
