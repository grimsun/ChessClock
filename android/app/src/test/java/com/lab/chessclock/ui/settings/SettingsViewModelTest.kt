package com.lab.chessclock.ui.settings

import com.lab.chessclock.clock.IncrementMode
import com.lab.chessclock.clock.TimeStage
import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.ColorPalette
import com.lab.chessclock.settings.FaceStyle
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.settings.SettingsRepository
import com.lab.chessclock.settings.ThemeMode
import com.lab.chessclock.settings.TimePreset
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository(ClockSettings())

    @Test
    fun exposesStoredSettings() = runTest {
        val viewModel = createViewModel()
        assertEquals(ClockSettings(), viewModel.settings.value)
    }

    @Test
    fun settingsAreNullUntilLoaded() = runTest {
        val viewModel = createViewModel(NeverLoadingSettingsRepository)
        assertNull(viewModel.settings.value)
    }

    @Test
    fun startsWithAlreadyLoadedSettings() = runTest {
        val loaded = ClockSettings(time = 3.minutes)
        val viewModel = createViewModel(NeverLoadingSettingsRepository, initialSettings = loaded)
        assertEquals(loaded, viewModel.settings.value)
    }

    @Test
    fun setIncrementSavesOnlyThatSetting() = runTest {
        val viewModel = createViewModel()

        viewModel.setIncrement(7)

        assertEquals(ClockSettings(incrementSeconds = 7), repository.settings.value)
        assertEquals(repository.settings.value, viewModel.settings.value)
    }

    @Test
    fun separateTimesStartFromTheSharedTimeAndGoBackToIt() = runTest {
        val viewModel = createViewModel()
        viewModel.setTime(10.minutes)

        viewModel.setSeparateTimes(true)
        assertEquals(10.minutes, repository.settings.value.secondTime)

        viewModel.setSecondTime(7.minutes)
        assertEquals(7.minutes, repository.settings.value.timeControl.secondInitial)
        assertEquals(10.minutes, repository.settings.value.timeControl.initial)

        viewModel.setSeparateTimes(false)
        assertEquals(ClockSettings(time = 10.minutes), repository.settings.value)
    }

    @Test
    fun stagesAreAddedChangedAndRemoved() = runTest {
        val viewModel = createViewModel()

        viewModel.addStage()
        viewModel.addStage()
        viewModel.setStage(0, TimeStage(30, 1.hours))
        assertEquals(listOf(TimeStage(30, 1.hours), TimeStage(20, 15.minutes)), repository.settings.value.stages)

        viewModel.removeStage(1)
        assertEquals(listOf(TimeStage(30, 1.hours)), repository.settings.value.stages)
    }

    @Test
    fun setIncrementModeSavesIt() = runTest {
        val viewModel = createViewModel()

        viewModel.setIncrementMode(IncrementMode.SimpleDelay)

        assertEquals(ClockSettings(incrementMode = IncrementMode.SimpleDelay), repository.settings.value)
    }

    @Test
    fun vibrationSettingsAreSavedSeparately() = runTest {
        val viewModel = createViewModel()

        viewModel.setVibrateOnTurnEnd(false)
        assertEquals(ClockSettings(vibrateOnTurnEnd = false), repository.settings.value)
        viewModel.setVibrateOnLowTime(false)
        assertEquals(
            ClockSettings(vibrateOnTurnEnd = false, vibrateOnLowTime = false),
            repository.settings.value,
        )
    }

    @Test
    fun setOrientationSavesIt() = runTest {
        val viewModel = createViewModel()

        viewModel.setOrientation(Orientation.VerticalOpposite)

        assertEquals(Orientation.VerticalOpposite, repository.settings.value.orientation)
    }

    @Test
    fun setFaceStyleSavesIt() = runTest {
        val viewModel = createViewModel()

        viewModel.setFaceStyle(FaceStyle.Panels)

        assertEquals(ClockSettings(faceStyle = FaceStyle.Panels), repository.settings.value)
    }

    @Test
    fun setThemeSavesIt() = runTest {
        val viewModel = createViewModel()

        viewModel.setTheme(ThemeMode.Dark)

        assertEquals(ClockSettings(theme = ThemeMode.Dark), repository.settings.value)
    }

    @Test
    fun setPaletteSavesIt() = runTest {
        val viewModel = createViewModel()

        viewModel.setPalette(ColorPalette.Graphite)

        assertEquals(ClockSettings(palette = ColorPalette.Graphite), repository.settings.value)
    }

    @Test
    fun applyPresetSavesItsTimeControl() = runTest {
        repository.settings.value = ClockSettings(
            time = 1.hours + 30.seconds,
            stages = listOf(TimeStage(40, 30.minutes)),
            orientation = Orientation.VerticalSame,
        )
        val viewModel = createViewModel()

        viewModel.applyPreset(TimePreset(10, 5))

        assertEquals(
            ClockSettings(time = 10.minutes, incrementSeconds = 5, orientation = Orientation.VerticalSame),
            repository.settings.value,
        )
    }

    private fun TestScope.createViewModel(
        source: SettingsRepository = repository,
        initialSettings: ClockSettings? = null,
    ): SettingsViewModel {
        val viewModel = SettingsViewModel(source, initialSettings)
        // settings is only produced while someone collects it, as the UI would.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.settings.collect {} }
        return viewModel
    }
}
