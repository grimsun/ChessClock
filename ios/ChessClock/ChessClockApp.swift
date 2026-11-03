import ClockCore
import SwiftUI

@main
struct ChessClockApp: App {
    @State private var settings: SettingsStore
    @State private var model: ClockModel

    init() {
        let settings = SettingsStore()
        _settings = State(initialValue: settings)
        _model = State(initialValue: ClockModel(settings: settings, haptics: Haptics()))
    }

    var body: some Scene {
        WindowGroup {
            ClockScreen(settings: settings, model: model)
                .environment(\.palette, .of(settings.settings.palette))
                .preferredColorScheme(settings.settings.theme.colorScheme)
        }
    }
}
