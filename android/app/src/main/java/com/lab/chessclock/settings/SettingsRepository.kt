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
import com.lab.chessclock.clock.TimeStage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
            // Split as the old preference screen stored it, so its keys stay the ones in use.
            updated.time.toComponents { hours, minutes, seconds, _ ->
                preferences[HOURS] = hours.toInt()
                preferences[MINUTES] = minutes
                preferences[SECONDS] = seconds
            }
            val secondTime = updated.secondTime
            if (secondTime != null) preferences[SECOND_TIME] = secondTime.inWholeSeconds.toInt() else preferences.remove(SECOND_TIME)
            preferences[INCREMENT] = updated.incrementSeconds
            preferences[STAGES] = updated.stages.joinToString(";") { "${it.moves}:${it.time.inWholeSeconds}" }
            preferences[INCREMENT_MODE] = updated.incrementMode.storageKey
            preferences[ORIENTATION] = updated.orientation.storageKey
            preferences[FACE_STYLE] = updated.faceStyle.storageKey
            preferences[THEME] = updated.theme.storageKey
            preferences[PALETTE] = updated.palette.storageKey
            preferences[VIBRATE_ON_TURN_END] = updated.vibrateOnTurnEnd
            preferences[VIBRATE_ON_LOW_TIME] = updated.vibrateOnLowTime
        }
    }

    private fun Preferences.toSettings(): ClockSettings {
        val defaults = ClockSettings()
        return ClockSettings(
            // Each part missing on its own reads as the default 5 minutes' part.
            time = (this[HOURS] ?: 0).hours + (this[MINUTES] ?: 5).minutes + (this[SECONDS] ?: 0).seconds,
            secondTime = this[SECOND_TIME]?.seconds,
            incrementSeconds = this[INCREMENT] ?: defaults.incrementSeconds,
            incrementMode = incrementModeFromStorageKey(this[INCREMENT_MODE]),
            stages = this[STAGES]?.let(::stagesFromStorage) ?: defaults.stages,
            orientation = Orientation.fromStorageKey(this[ORIENTATION]),
            faceStyle = FaceStyle.fromStorageKey(this[FACE_STYLE]),
            theme = ThemeMode.fromStorageKey(this[THEME]),
            palette = ColorPalette.fromStorageKey(this[PALETTE]),
            vibrateOnTurnEnd = this[VIBRATE_ON_TURN_END] ?: defaults.vibrateOnTurnEnd,
            vibrateOnLowTime = this[VIBRATE_ON_LOW_TIME] ?: defaults.vibrateOnLowTime,
        )
    }

    /** "40:1800;20:900": moves and seconds of each period, skipping any that can't be read. */
    private fun stagesFromStorage(stored: String): List<TimeStage> = stored.split(";").mapNotNull { entry ->
        val (moves, seconds) = entry.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
        val stageMoves = moves.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        TimeStage(stageMoves, seconds.toLongOrNull()?.seconds ?: return@mapNotNull null)
    }

    // Same keys the old preference screen used, so migrated values are picked up as-is.
    private companion object {
        val HOURS = intPreferencesKey("timer_h")
        val MINUTES = intPreferencesKey("timer_m")
        val SECONDS = intPreferencesKey("timer_s")
        val INCREMENT = intPreferencesKey("increment")
        val SECOND_TIME = intPreferencesKey("second_time_s")
        val STAGES = stringPreferencesKey("stages")
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
