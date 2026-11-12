# Prosto Launcher — v0.2.0

Pixel / Material You style Android launcher built with Kotlin + Jetpack Compose.

## Included
- HOME/DEFAULT launcher intent filters.
- System-aware light/dark appearance.
- Android 12+ dynamic Material You color schemes.
- Large clock and localized date.
- Simple search entry point that opens the app drawer.
- Swipe up to open the app drawer; swipe down or tap Done to close.
- Installed-app discovery, filtering, icons, and launching.
- Best-effort dock favorites based on common Google/AOSP package names, with fallback to first four apps.

## Build
Open in Android Studio, sync Gradle, then run `app` on an Android device or emulator.
Select Prosto Launcher as the default Home app in Android settings.

## Compatibility
minSdk 26 (Android 8.0). Dynamic wallpaper-derived color schemes are available on Android 12+;
older versions fall back to Material 3 light/dark palettes. Device-specific behavior still needs testing.
