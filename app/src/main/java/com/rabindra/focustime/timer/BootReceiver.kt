package com.rabindra.focustime.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rabindra.focustime.FocusTimeApplication
import kotlinx.coroutines.launch

/**
 * Reconciles the persisted timer after reboot, app update, or a wall-clock/timezone change.
 * It never starts a foreground service from the background, which Android does not allow.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> Unit

            else -> return
        }
        val application = context.applicationContext as? FocusTimeApplication ?: return
        val pendingResult = goAsync()
        application.container.applicationScope.launch {
            try {
                if (intent.action == Intent.ACTION_TIME_CHANGED ||
                    intent.action == Intent.ACTION_TIMEZONE_CHANGED
                ) {
                    application.container.controller.onWallClockChanged()
                } else {
                    application.container.controller.reconcileNow()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
