/// `H:MM:SS` from an hour up, `MM:SS` down to a minute, then `MM:SS.cc` for the final minute.
public func formatClockTime(_ time: Duration) -> String {
    let totalMilliseconds = time.components.seconds * 1000 + time.components.attoseconds / 1_000_000_000_000_000
    let hours = totalMilliseconds / 3_600_000
    let minutes = totalMilliseconds / 60_000 % 60
    let seconds = totalMilliseconds / 1000 % 60
    // Tenths of a second, shown in the hundredths position like the Android app.
    let hundredths = totalMilliseconds % 1000 / 100 * 10
    let minutesAndSeconds = "\(twoDigits(minutes)):\(twoDigits(seconds))"

    if hours >= 1 { return "\(hours):\(minutesAndSeconds)" }
    if time <= .seconds(60) { return "\(minutesAndSeconds).\(twoDigits(hundredths))" }
    return minutesAndSeconds
}

private func twoDigits(_ value: Int64) -> String { value < 10 ? "0\(value)" : "\(value)" }
