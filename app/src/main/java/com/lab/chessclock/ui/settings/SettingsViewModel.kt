package com.lab.chessclock.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lab.chessclock.ChessClockApplication
import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    /** Null until the stored settings have loaded. */
    val settings: StateFlow<ClockSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setOrientation(orientation: Orientation) = update { it.copy(orientation = orientation) }

    fun setHours(hours: Int) = update { it.copy(hours = hours) }

    fun setMinutes(minutes: Int) = update { it.copy(minutes = minutes) }

    fun setSeconds(seconds: Int) = update { it.copy(seconds = seconds) }

    fun setIncrementSeconds(seconds: Int) = update { it.copy(incrementSeconds = seconds) }

    private fun update(transform: (ClockSettings) -> ClockSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ChessClockApplication
                SettingsViewModel(app.container.settingsRepository)
            }
        }
    }
}
