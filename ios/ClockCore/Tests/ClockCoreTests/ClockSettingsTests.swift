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

    @Test func defaultsSuitPlayersAcrossABoard() {
        let settings = ClockSettings()
        #expect(settings.timeControl == TimeControl(initial: .seconds(300), increment: .seconds(3)))
        #expect(settings.seating == .facing)
        #expect(settings.vibrateOnTurnEnd && settings.vibrateOnLowTime)
    }

    @Test func settingsRoundTripThroughJSON() throws {
        var settings = ClockSettings()
        settings.minutes = 3
        settings.incrementMode = .simpleDelay
        settings.seating = .facingSides
        settings.faceStyle = .panels
        settings.theme = .dark
        settings.palette = .ocean
        settings.vibrateOnLowTime = false

        let decoded = try JSONDecoder().decode(ClockSettings.self, from: JSONEncoder().encode(settings))
        #expect(decoded == settings)
    }

    @Test func olderSavedSettingsKeepTheirValuesAndDefaultTheRest() throws {
        let saved = #"{"hours":0,"minutes":3,"seconds":0,"incrementSeconds":2,"incrementMode":"bronstein","seating":"upright"}"#

        let decoded = try JSONDecoder().decode(ClockSettings.self, from: Data(saved.utf8))

        #expect(decoded.timeControl == TimeControl(initial: .seconds(180), increment: .seconds(2), mode: .bronstein))
        #expect(decoded.seating == .upright)
        #expect(decoded.faceStyle == .rings)
        #expect(decoded.vibrateOnTurnEnd)
    }

    @Test func anUnknownValueFallsBackToItsDefaultAlone() throws {
        let saved = #"{"minutes":10,"palette":"sunset"}"#

        let decoded = try JSONDecoder().decode(ClockSettings.self, from: Data(saved.utf8))

        #expect(decoded.minutes == 10)
        #expect(decoded.palette == .walnut)
    }

    @Test func eachVibrationHasItsOwnSwitch() {
        var settings = ClockSettings()
        settings.vibrateOnTurnEnd = false
        #expect(!settings.vibrates(for: .turnEnded))
        #expect(settings.vibrates(for: .lowTime) && settings.vibrates(for: .outOfTime))

        settings.vibrateOnLowTime = false
        #expect(!settings.vibrates(for: .lowTime) && !settings.vibrates(for: .outOfTime))
    }

    @Test func timeControlsAreLabelledAsPlayersWriteThem() {
        #expect(TimeControl(initial: .seconds(300), increment: .seconds(3)).shortLabel == "5+3")
        #expect(TimeControl(initial: .seconds(5400), increment: .seconds(30)).shortLabel == "90+30")
        #expect(TimeControl(initial: .seconds(90), increment: .zero).shortLabel == "1:30+0")
        #expect(TimeControl(initial: .seconds(5), increment: .zero).shortLabel == "0:05+0")
    }
}
