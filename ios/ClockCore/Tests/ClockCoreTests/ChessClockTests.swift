import Testing
@testable import ClockCore

struct ChessClockTests {
    let clock = ChessClock(TimeControl(initial: .seconds(300), increment: .seconds(2)))

    @Test func newClockIsNotStarted() {
        #expect(clock.status == .notStarted)
        #expect(clock.first.remaining == .seconds(300))
        #expect(clock.active == nil)
    }

    @Test func toggleStartsSecondPlayer() {
        let started = clock.toggle()
        #expect(started.status == .running)
        #expect(started.active == .second)
    }

    @Test func endingATurnAwardsIncrementAndSwitchesPlayer() {
        let afterMove = clock.press(.first).tick(.seconds(10)).press(.second)
        #expect(afterMove.active == .first)
        #expect(afterMove.second.remaining == .seconds(292))
    }

    @Test func pressingWhileOpponentRunsDoesNothing() {
        let running = clock.press(.first)
        #expect(running.press(.first) == running)
    }

    @Test func resumingKeepsTheTurnWhicheverSideIsTapped() {
        let paused = clock.press(.first).tick(.seconds(1)).pause()
        for tapped in Player.allCases {
            let resumed = paused.press(tapped)
            #expect(resumed.active == .second)
            #expect(resumed.second == paused.second)
        }
    }

    @Test func runningOutOfTimeFinishesAtZero() {
        let finished = clock.press(.first).tick(.seconds(600))
        #expect(finished.status == .finished)
        #expect(finished.second.remaining == .zero)
        #expect(finished.press(.second) == finished)
        #expect(finished.toggle() == clock)
    }

    @Test func bronsteinGivesBackTheTimeUsedUpToTheIncrement() {
        let bronstein = ChessClock(TimeControl(initial: .seconds(300), increment: .seconds(3), mode: .bronstein))
        #expect(bronstein.press(.first).tick(.seconds(1)).press(.second).second.remaining == .seconds(300))
        #expect(bronstein.press(.first).tick(.seconds(10)).press(.second).second.remaining == .seconds(293))
    }

    @Test func simpleDelayStartsCountingAfterTheDelay() {
        let delay = ChessClock(TimeControl(initial: .seconds(300), increment: .seconds(3), mode: .simpleDelay))
        let thinking = delay.press(.first).tick(.seconds(2))
        #expect(thinking.second.remaining == .seconds(300))
        #expect(thinking.delayRemaining == .seconds(1))
        #expect(thinking.tick(.seconds(5)).second.remaining == .seconds(296))

        let nextMove = thinking.press(.second)
        #expect(nextMove.delayRemaining == .seconds(3))
        #expect(nextMove.second.max == .seconds(300))
    }

    @Test func progressIsTheShareOfTimeLeft() {
        #expect(PlayerTime(remaining: .seconds(15), max: .seconds(60)).progress == 0.25)
        #expect(PlayerTime.full(.zero).progress == 0)
    }
}

struct TimeFormatTests {
    @Test(arguments: [
        (Duration.seconds(3725), "1:02:05"),
        (.seconds(300), "05:00"),
        (.seconds(60), "01:00.00"),
        (.milliseconds(9_870), "00:09.80"),
        (.zero, "00:00.00"),
    ])
    func formats(time: Duration, expected: String) {
        #expect(formatClockTime(time) == expected)
    }
}

struct ClockSignalTests {
    let clock = ChessClock(TimeControl(initial: .seconds(12), increment: .zero))

    @Test func aPressThatStartsTheOpponentClicks() {
        let started = clock.press(.first)
        #expect(clock.signal(pressedTo: started) == .turnEnded)
        #expect(started.signal(pressedTo: started.press(.second)) == .turnEnded)
    }

    @Test func aPressThatChangesNothingIsSilent() {
        let running = clock.press(.first)
        #expect(running.signal(pressedTo: running.press(.first)) == nil)
        #expect(running.signal(pressedTo: running.pause()) == nil)
    }

    @Test func warnsOnceWhenTimeGetsLowAndAgainWhenItRunsOut() {
        let running = clock.press(.first)
        let low = running.tick(.seconds(3))
        #expect(running.signal(tickedTo: low) == .lowTime)
        #expect(low.signal(tickedTo: low.tick(.seconds(1))) == nil)
        #expect(low.signal(tickedTo: low.tick(.seconds(20))) == .outOfTime)
    }

    @Test func aTurnThatStartsLowIsNotWarned() {
        let short = ChessClock(TimeControl(initial: .seconds(5), increment: .zero)).press(.first)
        #expect(short.signal(tickedTo: short.tick(.seconds(1))) == nil)
    }

    @Test func eachPressThatEndsATurnCountsAMove() {
        let started = clock.press(.first)
        #expect(started.moves == 0)
        let afterWhite = started.press(.second)
        #expect(afterWhite.second.moves == 1 && afterWhite.moves == 1)
        let afterBlack = afterWhite.press(.first)
        #expect(afterBlack.first.moves == 1 && afterBlack.moves == 1)
        #expect(afterBlack.press(.second).moves == 2)
    }

    @Test func pressesThatDontEndATurnDontCount() {
        let paused = clock.press(.first).press(.second).pause()
        #expect(paused.press(.first).moves == 1)
        #expect(paused.press(.second).press(.second).moves == 1)
        #expect(paused.reset().moves == 0)
    }

    @Test func aPeriodsTimeIsAddedOnTheMoveThatCompletesThePeriodBefore() {
        let stages = [TimeStage(moves: 2, time: .seconds(1800)), TimeStage(moves: 1, time: .seconds(900))]
        var game = ChessClock(TimeControl(initial: .seconds(600), increment: .zero, stages: stages)).press(.first)

        game = game.press(.second)
        #expect(game.second.remaining == .seconds(600))
        game = game.press(.first).press(.second)
        #expect(game.second.remaining == .seconds(2400))
        #expect(game.second.max == .seconds(2400))
        #expect(game.first.remaining == .seconds(600))

        game = game.press(.first).press(.second).press(.first)
        #expect(game.second.remaining == .seconds(3300))
        #expect(game.first.remaining == .seconds(3300))

        // The last period lasts the rest of the game.
        game = game.press(.second).press(.first)
        #expect(game.second.remaining == .seconds(3300))
    }

    @Test func aPeriodsTimeComesOnTopOfTheIncrement() {
        let timeControl = TimeControl(initial: .seconds(600), increment: .seconds(5), stages: [TimeStage(moves: 1, time: .seconds(60))])
        #expect(ChessClock(timeControl).press(.first).press(.second).second.remaining == .seconds(665))
    }

    @Test func eachPlayerStartsWithTheirOwnTime() {
        let odds = ChessClock(TimeControl(initial: .seconds(300), increment: .zero, secondInitial: .seconds(180)))
        #expect(odds.first == .full(.seconds(300)))
        #expect(odds.second == .full(.seconds(180)))
        #expect(odds.press(.first).reset() == odds)
    }
}
