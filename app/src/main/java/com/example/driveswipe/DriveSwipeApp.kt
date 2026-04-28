package com.example.driveswipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object Route {
    const val Home = "home"
    const val Setup = "setup"
    const val Gestures = "gestures"
    const val Modes = "modes"
    const val History = "history"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveSwipeApp(
    uiState: MainUiState,
    onToggleService: (Boolean) -> Unit,
    onNightModeChanged: (Boolean) -> Unit,
    onRetryPermissions: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onPresetSelected: (GesturePreset) -> Unit,
    onMappingChanged: (String, DriveAction) -> Unit,
    onTuningChanged: (GestureTuning) -> Unit,
    onResetTuning: () -> Unit
) {
    val navController = rememberNavController()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("DriveSwipe") })
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
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onContinue = { navController.navigate(Route.Home) }
                )
            }
            composable(Route.Home) {
                HomeScreen(
                    uiState = uiState,
                    onToggleService = onToggleService,
                    onNightModeChanged = onNightModeChanged,
                    onGoSetup = { navController.navigate(Route.Setup) },
                    onGoGestures = { navController.navigate(Route.Gestures) },
                    onGoModes = { navController.navigate(Route.Modes) },
                    onGoHistory = { navController.navigate(Route.History) }
                )
            }
            composable(Route.Gestures) {
                GestureSettingsScreen(
                    uiState = uiState,
                    onPresetSelected = onPresetSelected,
                    onMappingChanged = onMappingChanged,
                    onTuningChanged = onTuningChanged,
                    onResetTuning = onResetTuning,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.Modes) {
                ModesScreen(uiState = uiState, onBack = { navController.popBackStack() })
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
    onNightModeChanged: (Boolean) -> Unit,
    onGoSetup: () -> Unit,
    onGoGestures: () -> Unit,
    onGoModes: () -> Unit,
    onGoHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (uiState.isDriveReady) "Drive Ready" else "Setup Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (uiState.isServiceRunning) "Gesture control is active." else "Gesture control is stopped."
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quick Start")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = uiState.isServiceRunning, onCheckedChange = onToggleService)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Night Mode")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = uiState.settings.isNightMode, onCheckedChange = onNightModeChanged)
        }

        if (!uiState.isDriveReady) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Some permissions are still missing.")
                    TextButton(onClick = onGoSetup) { Text("Open setup wizard") }
                }
            }
        }

        val lastEvent = uiState.gestureHistory.firstOrNull()
        Text("Last recognized: ${lastEvent?.gestureName ?: "None"}")
        Text("Last action: ${lastEvent?.action?.name ?: "None"}")
        Text("Cooldown: ${uiState.settings.tuning.actionCooldownMs}ms")

        Button(onClick = { onToggleService(false) }, enabled = uiState.isServiceRunning) {
            Text("Emergency Disable")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onGoGestures) { Text("Gestures") }
            Button(onClick = onGoModes) { Text("Modes") }
            Button(onClick = onGoHistory) { Text("History") }
        }
    }
}

@Composable
private fun SetupWizardScreen(
    uiState: MainUiState,
    onRetryPermissions: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onContinue: () -> Unit
) {
    val complete = uiState.isDriveReady
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Setup Wizard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Complete these checks before driving.")
        PermissionRow("Camera access", uiState.hasCameraPermission)
        PermissionRow("Notifications permission", uiState.hasNotificationsPermission)
        PermissionRow("Notification listener access", uiState.hasNotificationListenerAccess)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRetryPermissions) { Text("Retry checks") }
            TextButton(onClick = onOpenNotificationSettings) { Text("Open listener settings") }
        }
        Button(onClick = onContinue, enabled = complete) { Text("Continue to Home") }
    }
}

@Composable
private fun PermissionRow(label: String, isGranted: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Spacer(modifier = Modifier.weight(1f))
        Text(if (isGranted) "Ready" else "Missing")
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GestureSettingsScreen(
    uiState: MainUiState,
    onPresetSelected: (GesturePreset) -> Unit,
    onMappingChanged: (String, DriveAction) -> Unit,
    onTuningChanged: (GestureTuning) -> Unit,
    onResetTuning: () -> Unit,
    onBack: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val tuning = uiState.settings.tuning

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Gesture Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Presets") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Mapping") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Tuning") })
        }

        when (tab) {
            0 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GesturePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = preset == uiState.settings.selectedPreset,
                            onClick = { onPresetSelected(preset) },
                            label = { Text(preset.name) }
                        )
                    }
                }
            }
            1 -> {
                MappingEditor("Pinch_Drag_Right", uiState.settings.mappings.pinchDragRight, onMappingChanged)
                MappingEditor("Pinch_Drag_Left", uiState.settings.mappings.pinchDragLeft, onMappingChanged)
                MappingEditor("Two_Finger_Point", uiState.settings.mappings.twoFingerPoint, onMappingChanged)
                MappingEditor("Volume_Up", uiState.settings.mappings.volumeUp, onMappingChanged)
                MappingEditor("Volume_Down", uiState.settings.mappings.volumeDown, onMappingChanged)
            }
            else -> {
                TuningSlider(
                    title = "Action cooldown (${tuning.actionCooldownMs}ms)",
                    value = tuning.actionCooldownMs.toFloat(),
                    range = 500f..3000f,
                    onValueChange = { onTuningChanged(tuning.copy(actionCooldownMs = it.toLong())) }
                )
                TuningSlider(
                    title = "Volume tick (${tuning.volumeTickMs}ms)",
                    value = tuning.volumeTickMs.toFloat(),
                    range = 200f..1000f,
                    onValueChange = { onTuningChanged(tuning.copy(volumeTickMs = it.toLong())) }
                )
                TuningSlider(
                    title = "Pinch threshold (${String.format("%.2f", tuning.pinchThreshold)})",
                    value = tuning.pinchThreshold,
                    range = 0.05f..0.15f,
                    onValueChange = { onTuningChanged(tuning.copy(pinchThreshold = it)) }
                )
                TuningSlider(
                    title = "Swipe threshold (${String.format("%.2f", tuning.swipeThreshold)})",
                    value = tuning.swipeThreshold,
                    range = 0.10f..0.25f,
                    onValueChange = { onTuningChanged(tuning.copy(swipeThreshold = it)) }
                )
                TextButton(onClick = onResetTuning) { Text("Reset tuning") }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MappingEditor(
    gestureKey: String,
    currentAction: DriveAction,
    onMappingChanged: (String, DriveAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(gestureKey)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Action: ${currentAction.name}")
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Change")
                }
            }
            if (expanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DriveAction.entries.forEach { action ->
                        FilterChip(
                            selected = action == currentAction,
                            onClick = { onMappingChanged(gestureKey, action) },
                            label = { Text(action.name) }
                        )
                    }
                }
            }
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
        Text(title)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
private fun ModesScreen(uiState: MainUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Modes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }
        Text("Current mode: ${if (uiState.settings.isNightMode) "Night" else "Day"}")
        Text("Day Mode: camera gestures, full mapping support.")
        Text("Night Mode: proximity wave shortcut and low-light safety operation.")
    }
}

@Composable
private fun HistoryScreen(uiState: MainUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Activity", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }
        if (uiState.gestureHistory.isEmpty()) {
            Text("No gestures recorded yet.")
        } else {
            uiState.gestureHistory.forEach { event ->
                Text("${event.gestureName} -> ${event.action.name}")
            }
        }
    }
}
