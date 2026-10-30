package com.lab.chessclock.clock

import kotlin.time.Duration

enum class Player {
    First,
    Second;

    val opponent: Player
        get() = if (this == First) Second else First
}

/** How a time control's per-move [TimeControl.increment] is given. */
enum class IncrementMode {
    /** Added after every move, so a player can end up with more time than they started with. */
    Fischer,

    /** After every move, the time spent on it is given back, up to the increment: time never grows. */
    Bronstein,

    /** Each move, the clock waits the increment before it starts counting down. */
    SimpleDelay,
}

data class TimeControl(
    val initial: Duration,
    val increment: Duration,
    val mode: IncrementMode = IncrementMode.Fischer,
)

/**
 * One player's remaining time. [max] is the most time the player has had so far, which grows
 * with increments, so `remaining / max` is the fraction shown by the progress ring.
 */
data class PlayerTime(val remaining: Duration, val max: Duration) {
    val progress: Float
        get() = if (max == Duration.ZERO) 0f else (remaining / max).toFloat()

    /** The player's flag has fallen: out of time. */
    val isExpired: Boolean
        get() = remaining == Duration.ZERO

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
    /** The player on move, kept while paused; null before the game starts. */
    val turn: Player? = null,
    val running: Boolean = false,
    /** Time the player on move has spent on this move so far, across pauses. */
    val moveTime: Duration = Duration.ZERO,
) {
    val status: ClockStatus
        get() = when {
            first.isExpired || second.isExpired -> ClockStatus.Finished
            running -> ClockStatus.Running
            turn != null -> ClockStatus.Paused
            else -> ClockStatus.NotStarted
        }

    /** The player whose time is running, or null when the clock is stopped. */
    val active: Player?
        get() = turn.takeIf { running }

    fun timeOf(player: Player): PlayerTime = if (player == Player.First) first else second

    /** With a simple delay, how long the player on move can still think before their time runs. */
    val delayRemaining: Duration
        get() = if (timeControl.mode == IncrementMode.SimpleDelay && turn != null && status != ClockStatus.Finished) {
            (timeControl.increment - moveTime).coerceAtLeast(Duration.ZERO)
        } else {
            Duration.ZERO
        }

    /**
     * [player] presses their clock: the opponent's time starts running. Ending a running turn
     * awards the increment as its [IncrementMode] says. Pressing either side of a paused clock
     * resumes the player on move, so a pause can't change whose turn it is.
     */
    fun press(player: Player): ChessClock = when (status) {
        ClockStatus.Finished -> this
        ClockStatus.Running ->
            if (turn == player) update(player) { it.add(moveBonus()) }.copy(turn = player.opponent, moveTime = Duration.ZERO)
            else this
        ClockStatus.Paused -> copy(running = true)
        ClockStatus.NotStarted -> copy(turn = player.opponent, running = true)
    }

    /** The main button: start the game, pause it, or reset it once paused or finished. */
    fun toggle(): ChessClock = when (status) {
        ClockStatus.NotStarted -> press(Player.First)
        ClockStatus.Running -> pause()
        ClockStatus.Paused, ClockStatus.Finished -> reset()
    }

    fun pause(): ChessClock = if (status == ClockStatus.Finished) this else copy(running = false)

    fun reset(): ChessClock = ChessClock(timeControl)

    /** Charges [elapsed] to the running player, less what is left of a simple delay. */
    fun tick(elapsed: Duration): ChessClock {
        val player = active ?: return this
        if (status == ClockStatus.Finished) return this
        val charged = (elapsed - delayRemaining).coerceAtLeast(Duration.ZERO)
        return update(player) { it.subtract(charged) }.copy(moveTime = moveTime + elapsed)
    }

    /** The time given back to the player on move when they end this move. */
    private fun moveBonus(): Duration = when (timeControl.mode) {
        IncrementMode.Fischer -> timeControl.increment
        IncrementMode.Bronstein -> minOf(moveTime, timeControl.increment)
        IncrementMode.SimpleDelay -> Duration.ZERO
    }

    private fun update(player: Player, transform: (PlayerTime) -> PlayerTime): ChessClock {
        val time = transform(timeOf(player))
        return if (player == Player.First) copy(first = time) else copy(second = time)
    }
}
