package com.hsissa.zentra.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.hsissa.zentra.service.AppCategory

class CategoryPieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 36f
        strokeCap = Paint.Cap.ROUND
    }

    private val bounds = RectF()
    private var productiveShare = 0f
    private var neutralShare = 0f
    private var distractingShare = 0f

    fun setData(productiveMs: Long, neutralMs: Long, distractingMs: Long) {
        val total = (productiveMs + neutralMs + distractingMs).toFloat()
        if (total > 0) {
            productiveShare = (productiveMs / total) * 360f
            neutralShare = (neutralMs / total) * 360f
            distractingShare = (distractingMs / total) * 360f
        } else {
            productiveShare = 120f
            neutralShare = 120f
            distractingShare = 120f
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = paint.strokeWidth / 2 + 16f
        bounds.set(padding, padding, width - padding, height - padding)

        var startAngle = -90f

        // Productive (Green)
        paint.color = Color.parseColor("#4CAF50")
        canvas.drawArc(bounds, startAngle, productiveShare, false, paint)
        startAngle += productiveShare

        // Neutral (Blue/Grey)
        paint.color = Color.parseColor("#607D8B")
        canvas.drawArc(bounds, startAngle, neutralShare, false, paint)
        startAngle += neutralShare

        // Distracting (Orange/Red)
        paint.color = Color.parseColor("#FF5252")
        canvas.drawArc(bounds, startAngle, distractingShare, false, paint)
    }
}
