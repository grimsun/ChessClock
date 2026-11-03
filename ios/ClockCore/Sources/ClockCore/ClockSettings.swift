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
    public var hours = 0
    public var minutes = 5
    public var seconds = 0
    public var incrementSeconds = 3
    public var incrementMode = IncrementMode.fischer
    public var seating = Seating.facing
    public var faceStyle = FaceStyle.rings
    public var theme = ThemeMode.system
    public var palette = ColorPalette.walnut
    /// A click when a press ends the turn.
    public var vibrateOnTurnEnd = true
    /// Pulses when the player on move gets low on time, and a buzz when it runs out.
    public var vibrateOnLowTime = true

    public static let minInitialTime = Duration.seconds(1)
    public static let maxHours = 12
    public static let maxMinutes = 60
    public static let maxSeconds = 59
    public static let maxIncrementSeconds = 60

    public init() {}

    /// Settings saved by an older version lack the newer keys, and a newer version may save values
    /// this one doesn't know: either way each such setting keeps its default instead of all of them
    /// being lost.
    public init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        func value<T: Decodable>(_ key: CodingKeys, _ fallback: T) -> T {
            (try? container.decodeIfPresent(T.self, forKey: key)) ?? fallback
        }
        let defaults = ClockSettings()
        hours = value(.hours, defaults.hours)
        minutes = value(.minutes, defaults.minutes)
        seconds = value(.seconds, defaults.seconds)
        incrementSeconds = value(.incrementSeconds, defaults.incrementSeconds)
        incrementMode = value(.incrementMode, defaults.incrementMode)
        seating = value(.seating, defaults.seating)
        faceStyle = value(.faceStyle, defaults.faceStyle)
        theme = value(.theme, defaults.theme)
        palette = value(.palette, defaults.palette)
        vibrateOnTurnEnd = value(.vibrateOnTurnEnd, defaults.vibrateOnTurnEnd)
        vibrateOnLowTime = value(.vibrateOnLowTime, defaults.vibrateOnLowTime)
    }

    public var timeControl: TimeControl {
        TimeControl(
            // All-zero settings would give a game that is over before it starts and can't be reset.
            initial: max(.seconds(hours * 3600 + minutes * 60 + seconds), Self.minInitialTime),
            increment: .seconds(incrementSeconds),
            mode: incrementMode,
        )
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

    /// The settings with this time control, keeping everything else, the increment mode included.
    public func applied(to settings: ClockSettings) -> ClockSettings {
        var applied = settings
        applied.hours = 0
        applied.minutes = minutes
        applied.seconds = 0
        applied.incrementSeconds = incrementSeconds
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
