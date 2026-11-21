package com.rabindra.focustime.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StatisticsRepository(private val dataStore: DataStore<Preferences>) {

    val days: Flow<Map<LocalDate, DayStats>> =
        dataStore.data.map { StatsCodec.decode(it[KEY_DAYS]) }

    suspend fun recordFocusSession(date: LocalDate, focusMillis: Long) {
        dataStore.edit { prefs ->
            val days = StatsCodec.decode(prefs[KEY_DAYS])
            prefs[KEY_DAYS] = StatsCodec.encode(StatsCodec.addSession(days, date, focusMillis))
        }
    }

    suspend fun reset() {
        dataStore.edit { it.remove(KEY_DAYS) }
    }

    private companion object {
        val KEY_DAYS = stringPreferencesKey("daily_stats")
    }
}
