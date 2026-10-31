/// Where the players sit, which decides whether the first player's time is turned around.
public enum Seating: String, Sendable, CaseIterable, Codable {
    /// Both times read from the bottom of the screen.
    case upright
    /// Players at the top and bottom, each time turned to its player.
    case facing
}

public struct ClockSettings: Codable, Equatable, Sendable {
    public var hours = 0
    public var minutes = 5
    public var seconds = 0
    public var incrementSeconds = 0
    public var incrementMode = IncrementMode.fischer
    public var seating = Seating.upright

    public static let minInitialTime = Duration.seconds(1)
    public static let maxHours = 12
    public static let maxMinutes = 60
    public static let maxSeconds = 59
    public static let maxIncrementSeconds = 60

    public init() {}

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
