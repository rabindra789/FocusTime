package com.rabindra.focustime.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rabindra.focustime.dnd.DndMode
import com.rabindra.focustime.watchface.WatchFace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = dataStore.data.map { it.toSettings() }.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings()).validated()
            prefs[KEY_FOCUS] = updated.focusMinutes
            prefs[KEY_SHORT] = updated.shortBreakMinutes
            prefs[KEY_LONG] = updated.longBreakMinutes
            prefs[KEY_INTERVAL] = updated.longBreakInterval
            prefs[KEY_AUTO_BREAK] = updated.autoStartBreaks
            prefs[KEY_AUTO_FOCUS] = updated.autoStartFocus
            prefs[KEY_AUTO_DND] = updated.autoDnd
            prefs[KEY_DND_MODE] = updated.dndMode.name
            prefs[KEY_24H] = updated.use24Hour
            prefs[KEY_KEEP_AWAKE] = updated.keepScreenOn
            prefs[KEY_LOW_POWER] = updated.lowPowerMode
            prefs[KEY_SOUND] = updated.soundEnabled
            prefs[KEY_VIBRATE] = updated.vibrateEnabled
            prefs[KEY_TUTORIAL] = updated.tutorialCompleted
            prefs[KEY_WATCH_FACE] = updated.watchFace.id
        }
    }

    /**
     * One-time upgrade to the new default: automatic breaks. Older builds defaulted both flow
     * toggles to off and rewrote every preference on the first edit, so an existing install cannot
     * be distinguished from a deliberate opt-out. The migration therefore turns automatic breaks
     * on once, records that it ran, and every later manual choice is respected.
     */
    suspend fun applyAutomaticFlowDefault() {
        dataStore.edit { prefs ->
            if (prefs[KEY_AUTO_FLOW_DEFAULT] == true) return@edit
            prefs[KEY_AUTO_BREAK] = true
            prefs[KEY_AUTO_FLOW_DEFAULT] = true
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        focusMinutes = this[KEY_FOCUS] ?: 25,
        shortBreakMinutes = this[KEY_SHORT] ?: 5,
        longBreakMinutes = this[KEY_LONG] ?: 15,
        longBreakInterval = this[KEY_INTERVAL] ?: 4,
        autoStartBreaks = this[KEY_AUTO_BREAK] ?: true,
        autoStartFocus = this[KEY_AUTO_FOCUS] ?: false,
        autoDnd = this[KEY_AUTO_DND] ?: true,
        dndMode = DndMode.fromKey(this[KEY_DND_MODE]),
        use24Hour = this[KEY_24H] ?: false,
        keepScreenOn = this[KEY_KEEP_AWAKE] ?: true,
        lowPowerMode = this[KEY_LOW_POWER] ?: false,
        soundEnabled = this[KEY_SOUND] ?: true,
        vibrateEnabled = this[KEY_VIBRATE] ?: true,
        tutorialCompleted = this[KEY_TUTORIAL] ?: false,
        watchFace = WatchFace.fromId(this[KEY_WATCH_FACE]),
    )

    private fun AppSettings.validated(): AppSettings = copy(
        focusMinutes = focusMinutes.coerceIn(1, 180),
        shortBreakMinutes = shortBreakMinutes.coerceIn(1, 180),
        longBreakMinutes = longBreakMinutes.coerceIn(1, 180),
        longBreakInterval = longBreakInterval.coerceIn(2, 8),
    )

    private companion object {
        val KEY_FOCUS = intPreferencesKey("focus_minutes")
        val KEY_SHORT = intPreferencesKey("short_break_minutes")
        val KEY_LONG = intPreferencesKey("long_break_minutes")
        val KEY_INTERVAL = intPreferencesKey("long_break_interval")
        val KEY_AUTO_BREAK = booleanPreferencesKey("auto_start_breaks")
        val KEY_AUTO_FOCUS = booleanPreferencesKey("auto_start_focus")
        val KEY_AUTO_FLOW_DEFAULT = booleanPreferencesKey("auto_flow_default_applied")
        val KEY_AUTO_DND = booleanPreferencesKey("auto_dnd")
        val KEY_DND_MODE = stringPreferencesKey("dnd_mode")
        val KEY_24H = booleanPreferencesKey("clock_24_hour")
        val KEY_KEEP_AWAKE = booleanPreferencesKey("keep_screen_on")
        val KEY_LOW_POWER = booleanPreferencesKey("low_power_mode")
        val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        val KEY_VIBRATE = booleanPreferencesKey("vibrate_enabled")
        val KEY_TUTORIAL = booleanPreferencesKey("tutorial_completed")
        val KEY_WATCH_FACE = stringPreferencesKey("watch_face")
    }
}
