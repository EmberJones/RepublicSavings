package com.example.republicsavingsapp.ui.rewards

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.republicsavingsapp.fair.Prize
import com.example.republicsavingsapp.fair.PrizeTier
import kotlin.math.min

class WheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Slice(
        val label: String,
        val tier: PrizeTier,
        val color: Int
    )

    private val slices = listOf(
        Slice("Pizza", PrizeTier.SMALL, Color.parseColor("#4F772D")),
        Slice("Movie", PrizeTier.SMALL, Color.parseColor("#FDC500")),
        Slice("Aquarium", PrizeTier.MEDIUM, Color.parseColor("#DDA15E")),
        Slice("Pizza", PrizeTier.SMALL, Color.parseColor("#31572C")),
        Slice("Dinner", PrizeTier.MEDIUM, Color.parseColor("#BC6C25")),
        Slice("Surfing", PrizeTier.LARGE, Color.parseColor("#606C38")),
        Slice("Movie", PrizeTier.SMALL, Color.parseColor("#FDC500")),
        Slice("Sandboard", PrizeTier.LARGE, Color.parseColor("#283618"))
    )

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val centerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#31572C")
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }

    private val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#283618")
        style = Paint.Style.STROKE
        strokeWidth = 16f
    }

    private val wheelRect = RectF()
    private var currentRotationAngle = 0f
    private var isSpinning = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = min(w, h).toFloat()
        val padding = 20f
        val left = (w - size) / 2f + padding
        val top = (h - size) / 2f + padding
        val right = left + size - 2 * padding
        val bottom = top + size - 2 * padding
        wheelRect.set(left, top, right, bottom)

        textPaint.textSize = size * 0.045f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (wheelRect.isEmpty) return

        val centerX = wheelRect.centerX()
        val centerY = wheelRect.centerY()
        val radius = wheelRect.width() / 2f

        val sliceSweep = 360f / slices.size

        canvas.save()
        canvas.rotate(currentRotationAngle, centerX, centerY)

        for (i in slices.indices) {
            val slice = slices[i]
            val startAngle = i * sliceSweep
            slicePaint.color = slice.color
            canvas.drawArc(wheelRect, startAngle, sliceSweep, true, slicePaint)

            canvas.save()
            val sliceCenterAngle = startAngle + sliceSweep / 2f
            canvas.rotate(sliceCenterAngle, centerX, centerY)

            val textRadius = radius * 0.65f
            canvas.drawText(slice.label, centerX + textRadius, centerY + (textPaint.textSize / 3f), textPaint)
            canvas.restore()
        }

        canvas.restore()

        canvas.drawCircle(centerX, centerY, radius, outerBorderPaint)

        val hubRadius = radius * 0.18f
        canvas.drawCircle(centerX, centerY, hubRadius, centerPaint)
        canvas.drawCircle(centerX, centerY, hubRadius, centerBorderPaint)
    }

    fun spinToPrize(prize: Prize, onSpinEnd: () -> Unit) {
        if (isSpinning) return
        isSpinning = true

        val matchingIndices = slices.indices.filter {
            slices[it].tier == prize.tier || slices[it].label.equals(prize.name, ignoreCase = true)
        }
        val targetIndex = if (matchingIndices.isNotEmpty()) matchingIndices.random() else slices.indices.random()

        val sliceSweep = 360f / slices.size
        val sliceCenterAngle = targetIndex * sliceSweep + sliceSweep / 2f
        val desiredEndAngleRelativeToPointer = 270f - sliceCenterAngle

        val currentNormalized = currentRotationAngle % 360f
        var targetRotation = currentRotationAngle - currentNormalized + desiredEndAngleRelativeToPointer + 360f * 5
        if (targetRotation <= currentRotationAngle + 360f * 3) {
            targetRotation += 360f * 2
        }

        val animator = ValueAnimator.ofFloat(currentRotationAngle, targetRotation).apply {
            duration = 3500
            interpolator = DecelerateInterpolator(2.0f)
            addUpdateListener { anim ->
                currentRotationAngle = anim.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isSpinning = false
                    onSpinEnd()
                }
            })
        }
        animator.start()
    }
}
