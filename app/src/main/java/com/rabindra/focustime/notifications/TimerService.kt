package com.rabindra.focustime.notifications

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.rabindra.focustime.FocusTimeApplication
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.util.FocusLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the ongoing notification for an active session.
 * The countdown itself is timestamp based, so the service only reflects state.
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foregroundStarted = false

    private val container: com.rabindra.focustime.AppContainer
        get() = (application as FocusTimeApplication).container

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> container.controller.pauseSession()
            ACTION_RESUME -> container.controller.startSession()
            ACTION_SKIP -> container.controller.skipSession()
            ACTION_RESET -> container.controller.resetSession()
            else -> Unit
        }
        if (!foregroundStarted) {
            foregroundStarted = true
            startForegroundInternal()
            observeState()
        }
        return START_STICKY
    }

    private fun startForegroundInternal() {
        val notification = container.notificationHelper.progressNotification(
            container.controller.state.value,
            container.controller.remainingMillis.value,
        )
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ID_FOREGROUND,
                notification,
                type,
            )
        } catch (exception: Exception) {
            FocusLog.w("startForeground failed: ${exception.message}")
            stopSelf()
        }
    }

    private fun observeState() {
        scope.launch {
            combine(
                container.controller.state,
                container.controller.remainingMillis,
            ) { state, remaining -> state to remaining }
                .collect { (state, remaining) ->
                    if (state.status == TimerStatus.IDLE) {
                        ServiceCompat.stopForeground(this@TimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        container.notificationHelper.showForeground(state, remaining)
                    }
                }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_ENSURE = "com.rabindra.focustime.action.ENSURE"
        const val ACTION_PAUSE = "com.rabindra.focustime.action.PAUSE"
        const val ACTION_RESUME = "com.rabindra.focustime.action.RESUME"
        const val ACTION_SKIP = "com.rabindra.focustime.action.SKIP"
        const val ACTION_RESET = "com.rabindra.focustime.action.RESET"
    }
}
