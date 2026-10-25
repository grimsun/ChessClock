package com.lab.chessclock.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
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
            preferences[Keys.HOURS] = updated.hours
            preferences[Keys.MINUTES] = updated.minutes
            preferences[Keys.SECONDS] = updated.seconds
            preferences[Keys.INCREMENT] = updated.incrementSeconds
            preferences[Keys.ORIENTATION] = updated.orientation.storageKey
        }
    }

    private fun Preferences.toSettings(): ClockSettings {
        val defaults = ClockSettings()
        return ClockSettings(
            hours = this[Keys.HOURS] ?: defaults.hours,
            minutes = this[Keys.MINUTES] ?: defaults.minutes,
            seconds = this[Keys.SECONDS] ?: defaults.seconds,
            incrementSeconds = this[Keys.INCREMENT] ?: defaults.incrementSeconds,
            orientation = Orientation.fromStorageKey(this[Keys.ORIENTATION]),
        )
    }

    // Same keys the old preference screen used, so migrated values are picked up as-is.
    private object Keys {
        val HOURS = intPreferencesKey("timer_h")
        val MINUTES = intPreferencesKey("timer_m")
        val SECONDS = intPreferencesKey("timer_s")
        val INCREMENT = intPreferencesKey("increment")
        val ORIENTATION = stringPreferencesKey("orientation")
    }
}

/** Settings store, seeded once from the SharedPreferences file the old preference screen wrote. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "${context.packageName}_preferences"))
    },
)
