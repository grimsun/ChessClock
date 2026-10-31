import ClockCore
import Observation

/// Runs a ``ChessClock``: applies presses and charges the running player as real time passes.
@Observable
final class ClockModel {
    static let tickInterval = Duration.milliseconds(100)

    private(set) var clock: ChessClock
    @ObservationIgnored private var ticker: Task<Void, Never>?
    @ObservationIgnored private var lastTick: ContinuousClock.Instant?
    @ObservationIgnored private let time = ContinuousClock()

    init(timeControl: TimeControl) {
        clock = ChessClock(timeControl)
    }

    /// `player` touched their face `touchedAgo` before this call: the turn ends at that moment, so
    /// the time the app took to handle the touch is charged to neither player as thinking time.
    func press(_ player: Player, touchedAgo: Duration = .zero) {
        update(at: time.now - touchedAgo) { $0.press(player) }
    }

    func toggle() { update { $0.toggle() } }

    func pause() { update { $0.pause() } }

    /// A new time control starts a fresh game.
    func setTimeControl(_ timeControl: TimeControl) {
        guard timeControl != clock.timeControl else { return }
        stopTicker()
        clock = ChessClock(timeControl)
    }

    /// Applies `action` as of `moment`, when it happened, or now.
    private func update(at moment: ContinuousClock.Instant? = nil, _ action: (ChessClock) -> ChessClock) {
        let moment = moment ?? time.now
        chargeElapsedTime(until: moment)
        clock = action(clock)
        if clock.status == .running { startTicker(from: moment) } else { stopTicker() }
    }

    /// Starts charging the running player from `start`.
    private func startTicker(from start: ContinuousClock.Instant) {
        guard ticker == nil else { return }
        lastTick = start
        ticker = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(for: Self.tickInterval)
                guard let self, !Task.isCancelled else { return }
                chargeElapsedTime()
                if clock.status != .running { stopTicker() }
            }
        }
    }

    private func stopTicker() {
        ticker?.cancel()
        ticker = nil
        lastTick = nil
    }

    /// Charges the running player for the time from the last tick to `now`, so no time is lost
    /// between ticks. A `now` before the last tick charges nothing rather than giving time back.
    private func chargeElapsedTime(until now: ContinuousClock.Instant? = nil) {
        guard let last = lastTick else { return }
        let now = now ?? time.now
        guard now > last else { return }
        lastTick = now
        clock = clock.tick(now - last)
    }
}
