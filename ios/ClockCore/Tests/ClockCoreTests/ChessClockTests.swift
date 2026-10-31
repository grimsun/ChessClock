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
