package com.lab.chessclock.settings

import com.lab.chessclock.clock.IncrementMode
import com.lab.chessclock.clock.TimeControl
import com.lab.chessclock.clock.TimeStage
import kotlin.time.Duration
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
            entries.firstOrNull { it.storageKey == key } ?: HorizontalOpposite
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

/** The app's set of colors, each with a light and a dark variant. */
enum class ColorPalette(val storageKey: String) {
    Walnut("walnut"),
    Forest("forest"),
    Ocean("ocean"),
    Graphite("graphite");

    companion object {
        fun fromStorageKey(key: String?): ColorPalette = entries.firstOrNull { it.storageKey == key } ?: Walnut
    }
}

/** The key an [IncrementMode] is stored under. */
val IncrementMode.storageKey: String
    get() = when (this) {
        IncrementMode.Fischer -> "fischer"
        IncrementMode.Bronstein -> "bronstein"
        IncrementMode.SimpleDelay -> "delay"
    }

fun incrementModeFromStorageKey(key: String?): IncrementMode =
    IncrementMode.entries.firstOrNull { it.storageKey == key } ?: IncrementMode.Fischer

/**
 * The defaults are set for a phone lying between two players across a board: 5 + 3 blitz, whose
 * increment keeps a won game from being lost on time, and each time turned to its player.
 */
data class ClockSettings(
    /** Each player's starting time, or the first player's when [secondTime] sets the second's apart. */
    val time: Duration = 5.minutes,
    /** The second player's own starting time, to give one player odds; null when both get [time]. */
    val secondTime: Duration? = null,
    val incrementSeconds: Int = 3,
    val incrementMode: IncrementMode = IncrementMode.Fischer,
    /** Time added after a number of moves, for a control in several periods such as classical chess. */
    val stages: List<TimeStage> = emptyList(),
    val orientation: Orientation = Orientation.HorizontalOpposite,
    val faceStyle: FaceStyle = FaceStyle.Rings,
    val theme: ThemeMode = ThemeMode.System,
    val palette: ColorPalette = ColorPalette.Walnut,
    /** A click when a press ends the turn. */
    val vibrateOnTurnEnd: Boolean = true,
    /** Pulses when the player on move gets low on time, and a buzz when it runs out. */
    val vibrateOnLowTime: Boolean = true,
) {
    val timeControl: TimeControl
        get() = TimeControl(
            // A zero time would give a game that is over before it starts and can't be reset.
            initial = time.coerceAtLeast(MIN_INITIAL_TIME),
            increment = incrementSeconds.seconds,
            mode = incrementMode,
            stages = stages,
            secondInitial = (secondTime ?: time).coerceAtLeast(MIN_INITIAL_TIME),
        )

    /** Another period after the last, as classical chess has them: 30 minutes after 40 moves, then 15 after 20 more. */
    fun withStageAdded(): ClockSettings =
        if (stages.size >= MAX_STAGES) this
        else copy(stages = stages + if (stages.isEmpty()) TimeStage(40, 30.minutes) else TimeStage(20, 15.minutes))

    fun withStage(index: Int, stage: TimeStage): ClockSettings =
        copy(stages = stages.mapIndexed { i, it -> if (i == index) stage else it })

    fun withoutStage(index: Int): ClockSettings = copy(stages = stages.filterIndexed { i, _ -> i != index })

    companion object {
        val MIN_INITIAL_TIME = 1.seconds
        const val MAX_INCREMENT_SECONDS = 60

        /** Periods after the first: two cover the controls played over the board. */
        const val MAX_STAGES = 2

        /**
         * The times a slider offers: fine steps for bullet and blitz, coarser ones for longer games,
         * so one slider reaches from 15 seconds to 6 hours.
         */
        val TIME_STEPS: List<Duration> =
            listOf(15, 30, 45).map { it.seconds } +
                (1..10).map { it.minutes } +
                listOf(12, 15, 20, 25, 30, 40, 45, 50, 60, 75, 90, 105, 120, 150, 180, 240, 300, 360).map { it.minutes }

        /** The moves a period can last. */
        val MOVE_STEPS: List<Int> = (5..80 step 5).toList()
    }
}

/** A common time control: whole minutes plus a per-move increment, written "3 + 2". */
data class TimePreset(val minutes: Int, val incrementSeconds: Int) {
    /**
     * The settings with this time control, the same for both players in a single period, keeping
     * everything else such as the orientation. The increment mode is kept too, so a preset reads
     * "3 + 2" whether the 2 is an increment or a delay.
     */
    fun appliedTo(settings: ClockSettings): ClockSettings = settings.copy(
        time = minutes.minutes,
        secondTime = null,
        incrementSeconds = incrementSeconds,
        stages = emptyList(),
    )

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
