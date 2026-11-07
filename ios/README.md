# ChessClock for iPhone

The iPhone version of the Android app in [`../android`](../android), with the same features.

```
ClockCore/          Swift package: clock rules, settings and palettes, ported from the Android app
ChessClock/         SwiftUI app: clock screen, settings sheet, ClockModel that runs the clock, haptics
ChessClockUITests/  XCUITests that play the app on a simulator
ChessClock.xcodeproj
```

Open `ChessClock.xcodeproj` in Xcode 27 and run the ChessClock scheme, or build from the shell:

```sh
xcodebuild -project ios/ChessClock.xcodeproj -scheme ChessClock \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

```sh
cd ios/ClockCore && swift test   # rules, settings and palette contrast tests, no simulator needed
xcodebuild test -project ios/ChessClock.xcodeproj -scheme ChessClock \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro'   # UI tests
```

## Compared with Android

The two apps match feature for feature: presets and custom time, periods added after a number
of moves, a different time for each player, the move counter, Fischer, Bronstein and delay, all
four seats, rings and panels, the four palettes in light and dark, the theme choice, the
vibrations and their switches, low-time red, the animations, and turns that end on touch-down,
timed by the touch. Settings are saved as JSON in UserDefaults rather than in DataStore.

Platform conventions differ where they should: steppers instead of sliders, a settings sheet with
Done instead of a screen with Back.
