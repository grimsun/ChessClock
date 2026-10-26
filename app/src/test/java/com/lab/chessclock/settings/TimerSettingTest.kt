package com.lab.chessclock.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerSettingTest {

    @Test
    fun updateWritesTheFieldThatValueInReads() {
        TimerSetting.entries.forEachIndexed { index, setting ->
            val value = index + 7
            assertEquals(value, setting.valueIn(setting.update(ClockSettings(), value)))
        }
    }

    @Test
    fun eachSettingChangesOnlyItsOwnField() {
        val settings = TimerSetting.entries.fold(ClockSettings()) { acc, setting -> setting.update(acc, 1) }
        assertEquals(ClockSettings(hours = 1, minutes = 1, seconds = 1, incrementSeconds = 1), settings)
    }
}
