# ChessClock

A two-player chess clock for Android. Tap your half of the screen to end your turn and start your opponent's clock.

- Time control from seconds up to 12 hours, with an optional per-move increment
- Four orientations, for players sitting side by side or across the table
- Pauses automatically when the app goes to the background

## Tech stack

Kotlin 2.4 · Jetpack Compose (Material 3) · ViewModel + StateFlow · Navigation 3 · DataStore · AGP 9.4 · min SDK 28, target SDK 37

## Project layout

```
app/src/main/java/com/lab/chessclock/
  clock/       Chess clock rules: immutable, plain Kotlin
  settings/    ClockSettings and the DataStore-backed SettingsRepository
  ui/clock/    Clock screen and ClockViewModel
  ui/settings/ Settings screen and SettingsViewModel
  ui/theme/    Colors and Material theme
```

Dependencies are wired by hand in `AppContainer`. Versions live in `gradle/libs.versions.toml`.

## Build and run

Requires JDK 17+ (Android Studio's bundled JDK works) and the Android SDK with platform 37.

```sh
./gradlew installDebug              # build and install on a running emulator/device
./gradlew testDebugUnitTest         # unit tests, no device needed
./gradlew connectedDebugAndroidTest # Compose UI tests on an emulator/device
./gradlew assembleRelease           # R8-shrunk release APK (unsigned)
```

## Docs

[`docs/android-toolchain-map.html`](docs/android-toolchain-map.html) explains how the JDK, Gradle, AGP, Kotlin, the SDK and the emulator fit together.
