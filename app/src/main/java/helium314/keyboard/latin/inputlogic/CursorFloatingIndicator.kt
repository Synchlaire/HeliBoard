// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin.inputlogic

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Settings

/**
 * Floating cursor indicator that appears during spacebar cursor control.
 * Displays a smooth, animated indicator showing the current cursor position.
 */
class CursorFloatingIndicator(context: Context) : PopupWindow(context) {

    private val indicatorView: IndicatorView
    private val colors = Settings.getValues().mColors

    init {
        indicatorView = IndicatorView(context)
        contentView = indicatorView

        width = context.resources.getDimensionPixelSize(R.dimen.cursor_indicator_size)
        height = context.resources.getDimensionPixelSize(R.dimen.cursor_indicator_size)

        isOutsideTouchable = false
        isFocusable = false
        isTouchable = false

        setBackgroundDrawable(null)
    }

    /**
     * Show the indicator at the specified screen coordinates
     */
    fun showAt(parent: View, x: Int, y: Int) {
        if (isShowing) {
            update(x - width / 2, y - height, width, height)
        } else {
            showAtLocation(parent, 0, x - width / 2, y - height)
        }
        indicatorView.invalidate()
    }

    /**
     * Custom view that draws the floating cursor indicator
     */
    private inner class IndicatorView(context: Context) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = colors.get(helium314.keyboard.latin.common.ColorType.GESTURE_TRAIL)
        }

        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = context.resources.getDimension(R.dimen.cursor_indicator_stroke_width)
            color = colors.get(helium314.keyboard.latin.common.ColorType.KEY_TEXT)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val centerX = width / 2f
            val centerY = height / 2f
            val radius = (width.coerceAtMost(height) / 2f) - strokePaint.strokeWidth

            // Draw filled circle
            canvas.drawCircle(centerX, centerY, radius, paint)

            // Draw stroke
            canvas.drawCircle(centerX, centerY, radius, strokePaint)

            // Draw vertical line (cursor representation)
            val lineHeight = radius * 0.6f
            canvas.drawLine(
                centerX,
                centerY - lineHeight,
                centerX,
                centerY + lineHeight,
                strokePaint
            )
        }
    }
}
