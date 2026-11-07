package com.lab.chessclock.settings

import com.lab.chessclock.clock.IncrementMode
import com.lab.chessclock.clock.TimeStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimePresetTest {

    @Test
    fun appliedReplacesTheWholeTimeControlAndKeepsTheOrientation() {
        val settings = ClockSettings(
            time = 80.minutes + 30.seconds,
            secondTime = 1.minutes,
            stages = listOf(TimeStage(40, 30.minutes)),
            orientation = Orientation.VerticalSame,
        )

        val applied = TimePreset(3, 2).appliedTo(settings)

        assertEquals(ClockSettings(time = 3.minutes, incrementSeconds = 2, orientation = Orientation.VerticalSame), applied)
        assertEquals(3.minutes, applied.timeControl.initial)
        assertEquals(2.seconds, applied.timeControl.increment)
    }

    @Test
    fun appliedKeepsTheIncrementMode() {
        val applied = TimePreset(3, 2).appliedTo(ClockSettings(incrementMode = IncrementMode.SimpleDelay))

        assertEquals(IncrementMode.SimpleDelay, applied.timeControl.mode)
        assertTrue(TimePreset(3, 2).matches(applied))
    }

    @Test
    fun matchesOnlyTheExactTimeControl() {
        assertTrue(TimePreset(3, 2).matches(ClockSettings(time = 3.minutes, incrementSeconds = 2)))
        assertFalse(TimePreset(3, 2).matches(ClockSettings(time = 3.minutes, incrementSeconds = 0)))
        assertFalse(TimePreset(3, 2).matches(ClockSettings(time = 3.minutes + 10.seconds, incrementSeconds = 2)))
        assertFalse(TimePreset(3, 2).matches(ClockSettings(time = 3.minutes, secondTime = 2.minutes, incrementSeconds = 2)))
        assertFalse(
            TimePreset(3, 2).matches(
                ClockSettings(time = 3.minutes, incrementSeconds = 2, stages = listOf(TimeStage(40, 1.minutes))),
            ),
        )
    }

    @Test
    fun presetsAreDistinctAndFitTheIncrementSlider() {
        assertEquals(TimePreset.All.size, TimePreset.All.toSet().size)
        TimePreset.All.forEach { assertTrue(it.incrementSeconds in 0..ClockSettings.MAX_INCREMENT_SECONDS) }
    }
}
