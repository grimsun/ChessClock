package com.lab.chessclock

import android.os.CountDownTimer

abstract class ChessClockManager(private val clock: ChessClock) {

    private var countdownStarted = false
    private var countdown: CountDownTimer? = null

    init {
        onStateChange(clock.getState())
    }

    fun start(firstTimer: Boolean) {
        val state = clock.getState()
        if (!countdownStarted) {
            countdown = createCountdown(
                state.firstTimer.ms + state.firstTimer.ms, COUNTDOWN_INTERVAL_SHORT)
            countdown?.start()
            countdownStarted = true

        }
        clock.startClock(firstTimer)
        onStateChange(clock.getState())
    }

    fun pauseOrReset() {
        val state = clock.getState()
        if (state.isPaused() || state.isFinished()) {
            reset()
        }else if (state.isRunning()) {
            pause()
        } else {
            start(false)
        }
    }

    private fun reset() {
        clock.resetClock()
        countdown?.cancel()
        countdownStarted = false
        onStateChange(clock.getState())
    }

    fun pause() {
        clock.pauseClock()
        countdown?.cancel()
        countdownStarted = false
        onStateChange(clock.getState())
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
                onStateChange(state)
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
        const val COUNTDOWN_INTERVAL_SHORT = 100L
    }
}