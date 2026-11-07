/// Chess clock rules: immutable values, ported from the Android app's `clock` package.

public enum Player: Sendable, CaseIterable {
    case first, second

    public var opponent: Player { self == .first ? .second : .first }
}

/// How a time control's per-move ``TimeControl/increment`` is given.
public enum IncrementMode: String, Sendable, CaseIterable, Codable {
    /// Added after every move, so a player can end up with more time than they started with.
    case fischer
    /// After every move, the time spent on it is given back, up to the increment: time never grows.
    case bronstein
    /// Each move, the clock waits the increment before it starts counting down.
    case simpleDelay = "delay"
}

/// A later period of a multi-stage control, such as the 30 minutes after move 40 in classical chess:
/// `time` is added once a player has made `moves` more moves since the period before began.
public struct TimeStage: Hashable, Sendable {
    public var moves: Int
    public var time: Duration

    public init(moves: Int, time: Duration) {
        self.moves = moves
        self.time = time
    }
}

public struct TimeControl: Equatable, Sendable {
    public var initial: Duration
    public var increment: Duration
    public var mode: IncrementMode
    /// The periods after the first, in order; empty when `initial` is for the whole game.
    public var stages: [TimeStage]
    /// The second player's starting time, set apart from the first's to give one player odds.
    public var secondInitial: Duration

    public init(
        initial: Duration,
        increment: Duration,
        mode: IncrementMode = .fischer,
        stages: [TimeStage] = [],
        secondInitial: Duration? = nil,
    ) {
        self.initial = initial
        self.increment = increment
        self.mode = mode
        self.stages = stages
        self.secondInitial = secondInitial ?? initial
    }

    public func initial(of player: Player) -> Duration { player == .first ? initial : secondInitial }

    /// The time a player gets on making their `moves`th move: a new period's, if one starts there.
    public func stageTime(after moves: Int) -> Duration {
        var startsAt = 0
        for stage in stages {
            startsAt += stage.moves
            if startsAt == moves { return stage.time }
            if startsAt > moves { break }
        }
        return .zero
    }
}

/// One player's remaining time. `max` is the most time the player has had so far, which grows
/// with increments, so `remaining / max` is the fraction shown by the progress ring. `moves` counts
/// the moves the player has ended with a press.
public struct PlayerTime: Equatable, Sendable {
    public var remaining: Duration
    public var max: Duration
    public var moves: Int

    public init(remaining: Duration, max: Duration, moves: Int = 0) {
        self.remaining = remaining
        self.max = max
        self.moves = moves
    }

    public static func full(_ time: Duration) -> PlayerTime { PlayerTime(remaining: time, max: time) }

    public var progress: Double { max == .zero ? 0 : remaining / max }

    /// The player's flag has fallen: out of time.
    public var isExpired: Bool { remaining == .zero }

    func adding(_ time: Duration) -> PlayerTime {
        let newRemaining = remaining + time
        return PlayerTime(remaining: newRemaining, max: Swift.max(max, newRemaining), moves: moves)
    }

    func subtracting(_ time: Duration) -> PlayerTime {
        PlayerTime(remaining: Swift.max(remaining - time, .zero), max: max, moves: moves)
    }
}

public enum ClockStatus: Sendable {
    case notStarted, running, paused, finished
}

/// Immutable chess clock: every action returns the next state.
public struct ChessClock: Equatable, Sendable {
    public let timeControl: TimeControl
    public private(set) var first: PlayerTime
    public private(set) var second: PlayerTime
    /// The player on move, kept while paused; nil before the game starts.
    public private(set) var turn: Player?
    public private(set) var running = false
    /// Time the player on move has spent on this move so far, across pauses.
    public private(set) var moveTime: Duration = .zero

    public init(_ timeControl: TimeControl) {
        self.timeControl = timeControl
        first = .full(timeControl.initial(of: .first))
        second = .full(timeControl.initial(of: .second))
    }

    public var status: ClockStatus {
        if first.isExpired || second.isExpired { return .finished }
        if running { return .running }
        return turn == nil ? .notStarted : .paused
    }

