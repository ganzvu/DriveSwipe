package com.example.driveswipe

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.driveswipe.ui.theme.DriveSwipeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(
            this,
            MainViewModel.factory(SettingsRepository(applicationContext))
        )[MainViewModel::class.java]
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        if (!cameraGranted) {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
        }
        refreshPermissionState()
    }

    private val serviceEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ServiceContract.ACTION_GESTURE_EVENT -> {
                    val gesture = intent.getStringExtra(ServiceContract.EXTRA_EVENT_GESTURE) ?: return
                    val actionName = intent.getStringExtra(ServiceContract.EXTRA_EVENT_ACTION) ?: return
                    val action = runCatching { DriveAction.valueOf(actionName) }.getOrDefault(DriveAction.NONE)
                    viewModel.addGestureEvent(gesture, action)
                }
                ServiceContract.ACTION_ENGINE_STATE_EVENT -> {
                    val stateName = intent.getStringExtra(ServiceContract.EXTRA_ENGINE_STATE) ?: return
                    val state = runCatching { EngineState.valueOf(stateName) }.getOrDefault(EngineState.IDLE)
                    viewModel.setEngineState(state)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            DriveSwipeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState = viewModel.uiState.collectAsState().value
                    DriveSwipeApp(
                        uiState = uiState,
                        onToggleService = { start ->
                            if (start) startGestureService() else stopGestureService()
                        },
                        onNightModeChanged = { enabled ->
                            viewModel.setNightMode(enabled)
                            if (uiState.isServiceRunning) {
                                stopGestureService()
                                startGestureService()
                            }
                        },
                        onRetryPermissions = { checkPermissions() },
                        onOpenOverlaySettings = { openOverlaySettings() },
                        onPresetSelected = { viewModel.setPreset(it) },
                        onMappingChanged = { gestureKey, action -> viewModel.setMapping(gestureKey, action) },
                        onTuningChanged = { viewModel.updateTuning(it) },
                        onResetTuning = { viewModel.resetTuning() }
                    )
                }
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            refreshPermissionState()
        }
    }

    private fun refreshPermissionState() {
        val hasCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val hasOverlay = Settings.canDrawOverlays(this)
        viewModel.updatePermissionStatus(hasCamera, hasNotifications, hasOverlay)
    }

    fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun startGestureService() {
        val settings = viewModel.uiState.value.settings
        val intent = Intent(this, GestureService::class.java).apply {
            putExtra(ServiceContract.EXTRA_NIGHT_MODE, settings.isNightMode)
            putExtra(ServiceContract.EXTRA_ACTION_COOLDOWN_MS, settings.tuning.actionCooldownMs)
            putExtra(ServiceContract.EXTRA_VOLUME_TICK_MS, settings.tuning.volumeTickMs)
            putExtra(ServiceContract.EXTRA_PINCH_THRESHOLD, settings.tuning.pinchThreshold)
            putExtra(ServiceContract.EXTRA_PINCH_RELEASE_THRESHOLD, settings.tuning.pinchReleaseThreshold)
            putExtra(ServiceContract.EXTRA_SWIPE_THRESHOLD, settings.tuning.swipeThreshold)
            putExtra(ServiceContract.EXTRA_SWIPE_TIMEOUT_MS, settings.tuning.swipeTimeoutMs)
            putExtra(ServiceContract.EXTRA_ALERTING_BURST_MS, settings.tuning.alertingBurstMs)
            putExtra(ServiceContract.EXTRA_ACTIVE_TIMEOUT_MS, settings.tuning.activeTimeoutMs)
            putExtra(ServiceContract.EXTRA_IDLE_INFERENCE_INTERVAL_MS, settings.tuning.idleInferenceIntervalMs)
            putExtra(ServiceContract.EXTRA_MAP_PINCH_RIGHT, settings.mappings.pinchDragRight.name)
            putExtra(ServiceContract.EXTRA_MAP_PINCH_LEFT, settings.mappings.pinchDragLeft.name)
            putExtra(ServiceContract.EXTRA_MAP_TWO_FINGER, settings.mappings.twoFingerPoint.name)
            putExtra(ServiceContract.EXTRA_MAP_VOLUME_UP, settings.mappings.volumeUp.name)
            putExtra(ServiceContract.EXTRA_MAP_VOLUME_DOWN, settings.mappings.volumeDown.name)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
        viewModel.setServiceRunning(true)
    }

    private fun stopGestureService() {
        val intent = Intent(this, GestureService::class.java)
        stopService(intent)
        viewModel.setServiceRunning(false)
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(ServiceContract.ACTION_GESTURE_EVENT)
            addAction(ServiceContract.ACTION_ENGINE_STATE_EVENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceEventReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serviceEventReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(serviceEventReceiver)
    }
}
