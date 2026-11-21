package com.rabindra.focustime

import android.content.Context
import com.rabindra.focustime.data.DndStateRepository
import com.rabindra.focustime.data.SettingsRepository
import com.rabindra.focustime.data.StatisticsRepository
import com.rabindra.focustime.data.TimerStateRepository
import com.rabindra.focustime.data.dndDataStore
import com.rabindra.focustime.data.settingsDataStore
import com.rabindra.focustime.data.statisticsDataStore
import com.rabindra.focustime.data.timerDataStore
import com.rabindra.focustime.dnd.DndManager
import com.rabindra.focustime.notifications.NotificationHelper
import com.rabindra.focustime.timer.PomodoroController
import com.rabindra.focustime.timer.TimerAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository = SettingsRepository(appContext.settingsDataStore)
    val statisticsRepository = StatisticsRepository(appContext.statisticsDataStore)
    val timerStateRepository = TimerStateRepository(appContext.timerDataStore)
    private val dndStateRepository = DndStateRepository(appContext.dndDataStore)

    val notificationHelper = NotificationHelper(appContext)
    val dndManager = DndManager(appContext, dndStateRepository, settingsRepository)
    val alarmScheduler = TimerAlarmScheduler(appContext)

    val controller = PomodoroController(
        context = appContext,
        timerStateRepository = timerStateRepository,
        settingsRepository = settingsRepository,
        statisticsRepository = statisticsRepository,
        dndManager = dndManager,
        alarmScheduler = alarmScheduler,
        notifier = notificationHelper,
        scope = applicationScope,
    )
}
