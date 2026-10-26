package com.lab.chessclock.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ClockSettingsTest {

    @Test
    fun timeControlAddsUpTheTimerSettings() {
        val timeControl = ClockSettings(hours = 1, minutes = 2, seconds = 3, incrementSeconds = 4).timeControl
        assertEquals(1.hours + 2.minutes + 3.seconds, timeControl.initial)
        assertEquals(4.seconds, timeControl.increment)
    }

    @Test
    fun allZeroTimeStillGivesAPlayableGame() {
        val timeControl = ClockSettings(hours = 0, minutes = 0, seconds = 0).timeControl
        assertEquals(ClockSettings.MIN_INITIAL_TIME, timeControl.initial)
    }
}
