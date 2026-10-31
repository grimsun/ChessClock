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

public struct TimeControl: Equatable, Sendable {
    public var initial: Duration
    public var increment: Duration
    public var mode: IncrementMode

    public init(initial: Duration, increment: Duration, mode: IncrementMode = .fischer) {
        self.initial = initial
        self.increment = increment
        self.mode = mode
    }
}

/// One player's remaining time. `max` is the most time the player has had so far, which grows
/// with increments, so `remaining / max` is the fraction shown by the progress ring.
public struct PlayerTime: Equatable, Sendable {
    public var remaining: Duration
    public var max: Duration

    public init(remaining: Duration, max: Duration) {
        self.remaining = remaining
        self.max = max
    }

    public static func full(_ time: Duration) -> PlayerTime { PlayerTime(remaining: time, max: time) }

    public var progress: Double { max == .zero ? 0 : remaining / max }

    /// The player's flag has fallen: out of time.
    public var isExpired: Bool { remaining == .zero }

    func adding(_ time: Duration) -> PlayerTime {
        let newRemaining = remaining + time
        return PlayerTime(remaining: newRemaining, max: Swift.max(max, newRemaining))
    }

    func subtracting(_ time: Duration) -> PlayerTime {
        PlayerTime(remaining: Swift.max(remaining - time, .zero), max: max)
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
        first = .full(timeControl.initial)
        second = .full(timeControl.initial)
    }

    public var status: ClockStatus {
        if first.isExpired || second.isExpired { return .finished }
        if running { return .running }
        return turn == nil ? .notStarted : .paused
    }

    /// The player whose time is running, or nil when the clock is stopped.
    public var active: Player? { running ? turn : nil }

    public func time(of player: Player) -> PlayerTime { player == .first ? first : second }

    /// With a simple delay, how long the player on move can still think before their time runs.
    public var delayRemaining: Duration {
        guard timeControl.mode == .simpleDelay, turn != nil, status != .finished else { return .zero }
        return Swift.max(timeControl.increment - moveTime, .zero)
    }

    /// `player` presses their clock: the opponent's time starts running. Ending a running turn
    /// awards the increment as its ``IncrementMode`` says. Pressing either side of a paused clock
    /// resumes the player on move, so a pause can't change whose turn it is.
    public func press(_ player: Player) -> ChessClock {
        var next = self
        switch status {
        case .finished:
            return self
        case .running:
            guard turn == player else { return self }
            next.update(player) { $0.adding(moveBonus) }
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
