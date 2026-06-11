package com.example.driveswipe

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object GestureServiceController {
    fun buildStartIntent(context: Context, settings: AppSettings): Intent {
        return Intent(context, GestureService::class.java).apply {
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
            putExtra(ServiceContract.EXTRA_HUD_DURATION_MS, settings.hudDurationMs)
        }
    }

    fun hasRequiredStartPermissions(context: Context): Boolean {
        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return hasCamera && hasNotifications
    }

    fun start(context: Context, settings: AppSettings) {
        val intent = buildStartIntent(context, settings)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, GestureService::class.java))
    }

    @Suppress("DEPRECATION")
    fun isRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.getRunningServices(Int.MAX_VALUE).any { serviceInfo ->
            serviceInfo.service.className == GestureService::class.java.name
        }
    }
}
