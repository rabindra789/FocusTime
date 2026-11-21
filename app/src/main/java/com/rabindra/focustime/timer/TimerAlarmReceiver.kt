package com.rabindra.focustime.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rabindra.focustime.FocusTimeApplication
import kotlinx.coroutines.launch

/** Session-end safety net: reconciles persisted state when the service is not around. */
class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SESSION_END) return
        val application = context.applicationContext as? FocusTimeApplication ?: return
        val pendingResult = goAsync()
        application.container.applicationScope.launch {
            try {
                application.container.controller.reconcileNow()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SESSION_END = "com.rabindra.focustime.action.SESSION_END"
    }
}
