package com.rabindra.focustime.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val Context.timerDataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_state")
val Context.statisticsDataStore: DataStore<Preferences> by preferencesDataStore(name = "statistics")
val Context.dndDataStore: DataStore<Preferences> by preferencesDataStore(name = "dnd_state")
