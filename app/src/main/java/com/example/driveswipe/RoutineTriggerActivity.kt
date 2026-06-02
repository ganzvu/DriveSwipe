package com.example.driveswipe

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RoutineTriggerActivity : ComponentActivity() {
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleRoutineIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRoutineIntent(intent)
    }

    private fun handleRoutineIntent(intent: Intent?) {
        when (intent?.action) {
            ServiceContract.ACTION_ROUTINE_START -> startFromRoutine()
            ServiceContract.ACTION_ROUTINE_STOP -> stopFromRoutine()
            else -> openMainActivityAndFinish()
        }
    }

    private fun startFromRoutine() {
        if (!GestureServiceController.hasRequiredStartPermissions(this)) {
            Toast.makeText(
                this,
                "DriveSwipe needs camera and notification permissions before Routine start.",
                Toast.LENGTH_LONG
            ).show()
            openMainActivityAndFinish()
            return
        }

        lifecycleScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            runCatching {
                GestureServiceController.start(this@RoutineTriggerActivity, settings)
            }.onSuccess {
                Toast.makeText(
                    this@RoutineTriggerActivity,
                    "DriveSwipe listening started.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }.onFailure {
                Toast.makeText(
                    this@RoutineTriggerActivity,
                    "DriveSwipe could not start from Routine. Opening app.",
                    Toast.LENGTH_LONG
                ).show()
                openMainActivityAndFinish()
            }
        }
    }

    private fun stopFromRoutine() {
        GestureServiceController.stop(this)
        Toast.makeText(this, "DriveSwipe listening stopped.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun openMainActivityAndFinish() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        finish()
    }
}
