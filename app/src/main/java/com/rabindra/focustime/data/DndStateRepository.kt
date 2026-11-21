package com.rabindra.focustime.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.rabindra.focustime.dnd.SavedDndState
import kotlinx.coroutines.flow.first

class DndStateRepository(private val dataStore: DataStore<Preferences>) {

    suspend fun read(): SavedDndState {
        val prefs = dataStore.data.first()
        return SavedDndState(
            previousFilter = prefs[KEY_PREVIOUS],
            appliedFilter = prefs[KEY_APPLIED],
        )
    }

    suspend fun write(previousFilter: Int, appliedFilter: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_PREVIOUS] = previousFilter
            prefs[KEY_APPLIED] = appliedFilter
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_PREVIOUS)
            prefs.remove(KEY_APPLIED)
        }
    }

    private companion object {
        val KEY_PREVIOUS = intPreferencesKey("previous_filter")
        val KEY_APPLIED = intPreferencesKey("applied_filter")
    }
}
