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
        return state.copy()
    }

    fun resetClock(): ChessClockState {
        state = ChessClockState(
            ChessTimerState(firstTimerMs, false),
            ChessTimerState(secondTimerMs, false))

        return getState()
    }

    fun startClock(isFirstTimer: Boolean): ChessClockState {
        if (isFinished()) return getState()

        state.firstTimer.active = isFirstTimer
        state.secondTimer.active = !isFirstTimer

        return getState()
    }

    fun pauseClock(): ChessClockState {
        if (isFinished()) return getState()

        state.firstTimer.active = false
        state.secondTimer.active = false

        return getState()
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