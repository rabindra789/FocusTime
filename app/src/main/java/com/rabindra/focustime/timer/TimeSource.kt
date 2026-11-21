package com.rabindra.focustime.timer

import android.os.SystemClock

interface TimeSource {
    fun elapsedRealtimeMillis(): Long
    fun wallClockMillis(): Long
}

object AndroidTimeSource : TimeSource {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
    override fun wallClockMillis(): Long = System.currentTimeMillis()
}
