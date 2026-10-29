package com.lab.chessclock

import android.content.Context
import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.DataStoreSettingsRepository
import com.lab.chessclock.settings.SettingsRepository
import com.lab.chessclock.settings.settingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** App-wide dependencies, created once in [ChessClockApplication]. */
class AppContainer(context: Context) {
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context.settingsDataStore)

    /**
     * The settings, read as soon as the app starts and kept current; null until the first read.
     * Screens start from this value, so they draw with the settings on their first frame instead
     * of a blank one while they subscribe and wait for the next.
     */
    val currentSettings: StateFlow<ClockSettings?> = settingsRepository.settings
        .stateIn(CoroutineScope(SupervisorJob() + Dispatchers.Default), SharingStarted.Eagerly, null)
}
