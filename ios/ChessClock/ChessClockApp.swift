import SwiftUI

@main
struct ChessClockApp: App {
    @State private var settings = SettingsStore()

    var body: some Scene {
        WindowGroup {
            ClockScreen(settings: settings)
        }
    }
}
