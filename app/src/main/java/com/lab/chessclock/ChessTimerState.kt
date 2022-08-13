package com.lab.chessclock

data class ChessTimerState(val msInitial: Long, var active: Boolean, var ms: Long = msInitial)

fun ChessTimerState.copy(): ChessTimerState {
    return ChessTimerState(msInitial, active, ms)
}
