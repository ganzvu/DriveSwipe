package com.example.driveswipe

import com.example.driveswipe.ui.theme.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                title = {
                    Text(
                        text = "DRIVESWIPE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status indicator banner
        if (!uiState.isDriveReady) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Permission Alert",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Setup Incomplete",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Camera & notifications are required.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                    TextButton(onClick = onGoSetup) {
                        Text("FIX", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Subtly show last gesture at the top
        val lastEvent = uiState.gestureHistory.firstOrNull()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LAST TRIGGERED ACTION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lastEvent?.let { "${it.gestureName.replace('_', ' ')} → ${it.action.name.replace('_', ' ')}" } ?: "No action triggered yet",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (lastEvent != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.weight(1.2f))

        // Hero Pulsing Button
        val infiniteTransition = rememberInfiniteTransition(label = "pulseRing")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (uiState.isServiceRunning) 1.06f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = if (uiState.isServiceRunning) 0.35f else 0.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        val stateColor = when {
            !uiState.isServiceRunning -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            uiState.engineState == EngineState.IDLE -> AccentSteel
            uiState.engineState == EngineState.ALERTING -> StateAlerting
            uiState.engineState == EngineState.ACTIVE -> StateActive
            else -> AccentCyan
        }

        val stateLabel = when {
            !uiState.isServiceRunning -> "SYSTEM DEACTIVATED"
            uiState.engineState == EngineState.IDLE -> "ENGINE SLEEPING"
            uiState.engineState == EngineState.ALERTING -> "HAND DETECTED"
            uiState.engineState == EngineState.ACTIVE -> "ENGINE LISTENING"
            else -> "ACTIVE"
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            // Outer Pulsing Glow Ring
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                stateColor.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Border stroke feedback
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(3.dp, stateColor), CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onToggleService(!uiState.isServiceRunning) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isServiceRunning) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isServiceRunning) "Stop Engine" else "Start Engine",
                        tint = if (uiState.isServiceRunning) TextPrimary else stateColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.isServiceRunning) "STOP" else "START",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // State label pill
        Box(
            modifier = Modifier
                .border(BorderStroke(1.dp, stateColor.copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
                .background(stateColor.copy(alpha = 0.06f))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (!uiState.isServiceRunning) TextSecondary else stateColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (!uiState.isServiceRunning) "Tap the engine to activate hand gesture mapping."
            else when (uiState.engineState) {
                EngineState.IDLE -> if (uiState.settings.isNightMode) "Proximity active. Wave twice near screen to start." else "Camera sleeping. Show hand to wake."
                EngineState.ALERTING -> "Calibrating tracking details..."
                EngineState.ACTIVE -> "System ready. Gesture controls active."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1.5f))

        // Float status permission trigger (if not granted)
        if (!uiState.hasOverlayPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Status Overlay Dot",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Overlay status indicator above other apps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = onOpenOverlaySettings,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enable", color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sleek floating navigation bar for Quick Links
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onGoHistory, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "History Log",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                )
                IconButton(onClick = onGoSettings, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuration",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
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
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Setup Wizard",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            text = "DriveSwipe uses your camera to capture hand movement. Grant permissions below to configure your driving assistant.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionRow(
                    label = "Camera Access",
                    description = "Required to detect hand gestures in real-time.",
                    isGranted = uiState.hasCameraPermission
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                )
                PermissionRow(
                    label = "System Notifications",
                    description = "Needed to keep the gesture engine running in background.",
                    isGranted = uiState.hasNotificationsPermission
                )
            }
        }
        
        Button(
            onClick = onRetryPermissions,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = TextPrimary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry Permission Checks", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onContinue, 
            enabled = complete,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = DarkBg,
                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
            )
        ) { 
            Text(
                text = "Continue to App",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            ) 
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    description: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = if (isGranted) StateActive else StateError,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) {
                Text("Done", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Preferences Group
        Text(
            text = "PREFERENCES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Night Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "Use proximity sensor instead of camera for low light.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = uiState.settings.isNightMode,
                        onCheckedChange = onNightModeChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = AccentCyan.copy(alpha = 0.4f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkBorder
                        )
                    )
                }
                if (uiState.hasOverlayPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Floating Status Dot", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = "Currently active over other applications.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        TextButton(onClick = onOpenOverlaySettings) {
                            Text("Revoke", fontWeight = FontWeight.Bold, color = StateError)
                        }
                    }
                }
            }
        }

        // Autostart tips panel
        Text(
            text = "AUTOSTART INTEGRATION",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Samsung Routine Integration", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = "Launch and exit DriveSwipe automatically using Samsung Modes & Routines when your car Bluetooth connects.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg)
                        .padding(12.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "1. Condition: Bluetooth device -> Connected -> [Car Bluetooth]",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "2. Action: Start DriveSwipe shortcut or app action",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan
                    )
                    Text(
                        text = "3. Create second disconnect routine for auto-stop",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Gestures Group
        Text(
            text = "GESTURE MAPPINGS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Configuration Preset", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                            label = { Text(preset.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentCyan.copy(alpha = 0.15f),
                                selectedLabelColor = AccentCyan,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = DarkBorder,
                                selectedBorderColor = AccentCyan,
                                selectedBorderWidth = 1.dp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MappingEditor("Pinch Drag Right", uiState.settings.mappings.pinchDragRight) { onMappingChanged("Pinch_Drag_Right", it) }
                    MappingEditor("Pinch Drag Left", uiState.settings.mappings.pinchDragLeft) { onMappingChanged("Pinch_Drag_Left", it) }
                    MappingEditor("Two Finger Point", uiState.settings.mappings.twoFingerPoint) { onMappingChanged("Two_Finger_Point", it) }
                    MappingEditor("Volume Up", uiState.settings.mappings.volumeUp) { onMappingChanged("Volume_Up", it) }
                    MappingEditor("Volume Down", uiState.settings.mappings.volumeDown) { onMappingChanged("Volume_Down", it) }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onGoAdvanced,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = TextPrimary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Advanced Tuning & Thresholds", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AccentCyan
                )
            }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBg),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        text = if (expanded) "Hide Options" else currentAction.name.replace('_', ' '),
                        fontWeight = FontWeight.Bold,
                        color = if (expanded) TextSecondary else AccentCyan
                    )
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DriveAction.entries.forEach { action ->
                        FilterChip(
                            selected = action == currentAction,
                            onClick = { 
                                onActionSelected(action)
                                expanded = false 
                            },
                            label = { Text(action.name.replace('_', ' ')) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                                selectedLabelColor = AccentCyan,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = DarkBorder,
                                selectedBorderColor = AccentCyan,
                                selectedBorderWidth = 1.dp
                            )
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tuning",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) {
                Text("Back", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Section: Core Limits
        Text(
            text = "CORE LIMITS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TuningSlider("Action Cooldown", "${tuning.actionCooldownMs}ms", tuning.actionCooldownMs.toFloat(), 500f..3000f) {
                    onTuningChanged(tuning.copy(actionCooldownMs = it.toLong()))
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)))
                TuningSlider("Volume Tick Speed", "${tuning.volumeTickMs}ms", tuning.volumeTickMs.toFloat(), 200f..1000f) {
                    onTuningChanged(tuning.copy(volumeTickMs = it.toLong()))
                }
            }
        }

        // Section: Sensitivity
        Text(
            text = "SENSITIVITY THRESHOLDS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TuningSlider("Pinch Threshold", String.format("%.2f", tuning.pinchThreshold), tuning.pinchThreshold, 0.05f..0.15f) {
                    onTuningChanged(tuning.copy(pinchThreshold = it))
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)))
                TuningSlider("Pinch Release", String.format("%.2f", tuning.pinchReleaseThreshold), tuning.pinchReleaseThreshold, 0.10f..0.30f) {
                    onTuningChanged(tuning.copy(pinchReleaseThreshold = it))
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)))
                TuningSlider("Swipe Threshold", String.format("%.2f", tuning.swipeThreshold), tuning.swipeThreshold, 0.10f..0.25f) {
                    onTuningChanged(tuning.copy(swipeThreshold = it))
                }
            }
        }

        // Section: Engine Behavior
        Text(
            text = "ENGINE INTERVALS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TuningSlider("Alerting Burst", "${tuning.alertingBurstMs}ms", tuning.alertingBurstMs.toFloat(), 500f..2500f) {
                    onTuningChanged(tuning.copy(alertingBurstMs = it.toLong()))
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)))
                TuningSlider("Active Timeout", "${tuning.activeTimeoutMs}ms", tuning.activeTimeoutMs.toFloat(), 3000f..15000f) {
                    onTuningChanged(tuning.copy(activeTimeoutMs = it.toLong()))
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)))
                TuningSlider("Idle Polling", "${tuning.idleInferenceIntervalMs}ms", tuning.idleInferenceIntervalMs.toFloat(), 250f..800f) {
                    onTuningChanged(tuning.copy(idleInferenceIntervalMs = it.toLong()))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onResetTuning,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = TextPrimary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Text("Reset to System Defaults", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TuningSlider(
    title: String,
    valueStr: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                text = valueStr,
                style = MaterialTheme.typography.labelMedium,
                color = AccentCyan
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentCyan,
                inactiveTrackColor = DarkBorder
            )
        )
    }
}

@Composable
private fun HistoryScreen(uiState: MainUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) {
                Text("Back", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (uiState.gestureHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No gestures recorded yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "Gestures will appear here in real-time as they are detected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            val dateFormat = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.gestureHistory.forEachIndexed { index, event ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(AccentCyan)
                            )
                            Spacer(modifier = Modifier.width(16.dp))

                            val timeStr = remember(event.timestampMs) { dateFormat.format(java.util.Date(event.timestampMs)) }
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentSteel,
                                modifier = Modifier.width(70.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = event.gestureName.replace('_', ' '),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkBorder)
                                    .border(BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = event.action.name.replace('_', ' '),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan
                                )
                            }
                        }

                        if (index < uiState.gestureHistory.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            )
                        }
                    }
                }
            }
        }
    }
}
