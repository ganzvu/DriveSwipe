package com.example.driveswipe

enum class DriveAction {
    NEXT_TRACK,
    PREVIOUS_TRACK,
    PLAY_PAUSE,
    VOLUME_UP,
    VOLUME_DOWN,
    NONE
}

enum class GesturePreset {
    SIMPLE,
    STANDARD,
    CUSTOM
}

data class GestureMappings(
    val pinchDragRight: DriveAction = DriveAction.NEXT_TRACK,
    val pinchDragLeft: DriveAction = DriveAction.PREVIOUS_TRACK,
    val twoFingerPoint: DriveAction = DriveAction.PLAY_PAUSE,
    val volumeUp: DriveAction = DriveAction.VOLUME_UP,
    val volumeDown: DriveAction = DriveAction.VOLUME_DOWN
)

data class GestureTuning(
    val actionCooldownMs: Long = 1500L,
    val volumeTickMs: Long = 500L,
    val pinchThreshold: Float = 0.08f,
    val pinchReleaseThreshold: Float = 0.15f,
    val swipeThreshold: Float = 0.15f,
    val swipeTimeoutMs: Long = 1500L
)

data class AppSettings(
    val isNightMode: Boolean = false,
    val selectedPreset: GesturePreset = GesturePreset.STANDARD,
    val mappings: GestureMappings = GestureMappings(),
    val tuning: GestureTuning = GestureTuning()
)

data class GestureEvent(
    val gestureName: String,
    val action: DriveAction,
    val timestampMs: Long = System.currentTimeMillis()
)
