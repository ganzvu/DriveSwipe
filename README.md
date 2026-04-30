# DriveSwipe

DriveSwipe is an Android app for touchless in-car media control using camera-based hand gestures.
It uses CameraX + MediaPipe for live gesture recognition and maps recognized gestures to media actions (track, play/pause, volume).

## Features

- Compose multi-screen UX (`Home`, `Setup`, `Gestures`, `Modes`, `History`)
- Foreground gesture service for reliable in-drive operation
- Day/Night operating modes (camera + proximity flow)
- Gesture mapping and tuning controls in-app
- DataStore-backed persistent settings
- Runtime gesture history feed for quick validation
- Branded launcher icon and signed release build support

## Gesture Set

- `Pinch_Drag_Right` -> mapped action (default `NEXT_TRACK`)
- `Pinch_Drag_Left` -> mapped action (default `PREVIOUS_TRACK`)
- `Two_Finger_Point` -> mapped action (default `PLAY_PAUSE`)
- `Thumb_Up` -> mapped action (default `VOLUME_UP`, repeat by tick interval)
- `Thumb_Down` -> mapped action (default `VOLUME_DOWN`, repeat by tick interval)

## Stack

- Kotlin
- Jetpack Compose + Material 3
- AndroidX Navigation
- CameraX
- MediaPipe Tasks Vision (`com.google.mediapipe:tasks-vision`)
- Coroutines
- DataStore Preferences

## Requirements

- Android Studio (latest stable recommended)
- Android SDK 34
- Min SDK 26
- Front camera device (proximity sensor optional but recommended)

## Build

### Debug

```bash
./gradlew.bat :app:assembleDebug
```

Output:
- `app/build/outputs/apk/debug/app-debug.apk`

### Release

```bash
./gradlew.bat :app:assembleRelease
```

Output:
- `app/build/outputs/apk/release/DriveSwipe-v1.0.apk`

## Release Signing Setup

### Local builds

This project reads signing config from `keystore.properties` (gitignored).

1. Copy the template:
   - `keystore.properties.example` → `keystore.properties`
2. Fill in your real values (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`)
3. Optionally use the helper script to create a new keystore:
   - `.\scripts\generate-keystore.ps1`
4. Build the signed release APK:
   - `./gradlew.bat :app:assembleRelease`

> **Note:** A release build without signing credentials will now fail with a clear
> error rather than silently producing an unsigned (uninstallable) APK.

### CI / GitHub Actions

Releases are signed automatically when a `v*` tag is pushed.  The workflow
(`.github/workflows/release.yml`) reads four repository secrets:

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Base-64 encoded `.jks` keystore file — Linux: `base64 -w 0 release-keystore.jks`, macOS: `base64 -i release-keystore.jks` |
| `KEYSTORE_STORE_PASSWORD` | Keystore store password |
| `KEYSTORE_KEY_ALIAS` | Key alias inside the keystore |
| `KEYSTORE_KEY_PASSWORD` | Key password |

Set these under **Settings → Secrets and variables → Actions** in the repository.
The signed APK is automatically attached to the GitHub Release that is created for
the tag.


## Repository Layout

- `app/src/main/java/com/example/driveswipe/MainActivity.kt` - app host + permission/service orchestration
- `app/src/main/java/com/example/driveswipe/DriveSwipeApp.kt` - app UI shell and screens
- `app/src/main/java/com/example/driveswipe/GestureService.kt` - foreground service and media action dispatch
- `app/src/main/java/com/example/driveswipe/GestureRecognizerHelper.kt` - inference pipeline and pinch/gesture logic
- `app/src/main/java/com/example/driveswipe/SettingsRepository.kt` - DataStore persistence layer
- `app/src/main/java/com/example/driveswipe/DriveSwipeModels.kt` - shared settings/action models

## Notes

- Camera and notification listener permissions are required for full functionality.
- There are known non-blocking build warnings around `android:extractNativeLibs` and deprecated CameraX analyzer sizing API.
