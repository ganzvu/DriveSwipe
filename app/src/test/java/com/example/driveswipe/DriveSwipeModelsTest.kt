package com.example.driveswipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSwipeModelsTest {

    // ── DriveAction ──────────────────────────────────────────────────────────

    @Test
    fun driveActionEnumContainsAllExpectedValues() {
        val names = DriveAction.values().map { it.name }
        assertTrue(names.contains("NEXT_TRACK"))
        assertTrue(names.contains("PREVIOUS_TRACK"))
        assertTrue(names.contains("PLAY_PAUSE"))
        assertTrue(names.contains("VOLUME_UP"))
        assertTrue(names.contains("VOLUME_DOWN"))
        assertTrue(names.contains("NONE"))
        assertEquals(6, names.size)
    }

    @Test
    fun driveActionValueOfReturnsCorrectConstant() {
        assertEquals(DriveAction.NEXT_TRACK, DriveAction.valueOf("NEXT_TRACK"))
        assertEquals(DriveAction.NONE, DriveAction.valueOf("NONE"))
    }

    // ── GesturePreset ────────────────────────────────────────────────────────

    @Test
    fun gesturePresetEnumContainsAllExpectedValues() {
        val names = GesturePreset.values().map { it.name }
        assertTrue(names.contains("SIMPLE"))
        assertTrue(names.contains("STANDARD"))
        assertTrue(names.contains("CUSTOM"))
        assertEquals(3, names.size)
    }

    // ── GestureMappings ──────────────────────────────────────────────────────

    @Test
    fun gestureMappingsDefaultValuesAreCorrect() {
        val m = GestureMappings()
        assertEquals(DriveAction.NEXT_TRACK, m.pinchDragRight)
        assertEquals(DriveAction.PREVIOUS_TRACK, m.pinchDragLeft)
        assertEquals(DriveAction.PLAY_PAUSE, m.twoFingerPoint)
        assertEquals(DriveAction.VOLUME_UP, m.volumeUp)
        assertEquals(DriveAction.VOLUME_DOWN, m.volumeDown)
    }

    @Test
    fun gestureMappingsCopyUpdatesOnlyRequestedField() {
        val original = GestureMappings()
        val updated = original.copy(pinchDragRight = DriveAction.PLAY_PAUSE)
        assertEquals(DriveAction.PLAY_PAUSE, updated.pinchDragRight)
        // All other fields should remain at their defaults
        assertEquals(original.pinchDragLeft, updated.pinchDragLeft)
        assertEquals(original.twoFingerPoint, updated.twoFingerPoint)
        assertEquals(original.volumeUp, updated.volumeUp)
        assertEquals(original.volumeDown, updated.volumeDown)
    }

    @Test
    fun gestureMappingsCopyProducesNewInstance() {
        val original = GestureMappings()
        val copy = original.copy()
        assertNotSame(original, copy)
        assertEquals(original, copy)
    }

    // ── GestureTuning ────────────────────────────────────────────────────────

    @Test
    fun gestureTuningDefaultValuesAreCorrect() {
        val t = GestureTuning()
        assertEquals(1500L, t.actionCooldownMs)
        assertEquals(500L, t.volumeTickMs)
        assertEquals(0.08f, t.pinchThreshold)
        assertEquals(0.15f, t.pinchReleaseThreshold)
        assertEquals(0.15f, t.swipeThreshold)
        assertEquals(1500L, t.swipeTimeoutMs)
    }

    @Test
    fun gestureTuningCopyUpdatesOnlyRequestedField() {
        val original = GestureTuning()
        val updated = original.copy(actionCooldownMs = 3000L)
        assertEquals(3000L, updated.actionCooldownMs)
        assertEquals(original.volumeTickMs, updated.volumeTickMs)
        assertEquals(original.pinchThreshold, updated.pinchThreshold)
        assertEquals(original.pinchReleaseThreshold, updated.pinchReleaseThreshold)
        assertEquals(original.swipeThreshold, updated.swipeThreshold)
        assertEquals(original.swipeTimeoutMs, updated.swipeTimeoutMs)
    }

    @Test
    fun gestureTuningCopyProducesNewInstance() {
        val original = GestureTuning()
        val copy = original.copy()
        assertNotSame(original, copy)
        assertEquals(original, copy)
    }

    // ── AppSettings ──────────────────────────────────────────────────────────

    @Test
    fun appSettingsDefaultValuesAreCorrect() {
        val s = AppSettings()
        assertFalse(s.isNightMode)
        assertEquals(GesturePreset.STANDARD, s.selectedPreset)
        assertEquals(GestureMappings(), s.mappings)
        assertEquals(GestureTuning(), s.tuning)
    }

    @Test
    fun appSettingsCopyUpdatesIsNightMode() {
        val original = AppSettings()
        val updated = original.copy(isNightMode = true)
        assertTrue(updated.isNightMode)
        // Other fields unchanged
        assertEquals(original.selectedPreset, updated.selectedPreset)
        assertEquals(original.mappings, updated.mappings)
        assertEquals(original.tuning, updated.tuning)
    }

    @Test
    fun appSettingsCopyUpdatesSelectedPreset() {
        val original = AppSettings()
        val updated = original.copy(selectedPreset = GesturePreset.CUSTOM)
        assertEquals(GesturePreset.CUSTOM, updated.selectedPreset)
        assertEquals(original.isNightMode, updated.isNightMode)
    }

    @Test
    fun appSettingsCopyProducesNewInstance() {
        val original = AppSettings()
        val copy = original.copy()
        assertNotSame(original, copy)
        assertEquals(original, copy)
    }

    // ── GestureEvent ─────────────────────────────────────────────────────────

    @Test
    fun gestureEventStoresGestureNameAndAction() {
        val event = GestureEvent(gestureName = "Pinch_Drag_Right", action = DriveAction.NEXT_TRACK)
        assertEquals("Pinch_Drag_Right", event.gestureName)
        assertEquals(DriveAction.NEXT_TRACK, event.action)
    }

    @Test
    fun gestureEventTimestampIsPositive() {
        val event = GestureEvent(gestureName = "Two_Finger_Point", action = DriveAction.PLAY_PAUSE)
        assertTrue(event.timestampMs > 0)
    }

    @Test
    fun gestureEventCopyPreservesAllFields() {
        val original = GestureEvent(
            gestureName = "Volume_Up",
            action = DriveAction.VOLUME_UP,
            timestampMs = 12345L
        )
        val copy = original.copy()
        assertEquals(original.gestureName, copy.gestureName)
        assertEquals(original.action, copy.action)
        assertEquals(original.timestampMs, copy.timestampMs)
    }

    @Test
    fun gestureEventCopyCanOverrideAction() {
        val original = GestureEvent(gestureName = "Volume_Up", action = DriveAction.VOLUME_UP, timestampMs = 1000L)
        val remapped = original.copy(action = DriveAction.NEXT_TRACK)
        assertEquals(DriveAction.NEXT_TRACK, remapped.action)
        assertEquals(original.gestureName, remapped.gestureName)
        assertEquals(original.timestampMs, remapped.timestampMs)
    }
}
