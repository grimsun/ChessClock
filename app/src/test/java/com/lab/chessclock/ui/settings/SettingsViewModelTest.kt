package com.lab.chessclock.ui.settings

import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.settings.SettingsRepository
import com.lab.chessclock.settings.TimerSetting
import com.lab.chessclock.testing.FakeSettingsRepository
import com.lab.chessclock.testing.MainDispatcherRule
import com.lab.chessclock.testing.NeverLoadingSettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository(ClockSettings(minutes = 5))

    @Test
    fun exposesStoredSettings() = runTest {
        val viewModel = createViewModel()
        assertEquals(ClockSettings(minutes = 5), viewModel.settings.value)
    }

    @Test
    fun settingsAreNullUntilLoaded() = runTest {
        val viewModel = createViewModel(NeverLoadingSettingsRepository)
        assertNull(viewModel.settings.value)
    }

    @Test
    fun setTimerSavesOnlyThatSetting() = runTest {
        val viewModel = createViewModel()

        viewModel.setTimer(TimerSetting.Increment, 7)

        assertEquals(ClockSettings(minutes = 5, incrementSeconds = 7), repository.settings.value)
        assertEquals(repository.settings.value, viewModel.settings.value)
    }

    @Test
    fun setOrientationSavesIt() = runTest {
        val viewModel = createViewModel()

        viewModel.setOrientation(Orientation.VerticalOpposite)

        assertEquals(Orientation.VerticalOpposite, repository.settings.value.orientation)
    }

    private fun TestScope.createViewModel(source: SettingsRepository = repository): SettingsViewModel {
        val viewModel = SettingsViewModel(source)
        // settings is only produced while someone collects it, as the UI would.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.settings.collect {} }
        return viewModel
    }
}
