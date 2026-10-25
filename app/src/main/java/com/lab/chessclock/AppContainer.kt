package com.lab.chessclock

import android.content.Context
import com.lab.chessclock.settings.DataStoreSettingsRepository
import com.lab.chessclock.settings.SettingsRepository
import com.lab.chessclock.settings.settingsDataStore

/** App-wide dependencies, created once in [ChessClockApplication]. */
class AppContainer(context: Context) {
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context.settingsDataStore)
}
