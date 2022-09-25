package com.lab.chessclock

class ChessClock(firstTimerMs: Long, secondTimerMs: Long, private val incrementMs: Long) {
    // Chess clock state
    private var state: ChessClockState

    init {
        state = ChessClockState(
            ChessTimerState(firstTimerMs, false),
            ChessTimerState(secondTimerMs, false),
            incrementMs)
    }

    fun getState(): ChessClockState {
        return state.copy()
    }

    fun resetClock(): ChessClockState {
        state = ChessClockState(
            ChessTimerState(state.firstTimer.msInitial, false),
            ChessTimerState(state.secondTimer.msInitial, false),
            state.increment)

        return getState()
    }

    fun startClock(isFirstTimer: Boolean, useIncrement: Boolean): ChessClockState {
        if (state.isFinished()) return getState()
        if (state.isRunning() && state.firstTimer.active == isFirstTimer) return getState()

        if (useIncrement && !state.isPaused() && !state.isFinished() && state.isRunning()) {
            if (isFirstTimer) {
                state.secondTimer.ms += incrementMs
            } else {
                state.firstTimer.ms += incrementMs
            }
        }

        state.firstTimer.active = isFirstTimer
        state.secondTimer.active = !isFirstTimer

        return getState()
    }

    fun pauseClock(): ChessClockState {
        if (state.isFinished()) return getState()

        state.firstTimer.active = false
        state.secondTimer.active = false

        return getState()
    }

    fun tick(ms: Long) {
        if (state.isFinished()) return

        if (state.firstTimer.active) {
            state.firstTimer.ms = if (state.firstTimer.ms - ms < 0) 0 else state.firstTimer.ms - ms
        } else if (state.secondTimer.active) {
            state.secondTimer.ms = if (state.secondTimer.ms - ms < 0) 0 else state.secondTimer.ms - ms
        }
    }
}