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