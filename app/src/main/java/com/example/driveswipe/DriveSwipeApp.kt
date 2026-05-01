package com.example.driveswipe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object Route {
    const val Home = "home"
    const val Setup = "setup"
    const val Settings = "settings"
    const val AdvancedSettings = "advanced_settings"
    const val History = "history"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveSwipeApp(
    uiState: MainUiState,
    onToggleService: (Boolean) -> Unit,
    onNightModeChanged: (Boolean) -> Unit,
    onRetryPermissions: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onPresetSelected: (GesturePreset) -> Unit,
    onMappingChanged: (String, DriveAction) -> Unit,
    onTuningChanged: (GestureTuning) -> Unit,
    onResetTuning: () -> Unit
) {
    val navController = rememberNavController()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DriveSwipe", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (uiState.isDriveReady) Route.Home else Route.Setup,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.Setup) {
                SetupWizardScreen(
                    uiState = uiState,
                    onRetryPermissions = onRetryPermissions,
                    onContinue = { navController.navigate(Route.Home) }
                )
            }
            composable(Route.Home) {
                HomeScreen(
                    uiState = uiState,
                    onToggleService = onToggleService,
                    onGoSetup = { navController.navigate(Route.Setup) },
                    onGoHistory = { navController.navigate(Route.History) },
                    onGoSettings = { navController.navigate(Route.Settings) },
                    onOpenOverlaySettings = onOpenOverlaySettings
                )
            }
            composable(Route.Settings) {
                SettingsScreen(
                    uiState = uiState,
                    onNightModeChanged = onNightModeChanged,
                    onPresetSelected = onPresetSelected,
                    onMappingChanged = onMappingChanged,
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onGoAdvanced = { navController.navigate(Route.AdvancedSettings) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.AdvancedSettings) {
                AdvancedSettingsScreen(
                    tuning = uiState.settings.tuning,
                    onTuningChanged = onTuningChanged,
                    onResetTuning = onResetTuning,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.History) {
                HistoryScreen(uiState = uiState, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun HomeScreen(
    uiState: MainUiState,
    onToggleService: (Boolean) -> Unit,
    onGoSetup: () -> Unit,
    onGoHistory: () -> Unit,
    onGoSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!uiState.isDriveReady) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permissions required to run.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = onGoSetup) { Text("Open Setup", color = MaterialTheme.colorScheme.onErrorContainer) }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Hero Button
        val infiniteTransition = rememberInfiniteTransition()
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (uiState.isServiceRunning && uiState.engineState == EngineState.ALERTING) 1.05f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        val buttonColor by animateColorAsState(
            targetValue = if (!uiState.isServiceRunning) MaterialTheme.colorScheme.surfaceVariant
            else when (uiState.engineState) {
                EngineState.IDLE -> MaterialTheme.colorScheme.secondaryContainer
                EngineState.ALERTING -> Color(0xFFFF9800) // Orange
                EngineState.ACTIVE -> Color(0xFF4CAF50) // Green
            },
            animationSpec = tween(400),
            label = "buttonColor"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(buttonColor)
                .clickable { onToggleService(!uiState.isServiceRunning) }
        ) {
            Text(
                text = if (uiState.isServiceRunning) "STOP" else "START",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = if (!uiState.isServiceRunning) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (!uiState.isServiceRunning) "Gesture control is stopped."
            else when (uiState.engineState) {
                EngineState.IDLE -> if (uiState.settings.isNightMode) "Proximity active. Wave twice to wake." else "Camera sleeping. Wave to wake."
                EngineState.ALERTING -> "Hand detected. Checking gesture..."
                EngineState.ACTIVE -> "Listening for gestures..."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        if (!uiState.hasOverlayPermission) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Floating Status Dot", fontWeight = FontWeight.Bold)
                    Text("See engine state while using Maps or Spotify.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onOpenOverlaySettings) { Text("Enable") }
                }
            }
        }

        // Quick Links Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGoHistory, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.List, contentDescription = "History", modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = onGoSettings, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun SetupWizardScreen(
    uiState: MainUiState,
    onRetryPermissions: () -> Unit,
    onContinue: () -> Unit
) {
    val complete = uiState.isDriveReady
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Setup Wizard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Please grant the required permissions to begin.")
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionRow("Camera access", uiState.hasCameraPermission)
                PermissionRow("Notifications", uiState.hasNotificationsPermission)
            }
        }
        
        Button(onClick = onRetryPermissions, modifier = Modifier.fillMaxWidth()) { Text("Retry checks") }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onContinue, 
            enabled = complete,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { 
            Text("Continue to App") 
        }
    }
}

@Composable
private fun PermissionRow(label: String, isGranted: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            if (isGranted) "Ready" else "Missing",
            color = if (isGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    uiState: MainUiState,
    onNightModeChanged: (Boolean) -> Unit,
    onPresetSelected: (GesturePreset) -> Unit,
    onMappingChanged: (String, DriveAction) -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onGoAdvanced: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Done") }
        }

        // Preferences
        Text("Preferences", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Night Mode", fontWeight = FontWeight.Bold)
                        Text("Use proximity sensor instead of camera", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = uiState.settings.isNightMode, onCheckedChange = onNightModeChanged)
                }
                if (uiState.hasOverlayPermission) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Floating Status Dot", fontWeight = FontWeight.Bold)
                            Text("Active over other apps", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = onOpenOverlaySettings) { Text("Revoke") }
                    }
                }
            }
        }

        // Gestures
        Text("Gestures", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("Preset Configuration", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GesturePreset.entries.forEach { preset ->
                FilterChip(
                    selected = uiState.settings.selectedPreset == preset,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.name) }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MappingEditor("Pinch Drag Right", uiState.settings.mappings.pinchDragRight) { onMappingChanged("Pinch_Drag_Right", it) }
            MappingEditor("Pinch Drag Left", uiState.settings.mappings.pinchDragLeft) { onMappingChanged("Pinch_Drag_Left", it) }
            MappingEditor("Two Finger Point", uiState.settings.mappings.twoFingerPoint) { onMappingChanged("Two_Finger_Point", it) }
            MappingEditor("Volume Up", uiState.settings.mappings.volumeUp) { onMappingChanged("Volume_Up", it) }
            MappingEditor("Volume Down", uiState.settings.mappings.volumeDown) { onMappingChanged("Volume_Down", it) }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGoAdvanced, modifier = Modifier.fillMaxWidth()) {
            Text("Advanced Tuning")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MappingEditor(
    label: String,
    currentAction: DriveAction,
    onActionSelected: (DriveAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else currentAction.name)
                }
            }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    DriveAction.entries.forEach { action ->
                        FilterChip(
                            selected = action == currentAction,
                            onClick = { 
                                onActionSelected(action)
                                expanded = false 
                            },
                            label = { Text(action.name.replace('_', ' ')) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettingsScreen(
    tuning: GestureTuning,
    onTuningChanged: (GestureTuning) -> Unit,
    onResetTuning: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tuning", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }

        Text("Core Limits", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        TuningSlider("Action Cooldown (${tuning.actionCooldownMs}ms)", tuning.actionCooldownMs.toFloat(), 500f..3000f) {
            onTuningChanged(tuning.copy(actionCooldownMs = it.toLong()))
        }
        TuningSlider("Volume Tick Speed (${tuning.volumeTickMs}ms)", tuning.volumeTickMs.toFloat(), 200f..1000f) {
            onTuningChanged(tuning.copy(volumeTickMs = it.toLong()))
        }
        
        Text("Sensitivity", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        TuningSlider("Pinch Threshold (${String.format("%.2f", tuning.pinchThreshold)})", tuning.pinchThreshold, 0.05f..0.15f) {
            onTuningChanged(tuning.copy(pinchThreshold = it))
        }
        TuningSlider("Pinch Release (${String.format("%.2f", tuning.pinchReleaseThreshold)})", tuning.pinchReleaseThreshold, 0.10f..0.30f) {
            onTuningChanged(tuning.copy(pinchReleaseThreshold = it))
        }
        TuningSlider("Swipe Threshold (${String.format("%.2f", tuning.swipeThreshold)})", tuning.swipeThreshold, 0.10f..0.25f) {
            onTuningChanged(tuning.copy(swipeThreshold = it))
        }

        Text("Engine Behavior", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        TuningSlider("Alerting Burst (${tuning.alertingBurstMs}ms)", tuning.alertingBurstMs.toFloat(), 500f..2500f) {
            onTuningChanged(tuning.copy(alertingBurstMs = it.toLong()))
        }
        TuningSlider("Active Timeout (${tuning.activeTimeoutMs}ms)", tuning.activeTimeoutMs.toFloat(), 3000f..15000f) {
            onTuningChanged(tuning.copy(activeTimeoutMs = it.toLong()))
        }
        TuningSlider("Idle Polling (${tuning.idleInferenceIntervalMs}ms)", tuning.idleInferenceIntervalMs.toFloat(), 250f..800f) {
            onTuningChanged(tuning.copy(idleInferenceIntervalMs = it.toLong()))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onResetTuning, modifier = Modifier.fillMaxWidth()) {
            Text("Reset to Defaults")
        }
    }
}

@Composable
private fun TuningSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(title, fontWeight = FontWeight.SemiBold)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun HistoryScreen(uiState: MainUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }
        
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.gestureHistory.isEmpty()) {
                    Text("No gestures recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    uiState.gestureHistory.forEach { event ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(event.gestureName.replace('_', ' '), fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(event.action.name.replace('_', ' '), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
