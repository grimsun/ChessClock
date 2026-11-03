import ClockCore
import CoreHaptics
import UIKit

/// Vibrations that tell players what the clock did without them looking at it, matching the
/// Android app's: a click, two pulses and a long buzz.
final class Haptics {
    // A crisp click, like a physical clock's button. Kept prepared, so it plays the moment a turn
    // ends instead of after the Taptic Engine wakes up.
    private let click = UIImpactFeedbackGenerator(style: .rigid)
    // Patterns the feedback generators can't play; nil on devices without haptics, such as iPads.
    private let engine: CHHapticEngine? =
        CHHapticEngine.capabilitiesForHardware().supportsHaptics ? try? CHHapticEngine() : nil

    init() {
        engine?.isAutoShutdownEnabled = true
        prepare()
    }

    /// Readies the click for a press that may come soon, such as when the clock starts.
    func prepare() { click.prepare() }

    func play(_ signal: ClockSignal) {
        switch signal {
        case .turnEnded:
            click.impactOccurred()
            click.prepare()
        // Two firm pulses, so it can't be mistaken for the click of a press.
        case .lowTime: play([buzz(at: 0, for: 0.08), buzz(at: 0.18, for: 0.08)])
        // One long buzz: the game is over.
        case .outOfTime: play([buzz(at: 0, for: 0.6)])
        }
    }

    private func buzz(at time: TimeInterval, for duration: TimeInterval) -> CHHapticEvent {
        CHHapticEvent(
            eventType: .hapticContinuous,
            parameters: [
                CHHapticEventParameter(parameterID: .hapticIntensity, value: 1),
                CHHapticEventParameter(parameterID: .hapticSharpness, value: 0.5),
            ],
            relativeTime: time,
            duration: duration,
        )
    }

    private func play(_ events: [CHHapticEvent]) {
        guard let engine else { return }
        do {
            // Starts the engine again after it shut itself down or the system reset it.
            try engine.start()
            try engine.makePlayer(with: CHHapticPattern(events: events, parameters: [])).start(atTime: CHHapticTimeImmediate)
        } catch {
            // A missed vibration must never stop the clock.
        }
    }
}
