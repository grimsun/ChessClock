import ClockCore
import SwiftUI

struct ClockScreen: View {
    @Bindable var settings: SettingsStore
    let model: ClockModel
    @State private var showingSettings = false
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.palette) private var palette

    var body: some View {
        let clock = model.clock
        let seating = settings.settings.seating
        let faceStyle = settings.settings.faceStyle
        GeometryReader { geometry in
            // Faces stacked on a tall screen, side by side on a wide one, so each gets half the room.
            let layout = geometry.size.width > geometry.size.height
                ? AnyLayout(HStackLayout(spacing: 0))
                : AnyLayout(VStackLayout(spacing: 0))
            layout {
                PlayerFace(clock: clock, player: .first, style: faceStyle, rotation: seating.rotation(of: .first)) {
                    model.press(.first, touchedAgo: $0)
                }
                ControlButton(status: clock.status) { model.toggle() }
                    .rotationEffect(seating.controlRotation)
                PlayerFace(clock: clock, player: .second, style: faceStyle, rotation: seating.rotation(of: .second)) {
                    model.press(.second, touchedAgo: $0)
                }
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
                .foregroundStyle(palette.onInactive)
                .padding(12)
                .background(palette.inactive, in: .circle)
                .padding(16)
            }
        }
        .background(palette.background)
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

private struct PlayerFace: View {
    let clock: ChessClock
    let player: Player
    let style: FaceStyle
    /// Turns the face to where its player sits.
    let rotation: Angle
    /// The player touched the face, and how long ago the screen sensed the touch.
    let onPress: (Duration) -> Void
    @State private var pressed = false
    @Environment(\.palette) private var palette

    var body: some View {
        let time = clock.time(of: player)
        let finished = clock.status == .finished
        let onMove = !finished && clock.turn == player

        // The whole half of the screen is the player's button, like a physical clock.
        face(time: time, finished: finished, onMove: onMove)
            .frame(maxWidth: faceSize, maxHeight: faceSize)
            .aspectRatio(1, contentMode: .fit)
            .padding(16)
            .rotationEffect(rotation)
            .scaleEffect(pressed ? 0.96 : 1)
            .animation(.spring(duration: 0.2), value: pressed)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            // Like a physical clock, it acts the moment it is touched rather than when the finger
            // lifts, as a Button would: the opponent's clock starts a tap's length sooner.
            .overlay {
                TouchDownArea(onTouchDown: onPress) { pressed = $0 }
            }
            // VoiceOver double-taps instead of touching, so it gets the same action as a button.
            .accessibilityElement(children: .ignore)
            .accessibilityAddTraits(.isButton)
            .accessibilityLabel(formatClockTime(time.remaining))
            .accessibilityValue(time.isExpired ? "Out of time" : onMove ? "On move" : "")
            .accessibilityHint("End turn")
            .accessibilityAction(.default) { onPress(.zero) }
    }

    /// The square both face styles draw in. Rings and panels share its size and content, so they
    /// differ only in shape: an outline that empties or a filled block.
    private func face(time: PlayerTime, finished: Bool, onMove: Bool) -> some View {
        let (color, onColor) = colors(time: time, finished: finished, onMove: onMove)
        let progress = finished ? 1 : time.progress
        return ZStack {
            switch style {
            case .rings:
                Circle()
                    .inset(by: ringStroke / 2)
                    .trim(from: 0, to: progress)
                    .stroke(color, style: StrokeStyle(lineWidth: ringStroke, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(clock.status == .running ? .linear(duration: 0.1) : nil, value: progress)
                content(time: time, color: palette.ringText)
            case .panels:
                RoundedRectangle(cornerRadius: panelCorner).fill(color)
                content(time: time, color: onColor)
                // The remaining share of the player's time, as a thin bar along the panel's bottom edge.
                ProgressBar(progress: progress, color: onColor)
                    .animation(clock.status == .running ? .linear(duration: 0.1) : nil, value: progress)
                    .padding(.horizontal, panelCorner)
                    .padding(.bottom, 20)
                    .frame(maxHeight: .infinity, alignment: .bottom)
            }
        }
    }

    /// The time, and below it any delay countdown, laid out the same on every face.
    private func content(time: PlayerTime, color: Color) -> some View {
        ZStack {
            Text(formatClockTime(time.remaining))
                .font(.system(size: 96, weight: .medium).monospacedDigit())
                // Shrinks to fit a small face or a large text size instead of overflowing.
                .minimumScaleFactor(0.125)
                .lineLimit(1)
                // Clear of the ring's stroke even at the corners of the text.
                .padding(.horizontal, ringStroke + 24)
            // With a simple delay, the seconds the player on move can still think before their time runs.
            if clock.turn == player, clock.delayRemaining > .zero {
                Text("Delay \(clock.delayRemaining.wholeSecondsRoundedUp) s")
                    .font(.headline)
                    .frame(maxHeight: .infinity, alignment: .bottom)
                    .padding(.bottom, ringStroke + 40)
            }
        }
        .foregroundStyle(color)
    }

    /// The face color, and the color of a panel's content drawn on it.
    private func colors(time: PlayerTime, finished: Bool, onMove: Bool) -> (Color, Color) {
        if finished { return time.isExpired ? (palette.loss, palette.onLoss) : (palette.win, palette.onWin) }
        if clock.isLowOnTime(player) { return (palette.loss, palette.onLoss) }
        // While paused too, so it's clear whose move it is when play resumes.
        return onMove ? (palette.primary, palette.onPrimary) : (palette.inactive, palette.onInactive)
    }
}

/// A thin track with the remaining share of time filled in.
private struct ProgressBar: View {
    let progress: Double
    let color: Color

    var body: some View {
        Capsule()
            .fill(color.opacity(0.25))
            .overlay(alignment: .leading) {
                GeometryReader { geometry in
                    Capsule().fill(color).frame(width: geometry.size.width * progress)
                }
            }
            .frame(height: 4)
    }
}

private let ringStroke: CGFloat = 15

/// Large enough for big digits, small enough that a ring stays a ring on an iPad.
private let faceSize: CGFloat = 340

private let panelCorner: CGFloat = 36

/// Reports each finger as it lands, with how long ago the screen sensed it, and whether any finger
/// is down. A UIKit view rather than a SwiftUI gesture, so it sees every finger, not just the first,
/// and each touch's own timestamp.
private struct TouchDownArea: UIViewRepresentable {
    let onTouchDown: (Duration) -> Void
    let onPressedChange: (Bool) -> Void

    func makeUIView(context: Context) -> TouchView { TouchView() }

    func updateUIView(_ view: TouchView, context: Context) {
        view.onTouchDown = onTouchDown
        view.onPressedChange = onPressedChange
    }

    final class TouchView: UIView {
        var onTouchDown: (Duration) -> Void = { _ in }
        var onPressedChange: (Bool) -> Void = { _ in }
        private var fingers = 0

        override init(frame: CGRect) {
            super.init(frame: frame)
            isMultipleTouchEnabled = true
        }

        required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

        override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
            // A touch's timestamp counts seconds since boot, like systemUptime.
            let now = ProcessInfo.processInfo.systemUptime
            let age = now - (touches.map(\.timestamp).min() ?? now)
            // Longer than the main thread is ever busy means the timestamp can't be trusted.
            onTouchDown(.seconds(age >= 0 && age <= 1 ? age : 0))
            fingers += touches.count
            onPressedChange(true)
        }

        override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) { lift(touches) }

        override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) { lift(touches) }

        private func lift(_ touches: Set<UITouch>) {
            fingers = max(fingers - touches.count, 0)
            if fingers == 0 { onPressedChange(false) }
        }
    }
}

private extension Seating {
    /// The second player (bottom) always reads the screen the right way up for their side.
    var controlRotation: Angle {
        switch self {
        case .upright, .facing: .zero
        case .sideways, .facingSides: .degrees(90)
        }
    }

    /// Facing each other turns the first player's face half a turn from the second's.
    func rotation(of player: Player) -> Angle {
        switch self {
        case .upright, .sideways: controlRotation
        case .facing, .facingSides: player == .first ? controlRotation + .degrees(180) : controlRotation
        }
    }
}

private extension Duration {
    /// Whole seconds, counting a started second as a whole one, as a countdown shows them.
    var wholeSecondsRoundedUp: Int64 { (self + .milliseconds(999)).components.seconds }
}

private struct ControlButton: View {
    let status: ClockStatus
    let action: () -> Void
    @Environment(\.palette) private var palette

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.title3.weight(.medium))
                .frame(width: 100, height: 100)
        }
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.circle)
        .tint(palette.primary)
        .foregroundStyle(palette.onPrimary)
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
    let settings = SettingsStore(defaults: UserDefaults(suiteName: "preview")!)
    ClockScreen(settings: settings, model: ClockModel(settings: settings, haptics: Haptics()))
}
