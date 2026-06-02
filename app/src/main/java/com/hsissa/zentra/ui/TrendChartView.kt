package com.hsissa.zentra.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.hsissa.zentra.R
import com.hsissa.zentra.service.DailyUsageSummary

class TrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<DailyUsageSummary> = emptyList()
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }
    private val barRect = RectF()
    private val cornerRadius = 12f
    private val barSpacing = 24f

    private var selectedIndex = -1
    var onItemSelected: ((DailyUsageSummary?) -> Unit)? = null

    init {
        barPaint.color = ContextCompat.getColor(context, R.color.accent)
        selectedBarPaint.color = ContextCompat.getColor(context, R.color.text_primary)
        textPaint.color = ContextCompat.getColor(context, R.color.text_tertiary)
    }

    fun setData(newData: List<DailyUsageSummary>) {
        this.data = newData
        selectedIndex = -1
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val maxUsage = data.maxOf { it.totalScreenTimeMillis }.coerceAtLeast(1L)
        val widthPerBar = (width - (data.size - 1) * barSpacing) / data.size
        val chartHeight = height - 60f // Leave space for labels

        data.forEachIndexed { index, summary ->
            val barHeight = (summary.totalScreenTimeMillis.toFloat() / maxUsage) * chartHeight
            val left = index * (widthPerBar + barSpacing)
            val right = left + widthPerBar
            val top = chartHeight - barHeight
            val bottom = chartHeight

            barRect.set(left, top, right, bottom)
            
            val paint = if (index == selectedIndex) selectedBarPaint else barPaint
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, paint)

            // Draw day label (simplified)
            val dayLabel = "D${index + 1}"
            canvas.drawText(dayLabel, left + widthPerBar / 2, height - 10f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val widthPerBar = (width - (data.size - 1) * barSpacing) / data.size
            val x = event.x
            val index = (x / (widthPerBar + barSpacing)).toInt().coerceIn(0, data.size - 1)
            
            if (index != selectedIndex) {
                selectedIndex = index
                onItemSelected?.invoke(data[selectedIndex])
                invalidate()
            }
            if (event.action == MotionEvent.ACTION_DOWN) {
                performClick()
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
