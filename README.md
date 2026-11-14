# Prosto Launcher

Android launcher built with Kotlin and Jetpack Compose.

## What is included

- Purple/lavender palette and **System / Light / Dark** theme choices.
- Bundled wallpaper, gallery wallpaper picker, and reset-to-default action.
- Home screen clock and date.
- Four-position app dock with the app-drawer button in the center.
- App drawer with search and long-press actions.
- App details and Android system uninstall intent (the system handles confirmation).
- Separate **Remove from home screen** action; it removes launcher placement, not the installed app.
- Home-screen folders: create by dragging one app onto another, open/close, add/remove apps, and persist folder contents.
- Persisted workspace and dock positions.
- Full-screen **Home screen settings** with theme and icon settings.
- Icon masks: Circle, Square, Triangle, 12-sided cookie, Soft burst, and 4-leaf clover. Compose clips the app icons to the selected mask; some adaptive icons may still have their own internal padding.
- Folder background opacity slider with immediate preview and persistence.

## Replace / install the project

1. Unzip the supplied project archive.
2. To replace an existing checkout, back up your current project first, then copy the files and folders from this ZIP over it. Keep the same project root (the folder containing `settings.gradle.kts`).
3. Open that root folder in Android Studio and allow Gradle sync to complete.
4. Select the Android device/emulator and run the `app` configuration.

## Build a debug APK

The project uses Android Gradle Plugin **8.7.3**, Kotlin **2.0.21**, Java **17**, compile SDK **35**, and minimum SDK **26**.

If Gradle and Android SDK are configured on your computer, run from the project root:

```bash
gradle assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

This archive does not include a Gradle wrapper. The repository's GitHub Actions workflow (`.github/workflows/android.yml`) can also build a debug APK after Actions is enabled for the repository.

## Notes

- The Android system controls app uninstall UI and confirmation; the launcher cannot bypass the OS.
- Folder metadata and workspace layout are saved in SharedPreferences.
- Changes are implemented in `app/src/main/java/com/prosto/launcher/MainActivity.kt`; launcher icon and wallpaper resources and the existing Gradle/Manifest configuration are retained.
- A local `assembleDebug` run could not be performed in the provided environment because Gradle and the Android SDK are not installed and external downloads are unavailable. Please build in Android Studio or CI before installing the APK.
