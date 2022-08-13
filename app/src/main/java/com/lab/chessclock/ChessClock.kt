package com.lab.chessclock

class ChessClock(private val firstTimerMs: Long, private val secondTimerMs: Long) {
    // Chess clock state
    private var state: ChessClockState

    init {
        state = ChessClockState(
            ChessTimerState(firstTimerMs, false),
            ChessTimerState(secondTimerMs, false))
    }

    fun getState(): ChessClockState {
        return ChessClockState(state.firstTimer.copy(), state.secondTimer.copy())
    }

    fun resetClock() {
        state = ChessClockState(
            ChessTimerState(firstTimerMs, false),
            ChessTimerState(secondTimerMs, false))
    }

    fun startClock(isFirstTimer: Boolean) {
        if (isFinished()) return

        state.firstTimer.active = isFirstTimer
        state.secondTimer.active = !isFirstTimer
    }

    fun pauseClock() {
        if (isFinished()) return

        state.firstTimer.active = false
        state.secondTimer.active = false
    }

    fun tick(ms: Long) {
        if (isFinished()) return

        if (state.firstTimer.active) {
            state.firstTimer.ms = if (state.firstTimer.ms - ms < 0) 0 else state.firstTimer.ms - ms
        } else if (state.secondTimer.active) {
            state.secondTimer.ms = if (state.secondTimer.ms - ms < 0) 0 else state.secondTimer.ms - ms
        }
    }

    private fun isFinished(): Boolean {
        return state.firstTimer.ms == 0L || state.secondTimer.ms == 0L
    }
}