package com.example.driveswipe

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.driveswipe.ui.theme.DriveSwipeTheme

@Preview(showBackground = true)
@Composable
private fun DriveSwipeHomePreview() {
    DriveSwipeTheme {
        DriveSwipeApp(
            uiState = MainUiState(
                settings = AppSettings(),
                isServiceRunning = true,
                hasCameraPermission = true,
                hasNotificationsPermission = true,
                hasNotificationListenerAccess = true,
                gestureHistory = listOf(
                    GestureEvent("Pinch_Drag_Right", DriveAction.NEXT_TRACK),
                    GestureEvent("Two_Finger_Point", DriveAction.PLAY_PAUSE)
                )
            ),
            onToggleService = {},
            onNightModeChanged = {},
            onRetryPermissions = {},
            onOpenNotificationSettings = {},
            onPresetSelected = {},
            onMappingChanged = { _, _ -> },
            onTuningChanged = {},
            onResetTuning = {}
        )
    }
}
