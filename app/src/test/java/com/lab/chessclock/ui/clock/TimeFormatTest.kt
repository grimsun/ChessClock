package com.lab.chessclock.ui.clock

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimeFormatTest {

    @Test
    fun hoursUseHMMSS() = assertEquals("1:02:03", formatClockTime(1.hours + 2.minutes + 3.seconds))

    @Test
    fun minutesUseMMSS() = assertEquals("05:00", formatClockTime(5.minutes))

    @Test
    fun lastMinuteShowsTenths() {
        assertEquals("01:00.00", formatClockTime(60.seconds))
        assertEquals("00:59.90", formatClockTime(59_950.milliseconds))
        assertEquals("00:00.00", formatClockTime(Duration.ZERO))
    }

    @Test
    fun switchesFormatAtTheMinuteAndHourBoundaries() {
        assertEquals("01:00", formatClockTime(60_001.milliseconds))
        assertEquals("59:59", formatClockTime(1.hours - 1.milliseconds))
        assertEquals("1:00:00", formatClockTime(1.hours))
    }

    @Test
    fun hoursAreNotPadded() = assertEquals("12:00:00", formatClockTime(12.hours))

    @Test
    fun partialSecondsRoundDown() = assertEquals("04:59", formatClockTime(5.minutes - 1.milliseconds))
}
