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
                .transition(.opacity.combined(with: .scale(scale: 0.8)))
            }
        }
        .animation(.easeInOut(duration: 0.2), value: clock.status == .running)
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
        face(time: time, finished: finished)
            .frame(maxWidth: faceSize, maxHeight: faceSize)
            .aspectRatio(1, contentMode: .fit)
            .padding(16)
            .rotationEffect(rotation)
            // The face gives a little under the finger while it is down.
            .animation(.spring(duration: 0.2)) { $0.scaleEffect(pressed ? 0.96 : 1) }
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
            .accessibilityValue(accessibilityState(time: time, onMove: onMove))
            .accessibilityHint("End turn")
            .accessibilityIdentifier(player == .first ? "face.first" : "face.second")
            .accessibilityAction(.default) { onPress(.zero) }
    }

    /// The square both face styles draw in. Rings and panels share its size and content, so they
    /// differ only in shape: an outline that empties or a filled block.
    private func face(time: PlayerTime, finished: Bool) -> some View {
        let role = role(time: time, finished: finished)
        let (color, onColor) = colors(role)
        let progress = finished ? 1 : time.progress
        // Smooth between ticks while running; otherwise a reset or a new time control sweeps the
        // ring back to full instead of jumping.
        let sweep: Animation = clock.status == .running ? .linear(duration: 0.1) : .easeInOut(duration: 0.5)
        return ZStack {
            switch style {
            case .rings:
                Circle()
                    .inset(by: ringStroke / 2)
                    .trim(from: 0, to: progress)
                    .stroke(color, style: StrokeStyle(lineWidth: ringStroke, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(sweep, value: progress)
                content(time: time, color: palette.ringText)
            case .panels:
                RoundedRectangle(cornerRadius: panelCorner).fill(color)
                content(time: time, color: onColor)
                // The remaining share of the player's time, as a thin bar along the panel's bottom edge.
                ProgressBar(progress: progress, color: onColor)
                    .animation(sweep, value: progress)
                    .padding(.horizontal, panelCorner)
                    .padding(.bottom, 20)
                    .frame(maxHeight: .infinity, alignment: .bottom)
            }
        }
        // The turn passes from one face to the other as a short cross-fade instead of a jump.
        .animation(.easeInOut(duration: colorFade), value: role)
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
            if let delayCaption {
                Text(delayCaption)
                    .font(.headline)
                    .frame(maxHeight: .infinity, alignment: .bottom)
                    .padding(.bottom, ringStroke + 40)
            }
        }
        .foregroundStyle(color)
    }

    /// Whose move it is or that time ran out, and any delay countdown, as VoiceOver reads them.
    private func accessibilityState(time: PlayerTime, onMove: Bool) -> String {
        var parts: [String] = []
        if time.isExpired { parts.append("Out of time") } else if onMove { parts.append("On move") }
        if let delay = delayCaption { parts.append(delay) }
        return parts.joined(separator: ", ")
    }

    /// With a simple delay, the seconds the player on move can still think before their time runs.
    private var delayCaption: String? {
        guard clock.turn == player, clock.delayRemaining > .zero else { return nil }
        return "Delay \(clock.delayRemaining.wholeSecondsRoundedUp) s"
    }

    private enum Role { case waiting, onMove, lowOnTime, won, lost }

    private func role(time: PlayerTime, finished: Bool) -> Role {
        if finished { return time.isExpired ? .lost : .won }
        if clock.isLowOnTime(player) { return .lowOnTime }
        // While paused too, so it's clear whose move it is when play resumes.
        return clock.turn == player ? .onMove : .waiting
    }

    /// The face color, and the color of a panel's content drawn on it.
    private func colors(_ role: Role) -> (Color, Color) {
        switch role {
        case .waiting: (palette.inactive, palette.onInactive)
        case .onMove: (palette.primary, palette.onPrimary)
        case .lowOnTime, .lost: (palette.loss, palette.onLoss)
        case .won: (palette.win, palette.onWin)
        }
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

/// Short, so the turn is seen to pass at once.
private let colorFade = 0.1

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
                .contentTransition(.opacity)
                .animation(.easeInOut(duration: colorFade), value: status)
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
