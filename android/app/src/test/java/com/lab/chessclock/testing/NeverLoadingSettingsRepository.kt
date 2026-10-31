package com.lab.chessclock.testing

import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** A repository whose settings never load, to test the loading state. */
object NeverLoadingSettingsRepository : SettingsRepository {
    override val settings: Flow<ClockSettings> = emptyFlow()

    override suspend fun update(transform: (ClockSettings) -> ClockSettings) = Unit
}
