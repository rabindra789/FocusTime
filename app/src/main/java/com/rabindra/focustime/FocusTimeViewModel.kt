package com.rabindra.focustime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rabindra.focustime.data.AppSettings
import com.rabindra.focustime.data.DayStats
import com.rabindra.focustime.data.StatsCodec
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StatisticsUi(
    val todaySessions: Int = 0,
    val todayFocusMillis: Long = 0L,
    val weekFocusMillis: Long = 0L,
    val totalSessions: Int = 0,
    val totalFocusMillis: Long = 0L,
)

class FocusTimeViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<TimerState> = container.controller.state
    val remainingMillis: StateFlow<Long> = container.controller.remainingMillis

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** False until DataStore has answered, so onboarding never flashes on an existing install. */
    val settingsLoaded: StateFlow<Boolean> = container.settingsRepository.settings
        .map { true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val statistics: StateFlow<StatisticsUi> = container.statisticsRepository.days
        .map { days -> days.toUi(LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUi())

    private val _dndAccess = MutableStateFlow(container.dndManager.hasAccess())
    val dndAccess: StateFlow<Boolean> = _dndAccess.asStateFlow()

    fun refreshDndAccess() {
        _dndAccess.value = container.dndManager.hasAccess()
    }

    fun dndSettingsIntent() = container.dndManager.accessSettingsIntent()

    fun toggleTimer() = container.controller.toggleSession()

    fun resetTimer() = container.controller.resetSession()

    fun skipSession() = container.controller.skipSession()

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { container.settingsRepository.update(transform) }
    }

    fun resetStatistics() {
        viewModelScope.launch { container.statisticsRepository.reset() }
    }

    private fun Map<LocalDate, DayStats>.toUi(today: LocalDate): StatisticsUi {
        val day = this[today] ?: DayStats()
        return StatisticsUi(
            todaySessions = day.focusSessions,
            todayFocusMillis = day.focusMillis,
            weekFocusMillis = StatsCodec.weeklyMillis(this, today),
            totalSessions = StatsCodec.totalSessions(this),
            totalFocusMillis = StatsCodec.totalMillis(this),
        )
    }

    companion object {
        fun factory(application: FocusTimeApplication): ViewModelProvider.Factory = viewModelFactory {
            initializer { FocusTimeViewModel(application.container) }
        }

        const val DAILY_GOAL = 8
    }
}

fun TimerState.isActiveFocus(): Boolean =
    status == TimerStatus.RUNNING && sessionType == SessionType.FOCUS
