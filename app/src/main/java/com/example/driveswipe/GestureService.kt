package com.example.driveswipe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.util.Size
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GestureService : LifecycleService(), SensorEventListener {

    private lateinit var cameraExecutor: ExecutorService
    @Volatile
    private var gestureRecognizerHelper: GestureRecognizerHelper? = null
    
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    
    private var isNightMode: Boolean? = null
    private var lastProximityTime = 0L
    private var proximityWaveCount = 0
    private var settings = AppSettings()

    private var overlayView: StatusOverlayView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        startForegroundService()
        mainHandler.post { attachOverlay() }
    }

    private fun checkOverlayState() {
        if (Settings.canDrawOverlays(this)) {
            if (overlayView == null) attachOverlay()
        } else {
            if (overlayView != null) detachOverlay()
        }
    }

    private fun attachOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        if (overlayView != null) return

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            StatusOverlayView.DOT_SIZE_DP.toFloat(),
            resources.displayMetrics
        ).toInt()
        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics
        ).toInt()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            sizePx, sizePx, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = marginPx
            y = marginPx
        }

        val view = StatusOverlayView(this).also { overlayView = it }
        view.setState(EngineState.IDLE)
        wm.addView(view, params)
    }

    private fun detachOverlay() {
        val view = overlayView ?: return
        overlayView = null
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        runCatching { wm.removeView(view) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        applyConfig(intent)
        val nightMode = settings.isNightMode
        setNightMode(nightMode)
        mainHandler.post { checkOverlayState() }
        return START_STICKY
    }

    private fun applyConfig(intent: Intent?) {
        if (intent == null) return
        val tuning = settings.tuning.copy(
            actionCooldownMs = intent.getLongExtra(ServiceContract.EXTRA_ACTION_COOLDOWN_MS, settings.tuning.actionCooldownMs),
            volumeTickMs = intent.getLongExtra(ServiceContract.EXTRA_VOLUME_TICK_MS, settings.tuning.volumeTickMs),
            pinchThreshold = intent.getFloatExtra(ServiceContract.EXTRA_PINCH_THRESHOLD, settings.tuning.pinchThreshold),
            pinchReleaseThreshold = intent.getFloatExtra(ServiceContract.EXTRA_PINCH_RELEASE_THRESHOLD, settings.tuning.pinchReleaseThreshold),
            swipeThreshold = intent.getFloatExtra(ServiceContract.EXTRA_SWIPE_THRESHOLD, settings.tuning.swipeThreshold),
            swipeTimeoutMs = intent.getLongExtra(ServiceContract.EXTRA_SWIPE_TIMEOUT_MS, settings.tuning.swipeTimeoutMs),
            alertingBurstMs = intent.getLongExtra(ServiceContract.EXTRA_ALERTING_BURST_MS, settings.tuning.alertingBurstMs),
            activeTimeoutMs = intent.getLongExtra(ServiceContract.EXTRA_ACTIVE_TIMEOUT_MS, settings.tuning.activeTimeoutMs),
            idleInferenceIntervalMs = intent.getLongExtra(
                ServiceContract.EXTRA_IDLE_INFERENCE_INTERVAL_MS,
                settings.tuning.idleInferenceIntervalMs
            )
        )

        settings = settings.copy(
            isNightMode = intent.getBooleanExtra(ServiceContract.EXTRA_NIGHT_MODE, settings.isNightMode),
            tuning = tuning,
            mappings = GestureMappings(
                pinchDragRight = parseAction(intent.getStringExtra(ServiceContract.EXTRA_MAP_PINCH_RIGHT), settings.mappings.pinchDragRight),
                pinchDragLeft = parseAction(intent.getStringExtra(ServiceContract.EXTRA_MAP_PINCH_LEFT), settings.mappings.pinchDragLeft),
                twoFingerPoint = parseAction(intent.getStringExtra(ServiceContract.EXTRA_MAP_TWO_FINGER), settings.mappings.twoFingerPoint),
                volumeUp = parseAction(intent.getStringExtra(ServiceContract.EXTRA_MAP_VOLUME_UP), settings.mappings.volumeUp),
                volumeDown = parseAction(intent.getStringExtra(ServiceContract.EXTRA_MAP_VOLUME_DOWN), settings.mappings.volumeDown)
            )
        )

        gestureRecognizerHelper?.updateTuning(settings.tuning)
    }

    private fun startForegroundService() {
        val channelId = "GestureServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Gesture Recognition Service", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Use a default android icon since we might not have a mipmap launcher icon yet
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("DriveSwipe Active")
            .setContentText("Listening for hand gestures...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        startForeground(1, notification)
    }

    private fun setNightMode(enabled: Boolean) {
        if (isNightMode == enabled) return
        isNightMode = enabled
        if (enabled) {
            Log.d("DriveSwipe", "Switching to Night Mode (Proximity Sensor)")
            stopCamera()
            proximitySensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } else {
            Log.d("DriveSwipe", "Switching to Day Mode (Camera)")
            sensorManager.unregisterListener(this)
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(480, 360))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        val helper = gestureRecognizerHelper ?: GestureRecognizerHelper(
                            context = this,
                            gestureListener = object : GestureRecognizerHelper.GestureListener {
                                override fun onGestureRecognized(gestureName: String) {
                                    handleGesture(gestureName)
                                }

                                override fun onEngineStateChanged(state: EngineState) {
                                    handleEngineStateChanged(state)
                                }
                            },
                            tuning = settings.tuning
                        ).also { created ->
                            gestureRecognizerHelper = created
                        }
                        helper.recognizeImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("DriveSwipe", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        cameraExecutor.execute {
            gestureRecognizerHelper?.clear()
            gestureRecognizerHelper = null
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProviderFuture.get().unbindAll()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleGesture(gestureName: String) {
        Log.d("DriveSwipe", "Handling Gesture: $gestureName")

        val action = resolveAction(gestureName)
        mainHandler.post {
            overlayView?.showGestureConfirmation(gestureName, action.name)
        }

        when (action) {
            DriveAction.NEXT_TRACK -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            DriveAction.PREVIOUS_TRACK -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            DriveAction.PLAY_PAUSE -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            DriveAction.VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
            DriveAction.VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
            DriveAction.NONE -> Unit
        }
        publishGestureEvent(gestureName, action)
    }

    private fun handleEngineStateChanged(state: EngineState) {
        mainHandler.post { overlayView?.setState(state) }
        publishEngineStateEvent(state)
    }

    private fun resolveAction(gestureName: String): DriveAction {
        return when (gestureName) {
            "Pinch_Drag_Right" -> settings.mappings.pinchDragRight
            "Pinch_Drag_Left" -> settings.mappings.pinchDragLeft
            "Two_Finger_Point" -> settings.mappings.twoFingerPoint
            "Volume_Up" -> settings.mappings.volumeUp
            "Volume_Down" -> settings.mappings.volumeDown
            "Closed_Fist_Sleep" -> DriveAction.NONE
            else -> DriveAction.NONE
        }
    }

    private fun publishGestureEvent(gestureName: String, action: DriveAction) {
        sendBroadcast(Intent(ServiceContract.ACTION_GESTURE_EVENT).apply {
            setPackage(packageName)
            putExtra(ServiceContract.EXTRA_EVENT_GESTURE, gestureName)
            putExtra(ServiceContract.EXTRA_EVENT_ACTION, action.name)
        })
    }

    private fun publishEngineStateEvent(state: EngineState) {
        sendBroadcast(Intent(ServiceContract.ACTION_ENGINE_STATE_EVENT).apply {
            setPackage(packageName)
            putExtra(ServiceContract.EXTRA_ENGINE_STATE, state.name)
        })
    }

    private fun adjustVolume(direction: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        Log.d("DriveSwipe", "Adjusted Volume: $direction")
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)
        Log.d("DriveSwipe", "Dispatched Media Key: $keyCode")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            if (distance < (proximitySensor?.maximumRange ?: 5f)) {
                // Hand is near
                val now = SystemClock.elapsedRealtime()
                if (now - lastProximityTime < 1000) {
                    proximityWaveCount++
                    if (proximityWaveCount == 2) {
                        Log.d("DriveSwipe", "Proximity Double Wave detected!")
                        val action = settings.mappings.pinchDragRight
                        when (action) {
                            DriveAction.NEXT_TRACK -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                            DriveAction.PREVIOUS_TRACK -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                            DriveAction.PLAY_PAUSE -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                            DriveAction.VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
                            DriveAction.VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
                            DriveAction.NONE -> Unit
                        }
                        publishGestureEvent("Proximity_Double_Wave", action)
                        proximityWaveCount = 0
                    }
                } else {
                    proximityWaveCount = 1
                }
                lastProximityTime = now
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        sensorManager.unregisterListener(this)
        gestureRecognizerHelper?.clear()
        mainHandler.post { detachOverlay() }
    }

    private fun parseAction(actionName: String?, fallback: DriveAction): DriveAction {
        return runCatching { DriveAction.valueOf(actionName ?: fallback.name) }.getOrDefault(fallback)
    }
}
