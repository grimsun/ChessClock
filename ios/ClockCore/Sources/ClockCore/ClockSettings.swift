/// Where the players sit, which decides how each player's time is turned.
public enum Seating: String, Sendable, CaseIterable, Codable {
    /// Both times read from the bottom of the screen.
    case upright
    /// Players at the top and bottom, each time turned to its player.
    case facing
    /// Both times read from the same long side.
    case sideways
    /// Players at the left and right, each time turned to its player.
    case facingSides
}

/// How each player's remaining time is drawn on the clock screen.
public enum FaceStyle: String, Sendable, CaseIterable, Codable {
    /// The time inside a ring that empties as it runs down.
    case rings
    /// Each half a solid colored panel with large digits, readable from across the table.
    case panels
}

/// Whether the app is light or dark.
public enum ThemeMode: String, Sendable, CaseIterable, Codable {
    /// Light or dark following the system setting.
    case system, light, dark
}

/// The app's set of colors, each with a light and a dark variant.
public enum ColorPalette: String, Sendable, CaseIterable, Codable {
    case walnut, forest, ocean, graphite
}

/// The defaults are set for a phone lying between two players across a board: 5 + 3 blitz, whose
/// increment keeps a won game from being lost on time, and each time turned to its player.
public struct ClockSettings: Codable, Equatable, Sendable {
    /// Each player's starting time, or the first player's when `secondTime` sets the second's apart.
    public var time = Duration.seconds(5 * 60)
    /// The second player's own starting time, to give one player odds; nil when both get `time`.
    public var secondTime: Duration?
    public var incrementSeconds = 3
    public var incrementMode = IncrementMode.fischer
    /// Time added after a number of moves, for a control in several periods such as classical chess.
    public var stages: [TimeStage] = []
    public var seating = Seating.facing
    public var faceStyle = FaceStyle.rings
    public var theme = ThemeMode.system
    public var palette = ColorPalette.walnut
    /// A click when a press ends the turn.
    public var vibrateOnTurnEnd = true
    /// Pulses when the player on move gets low on time, and a buzz when it runs out.
    public var vibrateOnLowTime = true

    public static let minInitialTime = Duration.seconds(1)
    public static let maxIncrementSeconds = 60
    /// Periods after the first: two cover the controls played over the board.
    public static let maxStages = 2

    /// The times a stepper offers: fine steps for bullet and blitz, coarser ones for longer games,
    /// so one control reaches from 15 seconds to 6 hours.
    public static let timeSteps: [Duration] =
        [15, 30, 45].map { Duration.seconds($0) }
            + (1...10).map { Duration.seconds($0 * 60) }
            + [12, 15, 20, 25, 30, 40, 45, 50, 60, 75, 90, 105, 120, 150, 180, 240, 300, 360].map { Duration.seconds($0 * 60) }

    /// The moves a period can last.
    public static let moveSteps = Array(stride(from: 5, through: 80, by: 5))

    public init() {}

    private enum CodingKeys: String, CodingKey {
        case timeSeconds, secondTimeSeconds, incrementSeconds, incrementMode, stages, seating, faceStyle, theme
        case palette, vibrateOnTurnEnd, vibrateOnLowTime
        // How the first versions stored the time.
        case hours, minutes, seconds
    }

