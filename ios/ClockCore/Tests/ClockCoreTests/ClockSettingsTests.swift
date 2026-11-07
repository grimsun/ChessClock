import Foundation
import Testing
@testable import ClockCore

struct ClockSettingsTests {
    @Test func timeControlHasTheTimeIncrementAndStages() {
        var settings = ClockSettings()
        settings.time = .seconds(5400)
        settings.incrementSeconds = 30
        settings.incrementMode = .bronstein
        settings.stages = [TimeStage(moves: 40, time: .seconds(1800))]

        #expect(settings.timeControl == TimeControl(
            initial: .seconds(5400), increment: .seconds(30), mode: .bronstein, stages: settings.stages,
        ))
        #expect(settings.timeControl.secondInitial == .seconds(5400))
    }

    @Test func aSecondTimeIsTheSecondPlayersOnly() {
        var settings = ClockSettings()
        settings.secondTime = .seconds(180)
        #expect(settings.timeControl.initial(of: .first) == .seconds(300))
        #expect(settings.timeControl.initial(of: .second) == .seconds(180))
    }

    @Test func zeroTimeStillGivesAPlayableGame() {
        var settings = ClockSettings()
        settings.time = .zero
        settings.secondTime = .zero
        #expect(settings.timeControl.initial == ClockSettings.minInitialTime)
        #expect(settings.timeControl.secondInitial == ClockSettings.minInitialTime)
    }

    @Test func stagesAreAddedUpToTheMost() {
        var settings = ClockSettings()
        settings.addStage()
        settings.addStage()
        settings.addStage()
        #expect(settings.stages == [TimeStage(moves: 40, time: .seconds(1800)), TimeStage(moves: 20, time: .seconds(900))])
    }

    @Test func stepsAreOrderedAndHoldThePresetsAndStageDefaults() {
        #expect(ClockSettings.timeSteps == ClockSettings.timeSteps.sorted())
        #expect(Set(ClockSettings.timeSteps).count == ClockSettings.timeSteps.count)
        #expect(ClockSettings.moveSteps == ClockSettings.moveSteps.sorted())
        for preset in TimePreset.all { #expect(ClockSettings.timeSteps.contains(.seconds(preset.minutes * 60))) }
        var settings = ClockSettings()
        settings.addStage()
        settings.addStage()
        for stage in settings.stages {
            #expect(ClockSettings.timeSteps.contains(stage.time))
            #expect(ClockSettings.moveSteps.contains(stage.moves))
        }
    }

    @Test func presetReplacesTheTimeControlAndKeepsTheRest() {
        var settings = ClockSettings()
        settings.time = .seconds(3630)
        settings.secondTime = .seconds(60)
        settings.stages = [TimeStage(moves: 40, time: .seconds(1800))]
        settings.incrementMode = .simpleDelay
        settings.seating = .facing

        let applied = TimePreset(3, 2).applied(to: settings)

        #expect(applied.timeControl == TimeControl(initial: .seconds(180), increment: .seconds(2), mode: .simpleDelay))
        #expect(applied.seating == .facing)
        #expect(TimePreset(3, 2).matches(applied))
        #expect(!TimePreset(3, 0).matches(applied))
    }

    @Test func aPresetMatchesOnlyOnePeriodWithOneTime() {
        var settings = TimePreset(3, 2).applied(to: ClockSettings())
        settings.secondTime = .seconds(120)
        #expect(!TimePreset(3, 2).matches(settings))
        settings.secondTime = nil
        settings.addStage()
        #expect(!TimePreset(3, 2).matches(settings))
    }

    @Test func presetsAreDistinctAndFitTheIncrement() {
        #expect(Set(TimePreset.all).count == TimePreset.all.count)
        for preset in TimePreset.all {
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
        settings.time = .seconds(185)
        settings.secondTime = .seconds(120)
        settings.stages = [TimeStage(moves: 40, time: .seconds(1800)), TimeStage(moves: 20, time: .seconds(900))]
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
        let saved = #"{"hours":1,"minutes":3,"seconds":5,"incrementSeconds":2,"incrementMode":"bronstein","seating":"upright"}"#

        let decoded = try JSONDecoder().decode(ClockSettings.self, from: Data(saved.utf8))

        #expect(decoded.timeControl == TimeControl(initial: .seconds(3785), increment: .seconds(2), mode: .bronstein))
        #expect(decoded.seating == .upright)
        #expect(decoded.faceStyle == .rings)
        #expect(decoded.stages.isEmpty && decoded.secondTime == nil)
        #expect(decoded.vibrateOnTurnEnd)
    }

    @Test func anUnknownValueFallsBackToItsDefaultAlone() throws {
        let saved = #"{"timeSeconds":600,"palette":"sunset","stages":[{"moves":40,"seconds":1800},{"moves":0,"seconds":60}]}"#

        let decoded = try JSONDecoder().decode(ClockSettings.self, from: Data(saved.utf8))

        #expect(decoded.time == .seconds(600))
        #expect(decoded.palette == .walnut)
        #expect(decoded.stages == [TimeStage(moves: 40, time: .seconds(1800))])
    }

    @Test func eachVibrationHasItsOwnSwitch() {
        var settings = ClockSettings()
        settings.vibrateOnTurnEnd = false
        #expect(!settings.vibrates(for: .turnEnded))
        #expect(settings.vibrates(for: .lowTime) && settings.vibrates(for: .outOfTime))

        settings.vibrateOnLowTime = false
        #expect(!settings.vibrates(for: .lowTime) && !settings.vibrates(for: .outOfTime))
    }
}
