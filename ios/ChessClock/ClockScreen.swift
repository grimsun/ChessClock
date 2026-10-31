import ClockCore
import SwiftUI

struct ClockScreen: View {
    @Bindable var settings: SettingsStore
    @State private var model: ClockModel
    @State private var showingSettings = false
    @Environment(\.scenePhase) private var scenePhase

    init(settings: SettingsStore) {
        self.settings = settings
        _model = State(initialValue: ClockModel(timeControl: settings.settings.timeControl))
    }

    var body: some View {
        let clock = model.clock
        let facing = settings.settings.seating == .facing
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
        // The button sits on the second player's half: hide it during play so a hard tap near the
        // corner ends the turn instead of opening settings and pausing the game.
        .overlay(alignment: .bottomTrailing) {
            if clock.status != .running {
                Button("Settings", systemImage: "gearshape.fill") {
                    model.pause()
                    showingSettings = true
                }
                .labelStyle(.iconOnly)
                .font(.title2)
                .foregroundStyle(Color.onWaiting)
                .padding(12)
                .background(Color.waiting, in: .circle)
                .padding(16)
            }
        }
        .sheet(isPresented: $showingSettings) {
            SettingsView(settings: $settings.settings)
        }
        // A new time control starts a fresh game; other setting changes leave the game alone.
        .onChange(of: settings.settings.timeControl) { _, timeControl in
            model.setTimeControl(timeControl)
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
    @State private var pressed = false

    var body: some View {
        let time = clock.time(of: player)
        let finished = clock.status == .finished
        let onMove = !finished && clock.turn == player

        // The whole half of the screen is the player's button, like a physical clock.
        face(time: time, finished: finished, onMove: onMove)
            .padding(16)
            .frame(maxWidth: 300, maxHeight: 300)
            .aspectRatio(1, contentMode: .fit)
            .scaleEffect(pressed ? 0.96 : 1)
            .animation(.spring(duration: 0.2), value: pressed)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
            // Like a physical clock, it acts the moment it is touched rather than when the finger
            // lifts, as a Button would: the opponent's clock starts a tap's length sooner.
            .gesture(touchDown)
            // VoiceOver double-taps instead of touching, so it gets the same action as a button.
            .accessibilityElement(children: .ignore)
            .accessibilityAddTraits(.isButton)
            .accessibilityLabel(formatClockTime(time.remaining))
            .accessibilityValue(time.isExpired ? "Out of time" : onMove ? "On move" : "")
            .accessibilityHint("End turn")
            .accessibilityAction(.default) { onPress() }
    }

    private var touchDown: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { _ in
                guard !pressed else { return }
                pressed = true
                onPress()
            }
            .onEnded { _ in pressed = false }
    }

    private func face(time: PlayerTime, finished: Bool, onMove: Bool) -> some View {
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
            // With a simple delay, the seconds the player on move can still think before their time runs.
            if clock.turn == player, clock.delayRemaining > .zero {
                Text("Delay \(clock.delayRemaining.wholeSecondsRoundedUp) s")
                    .font(.headline)
                    .frame(maxHeight: .infinity, alignment: .bottom)
                    .padding(.bottom, 40)
            }
        }
    }

    private func color(time: PlayerTime, finished: Bool, onMove: Bool) -> Color {
        if finished { return time.isExpired ? .loss : .win }
        if onMove && time.remaining < lowTime { return .loss }
        // While paused too, so it's clear whose move it is when play resumes.
        return onMove ? .onMove : .waiting
    }
}

private extension Duration {
    /// Whole seconds, counting a started second as a whole one, as a countdown shows them.
    var wholeSecondsRoundedUp: Int64 { (self + .milliseconds(999)).components.seconds }
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
    ClockScreen(settings: SettingsStore(defaults: UserDefaults(suiteName: "preview")!))
}
