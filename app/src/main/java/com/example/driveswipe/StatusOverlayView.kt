package com.example.driveswipe

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
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

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80BACAC5") // Muted TextSecondary
        style = Paint.Style.FILL
        typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL)
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
    private var displaySub = ""

    init {
        borderPaint.strokeWidth = dpToPx(1f)
        textPaint.textSize = dpToPx(12f)
        subTextPaint.textSize = dpToPx(9f)
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

    fun showGestureConfirmation(gestureName: String, actionName: String) {
        pulseAnimator?.cancel()
        transitionAnimator?.cancel()
        
        expanded = true
        displayText = actionName.replace('_', ' ')
        displaySub = gestureName.replace('_', ' ')
        
        animateLayout(EXPANDED_WIDTH_DP, EXPANDED_HEIGHT_DP) {
            // Wait 2 seconds, then collapse
            postDelayed({
                collapse()
            }, 2000)
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

            // 1. Draw pulsing active teal status dot
            dotPaint.color = Color.parseColor("#FF57F1DB")
            canvas.drawCircle(leftMargin, h / 2f, dotRadius, dotPaint)

            // 2. Draw Main Action Text
            val textX = leftMargin + dotRadius + dpToPx(8f)
            val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(displayText, textX, textY, textPaint)

            // 3. Draw Muted Subtext / Gesture (Right)
            if (displaySub.isNotEmpty()) {
                val subX = w - cornerRadius - subTextPaint.measureText(displaySub) - dpToPx(4f)
                val subY = h / 2f - (subTextPaint.descent() + subTextPaint.ascent()) / 2f
                canvas.drawText(displaySub, subX, subY, subTextPaint)
            }
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
        pulseAnimator?.cancel()
        transitionAnimator?.cancel()
        pulseAnimator = null
        transitionAnimator = null
    }

    companion object {
        private val COLOR_IDLE = Color.parseColor("#80888888")
        private val COLOR_ALERTING = Color.parseColor("#FFFF9100")
        private val COLOR_ACTIVE = Color.parseColor("#FF57F1DB")
        private const val ALPHA_IDLE = 0.45f

        const val NEUTRAL_SIZE_DP = 28f
        const val EXPANDED_WIDTH_DP = 180f
        const val EXPANDED_HEIGHT_DP = 36f
        
        // Expose size to service initialization
        const val DOT_SIZE_DP = NEUTRAL_SIZE_DP.toInt()
    }
}
