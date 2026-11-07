package com.lab.chessclock.settings

import com.lab.chessclock.clock.IncrementMode
import com.lab.chessclock.clock.Player
import com.lab.chessclock.clock.TimeControl
import com.lab.chessclock.clock.TimeStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ClockSettingsTest {

    @Test
    fun timeControlHasTheTimeIncrementAndStages() {
        val stages = listOf(TimeStage(40, 30.minutes))
        val timeControl = ClockSettings(time = 1.hours + 30.minutes, incrementSeconds = 30, stages = stages).timeControl
        assertEquals(TimeControl(90.minutes, 30.seconds, stages = stages), timeControl)
        assertEquals(90.minutes, timeControl.secondInitial)
    }

    @Test
    fun aSecondTimeIsTheSecondPlayersOnly() {
        val timeControl = ClockSettings(time = 5.minutes, secondTime = 3.minutes).timeControl
        assertEquals(5.minutes, timeControl.initialOf(Player.First))
        assertEquals(3.minutes, timeControl.initialOf(Player.Second))
    }

    @Test
    fun zeroTimeStillGivesAPlayableGame() {
        val timeControl = ClockSettings(time = Duration.ZERO, secondTime = Duration.ZERO).timeControl
        assertEquals(ClockSettings.MIN_INITIAL_TIME, timeControl.initial)
        assertEquals(ClockSettings.MIN_INITIAL_TIME, timeControl.secondInitial)
    }

    @Test
    fun stagesAreAddedUpToTheMostAndRemovedByIndex() {
        val two = ClockSettings().withStageAdded().withStageAdded()
        assertEquals(listOf(TimeStage(40, 30.minutes), TimeStage(20, 15.minutes)), two.stages)
        assertEquals(two, two.withStageAdded())

        assertEquals(listOf(TimeStage(20, 15.minutes)), two.withoutStage(0).stages)
        assertEquals(TimeStage(30, 1.hours), two.withStage(1, TimeStage(30, 1.hours)).stages[1])
    }

    @Test
    fun stepsAreOrderedAndHoldThePresetsAndStageDefaults() {
        assertEquals(ClockSettings.TIME_STEPS.sorted().distinct(), ClockSettings.TIME_STEPS)
        assertEquals(ClockSettings.MOVE_STEPS.sorted().distinct(), ClockSettings.MOVE_STEPS)
        TimePreset.All.forEach { assertTrue(it.minutes.minutes in ClockSettings.TIME_STEPS) }
        ClockSettings().withStageAdded().withStageAdded().stages.forEach {
            assertTrue(it.time in ClockSettings.TIME_STEPS)
            assertTrue(it.moves in ClockSettings.MOVE_STEPS)
        }
        assertTrue(90.minutes in ClockSettings.TIME_STEPS)
    }

    @Test
    fun defaultsAreBlitzWithAnIncrementForPlayersFacingEachOther() {
        val defaults = ClockSettings()
        assertEquals(TimeControl(5.minutes, 3.seconds, IncrementMode.Fischer), defaults.timeControl)
        assertEquals(Orientation.HorizontalOpposite, defaults.orientation)
        assertTrue(TimePreset.All.any { it.matches(defaults) })
        assertTrue(defaults.vibrateOnTurnEnd && defaults.vibrateOnLowTime)
    }
}
