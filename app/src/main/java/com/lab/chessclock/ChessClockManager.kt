package com.lab.chessclock

import android.os.CountDownTimer

abstract class ChessClockManager(private val clock: ChessClock) {

    private var countdownStarted = false
    private var countdown: CountDownTimer? = null

    init {
        onStateChange(clock.getState())
    }

    fun start(firstTimer: Boolean, useIncrement: Boolean) {
        val state = clock.getState()
        if (!countdownStarted) {
            countdown = createCountdown(
                state.firstTimer.ms + state.firstTimer.ms, COUNTDOWN_INTERVAL)
            countdown?.start()
            countdownStarted = true

        }
        onStateChange(clock.startClock(firstTimer, useIncrement))
    }

    fun pauseOrReset() {
        val state = clock.getState()
        if (state.isPaused() || state.isFinished()) {
            reset()
        } else if (state.isRunning()) {
            pause()
        } else {
            // Clock is not started yet.
            start(firstTimer = false, useIncrement = false)
        }
    }

    private fun reset() {
        countdown?.cancel()
        countdownStarted = false
        onStateChange(clock.resetClock())
    }

    fun pause() {
        countdown?.cancel()
        countdownStarted = false
        onStateChange(clock.pauseClock())
    }

    private fun createCountdown(millisInFuture: Long, countDownInterval: Long): CountDownTimer {
        return object : CountDownTimer(millisInFuture, countDownInterval) {

            var msUntilFinished = millisInFuture

            override fun onTick(msUntilFinished: Long) {
                val state = clock.getState()
                if (state.isFinished()) {
                    countdown?.cancel()
                }

                clock.tick(this.msUntilFinished - msUntilFinished)
                this.msUntilFinished = msUntilFinished
                onStateChange(clock.getState())
            }

            override fun onFinish() {
                clock.tick(countDownInterval)
                countdown?.cancel()
                onStateChange(clock.getState())
            }
        }
    }

    abstract fun onStateChange(state: ChessClockState)

    companion object {
        const val COUNTDOWN_INTERVAL = 100L
    }
}