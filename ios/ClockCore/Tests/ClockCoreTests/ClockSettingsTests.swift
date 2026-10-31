import Foundation
import Testing
@testable import ClockCore

struct ClockSettingsTests {
    @Test func timeControlAddsUpTheTimerSettings() {
        var settings = ClockSettings()
        settings.hours = 1
        settings.minutes = 2
        settings.seconds = 3
        settings.incrementSeconds = 4
        settings.incrementMode = .bronstein

        #expect(settings.timeControl == TimeControl(initial: .seconds(3723), increment: .seconds(4), mode: .bronstein))
    }

    @Test func allZeroTimeStillGivesAPlayableGame() {
        var settings = ClockSettings()
        settings.minutes = 0
        #expect(settings.timeControl.initial == ClockSettings.minInitialTime)
    }

    @Test func presetReplacesTheTimeAndKeepsTheRest() {
        var settings = ClockSettings()
        settings.hours = 1
        settings.seconds = 30
        settings.incrementMode = .simpleDelay
        settings.seating = .facing

        let applied = TimePreset(3, 2).applied(to: settings)

        #expect(applied.timeControl == TimeControl(initial: .seconds(180), increment: .seconds(2), mode: .simpleDelay))
        #expect(applied.seating == .facing)
        #expect(TimePreset(3, 2).matches(applied))
        #expect(!TimePreset(3, 0).matches(applied))
    }

    @Test func presetsAreDistinctAndFitTheControls() {
        #expect(Set(TimePreset.all).count == TimePreset.all.count)
        for preset in TimePreset.all {
            #expect((1...ClockSettings.maxMinutes).contains(preset.minutes))
            #expect((0...ClockSettings.maxIncrementSeconds).contains(preset.incrementSeconds))
        }
    }

    @Test func settingsRoundTripThroughJSON() throws {
        var settings = ClockSettings()
        settings.minutes = 3
        settings.incrementMode = .simpleDelay
        settings.seating = .facing

        let decoded = try JSONDecoder().decode(ClockSettings.self, from: JSONEncoder().encode(settings))
        #expect(decoded == settings)
    }
}
