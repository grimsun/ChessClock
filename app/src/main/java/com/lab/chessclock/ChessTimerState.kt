package com.lab.chessclock

import kotlin.math.max

data class ChessTimerState(val msInitial: Long, var active: Boolean) {
    var ms: Long = msInitial
        set(value) {
            field = value
            msMax = max(msMax, value)
        }

    var msMax = msInitial
}

fun ChessTimerState.copy(): ChessTimerState {
    return ChessTimerState(msInitial, active)
}
