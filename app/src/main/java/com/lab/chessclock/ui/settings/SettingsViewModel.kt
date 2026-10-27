package com.lab.chessclock.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.FaceStyle
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.settings.SettingsRepository
import com.lab.chessclock.settings.TimePreset
import com.lab.chessclock.settings.TimerSetting
import com.lab.chessclock.ui.appViewModelFactory
import com.lab.chessclock.ui.stateInUi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    /** Null until the stored settings have loaded. */
    val settings: StateFlow<ClockSettings?> = repository.settings.stateInUi(this, null)

    fun setOrientation(orientation: Orientation) = update { it.copy(orientation = orientation) }

    fun setFaceStyle(faceStyle: FaceStyle) = update { it.copy(faceStyle = faceStyle) }

    fun setTimer(setting: TimerSetting, value: Int) = update { setting.update(it, value) }

    fun applyPreset(preset: TimePreset) = update(preset::appliedTo)

    private fun update(transform: (ClockSettings) -> ClockSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    companion object {
        val Factory = appViewModelFactory { SettingsViewModel(settingsRepository) }
    }
}
