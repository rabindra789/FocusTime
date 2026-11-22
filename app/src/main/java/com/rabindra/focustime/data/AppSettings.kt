package com.rabindra.focustime.data

import com.rabindra.focustime.dnd.DndMode
import com.rabindra.focustime.timer.TimerConfig
import com.rabindra.focustime.watchface.WatchFace

data class AppSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakInterval: Int = 4,
    val autoStartBreaks: Boolean = true,
    val autoStartFocus: Boolean = false,
    val autoDnd: Boolean = true,
    val dndMode: DndMode = DndMode.PRIORITY,
    val use24Hour: Boolean = false,
    val keepScreenOn: Boolean = true,
    val lowPowerMode: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val tutorialCompleted: Boolean = false,
    val watchFace: WatchFace = WatchFace.DESK_CLOCK,
) {
    fun toTimerConfig(): TimerConfig = TimerConfig(
        focusMinutes = focusMinutes,
        shortBreakMinutes = shortBreakMinutes,
        longBreakMinutes = longBreakMinutes,
        longBreakInterval = longBreakInterval,
        autoStartBreaks = autoStartBreaks,
        autoStartFocus = autoStartFocus,
    ).sanitized()
}
