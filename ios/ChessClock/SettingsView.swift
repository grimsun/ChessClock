import ClockCore
import SwiftUI

struct SettingsView: View {
    @Binding var settings: ClockSettings
    @Environment(\.dismiss) private var dismiss

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
                        Text("Upright").tag(Seating.upright)
                        Text("Facing each other at the ends").tag(Seating.facing)
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }
            }
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

    var body: some View {
        // The current control filled in, the others outlined.
        Group {
            if selected {
                button.buttonStyle(.borderedProminent).tint(.onMove).foregroundStyle(Color.onOnMove)
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
