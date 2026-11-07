package com.lab.chessclock.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lab.chessclock.clock.IncrementMode
import com.lab.chessclock.clock.TimeStage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.time.Duration.Companion.hours
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
                time = 3.minutes,
                secondTime = 2.minutes,
                incrementSeconds = 2,
                stages = listOf(TimeStage(40, 30.minutes), TimeStage(20, 15.minutes)),
                incrementMode = IncrementMode.Bronstein,
                orientation = Orientation.VerticalOpposite,
                faceStyle = FaceStyle.Panels,
                theme = ThemeMode.Dark,
                palette = ColorPalette.Ocean,
                vibrateOnLowTime = false,
            )
        }

        val settings = repository.settings.first()
        assertEquals(3.minutes, settings.time)
        assertEquals(2.minutes, settings.timeControl.secondInitial)
        assertEquals(listOf(TimeStage(40, 30.minutes), TimeStage(20, 15.minutes)), settings.timeControl.stages)
        assertEquals(Orientation.VerticalOpposite, settings.orientation)
        assertEquals(FaceStyle.Panels, settings.faceStyle)
        assertEquals(ThemeMode.Dark, settings.theme)
        assertEquals(ColorPalette.Ocean, settings.palette)
        assertEquals(3.minutes, settings.timeControl.initial)
        assertEquals(2.seconds, settings.timeControl.increment)
        assertEquals(IncrementMode.Bronstein, settings.timeControl.mode)
        assertEquals(true, settings.vibrateOnTurnEnd)
        assertEquals(false, settings.vibrateOnLowTime)
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
                time = 1.hours + 30.minutes + 15.seconds,
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
            it[stringPreferencesKey("palette")] = "neon"
        }

        val settings = DataStoreSettingsRepository(dataStore).settings.first()

        assertEquals(ClockSettings(time = 5.minutes + 20.seconds), settings)
    }

    @Test
    fun turningSeparateTimesOffAndStagesOffClearsThem() = runTest {
        val repository = createRepository()
        repository.update { it.copy(secondTime = 3.minutes, stages = listOf(TimeStage(40, 30.minutes))) }

        repository.update { it.copy(secondTime = null, stages = emptyList()) }

        assertEquals(ClockSettings(), repository.settings.first())
    }

    @Test
    fun unreadableStagesAreSkipped() = runTest {
        val dataStore = createDataStore()
        dataStore.edit { it[stringPreferencesKey("stages")] = "40:1800;x:1;0:60;20" }

        val settings = DataStoreSettingsRepository(dataStore).settings.first()

        assertEquals(listOf(TimeStage(40, 30.minutes)), settings.stages)
    }

    @Test
    fun updateTransformsTheStoredValue() = runTest {
        val repository = createRepository()
        repository.update { it.copy(time = 10.seconds) }

        repository.update { it.copy(time = it.time + 5.seconds) }

        assertEquals(ClockSettings(time = 15.seconds), repository.settings.first())
    }

    private fun TestScope.createRepository() = DataStoreSettingsRepository(createDataStore())

    private fun TestScope.createDataStore() = PreferenceDataStoreFactory.create(scope = backgroundScope) {
        folder.root.resolve("settings.preferences_pb")
    }
}
