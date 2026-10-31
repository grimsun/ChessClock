package com.lab.chessclock.settings

import com.lab.chessclock.clock.IncrementMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimePresetTest {

    @Test
    fun appliedReplacesTheWholeTimeControlAndKeepsTheOrientation() {
        val settings = ClockSettings(hours = 1, minutes = 20, seconds = 30, orientation = Orientation.VerticalSame)

        val applied = TimePreset(3, 2).appliedTo(settings)

        assertEquals(ClockSettings(minutes = 3, incrementSeconds = 2, orientation = Orientation.VerticalSame), applied)
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
        assertTrue(TimePreset(3, 2).matches(ClockSettings(minutes = 3, incrementSeconds = 2)))
        assertFalse(TimePreset(3, 2).matches(ClockSettings(minutes = 3, incrementSeconds = 0)))
        assertFalse(TimePreset(3, 2).matches(ClockSettings(minutes = 3, seconds = 10, incrementSeconds = 2)))
    }

    @Test
    fun presetsAreDistinctAndFitTheSliders() {
        assertEquals(TimePreset.All.size, TimePreset.All.toSet().size)
        TimePreset.All.forEach {
            assertTrue(it.minutes in 1..TimerSetting.Minutes.max)
            assertTrue(it.incrementSeconds in 0..TimerSetting.Increment.max)
        }
    }
}
