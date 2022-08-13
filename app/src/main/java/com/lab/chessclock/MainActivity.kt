package com.lab.chessclock

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.google.android.material.progressindicator.CircularProgressIndicator

class MainActivity : AppCompatActivity() {

    private lateinit var timerView1: TextView
    private lateinit var timerView2: TextView
    private lateinit var progressIndicator1: CircularProgressIndicator
    private lateinit var progressIndicator2: CircularProgressIndicator
    private lateinit var controlButton: Button

    private lateinit var clockManager: ChessClockManager
    private lateinit var viewModel: ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerView1 = findViewById<TextView?>(R.id.timer1).apply {
            setOnClickListener {
                clockManager.start(true)
            }
        }
        timerView2 = findViewById<TextView?>(R.id.timer2).apply {
            setOnClickListener {
                clockManager.start(false)
            }
        }
        progressIndicator1 = findViewById(R.id.progress1)
        progressIndicator2 = findViewById(R.id.progress2)
        controlButton = findViewById<Button?>(R.id.controlButton).apply {
            setOnClickListener {
                clockManager.pauseOrReset()
            }
        }

        clockManager = createClock()
    }

    override fun onPause() {
        super.onPause()
        clockManager.pause()
    }

    private fun onStateChange(state: ChessClockState) {
        viewModel = ViewModel(state)

        timerView1.text = viewModel.getFirstTimerValue()
        timerView2.text = viewModel.getSecondTimerValue()
        controlButton.text = viewModel.getButtonText(this)

        if (viewModel.shouldAnimateProgressIndicator()) {
            progressIndicator1.setProgress(viewModel.getFirstTimerProgress(), true)
            progressIndicator2.setProgress(viewModel.getSecondTimerProgress(), true)
        } else {
            progressIndicator1.progress = viewModel.getFirstTimerProgress()
            progressIndicator2.progress = viewModel.getSecondTimerProgress()
        }

        progressIndicator1.setIndicatorColor(viewModel.getFirstTimerColor(this))
        progressIndicator2.setIndicatorColor(viewModel.getSecondTimerColor(this))
    }

    private fun createClock(): ChessClockManager {
        return object: ChessClockManager(ChessClock(ViewModel.TIMER_MS, ViewModel.TIMER_MS)) {
            override fun onStateChange(state: ChessClockState) {
                this@MainActivity.onStateChange(state)
            }
        }
    }
}