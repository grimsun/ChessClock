import ClockCore
import SwiftUI

struct ClockScreen: View {
    @State private var model = ClockModel(timeControl: TimeControl(initial: .seconds(300), increment: .zero))
    @Environment(\.scenePhase) private var scenePhase

    /// Players sit at the ends of the phone, so the first player's time is turned toward them.
    private let facing = true

    var body: some View {
        let clock = model.clock
        GeometryReader { geometry in
            // Faces stacked on a tall screen, side by side on a wide one, so each gets half the room.
            let layout = geometry.size.width > geometry.size.height
                ? AnyLayout(HStackLayout(spacing: 0))
                : AnyLayout(VStackLayout(spacing: 0))
            layout {
                PlayerFace(clock: clock, player: .first) { model.press(.first) }
                    .rotationEffect(.degrees(facing ? 180 : 0))
                ControlButton(status: clock.status) { model.toggle() }
                PlayerFace(clock: clock, player: .second) { model.press(.second) }
            }
        }
        // Stop the clock when the app leaves the foreground.
        .onChange(of: scenePhase) { _, phase in
            if phase != .active { model.pause() }
        }
        // A long think would otherwise let the screen lock, which pauses the game.
        .onChange(of: clock.status) { _, status in
            UIApplication.shared.isIdleTimerDisabled = status == .running
        }
    }
}

/// Below this the player on move sees their face turn red.
let lowTime = Duration.seconds(10)

private struct PlayerFace: View {
    let clock: ChessClock
    let player: Player
    let onPress: () -> Void

    var body: some View {
        let time = clock.time(of: player)
        let finished = clock.status == .finished
        let onMove = !finished && clock.turn == player

        // The whole half of the screen is the player's button, like a physical clock.
        Button(action: onPress) {
            ZStack {
                Circle()
                    .trim(from: 0, to: finished ? 1 : time.progress)
                    .stroke(color(time: time, finished: finished, onMove: onMove), style: StrokeStyle(lineWidth: 15, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(clock.status == .running ? .linear(duration: 0.1) : nil, value: time.progress)
                Text(formatClockTime(time.remaining))
                    .font(.system(size: 60).monospacedDigit())
                    .minimumScaleFactor(0.3)
                    .lineLimit(1)
                    .padding(.horizontal, 28)
            }
            .padding(16)
            .frame(maxWidth: 300, maxHeight: 300)
            .aspectRatio(1, contentMode: .fit)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(formatClockTime(time.remaining))
        .accessibilityValue(time.isExpired ? "Out of time" : onMove ? "On move" : "")
        .accessibilityHint("End turn")
    }

    private func color(time: PlayerTime, finished: Bool, onMove: Bool) -> Color {
        if finished { return time.isExpired ? .loss : .win }
        if onMove && time.remaining < lowTime { return .loss }
        // While paused too, so it's clear whose move it is when play resumes.
        return onMove ? .onMove : .waiting
    }
}

private struct ControlButton: View {
    let status: ClockStatus
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.title3.weight(.medium))
                .frame(width: 100, height: 100)
        }
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.circle)
        .tint(.onMove)
        .foregroundStyle(Color.onOnMove)
    }

    private var label: LocalizedStringKey {
        switch status {
        case .notStarted: "Start"
        case .running: "Pause"
        case .paused, .finished: "Reset"
        }
    }
}

#Preview {
    ClockScreen()
}
