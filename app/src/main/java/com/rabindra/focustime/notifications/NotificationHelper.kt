package com.rabindra.focustime.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.rabindra.focustime.MainActivity
import com.rabindra.focustime.R
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import com.rabindra.focustime.util.TimeFormatter

class NotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    fun createChannels() {
        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            context.getString(R.string.channel_timer_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_timer_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.channel_alerts_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_alerts_description)
            setShowBadge(true)
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannels(listOf(timerChannel, alertsChannel))
    }

    fun progressNotification(state: TimerState, remainingMillis: Long): Notification {
        val time = TimeFormatter.countdown(remainingMillis)
        val statusLine = when (state.status) {
            TimerStatus.RUNNING -> "$time remaining"
            TimerStatus.PAUSED -> "Paused · $time remaining"
            TimerStatus.IDLE -> "${state.sessionType.label} ready"
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(state.sessionType.compactLabel)
            .setContentText(statusLine)
            .setSubText(TimeFormatter.sessionLabel(state.sessionNumber))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(openAppIntent())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (state.status == TimerStatus.RUNNING) {
            builder.addAction(0, "Pause", serviceAction(TimerService.ACTION_PAUSE))
            builder.addAction(0, "Skip", serviceAction(TimerService.ACTION_SKIP))
        } else if (state.status == TimerStatus.PAUSED) {
            builder.addAction(0, "Resume", serviceAction(TimerService.ACTION_RESUME))
            builder.addAction(0, "Reset", serviceAction(TimerService.ACTION_RESET))
        }
        return builder.build()
    }

    fun notifySessionCompleted(completed: SessionType, next: SessionType) {
        val title = if (completed.isFocus) "Focus session complete" else "Break over"
        val body = when {
            completed.isFocus && next == SessionType.LONG_BREAK -> "Long break unlocked. Step away for a while."
            completed.isFocus -> "Take a short break."
            else -> "Ready for the next focus session."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        runCatching { notificationManager.notify(NOTIFICATION_ID_ALERT, notification) }
    }

    fun showForeground(state: TimerState, remainingMillis: Long) {
        runCatching {
            notificationManager.notify(NOTIFICATION_ID_FOREGROUND, progressNotification(state, remainingMillis))
        }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun serviceAction(action: String): PendingIntent = PendingIntent.getService(
        context,
        action.hashCode(),
        Intent(context, TimerService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_TIMER = "focus_timer"
        const val CHANNEL_ALERTS = "focus_alerts"
        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_ALERT = 1002
    }
}
