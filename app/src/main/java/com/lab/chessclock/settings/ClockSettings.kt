package com.lab.chessclock.settings

import com.lab.chessclock.clock.TimeControl
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** How the two clock faces are rotated, depending on where the players sit. */
enum class Orientation(val storageKey: String) {
    HorizontalSame("horizontal_same"),
    HorizontalOpposite("horizontal_opposite"),
    VerticalSame("vertical_same"),
    VerticalOpposite("vertical_opposite");

    companion object {
        fun fromStorageKey(key: String?): Orientation =
            entries.firstOrNull { it.storageKey == key } ?: HorizontalSame
    }
}

data class ClockSettings(
    val hours: Int = 0,
    val minutes: Int = 5,
    val seconds: Int = 0,
    val incrementSeconds: Int = 0,
    val orientation: Orientation = Orientation.HorizontalSame,
) {
    val timeControl: TimeControl
        get() = TimeControl(
            initial = hours.hours + minutes.minutes + seconds.seconds,
            increment = incrementSeconds.seconds,
        )

    companion object {
        const val MAX_HOURS = 12
        const val MAX_MINUTES = 60
        const val MAX_SECONDS = 60
        const val MAX_INCREMENT_SECONDS = 60
    }
}
