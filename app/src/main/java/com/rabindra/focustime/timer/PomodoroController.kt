package com.rabindra.focustime.timer

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.rabindra.focustime.data.AppSettings
import com.rabindra.focustime.data.SettingsRepository
import com.rabindra.focustime.data.StatisticsRepository
import com.rabindra.focustime.data.TimerStateRepository
import com.rabindra.focustime.dnd.DndManager
import com.rabindra.focustime.notifications.NotificationHelper
import com.rabindra.focustime.notifications.TimerService
import com.rabindra.focustime.util.FocusLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for the Pomodoro timer. In-process countdown uses elapsed realtime so
 * clock changes cannot distort it; persisted deadlines use wall-clock time so recovery works
 * across process death, Doze and reboots.
 */
class PomodoroController(
    private val context: Context,
    private val timerStateRepository: TimerStateRepository,
    private val settingsRepository: SettingsRepository,
    private val statisticsRepository: StatisticsRepository,
    private val dndManager: DndManager,
    private val alarmScheduler: TimerAlarmScheduler,
    private val notifier: NotificationHelper,
    private val scope: CoroutineScope,
    private val timeSource: TimeSource = AndroidTimeSource,
) {

    private val mutex = Mutex()
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _remainingMillis = MutableStateFlow(25 * 60_000L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var config = TimerConfig()
    private var latestSettings = AppSettings()
    private var endElapsedRealtime = 0L
    private var initialized = false

    fun initialize() {
        scope.launch {
            mutex.withLock {
                if (initialized) return@withLock
                initialized = true
                settingsRepository.applyAutomaticFlowDefault()
                restoreLocked()
            }
            _ready.value = true
            scope.launch { ticker() }
            settingsRepository.settings.collect { settings ->
                latestSettings = settings
                onSettingsChanged(settings)
            }
        }
    }

    fun startSession() {
        scope.launch { mutex.withLock { startLocked() } }
    }

    fun pauseSession() {
        scope.launch { mutex.withLock { pauseLocked() } }
    }

    fun toggleSession() {
        scope.launch {
            mutex.withLock {
                if (_state.value.status == TimerStatus.RUNNING) pauseLocked() else startLocked()
            }
        }
    }

    fun resetSession() {
        scope.launch { mutex.withLock { resetLocked() } }
    }

    fun skipSession() {
        scope.launch { mutex.withLock { skipLocked() } }
    }

    /** Called by the alarm receiver and by the service on start. Safe to call at any time. */
    suspend fun reconcileNow() {
        mutex.withLock { reconcileLocked() }
    }

    suspend fun onWallClockChanged() {
        mutex.withLock { reanchorLocked() }
    }

    /** Re-attaches the foreground service after the app returns to the foreground. */
    fun ensureServiceRunning() {
        if (_state.value.status != TimerStatus.IDLE) startServiceInternal()
    }

    private suspend fun restoreLocked() {
        val settings = settingsRepository.current()
        latestSettings = settings
        config = settings.toTimerConfig()
        val persisted = timerStateRepository.read()
        if (persisted == null) {
            val initial = TimerTransitions.initialState(SessionType.FOCUS, config, nowWall())
            applyState(initial)
        } else {
            val result = TimerRecovery.reconcile(persisted, config, nowWall())
            applyState(result.state)
            if (result.completedFocus) recordFocus(result.completedFocusDurationMillis)
            result.completedSession?.let {
                notifier.notifySessionCompleted(it, result.state.sessionType)
            }
            if (result.state.status == TimerStatus.RUNNING) {
                alarmScheduler.schedule(result.state.endWallClockMillis)
                // A session recovered in the running state needs the service that matches it.
                startServiceInternal()
            } else {
                alarmScheduler.cancel()
            }
        }
        dndManager.reconcile(isFocusActive = isFocusRunning())
    }

    private suspend fun ticker() {
        while (true) {
            delay(TICK_INTERVAL_MILLIS)
            val current = _state.value
            if (current.status != TimerStatus.RUNNING) continue
            val remaining = remainingFromElapsed()
            publishRemaining(remaining)
            if (remaining <= 0L) {
                mutex.withLock { completeLocked(autoStart = true) }
            }
        }
    }

    private suspend fun startLocked() {
        val current = _state.value
        val next = TimerTransitions.start(current, config, nowWall())
        if (next == current) return
        applyState(next)
        if (next.status == TimerStatus.RUNNING) {
            alarmScheduler.schedule(next.endWallClockMillis)
            startServiceInternal()
            if (next.sessionType.isFocus) dndManager.applyForFocus()
        }
    }

    private suspend fun pauseLocked() {
        val current = _state.value
        if (current.status != TimerStatus.RUNNING) return
        val paused = TimerTransitions.withRemaining(
            TimerTransitions.pause(current, nowWall()),
            remainingFromElapsed(),
        )
        alarmScheduler.cancel()
        applyState(paused)
        dndManager.releaseIfOwned()
    }

    private suspend fun resetLocked() {
        val current = _state.value
        val next = TimerTransitions.reset(current, config, nowWall())
        alarmScheduler.cancel()
        applyState(next)
        dndManager.releaseIfOwned()
        stopServiceInternal()
    }

    private suspend fun skipLocked() {
        val next = TimerTransitions.skip(_state.value, config, nowWall())
        alarmScheduler.cancel()
        applyState(next)
        dndManager.releaseIfOwned()
        stopServiceInternal()
    }

    private suspend fun completeLocked(autoStart: Boolean) {
        val current = _state.value
        if (current.status != TimerStatus.RUNNING) return
        if (remainingFromElapsed() > 0L && nowWall() < current.endWallClockMillis) return
        val completedType = current.sessionType
        alarmScheduler.cancel()
        val next = TimerTransitions.advance(current, config, nowWall(), allowAutoStart = autoStart)
        applyState(next)
        if (completedType.isFocus) {
            recordFocus(current.durationMillis)
            dndManager.releaseIfOwned()
        }
        notifier.notifySessionCompleted(completedType, next.sessionType)
        playFeedback()
        if (next.status == TimerStatus.RUNNING) {
            alarmScheduler.schedule(next.endWallClockMillis)
            startServiceInternal()
            if (next.sessionType.isFocus) dndManager.applyForFocus()
        } else {
            stopServiceInternal()
        }
    }

    private suspend fun reconcileLocked() {
        val persisted = timerStateRepository.read() ?: return
        val settings = settingsRepository.current()
        latestSettings = settings
        config = settings.toTimerConfig()
        val result = TimerRecovery.reconcile(persisted, config, nowWall())
        if (result.state == persisted && persisted.status != TimerStatus.RUNNING) return
        alarmScheduler.cancel()
        applyState(result.state)
        if (result.completedFocus) recordFocus(result.completedFocusDurationMillis)
        // The alarm and boot paths complete a session the ticker may never have seen. They owe the
        // same side effects as the in-process completion: one alert, an alarm and service for the
        // auto-started session, and DND that follows the new session type.
        result.completedSession?.let {
            notifier.notifySessionCompleted(it, result.state.sessionType)
        }
        if (result.state.status == TimerStatus.RUNNING) {
            alarmScheduler.schedule(result.state.endWallClockMillis)
            if (result.startedSession) startServiceInternal()
        }
        dndManager.reconcile(isFocusActive = isFocusRunning())
    }

    private suspend fun reanchorLocked() {
        val current = _state.value
        if (current.status != TimerStatus.RUNNING) return
        val remaining = remainingFromElapsed()
        val next = TimerRecovery.reanchor(current, remaining, nowWall())
        applyState(next)
        alarmScheduler.schedule(next.endWallClockMillis)
    }

    private suspend fun onSettingsChanged(settings: AppSettings) {
        val previous = config
        val nextConfig = settings.toTimerConfig()
        val durationsChanged = previous.focusMinutes != nextConfig.focusMinutes ||
            previous.shortBreakMinutes != nextConfig.shortBreakMinutes ||
            previous.longBreakMinutes != nextConfig.longBreakMinutes ||
            previous.longBreakInterval != nextConfig.longBreakInterval
        config = nextConfig
        if (!settings.autoDnd) {
            dndManager.releaseIfOwned()
        }
        // Running sessions keep their agreed length; idle and paused sessions adopt the new one.
        if (durationsChanged && _state.value.status != TimerStatus.RUNNING) {
            mutex.withLock {
                val adjusted = TimerTransitions.applyConfig(_state.value, config, nowWall())
                if (adjusted != _state.value) applyState(adjusted)
            }
        }
    }

    private suspend fun applyState(next: TimerState, persist: Boolean = true) {
        _state.value = next
        val remaining = TimerTransitions.remainingAt(next, nowWall())
        publishRemaining(remaining)
        endElapsedRealtime = if (next.status == TimerStatus.RUNNING) {
            timeSource.elapsedRealtimeMillis() + remaining
        } else {
            0L
        }
        if (persist) timerStateRepository.write(next)
    }

    /** Publishes second-aligned remaining time so the UI and notification update once per second. */
    private fun publishRemaining(millis: Long) {
        val aligned = ((millis.coerceAtLeast(0L) + 999L) / 1000L) * 1000L
        if (_remainingMillis.value != aligned) _remainingMillis.value = aligned
    }

    private fun remainingFromElapsed(): Long {
        if (_state.value.status != TimerStatus.RUNNING) return _state.value.remainingMillis
        return (endElapsedRealtime - timeSource.elapsedRealtimeMillis()).coerceAtLeast(0L)
    }

    private fun isFocusRunning(): Boolean {
        val current = _state.value
        return current.status == TimerStatus.RUNNING && current.sessionType.isFocus
    }

    private suspend fun recordFocus(focusMillis: Long) {
        statisticsRepository.recordFocusSession(today(), focusMillis)
    }

    private fun today(): LocalDate = Instant.ofEpochMilli(nowWall()).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun nowWall(): Long = timeSource.wallClockMillis()

    private fun startServiceInternal() {
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TimerService::class.java).setAction(TimerService.ACTION_ENSURE),
            )
        }.onFailure { FocusLog.w("Timer service start refused: ${it.message}") }
    }

    private fun stopServiceInternal() {
        runCatching { context.stopService(Intent(context, TimerService::class.java)) }
    }

    private fun playFeedback() {
        if (latestSettings.soundEnabled) {
            runCatching {
                RingtoneManager.getRingtone(
                    context,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                )?.play()
            }
        }
        if (latestSettings.vibrateEnabled) {
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(VibratorManager::class.java).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Vibrator::class.java)
                }
                vibrator.vibrate(VibrationEffect.createOneShot(400L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 200L
    }
}
