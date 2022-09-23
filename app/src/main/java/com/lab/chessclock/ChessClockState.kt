package com.lab.chessclock

data class ChessClockState(val firstTimer: ChessTimerState, val secondTimer: ChessTimerState)

fun ChessClockState.isFinished(): Boolean {
    return firstTimer.ms == 0L || secondTimer.ms == 0L
}

fun ChessClockState.isRunning(): Boolean {
    return !isFinished() && (firstTimer.active || secondTimer.active)
}

fun ChessClockState.isPaused(): Boolean {
    return !isRunning() &&
            (firstTimer.ms < firstTimer.msInitial || secondTimer.ms < secondTimer.msInitial)
}

fun ChessClockState.initialStateEquals(state: ChessClockState): Boolean {
    return firstTimer.msInitial == state.firstTimer.msInitial &&
            secondTimer.msInitial == state.secondTimer.msInitial
}