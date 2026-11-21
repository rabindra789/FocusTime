package com.rabindra.focustime.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rabindra.focustime.timer.SessionType
import com.rabindra.focustime.timer.TimerState
import com.rabindra.focustime.timer.TimerStatus
import kotlinx.coroutines.flow.first

class TimerStateRepository(private val dataStore: DataStore<Preferences>) {

    suspend fun read(): TimerState? {
        val prefs = dataStore.data.first()
        val type = prefs[KEY_TYPE]?.let { key -> SessionType.entries.firstOrNull { it.name == key } }
            ?: return null
        val status = prefs[KEY_STATUS]?.let { key -> TimerStatus.entries.firstOrNull { it.name == key } }
            ?: return null
        return TimerState(
            sessionType = type,
            status = status,
            durationMillis = prefs[KEY_DURATION] ?: 0L,
            remainingMillis = prefs[KEY_REMAINING] ?: 0L,
            endWallClockMillis = prefs[KEY_END_WALL] ?: 0L,
            cyclePosition = prefs[KEY_CYCLE] ?: 0,
            sessionNumber = prefs[KEY_SESSION] ?: 1,
            updatedAtMillis = prefs[KEY_UPDATED] ?: 0L,
        )
    }

    suspend fun write(state: TimerState) {
        dataStore.edit { prefs ->
            prefs[KEY_TYPE] = state.sessionType.name
            prefs[KEY_STATUS] = state.status.name
            prefs[KEY_DURATION] = state.durationMillis
            prefs[KEY_REMAINING] = state.remainingMillis
            prefs[KEY_END_WALL] = state.endWallClockMillis
            prefs[KEY_CYCLE] = state.cyclePosition
            prefs[KEY_SESSION] = state.sessionNumber
            prefs[KEY_UPDATED] = state.updatedAtMillis
        }
    }

    private companion object {
        val KEY_TYPE = stringPreferencesKey("session_type")
        val KEY_STATUS = stringPreferencesKey("status")
        val KEY_DURATION = longPreferencesKey("duration_millis")
        val KEY_REMAINING = longPreferencesKey("remaining_millis")
        val KEY_END_WALL = longPreferencesKey("end_wall_clock")
        val KEY_CYCLE = intPreferencesKey("cycle_position")
        val KEY_SESSION = intPreferencesKey("session_number")
        val KEY_UPDATED = longPreferencesKey("updated_at")
    }
}
