# AGENTS.md

## Cursor Cloud specific instructions

DriveSwipe is a **single-module Android app** (Kotlin, Jetpack Compose, CameraX, MediaPipe). There is no backend, Docker stack, or `package.json`. All development happens through **Gradle** and **adb** against a device or emulator.

### One-time VM prerequisites (not in the update script)

These are installed once on the Cloud VM image (or manually if missing):

- **JDK 21** at `/usr/lib/jvm/java-21-openjdk-amd64`
- **Android SDK** at `/home/ubuntu/Android/Sdk` with platform 34, build-tools, platform-tools, emulator, and a Google APIs x86_64 system image
- **`ANDROID_HOME` / `ANDROID_SDK_ROOT`** and `PATH` entries in `~/.bashrc` (see below)

### `gradle.properties` Java home (Linux)

`gradle.properties` pins `org.gradle.java.home` to a **Windows** Android Studio JBR path. On Linux, **always** pass:

`-Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64`

on every `./gradlew` invocation (or remove that line locally—do not rely on the committed value in this environment).

### Standard commands

| Task | Command |
|------|---------|
| Unit tests | `./gradlew -Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64 :app:test` |
| Debug APK | `./gradlew -Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64 :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` |
| Lint | `./gradlew -Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64 :app:lintDebug` |
| Install on device | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| Launch UI | `adb shell am start -n com.example.driveswipe/.MainActivity` |

### Emulator (full UI / gesture E2E)

Software emulation is slow without KVM; expect long boots and occasional **System UI ANR** dialogs on the emulator—tap **Wait** and retry.

```bash
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"
emulator -avd driveswipe_api34 -no-audio -gpu swiftshader_indirect -accel off
# Wait until: adb shell getprop sys.boot_completed == 1
```

Grant permissions for automated runs:

```bash
adb shell pm grant com.example.driveswipe android.permission.CAMERA
adb shell pm grant com.example.driveswipe android.permission.POST_NOTIFICATIONS
```

Starting gesture control from Home runs **`GestureService`** as a foreground service (notification channel `GestureServiceChannel`). Verify with `adb shell dumpsys activity services | grep GestureService`.

### Known repo issues (as of setup)

- **`MainUiStateTest`** references `hasNotificationListenerAccess`, which is not on `MainUiState` in `MainViewModel.kt`, so `:app:test` fails at compile time until tests are updated.
- **Lint** may fail on `MainActivity.kt` (`UnspecifiedRegisterReceiverFlag` for `serviceEventReceiver`).

### Services summary

| Component | Required for dev | How to run |
|-----------|------------------|------------|
| Gradle build | Yes | `./gradlew` (with Java home override above) |
| Android device/emulator | For UI/gesture E2E | `emulator` + `adb` |
| `GestureService` | For gesture pipeline E2E | Start from in-app **START** on Home |

No TCP ports or web dev servers exist in this repository.
