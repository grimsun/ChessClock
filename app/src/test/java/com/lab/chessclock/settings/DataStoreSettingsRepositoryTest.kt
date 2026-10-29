package com.lab.chessclock.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DataStoreSettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun emptyStoreReturnsDefaults() = runTest {
        val repository = createRepository()
        assertEquals(ClockSettings(), repository.settings.first())
        assertEquals(5.minutes, repository.settings.first().timeControl.initial)
    }

    @Test
    fun updatesArePersisted() = runTest {
        val repository = createRepository()
        repository.update {
            it.copy(
                minutes = 3,
                incrementSeconds = 2,
                orientation = Orientation.VerticalOpposite,
                faceStyle = FaceStyle.Panels,
                theme = ThemeMode.Dark,
            )
        }

        val settings = repository.settings.first()
        assertEquals(3, settings.minutes)
        assertEquals(Orientation.VerticalOpposite, settings.orientation)
        assertEquals(FaceStyle.Panels, settings.faceStyle)
        assertEquals(ThemeMode.Dark, settings.theme)
        assertEquals(3.minutes, settings.timeControl.initial)
        assertEquals(2.seconds, settings.timeControl.increment)
    }

    @Test
    fun readsValuesStoredUnderTheLegacyPreferenceKeys() = runTest {
        // The keys the old preference screen wrote, which SharedPreferencesMigration copies over.
        val dataStore = createDataStore()
        dataStore.edit {
            it[intPreferencesKey("timer_h")] = 1
            it[intPreferencesKey("timer_m")] = 30
            it[intPreferencesKey("timer_s")] = 15
            it[intPreferencesKey("increment")] = 10
            it[stringPreferencesKey("orientation")] = "vertical_same"
        }

        val settings = DataStoreSettingsRepository(dataStore).settings.first()

        assertEquals(
            ClockSettings(
                hours = 1,
                minutes = 30,
                seconds = 15,
                incrementSeconds = 10,
                orientation = Orientation.VerticalSame,
            ),
            settings,
        )
    }

    @Test
    fun missingOrUnknownValuesFallBackToDefaults() = runTest {
        val dataStore = createDataStore()
        dataStore.edit {
            it[intPreferencesKey("timer_s")] = 20
            it[stringPreferencesKey("orientation")] = "diagonal"
            it[stringPreferencesKey("face_style")] = "hexagons"
            it[stringPreferencesKey("theme")] = "sepia"
        }

        val settings = DataStoreSettingsRepository(dataStore).settings.first()

        assertEquals(ClockSettings(seconds = 20), settings)
    }

    @Test
    fun updateTransformsTheStoredValue() = runTest {
        val repository = createRepository()
        repository.update { it.copy(seconds = 10) }

        repository.update { it.copy(seconds = it.seconds + 5) }

        assertEquals(ClockSettings(seconds = 15), repository.settings.first())
    }

    private fun TestScope.createRepository() = DataStoreSettingsRepository(createDataStore())

    private fun TestScope.createDataStore() = PreferenceDataStoreFactory.create(scope = backgroundScope) {
        folder.root.resolve("settings.preferences_pb")
    }
}
