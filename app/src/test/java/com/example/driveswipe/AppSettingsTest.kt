package com.example.driveswipe

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun defaultMappingsMatchExpectedMediaActions() {
        val mappings = AppSettings().mappings
        assertEquals(DriveAction.NEXT_TRACK, mappings.pinchDragRight)
        assertEquals(DriveAction.PREVIOUS_TRACK, mappings.pinchDragLeft)
        assertEquals(DriveAction.PLAY_PAUSE, mappings.twoFingerPoint)
        assertEquals(DriveAction.VOLUME_UP, mappings.volumeUp)
        assertEquals(DriveAction.VOLUME_DOWN, mappings.volumeDown)
    }

    @Test
    fun defaultTuningMatchesGestureServiceBehavior() {
        val tuning = AppSettings().tuning
        assertEquals(1500L, tuning.actionCooldownMs)
        assertEquals(500L, tuning.volumeTickMs)
        assertEquals(0.08f, tuning.pinchThreshold)
        assertEquals(0.15f, tuning.pinchReleaseThreshold)
        assertEquals(0.15f, tuning.swipeThreshold)
        assertEquals(1500L, tuning.swipeTimeoutMs)
    }
}
