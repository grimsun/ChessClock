package com.lab.chessclock.ui.clock

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** `H:MM:SS` from an hour up, `MM:SS` down to a minute, then `MM:SS.cc` for the final minute. */
fun formatClockTime(time: Duration): String = time.toComponents { hours, minutes, seconds, nanoseconds ->
    val minutesAndSeconds = "${minutes.twoDigits()}:${seconds.twoDigits()}"
    // Tenths of a second, shown in the hundredths position as before.
    val hundredths = nanoseconds / 100_000_000 * 10

    when {
        hours >= 1 -> "$hours:$minutesAndSeconds"
        time <= 1.minutes -> "$minutesAndSeconds.${hundredths.twoDigits()}"
        else -> minutesAndSeconds
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')
