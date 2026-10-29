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

/** How each player's remaining time is drawn on the clock screen. */
enum class FaceStyle(val storageKey: String) {
    /** The time inside a ring that empties as it runs down. */
    Rings("rings"),

    /** Each half a solid colored panel with large digits, readable from across the table. */
    Panels("panels");

    companion object {
        fun fromStorageKey(key: String?): FaceStyle = entries.firstOrNull { it.storageKey == key } ?: Rings
    }
}

/** Whether the app is light or dark. */
enum class ThemeMode(val storageKey: String) {
    /** Light or dark following the system setting. */
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromStorageKey(key: String?): ThemeMode = entries.firstOrNull { it.storageKey == key } ?: System
    }
}

/** The numeric settings that make up the time control, so screens and storage can treat them alike. */
enum class TimerSetting(val storageKey: String, val max: Int) {
    Hours("timer_h", 12),
    Minutes("timer_m", 60),
    Seconds("timer_s", 60),
    Increment("increment", 60);

    fun valueIn(settings: ClockSettings): Int = when (this) {
        Hours -> settings.hours
        Minutes -> settings.minutes
        Seconds -> settings.seconds
        Increment -> settings.incrementSeconds
    }

    fun update(settings: ClockSettings, value: Int): ClockSettings = when (this) {
        Hours -> settings.copy(hours = value)
        Minutes -> settings.copy(minutes = value)
        Seconds -> settings.copy(seconds = value)
        Increment -> settings.copy(incrementSeconds = value)
    }
}

data class ClockSettings(
    val hours: Int = 0,
    val minutes: Int = 5,
    val seconds: Int = 0,
    val incrementSeconds: Int = 0,
    val orientation: Orientation = Orientation.HorizontalSame,
    val faceStyle: FaceStyle = FaceStyle.Rings,
    val theme: ThemeMode = ThemeMode.System,
) {
    val timeControl: TimeControl
        get() = TimeControl(
            // All-zero settings would give a game that is over before it starts and can't be reset.
            initial = (hours.hours + minutes.minutes + seconds.seconds).coerceAtLeast(MIN_INITIAL_TIME),
            increment = incrementSeconds.seconds,
        )

    companion object {
        val MIN_INITIAL_TIME = 1.seconds
    }
}

/** A common time control: whole minutes plus a per-move increment, written "3 + 2". */
data class TimePreset(val minutes: Int, val incrementSeconds: Int) {
    /** The settings with this time control, keeping everything else such as the orientation. */
    fun appliedTo(settings: ClockSettings): ClockSettings =
        settings.copy(hours = 0, minutes = minutes, seconds = 0, incrementSeconds = incrementSeconds)

    /** Whether the settings already are this time control, so the screen can mark it. */
    fun matches(settings: ClockSettings): Boolean = appliedTo(settings) == settings

    companion object {
        /** Bullet, blitz, rapid and classical controls, shortest first. */
        val All = listOf(
            TimePreset(1, 0), TimePreset(2, 1), TimePreset(3, 0), TimePreset(3, 2), TimePreset(5, 0),
            TimePreset(5, 3), TimePreset(10, 0), TimePreset(10, 5), TimePreset(15, 10), TimePreset(30, 0),
        )
    }
}
