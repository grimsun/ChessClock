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

    func press(_ player: Player) { update { $0.press(player) } }

    func toggle() { update { $0.toggle() } }

    func pause() { update { $0.pause() } }

    /// A new time control starts a fresh game.
    func setTimeControl(_ timeControl: TimeControl) {
        guard timeControl != clock.timeControl else { return }
        stopTicker()
        clock = ChessClock(timeControl)
    }

    private func update(_ action: (ChessClock) -> ChessClock) {
        chargeElapsedTime()
        clock = action(clock)
        if clock.status == .running { startTicker() } else { stopTicker() }
    }

    private func startTicker() {
        guard ticker == nil else { return }
        lastTick = time.now
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

    /// Charges the running player for the time since the last tick, so no time is lost between ticks.
    private func chargeElapsedTime() {
        guard let last = lastTick else { return }
        let now = time.now
        lastTick = now
        clock = clock.tick(now - last)
    }
}
