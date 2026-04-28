package com.example.driveswipe

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "driveswipe_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val nightMode = booleanPreferencesKey("night_mode")
        val preset = stringPreferencesKey("gesture_preset")
        val mapRight = stringPreferencesKey("map_pinch_right")
        val mapLeft = stringPreferencesKey("map_pinch_left")
        val mapTwoFinger = stringPreferencesKey("map_two_finger")
        val mapVolumeUp = stringPreferencesKey("map_volume_up")
        val mapVolumeDown = stringPreferencesKey("map_volume_down")
        val actionCooldownMs = longPreferencesKey("action_cooldown_ms")
        val volumeTickMs = longPreferencesKey("volume_tick_ms")
        val pinchThreshold = floatPreferencesKey("pinch_threshold")
        val pinchReleaseThreshold = floatPreferencesKey("pinch_release_threshold")
        val swipeThreshold = floatPreferencesKey("swipe_threshold")
        val swipeTimeoutMs = longPreferencesKey("swipe_timeout_ms")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isNightMode = prefs[Keys.nightMode] ?: false,
            selectedPreset = runCatching {
                GesturePreset.valueOf(prefs[Keys.preset] ?: GesturePreset.STANDARD.name)
            }.getOrDefault(GesturePreset.STANDARD),
            mappings = GestureMappings(
                pinchDragRight = actionFrom(prefs[Keys.mapRight], DriveAction.NEXT_TRACK),
                pinchDragLeft = actionFrom(prefs[Keys.mapLeft], DriveAction.PREVIOUS_TRACK),
                twoFingerPoint = actionFrom(prefs[Keys.mapTwoFinger], DriveAction.PLAY_PAUSE),
                volumeUp = actionFrom(prefs[Keys.mapVolumeUp], DriveAction.VOLUME_UP),
                volumeDown = actionFrom(prefs[Keys.mapVolumeDown], DriveAction.VOLUME_DOWN)
            ),
            tuning = GestureTuning(
                actionCooldownMs = prefs[Keys.actionCooldownMs] ?: 1500L,
                volumeTickMs = prefs[Keys.volumeTickMs] ?: 500L,
                pinchThreshold = prefs[Keys.pinchThreshold] ?: 0.08f,
                pinchReleaseThreshold = prefs[Keys.pinchReleaseThreshold] ?: 0.15f,
                swipeThreshold = prefs[Keys.swipeThreshold] ?: 0.15f,
                swipeTimeoutMs = prefs[Keys.swipeTimeoutMs] ?: 1500L
            )
        )
    }

    suspend fun setNightMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.nightMode] = enabled }
    }

    suspend fun setPreset(preset: GesturePreset) {
        context.dataStore.edit { it[Keys.preset] = preset.name }
    }

    suspend fun setMapping(gestureKey: String, action: DriveAction) {
        context.dataStore.edit { prefs ->
            when (gestureKey) {
                "Pinch_Drag_Right" -> prefs[Keys.mapRight] = action.name
                "Pinch_Drag_Left" -> prefs[Keys.mapLeft] = action.name
                "Two_Finger_Point" -> prefs[Keys.mapTwoFinger] = action.name
                "Volume_Up" -> prefs[Keys.mapVolumeUp] = action.name
                "Volume_Down" -> prefs[Keys.mapVolumeDown] = action.name
            }
        }
    }

    suspend fun setTuning(tuning: GestureTuning) {
        context.dataStore.edit { prefs ->
            prefs[Keys.actionCooldownMs] = tuning.actionCooldownMs
            prefs[Keys.volumeTickMs] = tuning.volumeTickMs
            prefs[Keys.pinchThreshold] = tuning.pinchThreshold
            prefs[Keys.pinchReleaseThreshold] = tuning.pinchReleaseThreshold
            prefs[Keys.swipeThreshold] = tuning.swipeThreshold
            prefs[Keys.swipeTimeoutMs] = tuning.swipeTimeoutMs
        }
    }

    suspend fun resetTuningToDefaults() {
        setTuning(GestureTuning())
    }

    private fun actionFrom(value: String?, fallback: DriveAction): DriveAction {
        return runCatching { DriveAction.valueOf(value ?: fallback.name) }.getOrDefault(fallback)
    }
}
