package com.lab.chessclock.testing

import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(initial: ClockSettings = ClockSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(transform: (ClockSettings) -> ClockSettings) {
        settings.update(transform)
    }
}
