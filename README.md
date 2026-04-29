# DriveSwipe

DriveSwipe is an Android app for touchless in-car media control using hand gestures.
It uses CameraX + MediaPipe to recognize gestures and map them to media actions such as next/previous track, play/pause, and volume.

## Highlights

- Jetpack Compose multi-screen UI (`Home`, `Setup`, `Gestures`, `Modes`, `History`)
- Foreground camera gesture service for day mode
- Proximity-sensor shortcut mode for low-light/night conditions
- Configurable gesture-to-action mapping
- Gesture tuning controls (cooldowns, thresholds, swipe timing)
- Settings persistence via DataStore
- Runtime gesture event feed shown in app history

## Current Gesture Controls

- `Pinch_Drag_Right` -> mapped action (default: next track)
- `Pinch_Drag_Left` -> mapped action (default: previous track)
- `Two_Finger_Point` -> mapped action (default: play/pause)
- `Thumb_Up` -> mapped action (default: volume up, repeat by tick interval)
- `Thumb_Down` -> mapped action (default: volume down, repeat by tick interval)

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- AndroidX Navigation
- CameraX
- Google MediaPipe Tasks Vision (`com.google.mediapipe:tasks-vision`)
- DataStore Preferences
- Coroutines

## Requirements

- Android Studio (latest stable recommended)
- Android SDK 34
- Min Android version: 26
- A device with front camera (and optional proximity sensor for night mode)

## Build and Run

### Debug build

```bash
./gradlew.bat :app:assembleDebug
```

### Release build

```bash
./gradlew.bat :app:assembleRelease
```

Generated APKs:

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Project Structure

- `app/src/main/java/com/example/driveswipe/MainActivity.kt`  
  Compose host + service start/stop + permission orchestration
- `app/src/main/java/com/example/driveswipe/DriveSwipeApp.kt`  
  App navigation and screens
- `app/src/main/java/com/example/driveswipe/GestureService.kt`  
  Foreground service, camera analyzer, media action dispatch
- `app/src/main/java/com/example/driveswipe/GestureRecognizerHelper.kt`  
  MediaPipe inference, gesture logic, pinch-drag tracking
- `app/src/main/java/com/example/driveswipe/SettingsRepository.kt`  
  DataStore-backed app settings persistence
- `app/src/main/java/com/example/driveswipe/DriveSwipeModels.kt`  
  Shared models (actions, mappings, tuning, settings)

## Notes

- Release signing is currently default project setup. Configure your own signing config for production distribution.
- Camera/notification listener permissions are required for full functionality.
