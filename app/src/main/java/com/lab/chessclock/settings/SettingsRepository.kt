package com.lab.chessclock.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface SettingsRepository {
    val settings: Flow<ClockSettings>

    suspend fun update(transform: (ClockSettings) -> ClockSettings)
}

class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<ClockSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSettings() }

    override suspend fun update(transform: (ClockSettings) -> ClockSettings) {
        dataStore.edit { preferences ->
            val updated = transform(preferences.toSettings())
            TimerSetting.entries.forEach { preferences[it.key] = it.valueIn(updated) }
            preferences[INCREMENT_MODE] = updated.incrementMode.storageKey
            preferences[ORIENTATION] = updated.orientation.storageKey
            preferences[FACE_STYLE] = updated.faceStyle.storageKey
            preferences[THEME] = updated.theme.storageKey
            preferences[PALETTE] = updated.palette.storageKey
            preferences[VIBRATE_ON_TURN_END] = updated.vibrateOnTurnEnd
            preferences[VIBRATE_ON_LOW_TIME] = updated.vibrateOnLowTime
        }
    }

    private fun Preferences.toSettings(): ClockSettings =
        TimerSetting.entries.fold(
            ClockSettings(
                incrementMode = incrementModeFromStorageKey(this[INCREMENT_MODE]),
                orientation = Orientation.fromStorageKey(this[ORIENTATION]),
                faceStyle = FaceStyle.fromStorageKey(this[FACE_STYLE]),
                theme = ThemeMode.fromStorageKey(this[THEME]),
                palette = ColorPalette.fromStorageKey(this[PALETTE]),
                vibrateOnTurnEnd = this[VIBRATE_ON_TURN_END] ?: ClockSettings().vibrateOnTurnEnd,
                vibrateOnLowTime = this[VIBRATE_ON_LOW_TIME] ?: ClockSettings().vibrateOnLowTime,
            ),
        ) { settings, setting -> this[setting.key]?.let { setting.update(settings, it) } ?: settings }

    // Same keys the old preference screen used, so migrated values are picked up as-is.
    private val TimerSetting.key get() = intPreferencesKey(storageKey)

    private companion object {
        val INCREMENT_MODE = stringPreferencesKey("increment_mode")
        val ORIENTATION = stringPreferencesKey("orientation")
        val FACE_STYLE = stringPreferencesKey("face_style")
        val THEME = stringPreferencesKey("theme")
        val PALETTE = stringPreferencesKey("palette")
        val VIBRATE_ON_TURN_END = booleanPreferencesKey("vibrate_turn_end")
        val VIBRATE_ON_LOW_TIME = booleanPreferencesKey("vibrate_low_time")
    }
}

/** Settings store, seeded once from the SharedPreferences file the old preference screen wrote. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "${context.packageName}_preferences"))
    },
)
