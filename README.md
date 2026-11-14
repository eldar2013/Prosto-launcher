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
- Home-screen folders: create by dragging one app onto another, add apps by dragging them onto a folder, and persist folder contents.
- **Open folder card:** tapping a folder opens a light, semi-transparent rounded card in the center of the home screen (3-column icon grid with app names, folder name at the bottom, soft shadow). The wallpaper stays visible behind it. It opens like on iPhone: the card springs out of the folder icon while the home screen behind it shrinks slightly, dims and blurs (blur needs Android 12+; on older versions only the dimming is shown). Closing shrinks the card back into the icon. It also closes when you tap outside it or press Back.
- **Folder rename:** long-press a folder on the home screen -> **Переименовать** (or tap the folder name at the bottom of the open card). The name is saved and shown on the home screen and inside the folder.
- Folder actions: long-press a folder -> Открыть / Переименовать / Удалить папку. Inside an open folder: **+ Добавить** adds apps, long-press an app -> Убрать из папки / Переместить в другую папку.
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
- Folder names are stored in SharedPreferences (`folder_names`) next to the existing `folder_contents`; old saved data keeps working.
- `assembleDebug` was **not** run for this version: the environment where the changes were made has no Android SDK / Gradle and no access to Google's Maven repository. The Kotlin file was only checked with the Kotlin 2.0.21 compiler for syntax errors (none found) and for balanced brackets. Please build in Android Studio, or push to GitHub and download the APK from the Actions run (see `.github/workflows/android.yml`).
