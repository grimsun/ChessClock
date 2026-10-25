package com.lab.chessclock.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
            it.copy(minutes = 3, incrementSeconds = 2, orientation = Orientation.VerticalOpposite)
        }

        val settings = repository.settings.first()
        assertEquals(3, settings.minutes)
        assertEquals(Orientation.VerticalOpposite, settings.orientation)
        assertEquals(3.minutes, settings.timeControl.initial)
        assertEquals(2.seconds, settings.timeControl.increment)
    }

    private fun TestScope.createRepository() = DataStoreSettingsRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.root.resolve("settings.preferences_pb")
        },
    )
}
