package com.lab.chessclock.ui.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lab.chessclock.clock.ChessClock
import com.lab.chessclock.clock.ClockStatus
import com.lab.chessclock.clock.Player
import com.lab.chessclock.settings.FaceStyle
import com.lab.chessclock.settings.Orientation
import com.lab.chessclock.settings.SettingsRepository
import com.lab.chessclock.ui.appViewModelFactory
import com.lab.chessclock.ui.stateInUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

sealed interface ClockUiState {
    data object Loading : ClockUiState

    data class Ready(
        val clock: ChessClock,
        val orientation: Orientation,
        val faceStyle: FaceStyle = FaceStyle.Rings,
    ) : ClockUiState
}

class ClockViewModel(
    settingsRepository: SettingsRepository,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) : ViewModel() {

    private val clock = MutableStateFlow<ChessClock?>(null)
    private var ticker: Job? = null
    private var lastTick: ComparableTimeMark? = null

    val uiState: StateFlow<ClockUiState> =
        combine(clock.filterNotNull(), settingsRepository.settings) { clock, settings ->
            ClockUiState.Ready(clock, settings.orientation, settings.faceStyle)
        }.stateInUi(this, ClockUiState.Loading)

    init {
        // A new time control starts a fresh game; other setting changes leave the game alone.
        settingsRepository.settings
            .map { it.timeControl }
            .distinctUntilChanged()
            .onEach { timeControl ->
                if (clock.value?.timeControl != timeControl) {
                    stopTicker()
                    clock.value = ChessClock(timeControl)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onPlayerPressed(player: Player) = update { it.press(player) }

    fun onControlPressed() = update { it.toggle() }

    fun pause() = update { it.pause() }

    private fun update(action: (ChessClock) -> ChessClock) {
        chargeElapsedTime()
        val next = clock.updateAndGet { it?.let(action) }
        if (next?.status == ClockStatus.Running) startTicker() else stopTicker()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        lastTick = timeSource.markNow()
        ticker = viewModelScope.launch {
            while (clock.value?.status == ClockStatus.Running) {
                delay(TICK_INTERVAL)
                chargeElapsedTime()
            }
            lastTick = null
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
        lastTick = null
    }

    /** Charges the running player for the time since the last tick, so no time is lost between ticks. */
    private fun chargeElapsedTime() {
        val last = lastTick ?: return
        val now = timeSource.markNow()
        lastTick = now
        clock.update { it?.tick(now - last) }
    }

    companion object {
        val TICK_INTERVAL = 100.milliseconds

        val Factory = appViewModelFactory { ClockViewModel(settingsRepository) }
    }
}
