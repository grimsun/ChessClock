package com.lab.chessclock

class ChessClockState(val firstTimer: ChessTimerState, val secondTimer: ChessTimerState, val increment: Long) {

    fun isFinished(): Boolean {
        return firstTimer.ms == 0L || secondTimer.ms == 0L
    }

    fun isRunning(): Boolean {
        return !isFinished() && (firstTimer.active || secondTimer.active)
    }

    fun isPaused(): Boolean {
        return !isRunning() &&
                (firstTimer.ms < firstTimer.msMax || secondTimer.ms < secondTimer.msMax)
    }

    fun initialStateEquals(state: ChessClockState): Boolean {
        return firstTimer.msInitial == state.firstTimer.msInitial &&
                secondTimer.msInitial == state.secondTimer.msInitial && increment == state.increment
    }

    fun copy(): ChessClockState {
        return ChessClockState(firstTimer, secondTimer, increment)
    }
}