    /// Settings saved by an older version lack the newer keys, and a newer version may save values
    /// this one doesn't know: either way each such setting keeps its default instead of all of them
    /// being lost.
    public init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        func value<T: Decodable>(_ key: CodingKeys, _ fallback: T) -> T {
            (try? container.decodeIfPresent(T.self, forKey: key)) ?? fallback
        }
        let defaults = ClockSettings()
        if let seconds = value(.timeSeconds, Int?.none) {
            time = .seconds(seconds)
        } else {
            time = .seconds(value(.hours, 0) * 3600 + value(.minutes, 5) * 60 + value(.seconds, 0))
        }
        secondTime = value(.secondTimeSeconds, Int?.none).map { .seconds($0) }
        incrementSeconds = value(.incrementSeconds, defaults.incrementSeconds)
        incrementMode = value(.incrementMode, defaults.incrementMode)
        stages = value(.stages, [StoredStage]()).compactMap(\.stage)
        seating = value(.seating, defaults.seating)
        faceStyle = value(.faceStyle, defaults.faceStyle)
        theme = value(.theme, defaults.theme)
        palette = value(.palette, defaults.palette)
        vibrateOnTurnEnd = value(.vibrateOnTurnEnd, defaults.vibrateOnTurnEnd)
        vibrateOnLowTime = value(.vibrateOnLowTime, defaults.vibrateOnLowTime)
    }

    public func encode(to encoder: any Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(time.components.seconds, forKey: .timeSeconds)
        try container.encodeIfPresent(secondTime?.components.seconds, forKey: .secondTimeSeconds)
        try container.encode(incrementSeconds, forKey: .incrementSeconds)
        try container.encode(incrementMode, forKey: .incrementMode)
        try container.encode(stages.map(StoredStage.init), forKey: .stages)
        try container.encode(seating, forKey: .seating)
        try container.encode(faceStyle, forKey: .faceStyle)
        try container.encode(theme, forKey: .theme)
        try container.encode(palette, forKey: .palette)
        try container.encode(vibrateOnTurnEnd, forKey: .vibrateOnTurnEnd)
        try container.encode(vibrateOnLowTime, forKey: .vibrateOnLowTime)
    }

    /// A period as saved: moves and whole seconds.
    private struct StoredStage: Codable {
        var moves: Int
        var seconds: Int64

        init(_ stage: TimeStage) {
            moves = stage.moves
            seconds = stage.time.components.seconds
        }

        /// Nil for a period that can't be played, which is skipped.
        var stage: TimeStage? { moves > 0 && seconds >= 0 ? TimeStage(moves: moves, time: .seconds(seconds)) : nil }
    }

    public var timeControl: TimeControl {
        TimeControl(
            // A zero time would give a game that is over before it starts and can't be reset.
            initial: max(time, Self.minInitialTime),
            increment: .seconds(incrementSeconds),
            mode: incrementMode,
            stages: stages,
            secondInitial: max(secondTime ?? time, Self.minInitialTime),
        )
    }

    /// Another period after the last, as classical chess has them: 30 minutes after 40 moves, then
    /// 15 after 20 more.
    public mutating func addStage() {
        guard stages.count < Self.maxStages else { return }
        stages.append(stages.isEmpty ? TimeStage(moves: 40, time: .seconds(30 * 60)) : TimeStage(moves: 20, time: .seconds(15 * 60)))
    }
}

/// A common time control: whole minutes plus seconds per move, written "3 + 2".
public struct TimePreset: Hashable, Sendable {
    public let minutes: Int
    public let incrementSeconds: Int

    public init(_ minutes: Int, _ incrementSeconds: Int) {
        self.minutes = minutes
        self.incrementSeconds = incrementSeconds
    }

    /// The settings with this time control, the same for both players in a single period, keeping
    /// everything else, the increment mode included.
    public func applied(to settings: ClockSettings) -> ClockSettings {
        var applied = settings
        applied.time = .seconds(minutes * 60)
        applied.secondTime = nil
        applied.incrementSeconds = incrementSeconds
        applied.stages = []
        return applied
    }

    /// Whether the settings already are this time control, so the screen can mark it.
    public func matches(_ settings: ClockSettings) -> Bool { applied(to: settings) == settings }

    public var label: String { "\(minutes) + \(incrementSeconds)" }

    /// Bullet, blitz, rapid and classical controls, shortest first.
    public static let all = [
        TimePreset(1, 0), TimePreset(2, 1), TimePreset(3, 0), TimePreset(3, 2), TimePreset(5, 0),
        TimePreset(5, 3), TimePreset(10, 0), TimePreset(10, 5), TimePreset(15, 10), TimePreset(30, 0),
    ]
}

extension ClockSettings {
    /// Whether the player has left the vibration for `signal` on.
    public func vibrates(for signal: ClockSignal) -> Bool {
        switch signal {
        case .turnEnded: vibrateOnTurnEnd
        case .lowTime, .outOfTime: vibrateOnLowTime
        }
    }
}
