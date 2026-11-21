package com.rabindra.focustime.util

import android.util.Log
import com.rabindra.focustime.BuildConfig

object FocusLog {
    private const val TAG = "FocusTime"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }
}