    /// The player whose time is running, or nil when the clock is stopped.
    public var active: Player? { running ? turn : nil }

    public func time(of player: Player) -> PlayerTime { player == .first ? first : second }

    /// The moves played, counted as on a scoresheet: a move each, so the count rises when the
    /// player who started presses, and stays as their opponent replies.
    public var moves: Int { Swift.max(first.moves, second.moves) }

    /// With a simple delay, how long the player on move can still think before their time runs.
    public var delayRemaining: Duration {
        guard timeControl.mode == .simpleDelay, turn != nil, status != .finished else { return .zero }
        return Swift.max(timeControl.increment - moveTime, .zero)
    }

    /// `player` presses their clock: the opponent's time starts running. Ending a running turn
    /// counts the move and awards the increment as its ``IncrementMode`` says, plus the next
    /// period's time when the move completes a period. Pressing either side of a paused clock
    /// resumes the player on move, so a pause can't change whose turn it is.
    public func press(_ player: Player) -> ChessClock {
        var next = self
        switch status {
        case .finished:
            return self
        case .running:
            guard turn == player else { return self }
            let bonus = moveBonus
            next.update(player) { time in
                var ended = time.adding(bonus + timeControl.stageTime(after: time.moves + 1))
                ended.moves += 1
                return ended
            }
            next.turn = player.opponent
            next.moveTime = .zero
        case .paused:
            next.running = true
        case .notStarted:
            next.turn = player.opponent
            next.running = true
        }
        return next
    }

    /// The main button: start the game, pause it, or reset it once paused or finished.
    public func toggle() -> ChessClock {
        switch status {
        case .notStarted: press(.first)
        case .running: pause()
        case .paused, .finished: reset()
        }
    }

    public func pause() -> ChessClock {
        guard status != .finished else { return self }
        var next = self
        next.running = false
        return next
    }

    public func reset() -> ChessClock { ChessClock(timeControl) }

    /// Charges `elapsed` to the running player, less what is left of a simple delay.
    public func tick(_ elapsed: Duration) -> ChessClock {
        guard let player = active, status != .finished else { return self }
        let charged = Swift.max(elapsed - delayRemaining, .zero)
        var next = self
        next.update(player) { $0.subtracting(charged) }
        next.moveTime += elapsed
        return next
    }

    /// The time given back to the player on move when they end this move.
    private var moveBonus: Duration {
        switch timeControl.mode {
        case .fischer: timeControl.increment
        case .bronstein: Swift.min(moveTime, timeControl.increment)
        case .simpleDelay: .zero
        }
    }

    private mutating func update(_ player: Player, _ transform: (PlayerTime) -> PlayerTime) {
        if player == .first { first = transform(first) } else { second = transform(second) }
    }
}

/// What the clock should tell the players without them looking at it.
public enum ClockSignal: Sendable {
    /// A press ended a turn and the opponent's clock started.
    case turnEnded
    /// The player on move just dropped below ``ChessClock/lowTime``.
    case lowTime
    /// A player's time ran out.
    case outOfTime
}

extension ChessClock {
    /// Below this the player on move sees their face turn red, and is warned once.
    public static let lowTime = Duration.seconds(10)

    /// Whether `player` is on move in an unfinished game with less than ``lowTime`` left.
    public func isLowOnTime(_ player: Player) -> Bool {
        status != .finished && turn == player && time(of: player).remaining < Self.lowTime
    }

    /// The signal for a press that took this clock to `next`: a click only when it started the
    /// opponent's clock, not for a press that changed nothing.
    public func signal(pressedTo next: ChessClock) -> ClockSignal? {
        next.status == .running && next.turn != turn ? .turnEnded : nil
    }

    /// The signal for time passing from this clock to `next`. Only while the player's clock runs,
    /// so a player who starts their turn already short isn't warned on every move.
    public func signal(tickedTo next: ChessClock) -> ClockSignal? {
        guard let player = active else { return nil }
        if next.status == .finished, status != .finished { return .outOfTime }
        if next.isLowOnTime(player), !isLowOnTime(player) { return .lowTime }
        return nil
    }
}
