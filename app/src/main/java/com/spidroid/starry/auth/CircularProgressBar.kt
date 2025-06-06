package com.spidroid.starry.auth

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class CircularProgressBar(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var backgroundPaint: Paint? = null
    private var progressPaint: Paint? = null
    private var rectF: RectF? = null
    private var progress = 0f
    private var progressAnimator: ValueAnimator? = null

    // Customizable attributes
    private val backgroundColor = -0x2e2e2f
    private val progressColor = -0x1
    private val strokeWidth = 4f

    init {
        init()
    }

    private fun init() {
        // Background paint (gray circle)
        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint!!.setColor(backgroundColor)
        backgroundPaint!!.setStyle(Paint.Style.STROKE)
        backgroundPaint!!.setStrokeWidth(strokeWidth)

        // Progress paint (white circle)
        progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        progressPaint!!.setColor(progressColor)
        progressPaint!!.setStyle(Paint.Style.STROKE)
        progressPaint!!.setStrokeWidth(strokeWidth)
        progressPaint!!.setStrokeCap(Paint.Cap.ROUND)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawOval(rectF!!, backgroundPaint!!)
        val angle = 360 * progress / 100
        canvas.drawArc(rectF!!, -90f, angle, false, progressPaint!!)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectF = RectF(strokeWidth / 2, strokeWidth / 2, w - strokeWidth / 2, h - strokeWidth / 2)
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }

    // Optional: Add methods to customize colors
    fun setProgressColor(color: Int) {
        progressPaint!!.setColor(color)
        invalidate()
    }

    override fun setBackgroundColor(color: Int) {
        backgroundPaint!!.setColor(color)
        invalidate()
    }

    fun startIndeterminateAnimation() {
        if (progressAnimator != null && progressAnimator!!.isRunning()) {
            return
        }
        progressAnimator = ValueAnimator.ofFloat(0f, 100f)
        progressAnimator!!.setDuration(1000)
        progressAnimator!!.setRepeatCount(ValueAnimator.INFINITE)
        progressAnimator!!.setInterpolator(LinearInterpolator())
        progressAnimator!!.addUpdateListener(
            AnimatorUpdateListener { animation: ValueAnimator? ->
                val progress = animation!!.getAnimatedValue() as Float
                setProgress(progress)
            })
        progressAnimator!!.start()
    }

    fun stopIndeterminateAnimation() {
        if (progressAnimator != null) {
            progressAnimator!!.cancel()
            progressAnimator = null
        }
    }
}
