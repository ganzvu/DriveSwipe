package com.example.driveswipe

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator

class StatusOverlayView(context: Context) : View(context) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#E6081425") // Deep navy with 90% opacity (glassmorphism look)
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#3357F1DB") // Teal accent outline with 20% opacity
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D8E3FB") // TextPrimary
        style = Paint.Style.FILL
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var dotColor = COLOR_IDLE
    private var pulseAnimator: ValueAnimator? = null
    private var transitionAnimator: ValueAnimator? = null

    private var currentWidthDp = NEUTRAL_SIZE_DP
    private var currentHeightDp = NEUTRAL_SIZE_DP
    private var expanded = false
    private var displayText = ""
    private val collapseRunnable = Runnable { collapse() }

    init {
        borderPaint.strokeWidth = dpToPx(1f)
        textPaint.textSize = dpToPx(13f)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }

    fun setState(state: EngineState) {
        if (expanded) return // Keep expanded visible during gesture animations
        pulseAnimator?.cancel()
        pulseAnimator = null
        alpha = 1f

        when (state) {
            EngineState.IDLE -> {
                dotColor = COLOR_IDLE
                alpha = ALPHA_IDLE
                invalidate()
            }
            EngineState.ALERTING -> {
                dotColor = COLOR_ALERTING
                pulseAnimator = ValueAnimator.ofFloat(0.35f, 1f).apply {
                    duration = 600
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    addUpdateListener { animator ->
                        this@StatusOverlayView.alpha = animator.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
            EngineState.ACTIVE -> {
                dotColor = COLOR_ACTIVE
                alpha = 1f
                invalidate()
            }
        }
    }

    fun showGestureConfirmation(actionName: String, durationMs: Long) {
        pulseAnimator?.cancel()
        transitionAnimator?.cancel()
        removeCallbacks(collapseRunnable)
        
        expanded = true
        displayText = actionName.replace('_', ' ')

        // Measure text and calculate dynamic width
        val density = resources.displayMetrics.density
        val cornerRadiusPx = dpToPx(EXPANDED_HEIGHT_DP / 2f)
        val dotRadiusPx = dpToPx(4f)
        val mainTextWidth = textPaint.measureText(displayText)
        
        val totalWidthPx = cornerRadiusPx + 
                dotRadiusPx + 
                dpToPx(8f) + 
                mainTextWidth + 
                cornerRadiusPx
                
        val targetWidthDp = (totalWidthPx / density).coerceAtLeast(EXPANDED_WIDTH_DP)
        
        animateLayout(targetWidthDp, EXPANDED_HEIGHT_DP) {
            postDelayed(collapseRunnable, durationMs)
        }
    }

    private fun collapse() {
        expanded = false
        animateLayout(NEUTRAL_SIZE_DP, NEUTRAL_SIZE_DP) {
            // Restore proper status color state
            invalidate()
        }
    }

    private fun animateLayout(targetWidth: Float, targetHeight: Float, onEnd: () -> Unit) {
        val startW = currentWidthDp
        val startH = currentHeightDp

        transitionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                currentWidthDp = startW + (targetWidth - startW) * fraction
                currentHeightDp = startH + (targetHeight - startH) * fraction
                updateWindowLayout()
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    private fun updateWindowLayout() {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        params.width = dpToPx(currentWidthDp).toInt()
        params.height = dpToPx(currentHeightDp).toInt()
        runCatching { wm.updateViewLayout(this, params) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cornerRadius = h / 2f

        // Draw glassmorphic container background
        canvas.drawRoundRect(0f, 0f, w, h, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(0f, 0f, w, h, cornerRadius, cornerRadius, borderPaint)

        if (expanded) {
            // Active gesture text HUD
            val dotRadius = dpToPx(4f)
            val leftMargin = cornerRadius

            // 1. Draw white neon glowing status dot
            // Outer glow layer
            dotPaint.color = Color.argb(100, 255, 255, 255)
            canvas.drawCircle(leftMargin, h / 2f, dotRadius + dpToPx(3f), dotPaint)
            
            // Solid core
            dotPaint.color = Color.WHITE
            canvas.drawCircle(leftMargin, h / 2f, dotRadius, dotPaint)

            // 2. Draw Main Action Text
            val textX = leftMargin + dotRadius + dpToPx(8f)
            val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(displayText, textX, textY, textPaint)
        } else {
            // Draw Neutral dot in center
            dotPaint.color = dotColor
            val prevAlpha = dotPaint.alpha
            dotPaint.alpha = (alpha * 255).toInt()
            
            canvas.drawCircle(w / 2f, h / 2f, dpToPx(4f), dotPaint)
            
            dotPaint.alpha = prevAlpha
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(collapseRunnable)
        pulseAnimator?.cancel()
        transitionAnimator?.cancel()
        pulseAnimator = null
        transitionAnimator = null
    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return super.onTouchEvent(event)
        val params = layoutParams as? WindowManager.LayoutParams ?: return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    // Since gravity is TOP or END (right-aligned),
                    // moving left (negative dx) increases x (distance from right),
                    // and moving right (positive dx) decreases x.
                    params.x = initialX - dx.toInt()
                    params.y = initialY + dy.toInt()

                    // Bound within screen dimensions
                    val displayMetrics = resources.displayMetrics
                    params.x = params.x.coerceIn(0, displayMetrics.widthPixels - width)
                    params.y = params.y.coerceIn(0, displayMetrics.heightPixels - height)

                    runCatching { wm.updateViewLayout(this, params) }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private val COLOR_IDLE = Color.parseColor("#80888888")
        private val COLOR_ALERTING = Color.parseColor("#FFFF9100")
        private val COLOR_ACTIVE = Color.parseColor("#FF57F1DB")
        private const val ALPHA_IDLE = 0.45f

        const val NEUTRAL_SIZE_DP = 28f
        const val EXPANDED_WIDTH_DP = 220f
        const val EXPANDED_HEIGHT_DP = 36f
        
        // Expose size to service initialization
        const val DOT_SIZE_DP = NEUTRAL_SIZE_DP.toInt()
    }
}
