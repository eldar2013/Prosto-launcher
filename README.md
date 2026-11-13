# Prosto Launcher

Android launcher built with Kotlin + Jetpack Compose.

## Included in this version

- Purple/lavender visual palette with Light, Dark and System theme choices.
- Bundled purple landscape wallpaper (`prosto_wallpaper.png`).
- Wallpaper selection from the device gallery and a reset-to-default option.
- Long-press the free workspace area to open launcher settings.
- Home screen app grid and a five-position dock: four app slots and the app-drawer button in the center.
- Animated app drawer with app search.
- Long-press an app in the drawer to add it to the home screen; existing shortcuts are not replaced.
- Long-press a home-screen or dock app to remove it.
- Adaptive launcher icon on Android 8.0+ with a legacy PNG fallback.

## Build

The GitHub Actions workflow builds a debug APK on every push and on manual workflow dispatch.

In GitHub, open **Actions → Build Prosto Launcher APK → latest run → Artifacts → Prosto-Launcher-APK**.
