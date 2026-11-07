import ClockCore
import SwiftUI

struct ClockScreen: View {
    @Bindable var settings: SettingsStore
    let model: ClockModel
    @State private var showingSettings = false
    /// Whether the faces sit side by side, which settings name each player's time by.
    @State private var wide = false
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.palette) private var palette

    var body: some View {
        let clock = model.clock
        let seating = settings.settings.seating
        let faceStyle = settings.settings.faceStyle
        GeometryReader { geometry in
            // Faces stacked on a tall screen, side by side on a wide one, so each gets half the room.
            let wide = geometry.size.width > geometry.size.height
            let layout = wide ? AnyLayout(HStackLayout(spacing: 0)) : AnyLayout(VStackLayout(spacing: 0))
            layout {
                // Faces are drawn clear of the corner buttons, the one opposite them by as much so both
                // stay the same size; each whole half still takes touches.
                PlayerFace(
                    face: clock.face(of: .first), player: .first, style: faceStyle,
                    rotation: seating.rotation(of: .first), inset: wide ? .bottom : .top,
                ) {
                    model.press(.first, touchedAgo: $0)
                }
                .equatable()
                ControlButton(status: clock.status) { model.toggle() }
                    .rotationEffect(seating.controlRotation)
                PlayerFace(
                    face: clock.face(of: .second), player: .second, style: faceStyle,
                    rotation: seating.rotation(of: .second), inset: .bottom,
                ) {
                    model.press(.second, touchedAgo: $0)
                }
                .equatable()
            }
        }
        .onGeometryChange(for: Bool.self) { $0.size.width > $0.size.height } action: { wide = $0 }
        // Both stay in the corners at all times, clear of the faces drawn beside them. Opening
        // settings pauses the game.
        .overlay(alignment: .bottomTrailing) {
            Button("Settings", systemImage: "gearshape.fill") {
                model.pause()
                showingSettings = true
            }
            .labelStyle(.iconOnly)
            .cornerButton(palette)
        }
        // The moves played. Only a label: a touch on it does nothing, rather than reaching the face
        // below and ending the turn.
        .overlay(alignment: .bottomLeading) {
            Text("\(clock.moves)")
                .monospacedDigit()
                .contentTransition(.numericText(value: Double(clock.moves)))
                .animation(.snappy, value: clock.moves)
                .cornerButton(palette)
                .accessibilityLabel(clock.moves == 1 ? "1 move played" : "\(clock.moves) moves played")
        }
        .background(palette.background)
        .sheet(isPresented: $showingSettings) {
            SettingsView(settings: $settings.settings, wide: wide)
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

private extension View {
    /// The circles in the bottom corners, one size so the two match.
    func cornerButton(_ palette: Palette) -> some View {
        font(.subheadline.weight(.semibold))
            .foregroundStyle(palette.onInactive)
            // A long game's three digits shrink to fit rather than crowding the circle's edge.
            .lineLimit(1)
            .minimumScaleFactor(0.7)
            .padding(.horizontal, 4)
            .frame(width: 40, height: 40)
            .background(palette.inactive, in: .circle)
            // Taps land in 44 points around it, the smallest target the HIG allows, while it looks smaller.
            .padding(2)
            .contentShape(.rect)
            .padding(14)
    }
}

/// What one face shows. From tick to tick it stays equal for the face that waits, so only the face
/// whose time runs is drawn again.
nonisolated private struct FaceState: Equatable {
    var time: PlayerTime
    var role: FaceRole
    /// Whether the clock runs, so the ring follows each tick smoothly.
    var running: Bool
    /// With a simple delay, the whole seconds the player on move can still think; nil otherwise.
    var delaySeconds: Int64?
}

nonisolated private enum FaceRole { case waiting, onMove, lowOnTime, won, lost }

private extension ChessClock {
    func face(of player: Player) -> FaceState {
        let time = time(of: player)
        let role: FaceRole =
            if status == .finished { time.isExpired ? .lost : .won }
            else if isLowOnTime(player) { .lowOnTime }
            // While paused too, so it's clear whose move it is when play resumes.
            else if turn == player { .onMove }
            else { .waiting }
        let delay = turn == player && delayRemaining > .zero ? delayRemaining.wholeSecondsRoundedUp : nil
        return FaceState(time: time, role: role, running: status == .running, delaySeconds: delay)
    }
}

private struct PlayerFace: View, Equatable {
    let face: FaceState
    let player: Player
    let style: FaceStyle
    /// Turns the face to where its player sits.
    let rotation: Angle
    /// The edge where room is left beside the drawn face, which the touch area still covers.
    let inset: Edge.Set
    /// The player touched the face, and how long ago the screen sensed the touch.
    let onPress: (Duration) -> Void
    @State private var pressed = false
    @Environment(\.palette) private var palette

    /// The press action is left out: it is the same for a face's whole life.
    nonisolated static func == (lhs: PlayerFace, rhs: PlayerFace) -> Bool {
        lhs.face == rhs.face && lhs.player == rhs.player && lhs.style == rhs.style
            && lhs.rotation == rhs.rotation && lhs.inset == rhs.inset
    }

    var body: some View {
        let time = face.time

        // The whole half of the screen is the player's button, like a physical clock.
        drawnFace(time: time)
            .frame(maxWidth: faceSize, maxHeight: faceSize)
            .aspectRatio(1, contentMode: .fit)
            .padding(16)
            .rotationEffect(rotation)
            // The face gives a little under the finger while it is down.
            .animation(.spring(duration: 0.2)) { $0.scaleEffect(pressed ? 0.96 : 1) }
            .padding(inset, cornerButtonStrip)
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
            .accessibilityValue(accessibilityState(time: time))
            .accessibilityHint("End turn")
            .accessibilityIdentifier(player == .first ? "face.first" : "face.second")
            .accessibilityAction(.default) { onPress(.zero) }
    }

    /// The square both face styles draw in. Rings and panels share its size and content, so they
    /// differ only in shape: an outline that empties or a filled block.
    private func drawnFace(time: PlayerTime) -> some View {
        let role = face.role
        let (color, onColor) = colors(role)
        let progress = role == .won || role == .lost ? 1 : time.progress
        // Smooth between ticks while running; otherwise a reset or a new time control sweeps the
        // ring back to full instead of jumping.
        let sweep: Animation = face.running ? .linear(duration: 0.1) : .easeInOut(duration: 0.5)
        return ZStack {
            switch style {
            case .rings:
                Circle()
                    .inset(by: progressStroke / 2)
                    .trim(from: 0, to: progress)
                    .stroke(color, style: StrokeStyle(lineWidth: progressStroke, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(sweep, value: progress)
                content(time: time, color: palette.ringText)
            case .panels:
                RoundedRectangle(cornerRadius: panelCorner, style: .continuous).fill(color)
                content(time: time, color: onColor)
                // The remaining share of the player's time, as a bar along the panel's bottom edge.
                ProgressBar(progress: progress, color: onColor)
                    .animation(sweep, value: progress)
                    .padding(.horizontal, panelCorner)
                    .padding(.bottom, 24)
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
                .padding(.horizontal, progressStroke + 24)
            if let delayCaption {
                Text(delayCaption)
                    .font(.headline)
                    .frame(maxHeight: .infinity, alignment: .bottom)
                    .padding(.bottom, progressStroke + 40)
            }
        }
        .foregroundStyle(color)
    }

    /// Whose move it is or that time ran out, and any delay countdown, as VoiceOver reads them.
    private func accessibilityState(time: PlayerTime) -> String {
        var parts: [String] = []
        switch face.role {
        case .lost: parts.append("Out of time")
        case .onMove, .lowOnTime: parts.append("On move")
        case .waiting, .won: break
        }
        if let delay = delayCaption { parts.append(delay) }
        return parts.joined(separator: ", ")
    }

    /// With a simple delay, the seconds the player on move can still think before their time runs.
    private var delayCaption: String? { face.delaySeconds.map { "Delay \($0) s" } }

    /// The face color, and the color of a panel's content drawn on it.
    private func colors(_ role: FaceRole) -> (Color, Color) {
        switch role {
        case .waiting: (palette.inactive, palette.onInactive)
        case .onMove: (palette.primary, palette.onPrimary)
        case .lowOnTime, .lost: (palette.loss, palette.onLoss)
        case .won: (palette.win, palette.onWin)
        }
    }
}

/// A track with the remaining share of time filled in: the ring's stroke laid straight.
private struct ProgressBar: View {
    let progress: Double
    let color: Color

    var body: some View {
        Capsule()
            .fill(color.opacity(0.25))
            .overlay(alignment: .leading) {
                GeometryReader { geometry in
                    // Never narrower than its height, so a nearly empty bar shrinks to a dot instead of
                    // a sliver.
                    let width = max(geometry.size.width * min(progress, 1), progress > 0 ? geometry.size.height : 0)
                    Capsule().fill(color).frame(width: width)
                }
            }
            .frame(height: progressStroke)
    }
}

/// The thickness of a ring and of a panel's bar alike, so the two styles show time the same way.
private let progressStroke: CGFloat = 16

/// Large enough for big digits, small enough that a ring stays a ring on an iPad.
private let faceSize: CGFloat = 340

private let panelCorner: CGFloat = 28

/// What a face leaves for the corner buttons, beyond its own padding.
private let cornerButtonStrip: CGFloat = 40

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
            // A symbol reads the same from either side of the board, where a word is upside down for one.
            Image(systemName: symbol)
                .font(.system(size: 25, weight: .semibold))
                // Clear to hit, yet well short of the faces, so it doesn't draw the eye from them and a
                // hard tap near the middle ends the turn rather than pausing.
                .frame(width: 72, height: 72)
                .contentTransition(.symbolEffect(.replace))
                .animation(.easeInOut(duration: colorFade), value: status)
        }
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.circle)
        // Tonal, in the corner buttons' color: the face on move stays the one strong color.
        .tint(palette.inactive)
        .foregroundStyle(palette.onInactive)
        .accessibilityLabel(label)
    }

    private var symbol: String {
        switch status {
        case .notStarted: "play.fill"
        case .running: "pause.fill"
        case .paused, .finished: "arrow.counterclockwise"
        }
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
