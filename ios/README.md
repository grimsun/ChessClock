# ChessClock for iPhone

A SwiftUI sketch of the Android app in [`../android`](../android).

```
ClockCore/          Swift package: the clock rules, ported from the Android app's `clock` package
ChessClock/         SwiftUI app: clock screen, settings sheet, ClockModel that runs the clock
ChessClock.xcodeproj
```

Open `ChessClock.xcodeproj` in Xcode 27 and run the ChessClock scheme, or build from the shell:

```sh
xcodebuild -project ios/ChessClock.xcodeproj -scheme ChessClock \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

```sh
cd ios/ClockCore && swift test   # rules tests, no simulator needed
```

## Compared with Android

Done: ring faces, Start/Pause/Reset, presets and custom time, Fischer/Bronstein/delay with the delay
counted down, upright or facing seats, low-time red, light and dark Walnut colors, settings saved
in UserDefaults.

Not yet: panel faces, other palettes, theme choice, sideways seating, a UI test target, an app icon.
