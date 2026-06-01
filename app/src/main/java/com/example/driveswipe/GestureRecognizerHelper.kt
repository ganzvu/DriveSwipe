package com.example.driveswipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

class GestureRecognizerHelper(
    val context: Context,
    val gestureListener: GestureListener,
    private var tuning: GestureTuning = GestureTuning(),
    private val preferredDelegate: Delegate = Delegate.GPU
) {
    private var gestureRecognizer: GestureRecognizer? = null
    private var modelBuffer: java.nio.ByteBuffer? = null
    private var lastInferenceTimeMs = 0L
    private var inferenceInFlight = false
    private var engineState = EngineState.IDLE
    private var alertingStartTime = 0L
    private var missingLandmarkFrames = 0
    private var lastActiveGestureTime = 0L
    
    // Swipe & Motion tracking
    private var startX = -1f
    private var startY = -1f
    private var startTime = 0L
    private var isTracking = false
    private var lastActionTime = 0L
    private var lastRecognizedGesture = "" // State Latch to prevent holding a gesture from rapid firing
    private var pinchCandidateFrames = 0
    private var releaseCandidateFrames = 0
    private var previousPinchDist2D = Float.NaN
    private var fistCandidateFrames = 0

    private val pinchConfirmFrames = 2
    private val pinchReleaseFrames = 2
    private val minSwipeAgeMs = 80L
    private val horizontalDominanceRatio = 1.35f
    private val activeInferenceIntervalMs = 55L
    private val alertingNoHandFrames = 3

    init {
        setupGestureRecognizer()
    }

    private fun distance(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun loadModelAsMappedBuffer(): java.nio.ByteBuffer {
        val file = java.io.File(context.cacheDir, "gesture_recognizer.tflite")
        if (!file.exists() || file.length() == 0L) {
            context.assets.open("gesture_recognizer.tflite").use { inputStream ->
                java.io.FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        val randomAccessFile = java.io.RandomAccessFile(file, "r")
        val channel = randomAccessFile.channel
        return channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, file.length())
    }

    private fun setupGestureRecognizer() {
        modelBuffer = loadModelAsMappedBuffer() // Hold strong reference to prevent GC!
        val delegates = if (preferredDelegate == Delegate.GPU) {
            listOf(Delegate.GPU, Delegate.CPU)
        } else {
            listOf(Delegate.CPU)
        }
        for (delegate in delegates) {
            val baseOptions = BaseOptions.builder()
                .setModelAssetBuffer(modelBuffer)
                .setDelegate(delegate)
                .build()

            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)
                .build()

            try {
                gestureRecognizer = GestureRecognizer.createFromOptions(context.applicationContext, options)
                Log.i("DriveSwipe", "GestureRecognizer delegate active: $delegate")
                return
            } catch (e: Exception) {
                Log.w("DriveSwipe", "GestureRecognizer setup failed for $delegate", e)
            }
        }
        Log.e("DriveSwipe", "GestureRecognizer setup failed for all delegates")
    }

    fun recognizeImage(imageProxy: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis()
        val inferenceIntervalMs = if (engineState == EngineState.IDLE) {
            tuning.idleInferenceIntervalMs.coerceAtLeast(activeInferenceIntervalMs)
        } else {
            activeInferenceIntervalMs
        }
        if (inferenceInFlight || frameTime - lastInferenceTimeMs < inferenceIntervalMs) {
            imageProxy.close()
            return
        }
        inferenceInFlight = true
        try {
            val bitmap = imageProxy.toBitmap()

            // Front camera usually requires horizontal flip and rotation
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                postScale(-1f, 1f) // Mirror
            }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            gestureRecognizer?.recognizeAsync(mpImage, frameTime)
            lastInferenceTimeMs = frameTime
        } catch (e: Exception) {
            inferenceInFlight = false
            Log.e("DriveSwipe", "Failed to process frame", e)
        } finally {
            imageProxy.close()
        }
    }

    private var lastVolumeTickTime = 0L

    fun updateTuning(newTuning: GestureTuning) {
        tuning = newTuning
    }

    private fun transitionTo(state: EngineState, now: Long) {
        if (engineState == state) return
        engineState = state
        missingLandmarkFrames = 0
        alertingStartTime = if (state == EngineState.ALERTING) now else 0L
        if (state == EngineState.ACTIVE) {
            lastActiveGestureTime = now
            lastInferenceTimeMs = 0L
        } else if (state == EngineState.ALERTING) {
            lastInferenceTimeMs = 0L
        }
        gestureListener.onEngineStateChanged(state)
    }

    private fun handleAlertingWakeGesture(
        landmarks: List<NormalizedLandmark>,
        gestureName: String,
        score: Float,
        now: Long
    ) {
        if (validateOpenPalmGeometry(landmarks, gestureName, score)) {
            transitionTo(EngineState.ACTIVE, now)
            return
        }
        if (now - alertingStartTime > tuning.alertingBurstMs.coerceAtLeast(activeInferenceIntervalMs)) {
            transitionTo(EngineState.IDLE, now)
        }
    }

    private fun handleNoLandmarks(now: Long) {
        when (engineState) {
            EngineState.ALERTING -> {
                missingLandmarkFrames++
                if (
                    missingLandmarkFrames >= alertingNoHandFrames ||
                    now - alertingStartTime > tuning.alertingBurstMs.coerceAtLeast(activeInferenceIntervalMs)
                ) {
                    transitionTo(EngineState.IDLE, now)
                }
            }
            EngineState.ACTIVE -> maybeSleepAfterTimeout(now)
            EngineState.IDLE -> Unit
        }
    }

    private fun emitGesture(gestureName: String, now: Long) {
        lastActiveGestureTime = now
        gestureListener.onGestureRecognized(gestureName)
    }

    private fun maybeSleepAfterTimeout(now: Long) {
        if (engineState == EngineState.ACTIVE && now - lastActiveGestureTime > tuning.activeTimeoutMs.coerceAtLeast(1000L)) {
            transitionTo(EngineState.IDLE, now)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun returnLivestreamResult(result: GestureRecognizerResult, mpImage: MPImage) {
        inferenceInFlight = false
        val now = SystemClock.elapsedRealtime()

        if (result.landmarks().isNotEmpty()) {
            val landmarks = result.landmarks()[0]
            val wrist = landmarks[0] // Used as the hyper-stable anchor
            val thumbTip = landmarks[4]
            val thumbMCP = landmarks[2] // Base joint of thumb
            val indexTip = landmarks[8]
            val indexPip = landmarks[6]
            val indexMcp = landmarks[5]
            val middleTip = landmarks[12]
            val middlePip = landmarks[10]
            val middleMcp = landmarks[9]
            val ringTip = landmarks[16]
            val ringPip = landmarks[14]
            val ringMcp = landmarks[13]
            val pinkyTip = landmarks[20]
            val pinkyPip = landmarks[18]
            val pinkyMcp = landmarks[17]
            
            var gestureName = "None"
            var score = 0f
            if (result.gestures().isNotEmpty()) {
                gestureName = result.gestures()[0][0].categoryName()
                score = result.gestures()[0][0].score()
            }

            if (engineState == EngineState.IDLE) {
                transitionTo(EngineState.ALERTING, now)
                return
            }

            missingLandmarkFrames = 0
            if (engineState == EngineState.ALERTING) {
                handleAlertingWakeGesture(landmarks, gestureName, score, now)
                return
            }

            // 1. New Volume Control (Thumb Up / Down)
            // STRICT VERIFICATION: We manually check that the thumb tip is physically pointing UP or DOWN 
            // relative to its base knuckle. This prevents gripping the steering wheel from falsely triggering it!
            val wristToMiddleAngle = Math.toDegrees(
                Math.atan2(
                    (middleMcp.y() - wrist.y()).toDouble(),
                    (middleMcp.x() - wrist.x()).toDouble()
                )
            )
            val nearlyHorizontalHand = Math.abs(wristToMiddleAngle) < 35.0 || Math.abs(wristToMiddleAngle) > 145.0
            val fingersCurled = isFingerCurled(indexTip, indexPip, indexMcp) &&
                isFingerCurled(middleTip, middlePip, middleMcp) &&
                isFingerCurled(ringTip, ringPip, ringMcp)
            val likelySteeringGrip = nearlyHorizontalHand && fingersCurled
            val isStrictThumbUp = gestureName == "Thumb_Up" &&
                score > 0.50f &&
                thumbTip.y() < thumbMCP.y() - 0.04f &&
                !likelySteeringGrip
            val isStrictThumbDown = gestureName == "Thumb_Down" &&
                score > 0.50f &&
                thumbTip.y() > thumbMCP.y() + 0.04f &&
                !likelySteeringGrip

            if (isStrictThumbUp || isStrictThumbDown) {
                if (now - lastVolumeTickTime > tuning.volumeTickMs) {
                    if (isStrictThumbUp) {
                        emitGesture("Volume_Up", now)
                    } else {
                        emitGesture("Volume_Down", now)
                    }
                    lastVolumeTickTime = now
                }
            } else {
                lastVolumeTickTime = 0L // Instantly reset so the next time it's triggered, it fires immediately
            }

            // --- Pinch Drag Logic ---
            // PURE 2D TRACKING: Since the Volume Circle is gone, we don't have to worry about crosstalk.
            // We can safely drop the strict 3D depth and use a hyper-generous 2D threshold (8% of screen)
            // so your pinch is recognized instantly from ANY angle!
            val dxPinch = thumbTip.x() - indexTip.x()
            val dyPinch = thumbTip.y() - indexTip.y()
            val pinchDist2D = Math.sqrt((dxPinch * dxPinch + dyPinch * dyPinch).toDouble()).toFloat()

            Log.d("DriveSwipe_Vision", "Gesture: $gestureName, Score: $score, PinchDist2D: $pinchDist2D")

            val pinchEngageThreshold = tuning.pinchThreshold * 0.84f
            val pinchReleaseThreshold = maxOf(tuning.pinchReleaseThreshold, pinchEngageThreshold * 1.55f)
            val pinchDepthGap = Math.abs(thumbTip.z() - indexTip.z())
            val pinchSpeed = if (previousPinchDist2D.isFinite()) {
                Math.abs(previousPinchDist2D - pinchDist2D)
            } else {
                0f
            }
            previousPinchDist2D = pinchDist2D

            // Filter out likely false positives:
            // - low confidence hand classifications
            // - classes that explicitly indicate non-pinch postures
            // - fingertip overlap caused by perspective/depth mismatch
            val likelyNonPinchGesture = gestureName == "Open_Palm" ||
                gestureName == "Victory" ||
                gestureName == "Closed_Fist" ||
                gestureName == "Pointing_Up"
            val pinchGatePassed = score >= 0.45f && !likelyNonPinchGesture && pinchDepthGap < 0.09f
            val pinchCandidate = pinchGatePassed && pinchDist2D < pinchEngageThreshold && pinchSpeed < 0.035f

            if (pinchCandidate) {
                pinchCandidateFrames = (pinchCandidateFrames + 1).coerceAtMost(10)
            } else {
                pinchCandidateFrames = 0
            }
            val isPinching = pinchCandidateFrames >= pinchConfirmFrames

            // Cooldown for one-shot gestures, swipes and play/pause
            if (now - lastActionTime >= tuning.actionCooldownMs) {
                // 2. BMW "Two-Finger Point" (Play/Pause)
                if (gestureName == "Victory" && score > 0.40f) {
                    if (lastRecognizedGesture != "Two_Finger_Point") {
                        emitGesture("Two_Finger_Point", now) 
                        lastActionTime = now
                        isTracking = false
                        lastRecognizedGesture = "Two_Finger_Point"
                    }
                }

                // 3. BMW "Pinch & Drag" (Next / Prev Track)
                if (isPinching && !isStrictThumbUp && !isStrictThumbDown) {
                    if (!isTracking) {
                        isTracking = true
                        // HYPER-STABLE TRACKING: Once the pinch engages, we lock the anchor to the WRIST.
                        // The wrist never jitters or flickers like fingertips do when depth is miscalculated.
                        startX = wrist.x() 
                        startY = wrist.y()
                        startTime = now
                        Log.d("DriveSwipe_Vision", "Pinch Engaged! Wrist Anchor set.")
                    }

                }
                
                if (isTracking) {
                    val dx = wrist.x() - startX
                    val dy = wrist.y() - startY
                    val dt = now - startTime
                    
                    Log.d("DriveSwipe_Vision", "Pinch Drag Tracker - dx: $dx, dy: $dy, dt: $dt")
                    
                    // HYPER-RESPONSIVE SWIPE: We drastically loosened the requirements.
                    // It only needs to travel 15% of the screen, and we allow massive 45-degree diagonal slants!
                    if (dt > tuning.swipeTimeoutMs) {
                        isTracking = false // Timed out
                    } else if (
                        dt >= minSwipeAgeMs &&
                        Math.abs(dx) > tuning.swipeThreshold &&
                        Math.abs(dx) > (Math.abs(dy) * horizontalDominanceRatio)
                    ) {
                        if (dx > 0) {
                            emitGesture("Pinch_Drag_Right", now)
                            lastRecognizedGesture = "Pinch_Drag_Right"
                        } else {
                            emitGesture("Pinch_Drag_Left", now)
                            lastRecognizedGesture = "Pinch_Drag_Left"
                        }
                        lastActionTime = now
                        isTracking = false
                        Log.d("DriveSwipe_Vision", "Pinch Drag SUCCESS!")
                    }
                    
                    // Abort tracking only if they massively let go of the pinch (> 15% apart)
                    val releaseCandidate = pinchDist2D > pinchReleaseThreshold || !pinchGatePassed
                    if (releaseCandidate) {
                        releaseCandidateFrames = (releaseCandidateFrames + 1).coerceAtMost(10)
                    } else {
                        releaseCandidateFrames = 0
                    }
                    if (releaseCandidateFrames >= pinchReleaseFrames) {
                        isTracking = false
                        pinchCandidateFrames = 0
                        releaseCandidateFrames = 0
                        Log.d("DriveSwipe_Vision", "Pinch broken! Tracker aborted.")
                    }
                }
            }

            if (gestureName == "None") {
                lastRecognizedGesture = ""
            }

            // Closed Fist → explicit sleep
            // Requires the fist to be held for fistSleepConfirmFrames consecutive frames while
            // the hand is NOT in a horizontal steering-wheel grip orientation.
            val isFistCandidate = gestureName == "Closed_Fist" &&
                score > 0.55f &&
                !nearlyHorizontalHand &&
                isFingerCurled(indexTip, indexPip, indexMcp) &&
                isFingerCurled(middleTip, middlePip, middleMcp) &&
                isFingerCurled(ringTip, ringPip, ringMcp) &&
                isFingerCurled(pinkyTip, pinkyPip, pinkyMcp)

            if (isFistCandidate) {
                fistCandidateFrames = (fistCandidateFrames + 1).coerceAtMost(tuning.fistSleepConfirmFrames + 1)
                if (fistCandidateFrames >= tuning.fistSleepConfirmFrames) {
                    fistCandidateFrames = 0
                    isTracking = false
                    pinchCandidateFrames = 0
                    releaseCandidateFrames = 0
                    previousPinchDist2D = Float.NaN
                    emitGesture("Closed_Fist_Sleep", now)
                    transitionTo(EngineState.IDLE, now)
                    return
                }
            } else {
                fistCandidateFrames = 0
            }

            maybeSleepAfterTimeout(now)
        } else {
            isTracking = false
            lastRecognizedGesture = "" 
            pinchCandidateFrames = 0
            releaseCandidateFrames = 0
            previousPinchDist2D = Float.NaN
            fistCandidateFrames = 0
            handleNoLandmarks(now)
        }
    }

    private fun isFingerCurled(
        tip: NormalizedLandmark,
        pip: NormalizedLandmark,
        mcp: NormalizedLandmark
    ): Boolean {
        return tip.y() > pip.y() && distance(tip, mcp) < distance(pip, mcp) * 1.8f
    }

    private fun validateOpenPalmGeometry(
        landmarks: List<NormalizedLandmark>,
        gestureName: String,
        score: Float
    ): Boolean {
        if (gestureName != "Open_Palm" || score < 0.60f || landmarks.size <= 20) return false

        val wrist = landmarks[0]
        val thumbTip = landmarks[4]
        val indexTip = landmarks[8]
        val indexPip = landmarks[6]
        val indexMcp = landmarks[5]
        val middleTip = landmarks[12]
        val middlePip = landmarks[10]
        val middleMcp = landmarks[9]
        val ringTip = landmarks[16]
        val ringPip = landmarks[14]
        val pinkyTip = landmarks[20]
        val pinkyPip = landmarks[18]

        val fingersExtended = indexTip.y() < indexPip.y() &&
            middleTip.y() < middlePip.y() &&
            ringTip.y() < ringPip.y() &&
            pinkyTip.y() < pinkyPip.y()
        val wristToMiddleAngle = Math.toDegrees(
            Math.atan2(
                (middleMcp.y() - wrist.y()).toDouble(),
                (middleMcp.x() - wrist.x()).toDouble()
            )
        )
        val roughlyUpright = Math.abs(Math.abs(wristToMiddleAngle) - 90.0) < 45.0
        val palmScale = distance(wrist, middleMcp).coerceAtLeast(0.001f)
        val thumbSplayed = distance(thumbTip, indexMcp) > palmScale * 0.55f

        return fingersExtended && roughlyUpright && thumbSplayed
    }

    private fun returnLivestreamError(error: RuntimeException) {
        inferenceInFlight = false
        Log.e("DriveSwipe", "Gesture recognition error", error)
    }

    fun clear() {
        gestureRecognizer?.close()
        gestureRecognizer = null
    }

    interface GestureListener {
        fun onGestureRecognized(gestureName: String)
        fun onEngineStateChanged(state: EngineState)
    }
}
