package com.lab.chessclock

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator

class MainActivity : AppCompatActivity() {

    private lateinit var timerView1: TextView
    private lateinit var timerView2: TextView
    private lateinit var progressIndicator1: CircularProgressIndicator
    private lateinit var progressIndicator2: CircularProgressIndicator
    private lateinit var controlButton: Button
    private lateinit var settingsButton: FloatingActionButton

    private lateinit var clockManager: ChessClockManager
    private lateinit var viewModel: ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

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
        settingsButton = findViewById<FloatingActionButton?>(R.id.settings_button).apply {
            setOnClickListener {
                it.context.startActivity(Intent(it.context, SettingsActivity::class.java))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        clockManager.pause()
    }

    override fun onResume() {
        super.onResume()
        clockManager = createClock()
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
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val hours = sharedPreferences.getInt("timer_h", 0)
        val minutes = sharedPreferences.getInt("timer_m", 0)
        val seconds = sharedPreferences.getInt("timer_s", 0)
        val ms = 1000 * (3600 * hours + 60 * minutes + seconds)

        return object: ChessClockManager(ChessClock(ms.toLong(), ms.toLong())) {
            override fun onStateChange(state: ChessClockState) {
                this@MainActivity.onStateChange(state)
            }
        }
    }
}