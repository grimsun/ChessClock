package com.lab.chessclock.clock

import kotlin.time.Duration

enum class Player {
    First,
    Second;

    val opponent: Player
        get() = if (this == First) Second else First
}

data class TimeControl(val initial: Duration, val increment: Duration)

/**
 * One player's remaining time. [max] is the most time the player has had so far, which grows
 * with increments, so `remaining / max` is the fraction shown by the progress ring.
 */
data class PlayerTime(val remaining: Duration, val max: Duration) {
    val progress: Float
        get() = if (max == Duration.ZERO) 0f else (remaining / max).toFloat()

    internal fun add(time: Duration): PlayerTime {
        val newRemaining = remaining + time
        return PlayerTime(newRemaining, maxOf(max, newRemaining))
    }

    internal fun subtract(time: Duration): PlayerTime =
        copy(remaining = (remaining - time).coerceAtLeast(Duration.ZERO))

    companion object {
        fun full(time: Duration) = PlayerTime(time, time)
    }
}

enum class ClockStatus { NotStarted, Running, Paused, Finished }

/** Immutable chess clock: every action returns the next state. */
data class ChessClock(
    val timeControl: TimeControl,
    val first: PlayerTime = PlayerTime.full(timeControl.initial),
    val second: PlayerTime = PlayerTime.full(timeControl.initial),
    /** The player whose time is running, or null when the clock is stopped. */
    val active: Player? = null,
) {
    val status: ClockStatus
        get() = when {
            first.remaining == Duration.ZERO || second.remaining == Duration.ZERO -> ClockStatus.Finished
            active != null -> ClockStatus.Running
            first.remaining < first.max || second.remaining < second.max -> ClockStatus.Paused
            else -> ClockStatus.NotStarted
        }

    fun timeOf(player: Player): PlayerTime = if (player == Player.First) first else second

    /**
     * [player] presses their clock: the opponent's time starts running. Ending a running turn
     * awards the increment; starting or resuming does not.
     */
    fun press(player: Player): ChessClock = when (status) {
        ClockStatus.Finished -> this
        ClockStatus.Running ->
            if (active == player) withTime(player, timeOf(player).add(timeControl.increment))
                .copy(active = player.opponent)
            else this
        ClockStatus.NotStarted, ClockStatus.Paused -> copy(active = player.opponent)
    }

    /** The main button: start the game, pause it, or reset it once paused or finished. */
    fun toggle(): ChessClock = when (status) {
        ClockStatus.NotStarted -> copy(active = Player.Second)
        ClockStatus.Running -> pause()
        ClockStatus.Paused, ClockStatus.Finished -> reset()
    }

    fun pause(): ChessClock = if (status == ClockStatus.Finished) this else copy(active = null)

    fun reset(): ChessClock = ChessClock(timeControl)

    /** Charges [elapsed] to the running player. */
    fun tick(elapsed: Duration): ChessClock {
        val player = active ?: return this
        if (status == ClockStatus.Finished) return this
        return withTime(player, timeOf(player).subtract(elapsed))
    }

    private fun withTime(player: Player, time: PlayerTime): ChessClock =
        if (player == Player.First) copy(first = time) else copy(second = time)
}
