package com.lab.chessclock.ui.clock

import com.lab.chessclock.clock.ChessClock
import com.lab.chessclock.clock.Player
import com.lab.chessclock.clock.TimeControl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LowTimeTest {

    private val game = ChessClock(TimeControl(1.minutes, 0.seconds)).press(Player.First)

    @Test
    fun onlyThePlayerOnMoveBelowTheThresholdIsLow() {
        val clock = game.tick(1.minutes - LOW_TIME + 1.seconds)

        assertTrue(clock.isLowOnTime(Player.Second))
        assertFalse(clock.isLowOnTime(Player.First))
    }

    @Test
    fun exactlyAtTheThresholdIsNotLowYet() {
        assertFalse(game.tick(1.minutes - LOW_TIME).isLowOnTime(Player.Second))
    }

    @Test
    fun staysLowWhilePaused() {
        assertTrue(game.tick(55.seconds).pause().isLowOnTime(Player.Second))
    }

    @Test
    fun aFinishedGameIsNotLow() {
        assertFalse(game.tick(2.minutes).isLowOnTime(Player.Second))
    }
}
