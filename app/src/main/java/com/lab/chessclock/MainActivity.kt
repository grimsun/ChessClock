package com.lab.chessclock

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
                clockManager.start(false)
            }
        }
        timerView2 = findViewById<TextView?>(R.id.timer2).apply {
            setOnClickListener {
                clockManager.start(true)
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

        val chessClock = createClock()
        if (!this::viewModel.isInitialized ||
            !viewModel.initialStateEquals(chessClock.getState())) {
            clockManager = createClockManager(chessClock)
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        when(sharedPreferences.getString("orientation", "horizontal_same")) {
            "horizontal_same" -> {
                timerView1.animate().rotation(0f).setDuration(0).start()
                progressIndicator1.animate().rotation(0f).setDuration(0).start()
                timerView2.animate().rotation(0f).setDuration(0).start()
                progressIndicator2.animate().rotation(0f).setDuration(0).start()
                controlButton.animate().rotation(0f).setDuration(0).start()
            }
            "horizontal_opposite" -> {
                timerView1.animate().rotation(180f).setDuration(0).start()
                progressIndicator1.animate().rotation(180f).setDuration(0).start()
                timerView2.animate().rotation(0f).setDuration(0).start()
                progressIndicator2.animate().rotation(0f).setDuration(0).start()
                controlButton.animate().rotation(0f).setDuration(0).start()
            }
            "vertical_same" -> {
                timerView1.animate().rotation(90f).setDuration(0).start()
                progressIndicator1.animate().rotation(90f).setDuration(0).start()
                timerView2.animate().rotation(90f).setDuration(0).start()
                progressIndicator2.animate().rotation(90f).setDuration(0).start()
                controlButton.animate().rotation(90f).setDuration(0).start()
            }
            "vertical_opposite" -> {
                timerView1.animate().rotation(270f).setDuration(0).start()
                progressIndicator1.animate().rotation(270f).setDuration(0).start()
                timerView2.animate().rotation(90f).setDuration(0).start()
                progressIndicator2.animate().rotation(90f).setDuration(0).start()
                controlButton.animate().rotation(90f).setDuration(0).start()
            }
        }
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

    private fun createClockManager(chessClock: ChessClock): ChessClockManager {
        return object: ChessClockManager(chessClock) {
            override fun onStateChange(state: ChessClockState) {
                this@MainActivity.onStateChange(state)
            }
        }
    }

    private fun createClock(): ChessClock {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val hours = sharedPreferences.getInt("timer_h", 0)
        val minutes = sharedPreferences.getInt("timer_m", 0)
        val seconds = sharedPreferences.getInt("timer_s", 0)
        val ms = 1000 * (3600 * hours + 60 * minutes + seconds)
        val increment = sharedPreferences.getLong("increment", 0)

        return ChessClock(ms.toLong(), ms.toLong(), increment)
    }
}