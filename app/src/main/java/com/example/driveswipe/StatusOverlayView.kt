package com.example.driveswipe

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.animation.LinearInterpolator

class StatusOverlayView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var dotColor = COLOR_IDLE
    private var pulseAnimator: ValueAnimator? = null

    fun setState(state: EngineState) {
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

    fun showGestureConfirmation() {
        pulseAnimator?.cancel()
        val prevColor = dotColor
        val prevAlpha = alpha
        
        dotColor = Color.WHITE
        alpha = 1f
        invalidate()
        
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                if (fraction > 0.7f) {
                    dotColor = prevColor
                    this@StatusOverlayView.alpha = prevAlpha
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = dotColor
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy)
        canvas.drawCircle(cx, cy, radius, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    companion object {
        private val COLOR_IDLE = Color.parseColor("#80888888")
        private val COLOR_ALERTING = Color.parseColor("#FFFFA500")
        private val COLOR_ACTIVE = Color.parseColor("#FF4CAF50")
        private const val ALPHA_IDLE = 0.45f

        const val DOT_SIZE_DP = 18
    }
}
