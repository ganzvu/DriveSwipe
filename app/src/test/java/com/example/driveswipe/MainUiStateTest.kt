package com.example.driveswipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainUiStateTest {

    // ── isDriveReady ─────────────────────────────────────────────────────────

    @Test
    fun isDriveReadyTrueWhenAllPermissionsGranted() {
        val state = MainUiState(
            hasCameraPermission = true,
            hasNotificationsPermission = true
        )
        assertTrue(state.isDriveReady)
    }

    @Test
    fun isDriveReadyFalseWhenCameraPermissionMissing() {
        val state = MainUiState(
            hasCameraPermission = false,
            hasNotificationsPermission = true
        )
        assertFalse(state.isDriveReady)
    }

    @Test
    fun isDriveReadyFalseWhenNotificationsPermissionMissing() {
        val state = MainUiState(
            hasCameraPermission = true,
            hasNotificationsPermission = false
        )
        assertFalse(state.isDriveReady)
    }

    @Test
    fun isDriveReadyFalseWhenAllPermissionsMissing() {
        val state = MainUiState(
            hasCameraPermission = false,
            hasNotificationsPermission = false
        )
        assertFalse(state.isDriveReady)
    }

    @Test
    fun isDriveReadyFalseWhenOnlyCameraPermissionGranted() {
        val state = MainUiState(
            hasCameraPermission = true,
            hasNotificationsPermission = false
        )
        assertFalse(state.isDriveReady)
    }

    // ── Default state ────────────────────────────────────────────────────────

    @Test
    fun defaultStateHasNotificationsPermissionButCameraMissingAndServiceNotRunning() {
        val state = MainUiState()
        assertFalse(state.hasCameraPermission)
        assertTrue(state.hasNotificationsPermission)
        assertFalse(state.isServiceRunning)
        assertFalse(state.isDriveReady)
    }

    @Test
    fun defaultStateHasEmptyGestureHistory() {
        val state = MainUiState()
        assertTrue(state.gestureHistory.isEmpty())
    }

    @Test
    fun defaultStateHasDefaultAppSettings() {
        val state = MainUiState()
        assertFalse(state.settings.isNightMode)
        assertEquals(GesturePreset.STANDARD, state.settings.selectedPreset)
    }
}
