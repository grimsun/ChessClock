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
            TimerSetting.entries.forEach { preferences[it.key] = it.valueIn(updated) }
            preferences[ORIENTATION] = updated.orientation.storageKey
            preferences[FACE_STYLE] = updated.faceStyle.storageKey
            preferences[THEME] = updated.theme.storageKey
            preferences[PALETTE] = updated.palette.storageKey
        }
    }

    private fun Preferences.toSettings(): ClockSettings =
        TimerSetting.entries.fold(
            ClockSettings(
                orientation = Orientation.fromStorageKey(this[ORIENTATION]),
                faceStyle = FaceStyle.fromStorageKey(this[FACE_STYLE]),
                theme = ThemeMode.fromStorageKey(this[THEME]),
                palette = ColorPalette.fromStorageKey(this[PALETTE]),
            ),
        ) { settings, setting -> this[setting.key]?.let { setting.update(settings, it) } ?: settings }

    // Same keys the old preference screen used, so migrated values are picked up as-is.
    private val TimerSetting.key get() = intPreferencesKey(storageKey)

    private companion object {
        val ORIENTATION = stringPreferencesKey("orientation")
        val FACE_STYLE = stringPreferencesKey("face_style")
        val THEME = stringPreferencesKey("theme")
        val PALETTE = stringPreferencesKey("palette")
    }
}

/** Settings store, seeded once from the SharedPreferences file the old preference screen wrote. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "${context.packageName}_preferences"))
    },
)
