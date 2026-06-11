package com.example.driveswipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val isServiceRunning: Boolean = false,
    val engineState: EngineState = EngineState.IDLE,
    val hasCameraPermission: Boolean = false,
    val hasNotificationsPermission: Boolean = true,
    val hasOverlayPermission: Boolean = false,
    val gestureHistory: List<GestureEvent> = emptyList()
) {
    val isDriveReady: Boolean
        get() = hasCameraPermission && hasNotificationsPermission
}

class MainViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun setServiceRunning(running: Boolean) {
        _uiState.update {
            it.copy(
                isServiceRunning = running,
                engineState = if (running) it.engineState else EngineState.IDLE
            )
        }
    }

    fun setEngineState(state: EngineState) {
        _uiState.update { it.copy(engineState = state) }
    }

    fun updatePermissionStatus(
        hasCameraPermission: Boolean,
        hasNotificationsPermission: Boolean,
        hasOverlayPermission: Boolean = false
    ) {
        _uiState.update {
            it.copy(
                hasCameraPermission = hasCameraPermission,
                hasNotificationsPermission = hasNotificationsPermission,
                hasOverlayPermission = hasOverlayPermission
            )
        }
    }

    fun setNightMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNightMode(enabled) }
    }

    fun setHudDuration(durationMs: Long) {
        viewModelScope.launch { settingsRepository.setHudDuration(durationMs) }
    }

    fun setPreset(preset: GesturePreset) {
        viewModelScope.launch { settingsRepository.setPreset(preset) }
    }

    fun setMapping(gestureKey: String, action: DriveAction) {
        viewModelScope.launch { settingsRepository.setMapping(gestureKey, action) }
    }

    fun updateTuning(tuning: GestureTuning) {
        viewModelScope.launch { settingsRepository.setTuning(tuning) }
    }

    fun resetTuning() {
        viewModelScope.launch { settingsRepository.resetTuningToDefaults() }
    }

    fun addGestureEvent(gestureName: String, action: DriveAction) {
        val event = GestureEvent(gestureName = gestureName, action = action)
        _uiState.update { it.copy(gestureHistory = (listOf(event) + it.gestureHistory).take(20)) }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(settingsRepository) as T
                }
            }
        }
    }
}
