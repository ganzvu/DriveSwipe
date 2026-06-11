# 🚗 DriveSwipe — Touchless Gesture Control for Every Car

> **Transform your daily drive with futuristic, touchless hand-gesture controls. Bring the premium, high-end gesture control of luxury vehicles to any car today.**

---

## 🌟The Future of In-Car Media

Imagine skipping tracks, pausing your music, or adjusting the volume without ever taking your eyes off the road or fumbling for tiny touch screens. **DriveSwipe** turns your Android device into a smart, gesture-controlled dashboard co-pilot. Powered by advanced real-time hand tracking, DriveSwipe listens to your gestures and maps them instantly to your music player. 

Whether your car is a vintage classic or a daily commuter, DriveSwipe brings the next generation of smart cabin convenience to your fingertips.

---

## ✨ Standout Features

### 🛸 Interactive Glassmorphic HUD Pill
Keep your navigation or map open while you drive. DriveSwipe floats a tiny, elegant status indicator over any active app. The moment you trigger a gesture, the dot smoothly morphs into an expanded capsule display, giving you clean, visual confirmation of the media action before sliding back down. Best of all? You can drag and position it anywhere on your screen.

### ⚡ Breathing Engine Dashboard
The home screen of the app features a sleek, automotive-inspired Start/Stop engine button. It pulses with a breathing radial glow and a clean power symbol, letting you know the status of your co-pilot at a single glance.

### 🤖 Smart Autostart (Samsung Routines Integration)
Set and forget. DriveSwipe integrates with your car’s Bluetooth and automation triggers like Samsung Routines. The app starts and stops listening in the background silently, without screen flickers or annoying interruptions. 

### 🌓 Day & Night Intelligence
Driving at night? DriveSwipe automatically switches sensors. During the day, it uses your front camera for high-accuracy gesture tracking. At night, it switches to a proximity-based waving mode, saving battery and preventing distracting light from the screen.

### ⚙️ Customizable Control Presets
You are in the driver's seat. Use pre-configured standard presets, or remap actions (Next, Previous, Play/Pause, Volume Up/Down) to whichever hand gestures feel most natural to you.

---

## 🛠️ The Gesture Control Set

DriveSwipe responds to intuitive, easy-to-learn gestures:

*   **Pinch & Slide Left** ➔ Skip to the next song ⏭️
*   **Pinch & Slide Right** ➔ Rewind to the previous song ⏮️
*   **Two-Finger Point** ➔ Play / Pause media ⏯️
*   **Thumbs Up** ➔ Turn the volume up 🔊 (automatically ticks up)
*   **Thumbs Down** ➔ Turn the volume down m 🔉 (automatically ticks down)
*   **Double Wave (Night Mode)** ➔ Trigger your customized action in the dark! 🌌

---

## 🚀 Getting Started in 3 Steps

1.  **Grant Permissions:** Open the app and follow the interactive **Setup Wizard** to enable camera and notification access.
2.  **Mount Your Phone:** Secure your phone in a dashboard or air-vent mount facing you.
3.  **Start the Engine:** Tap the pulsing Start/Stop dashboard button, open your favorite music app (Spotify, YouTube Music, Apple Music, etc.), and drive!

---

## 💻 Technical Details (For Developers)

If you'd like to build the project yourself or contribute:

*   **Platform:** Android (Min SDK 26, Target SDK 34)
*   **Technologies:** Kotlin, Jetpack Compose, Material 3, CameraX, MediaPipe Tasks Vision.

### Quick Build Commands
-   **Debug Build:** `./gradlew :app:assembleDebug` ➔ Generates `app/build/outputs/apk/debug/app-debug.apk`
-   **Release Build:** Configure `keystore.properties` (see `keystore.properties.example` for details) and run `./gradlew :app:assembleRelease`
