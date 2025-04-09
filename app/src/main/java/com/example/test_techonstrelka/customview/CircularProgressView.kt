package com.example.test_techonstrelka.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CircularProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#222222")
        style = Paint.Style.STROKE
        strokeWidth = 20f
        isAntiAlias = true
    }

    private val progressPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private var progress = 0f // from 0f to 1f

    fun setProgress(p: Float) {
        progress = p.coerceIn(0f, 1f)
        invalidate()
    }



    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = 40f
        val rect = RectF(padding, padding, width - padding, height - padding)
        canvas.drawArc(rect, 0f, 360f, false, backgroundPaint)
        canvas.drawArc(rect, -90f, 360f * progress, false, progressPaint)
    }
}
