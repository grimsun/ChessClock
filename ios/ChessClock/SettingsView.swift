import ClockCore
import SwiftUI

struct SettingsView: View {
    @Binding var settings: ClockSettings
    @Environment(\.dismiss) private var dismiss
    @Environment(\.palette) private var palette

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    // One tap for the usual time controls, with the steppers right below for anything else.
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 80))], spacing: 8) {
                        ForEach(TimePreset.all, id: \.self) { preset in
                            PresetButton(preset: preset, selected: preset.matches(settings)) {
                                settings = preset.applied(to: settings)
                            }
                        }
                    }
                    .padding(.vertical, 4)
                    Stepper("Hours: \(settings.hours)", value: $settings.hours, in: 0...ClockSettings.maxHours)
                    Stepper("Minutes: \(settings.minutes)", value: $settings.minutes, in: 0...ClockSettings.maxMinutes)
                    Stepper("Seconds: \(settings.seconds)", value: $settings.seconds, in: 0...ClockSettings.maxSeconds)
                } header: {
                    Text("Time control")
                } footer: {
                    Text("Minutes each, plus seconds per move. Pick one or set your own.")
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
        // "3 + 2" read aloud means little: spell it out for VoiceOver.
        .accessibilityLabel("\(preset.minutes) minutes, plus \(preset.incrementSeconds) seconds per move")
        .accessibilityAddTraits(selected ? .isSelected : [])
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

#Preview {
    @Previewable @State var settings = ClockSettings()
    SettingsView(settings: $settings)
}
