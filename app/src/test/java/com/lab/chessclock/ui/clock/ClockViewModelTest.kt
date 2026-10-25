package com.lab.chessclock.ui.clock

import com.lab.chessclock.clock.ClockStatus
import com.lab.chessclock.clock.Player
import com.lab.chessclock.settings.ClockSettings
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.testing.FakeSettingsRepository
import com.lab.chessclock.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ClockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository(ClockSettings(minutes = 5))

    @Test
    fun startsWithTimeControlFromSettings() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.ready()
        assertEquals(ClockStatus.NotStarted, state.clock.status)
        assertEquals(5.minutes, state.clock.first.remaining)
    }

    @Test
    fun runningClockCountsDownTheActivePlayer() = runTest {
        val viewModel = createViewModel()
        viewModel.onControlPressed()

        advanceTimeBy(1_050)

        val clock = viewModel.ready().clock
        assertEquals(Player.Second, clock.active)
        assertEquals(5.minutes - 1.seconds, clock.second.remaining)
        assertEquals(5.minutes, clock.first.remaining)
    }

    @Test
    fun pauseChargesTimeSinceLastTickAndStopsCounting() = runTest {
        val viewModel = createViewModel()
        viewModel.onControlPressed()
        advanceTimeBy(250)

        viewModel.pause()
        advanceTimeBy(5_000)

        val clock = viewModel.ready().clock
        assertEquals(ClockStatus.Paused, clock.status)
        assertEquals(5.minutes - 250.milliseconds, clock.second.remaining)
    }

    @Test
    fun changingTimeControlStartsAFreshGame() = runTest {
        val viewModel = createViewModel()
        viewModel.onControlPressed()
        advanceTimeBy(1_000)

        settings.update { it.copy(minutes = 1) }

        val clock = viewModel.ready().clock
        assertEquals(ClockStatus.NotStarted, clock.status)
        assertEquals(1.minutes, clock.second.remaining)
    }

    @Test
    fun changingOrientationKeepsTheGame() = runTest {
        val viewModel = createViewModel()
        viewModel.onControlPressed()

        settings.update { it.copy(orientation = Orientation.VerticalOpposite) }

        val state = viewModel.ready()
        assertEquals(Orientation.VerticalOpposite, state.orientation)
        assertEquals(ClockStatus.Running, state.clock.status)
        viewModel.pause()
    }

    private fun TestScope.createViewModel(): ClockViewModel {
        val viewModel = ClockViewModel(settings, timeSource = testScheduler.timeSource)
        // uiState is only produced while someone collects it, as the UI would.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun ClockViewModel.ready() = uiState.value as ClockUiState.Ready
}
