package com.rabindra.focustime.dnd

import android.app.NotificationManager

enum class DndMode(val label: String, val filter: Int) {
    PRIORITY("Priority only", NotificationManager.INTERRUPTION_FILTER_PRIORITY),
    ALARMS("Alarms only", NotificationManager.INTERRUPTION_FILTER_ALARMS),
    ;

    companion object {
        fun fromKey(key: String?): DndMode = entries.firstOrNull { it.name == key } ?: PRIORITY
    }
}
