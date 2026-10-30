package com.lab.chessclock.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ChessClockTest {

    private val clock = ChessClock(TimeControl(initial = 5.minutes, increment = 2.seconds))

    @Test
    fun newClockIsNotStarted() {
        assertEquals(ClockStatus.NotStarted, clock.status)
        assertEquals(5.minutes, clock.first.remaining)
        assertEquals(5.minutes, clock.second.remaining)
        assertNull(clock.active)
    }

    @Test
    fun toggleStartsSecondPlayer() {
        val started = clock.toggle()
        assertEquals(ClockStatus.Running, started.status)
        assertEquals(Player.Second, started.active)
    }

    @Test
    fun pressingYourClockStartsOpponentWithoutIncrementOnFirstMove() {
        val started = clock.press(Player.First)
        assertEquals(Player.Second, started.active)
        assertEquals(5.minutes, started.first.remaining)
    }

    @Test
    fun endingATurnAwardsIncrementAndSwitchesPlayer() {
        val afterMove = clock.press(Player.First)
            .tick(10.seconds)
            .press(Player.Second)

        assertEquals(Player.First, afterMove.active)
        assertEquals(5.minutes - 10.seconds + 2.seconds, afterMove.second.remaining)
    }

    @Test
    fun incrementCanRaiseMaxTime() {
        val afterMove = clock.press(Player.First).press(Player.Second)
        assertEquals(5.minutes + 2.seconds, afterMove.second.max)
        assertEquals(1f, afterMove.second.progress)
    }

    @Test
    fun pressingWhileOpponentRunsDoesNothing() {
        val running = clock.press(Player.First)
        assertEquals(running, running.press(Player.First))
    }

    @Test
    fun tickOnlyChargesActivePlayer() {
        val ticked = clock.press(Player.First).tick(3.seconds)
        assertEquals(5.minutes, ticked.first.remaining)
        assertEquals(5.minutes - 3.seconds, ticked.second.remaining)
    }

    @Test
    fun tickWhenStoppedDoesNothing() {
        assertEquals(clock, clock.tick(3.seconds))
    }

    @Test
    fun pauseStopsClockAndResumeHasNoIncrement() {
        val paused = clock.press(Player.First).tick(1.seconds).toggle()
        assertEquals(ClockStatus.Paused, paused.status)
        assertNull(paused.active)

        val resumed = paused.press(Player.First)
        assertEquals(Player.Second, resumed.active)
        assertEquals(5.minutes - 1.seconds, resumed.second.remaining)
    }

    @Test
    fun runningOutOfTimeFinishesAtZero() {
        val finished = clock.press(Player.First).tick(10.minutes)
        assertEquals(ClockStatus.Finished, finished.status)
        assertEquals(Duration.ZERO, finished.second.remaining)
        assertEquals(finished, finished.press(Player.Second))
        assertEquals(finished, finished.tick(1.seconds))
    }

    @Test
    fun toggleResetsWhenPausedOrFinished() {
        val paused = clock.press(Player.First).tick(1.seconds).pause()
        assertEquals(clock, paused.toggle())

        val finished = clock.press(Player.First).tick(10.minutes)
        assertEquals(clock, finished.toggle())
    }

    @Test
    fun zeroTimeControlIsFinishedImmediately() {
        val empty = ChessClock(TimeControl(Duration.ZERO, Duration.ZERO))
        assertEquals(ClockStatus.Finished, empty.status)
    }

    @Test
    fun resumingKeepsTheTurnWhicheverSideIsTapped() {
        val paused = clock.press(Player.First).tick(1.seconds).pause()

        listOf(Player.First, Player.Second).forEach { tapped ->
            val resumed = paused.press(tapped)
            assertEquals(ClockStatus.Running, resumed.status)
            assertEquals(Player.Second, resumed.active)
            assertEquals(paused.first, resumed.first)
            assertEquals(paused.second, resumed.second)
        }
    }

    @Test
    fun pauseKeepsTheTurn() {
        val paused = clock.press(Player.First).press(Player.Second).pause()
        assertNull(paused.active)
        assertEquals(Player.First, paused.turn)
    }

    @Test
    fun pausingAfterMovesWithNoTimeUsedStillCountsAsPaused() {
        // Both players moved instantly, so the increments left each of them at full time.
        val paused = clock.press(Player.First).press(Player.Second).press(Player.First).pause()

        assertEquals(ClockStatus.Paused, paused.status)
        assertEquals(clock, paused.toggle())
    }

    @Test
    fun incrementsAccumulateOverAGame() {
        val game = clock.press(Player.First)
            .tick(3.seconds).press(Player.Second)
            .tick(1.seconds).press(Player.First)
            .tick(5.seconds).press(Player.Second)

        assertEquals(5.minutes - 1.seconds + 2.seconds, game.first.remaining)
        assertEquals(5.minutes - 8.seconds + 4.seconds, game.second.remaining)
        assertEquals(Player.First, game.active)
    }

    @Test
    fun firstPlayerCanRunOutOfTime() {
        val finished = clock.press(Player.Second).tick(5.minutes)

        assertEquals(ClockStatus.Finished, finished.status)
        assertTrue(finished.first.isExpired)
        assertFalse(finished.second.isExpired)
    }

    @Test
    fun pauseKeepsAFinishedGameFinished() {
        val finished = clock.press(Player.First).tick(10.minutes)
        assertEquals(finished, finished.pause())
    }

    @Test
    fun pauseBeforeStartDoesNothing() {
        assertEquals(clock, clock.pause())
    }

    @Test
    fun progressIsTheShareOfTimeLeft() {
        assertEquals(0.25f, PlayerTime(15.seconds, 60.seconds).progress)
        assertEquals(1f, PlayerTime.full(60.seconds).progress)
        assertEquals(0f, PlayerTime.full(Duration.ZERO).progress)
    }

    private val bronstein = ChessClock(TimeControl(5.minutes, 3.seconds, IncrementMode.Bronstein))
    private val delay = ChessClock(TimeControl(5.minutes, 3.seconds, IncrementMode.SimpleDelay))

    @Test
    fun bronsteinGivesBackTheTimeUsedUpToTheIncrement() {
        val quick = bronstein.press(Player.First).tick(1.seconds).press(Player.Second)
        assertEquals(5.minutes, quick.second.remaining)

        val slow = bronstein.press(Player.First).tick(10.seconds).press(Player.Second)
        assertEquals(5.minutes - 10.seconds + 3.seconds, slow.second.remaining)
    }

    @Test
    fun bronsteinNeverRaisesTheTime() {
        val game = bronstein.press(Player.First).press(Player.Second).press(Player.First)
        assertEquals(PlayerTime.full(5.minutes), game.first)
        assertEquals(PlayerTime.full(5.minutes), game.second)
    }

    @Test
    fun bronsteinCountsAMoveAcrossAPause() {
        val game = bronstein.press(Player.First)
            .tick(1.seconds).pause().press(Player.First)
            .tick(1.seconds).press(Player.Second)

        assertEquals(5.minutes, game.second.remaining)
    }

    @Test
    fun simpleDelayStartsCountingAfterTheDelay() {
        val thinking = delay.press(Player.First).tick(2.seconds)
        assertEquals(5.minutes, thinking.second.remaining)
        assertEquals(1.seconds, thinking.delayRemaining)

        val past = thinking.tick(5.seconds)
        assertEquals(5.minutes - 4.seconds, past.second.remaining)
        assertEquals(Duration.ZERO, past.delayRemaining)
    }

    @Test
    fun simpleDelayRestartsEachMoveAndAddsNothing() {
        val game = delay.press(Player.First).tick(10.seconds).press(Player.Second)

        assertEquals(5.minutes - 7.seconds, game.second.remaining)
        assertEquals(5.minutes, game.second.max)
        assertEquals(3.seconds, game.delayRemaining)
        assertEquals(5.minutes, game.tick(3.seconds).first.remaining)
    }

    @Test
    fun simpleDelayIsKeptAcrossAPause() {
        val resumed = delay.press(Player.First).tick(2.seconds).pause().press(Player.Second)
        assertEquals(1.seconds, resumed.delayRemaining)
        assertEquals(5.minutes - 1.seconds, resumed.tick(2.seconds).second.remaining)
    }

    @Test
    fun fischerAndBronsteinHaveNoDelay() {
        assertEquals(Duration.ZERO, clock.press(Player.First).delayRemaining)
        assertEquals(Duration.ZERO, bronstein.press(Player.First).delayRemaining)
    }
}
