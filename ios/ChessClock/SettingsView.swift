import ClockCore
import SwiftUI

struct SettingsView: View {
    @Binding var settings: ClockSettings
    /// Whether the clock lays its faces side by side, which names each player's time by its half.
    var wide = false
    @Environment(\.dismiss) private var dismiss
    @Environment(\.palette) private var palette

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    // One tap for the usual time controls, with the steppers right below for anything else.
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 80))], spacing: 8) {
                        ForEach(TimePreset.all, id: \.self) { preset in
                            PresetButton(preset: preset, mode: settings.incrementMode, selected: preset.matches(settings)) {
                                settings = preset.applied(to: settings)
                            }
                        }
                    }
                    .padding(.vertical, 4)
                    // Named by where each half of the screen is: the first player's is on top, or on the
                    // left when the faces are side by side.
                    TimeStepper(settings.secondTime == nil ? "Time" : wide ? "Left player" : "Top player", time: $settings.time)
                    if let secondTime = settings.secondTime {
                        TimeStepper(wide ? "Right player" : "Bottom player", time: Binding(
                            get: { secondTime },
                            set: { settings.secondTime = $0 },
                        ))
                    }
                    Toggle(isOn: separateTimes) {
                        Text("Different times")
                        Text("For time odds: the stronger player can start with less")
                    }
                } header: {
                    Text("Time control")
                } footer: {
                    Text("Minutes each, plus seconds per move. Pick one or set your own.")
                }

                ForEach(settings.stages.indices, id: \.self) { index in
                    let stage = stage(at: index)
                    Section("Period \(index + 2)") {
                        StepStepper(
                            "Starts after",
                            steps: ClockSettings.moveSteps,
                            value: stage.moves,
                            text: { $0 == 1 ? "1 move" : "\($0) moves" },
                        )
                        TimeStepper("Time added", time: stage.time)
                        Button("Remove Period \(index + 2)", role: .destructive) {
                            settings.stages.remove(at: index)
                        }
                    }
                }
                if settings.stages.count < ClockSettings.maxStages {
                    Section {
                        Button("Add Time After a Number of Moves", systemImage: "plus") { settings.addStage() }
                    } footer: {
                        Text("Each player gets the time once they have made the moves, as in classical chess.")
                    }
                }

                Section {
                    Picker("Seconds per move", selection: $settings.incrementMode) {
                        Text("Increment").tag(IncrementMode.fischer)
                        Text("Bronstein").tag(IncrementMode.bronstein)
                        Text("Delay").tag(IncrementMode.simpleDelay)
                    }
                    .pickerStyle(.segmented)
                    Stepper(
                        "\(settings.incrementMode == .simpleDelay ? "Delay" : "Increment"): \(settings.incrementSeconds) s",
                        value: $settings.incrementSeconds,
                        in: 0...ClockSettings.maxIncrementSeconds,
                    )
                } header: {
                    Text("Seconds per move")
                } footer: {
                    Text(settings.incrementMode.explanation + " Changing the time starts a new game.")
                }

                Section("Where players sit") {
                    Picker("Where players sit", selection: $settings.seating) {
                        ForEach(Seating.allCases, id: \.self) { seating in
                            VStack(alignment: .leading) {
                                Text(seating.label)
                                Text(seating.explanation).font(.footnote).foregroundStyle(.secondary)
                            }
                            .tag(seating)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }

                Section("Appearance") {
                    Picker("Clock face", selection: $settings.faceStyle) {
                        Text("Rings").tag(FaceStyle.rings)
                        Text("Panels").tag(FaceStyle.panels)
                    }
                    Picker("Theme", selection: $settings.theme) {
                        Text("System").tag(ThemeMode.system)
                        Text("Light").tag(ThemeMode.light)
                        Text("Dark").tag(ThemeMode.dark)
                    }
                    Picker("Colors", selection: $settings.palette) {
                        ForEach(ColorPalette.allCases, id: \.self) { choice in
                            HStack {
                                Text(choice.label)
                                Spacer()
                                PaletteSwatch(palette: .of(choice))
                            }
                            .tag(choice)
                        }
                    }
                    .pickerStyle(.inline)
                }

                Section("Vibration") {
                    Toggle(isOn: $settings.vibrateOnTurnEnd) {
                        Text("When a turn ends")
                        Text("A short click confirms your press without looking")
                    }
                    Toggle(isOn: $settings.vibrateOnLowTime) {
                        Text("When time runs low")
                        Text("Two pulses at 10 seconds left, a long buzz when time runs out")
                    }
                }
            }
            .tint(palette.primary)
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

private struct PresetButton: View {
    let preset: TimePreset
    let mode: IncrementMode
    let selected: Bool
    let action: () -> Void
    @Environment(\.palette) private var palette

    var body: some View {
        // The current control filled in, the others outlined.
        Group {
            if selected {
                button.buttonStyle(.borderedProminent).tint(palette.primary).foregroundStyle(palette.onPrimary)
            } else {
                button.buttonStyle(.bordered).tint(.primary)
            }
        }
        .accessibilityLabel(spokenLabel)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    /// "3 + 2" read aloud means little: spelled out, in the words of the increment mode.
    private var spokenLabel: String {
        let minutes = preset.minutes == 1 ? "1 minute" : "\(preset.minutes) minutes"
        let seconds = preset.incrementSeconds == 1 ? "1 second" : "\(preset.incrementSeconds) seconds"
        return switch mode {
        case .fischer: "\(minutes), plus \(seconds) per move"
        case .bronstein: "\(minutes), plus up to \(seconds) back per move"
        case .simpleDelay: "\(minutes), \(seconds) delay per move"
        }
    }

    private var button: some View {
        Button(action: action) {
            Text(preset.label)
                .monospacedDigit()
                .lineLimit(1)
                .fixedSize()
                .frame(maxWidth: .infinity)
        }
    }
}

/// The palette's on-move color beside its waiting color, in the current light or dark.
private struct PaletteSwatch: View {
    let palette: Palette

    var body: some View {
        Circle()
            .fill(palette.swatch.waiting)
            .overlay {
                Circle().fill(palette.swatch.active).mask(alignment: .leading) {
                    Rectangle().frame(width: 16)
                }
            }
            .overlay { Circle().stroke(.separator) }
            .frame(width: 32, height: 32)
            .accessibilityHidden(true)
    }
}

private extension ColorPalette {
    var label: String {
        switch self {
        case .walnut: "Walnut"
        case .forest: "Forest"
        case .ocean: "Ocean (color-blind friendly)"
        case .graphite: "Graphite (high contrast)"
        }
    }
}

private extension Seating {
    var label: String {
        switch self {
        case .upright: "Upright"
        case .facing: "Facing each other at the ends"
        case .sideways: "Sideways"
        case .facingSides: "Facing each other at the sides"
        }
    }

    var explanation: String {
        switch self {
        case .upright: "Both times read from the bottom of the screen"
        case .facing: "Players at the top and bottom, each time turned to its player"
        case .sideways: "Both times read from the same long side"
        case .facingSides: "Players at the left and right, each time turned to its player"
        }
    }
}

private extension IncrementMode {
    var explanation: String {
        switch self {
        case .fischer: "Fischer: added after every move, so time can grow."
        case .bronstein: "The time a move took is given back, up to these seconds, so time never grows."
        case .simpleDelay: "Simple delay: each move, the clock waits these seconds before counting down."
        }
    }
}

extension SettingsView {
    /// The period at `index`, read safely while a removed one's rows are still going away.
    private func stage(at index: Int) -> Binding<TimeStage> {
        Binding(
            get: { settings.stages.indices.contains(index) ? settings.stages[index] : TimeStage(moves: 1, time: .zero) },
            set: { if settings.stages.indices.contains(index) { settings.stages[index] = $0 } },
        )
    }

    /// Each player's own time starts out as the shared one, so turning it on changes nothing yet.
    private var separateTimes: Binding<Bool> {
        Binding(
            get: { settings.secondTime != nil },
            set: { settings.secondTime = $0 ? settings.secondTime ?? settings.time : nil },
        )
    }
}

/// A stepper through a time's steps, showing the time as "1 h 30 min".
private struct TimeStepper: View {
    let title: LocalizedStringKey
    @Binding var time: Duration

    init(_ title: LocalizedStringKey, time: Binding<Duration>) {
        self.title = title
        _time = time
    }

    var body: some View {
        StepStepper(title, steps: ClockSettings.timeSteps, value: $time, text: \.settingsText) {
            $0.formatted(.units(allowed: [.hours, .minutes, .seconds], width: .wide))
        }
    }
}

/// A stepper through a list of steps. A value between steps, such as one saved by an older version,
/// stays as it is until a step moves it to the next one either way.
private struct StepStepper<Value: Comparable>: View {
    let title: LocalizedStringKey
    let steps: [Value]
    @Binding var value: Value
    let text: (Value) -> String
    /// The value as VoiceOver reads it, when the shown text is abbreviated.
    var spoken: ((Value) -> String)?

    init(
        _ title: LocalizedStringKey,
        steps: [Value],
        value: Binding<Value>,
        text: @escaping (Value) -> String,
        spoken: ((Value) -> String)? = nil,
    ) {
        self.title = title
        self.steps = steps
        _value = value
        self.text = text
        self.spoken = spoken
    }

    var body: some View {
        // The shown value stays out of the stepper's label: VoiceOver reads the spoken one as its value.
        HStack {
            Text(title).accessibilityHidden(true)
            Spacer()
            Text(text(value))
                .monospacedDigit()
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            // A missing step leaves its button disabled at either end.
            let next = steps.first { $0 > value }
            let previous = steps.last { $0 < value }
            Stepper(
                title,
                onIncrement: next.map { next in { value = next } },
                onDecrement: previous.map { previous in { value = previous } },
            )
            .labelsHidden()
            .accessibilityValue((spoken ?? text)(value))
        }
    }
}

private extension Duration {
    /// "1 h 30 min", as a time is shown in settings.
    var settingsText: String {
        let total = components.seconds
        let parts = [(total / 3600, "h"), (total / 60 % 60, "min"), (total % 60, "s")]
            .filter { $0.0 > 0 }
            .map { "\($0.0) \($0.1)" }
        return parts.isEmpty ? "0 s" : parts.joined(separator: " ")
    }
}

#Preview {
    @Previewable @State var settings = ClockSettings()
    SettingsView(settings: $settings)
}
