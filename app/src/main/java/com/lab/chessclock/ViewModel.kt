package com.lab.chessclock

import android.content.Context
import android.util.TypedValue

class ViewModel(var chessClockState: ChessClockState) {

    private fun getProgress(isFirstTimer: Boolean): Int {
        if (chessClockState.isFinished()) {
            return 100
        }
        val msLeft = if (isFirstTimer)
            chessClockState.firstTimer.ms else chessClockState.secondTimer.ms

        return (msLeft.toDouble() / chessClockState.firstTimer.msInitial * 100).toInt()
    }

    fun getFirstTimerProgress(): Int {
        return getProgress(true)
    }

    fun getSecondTimerProgress(): Int {
        return getProgress(false)
    }

    fun getButtonText(context: Context): String {
        return if (chessClockState.isPaused() || chessClockState.isFinished()) {
            context.resources.getString(R.string.button_reset)
        } else if (chessClockState.isRunning()) {
            context.resources.getString(R.string.button_pause)
        } else {
            context.resources.getString(R.string.button_start)
        }
    }

    private fun getTimerValue(timerMs: Long): String {
        val hours = timerMs / 3600000
        val minutes = (timerMs / 60000) % 60
        val seconds = (timerMs / 1000) % 60
        val millis = (timerMs % 1000) / 100 * 10
        if (hours >= 1) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else if (timerMs <= 60000) {
            return String.format("%02d:%02d.%02d", minutes, seconds, millis)
        }

        return String.format("%02d:%02d", minutes, seconds)
    }

    fun getFirstTimerValue(): String {
        return getTimerValue(chessClockState.firstTimer.ms)
    }

    fun getSecondTimerValue(): String {
        return getTimerValue(chessClockState.secondTimer.ms)
    }

    fun shouldAnimateProgressIndicator(): Boolean {
        return chessClockState.isRunning() && !chessClockState.isFinished()
    }

    private fun getAttribute(attrId: Int, context: Context): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(
            attrId,
            value,
            true)
        return value.data
    }

    private fun getTimerColor(context: Context, timerActive: Boolean, timerMs: Long, clockFinished: Boolean): Int {
        return if (clockFinished) {
            if (timerMs == 0L)
                getAttribute(R.attr.looserColor, context)
            else
                getAttribute(R.attr.winnerColor, context)
        } else if (timerActive)
            getAttribute(androidx.appcompat.R.attr.colorPrimary, context)
        else getAttribute(R.attr.inactiveColor, context)
    }

    fun getFirstTimerColor(context: Context): Int {
        return getTimerColor(
            context,
            chessClockState.firstTimer.active,
            chessClockState.firstTimer.ms,
            chessClockState.isFinished())
    }

    fun getSecondTimerColor(context: Context): Int {
        return getTimerColor(
            context,
            chessClockState.secondTimer.active,
            chessClockState.secondTimer.ms,
            chessClockState.isFinished())
    }
}