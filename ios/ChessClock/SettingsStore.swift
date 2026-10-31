import ClockCore
import Foundation
import Observation

/// The settings, saved to UserDefaults as JSON on every change.
@Observable
final class SettingsStore {
    var settings: ClockSettings {
        didSet { save() }
    }

    @ObservationIgnored private let defaults: UserDefaults
    private static let key = "settings"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        // Unreadable data, such as from a future version, falls back to the defaults.
        settings = defaults.data(forKey: Self.key)
            .flatMap { try? JSONDecoder().decode(ClockSettings.self, from: $0) } ?? ClockSettings()
    }

    private func save() {
        if let data = try? JSONEncoder().encode(settings) {
            defaults.set(data, forKey: Self.key)
        }
    }
}
