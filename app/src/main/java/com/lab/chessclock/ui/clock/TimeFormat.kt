package com.lab.chessclock.ui.clock

import kotlin.time.Duration

/** `H:MM:SS` from an hour up, `MM:SS` down to a minute, then `MM:SS.cc` for the final minute. */
fun formatClockTime(time: Duration): String {
    val ms = time.inWholeMilliseconds
    val hours = ms / 3_600_000
    val minutes = ms / 60_000 % 60
    val seconds = ms / 1_000 % 60
    // Tenths of a second, shown in the hundredths position as before.
    val hundredths = ms % 1_000 / 100 * 10

    return when {
        hours >= 1 -> "$hours:${minutes.twoDigits()}:${seconds.twoDigits()}"
        ms <= 60_000 -> "${minutes.twoDigits()}:${seconds.twoDigits()}.${hundredths.twoDigits()}"
        else -> "${minutes.twoDigits()}:${seconds.twoDigits()}"
    }
}

private fun Long.twoDigits(): String = toString().padStart(2, '0')
