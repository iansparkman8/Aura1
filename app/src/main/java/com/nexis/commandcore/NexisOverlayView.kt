package com.nexis.commandcore

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class NexisOverlayView(
    context: Context,
    private val params: WindowManager.LayoutParams,
    private val onTap: () -> Unit
) : android.view.View(context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trail = ArrayList<Pair<Float, Float>>()

    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var downTime = 0L
    private var moodIndex = 0
    private var longPressed = false

    private val moodColors = intArrayOf(
        0xFF00E5FF.toInt(),
        0xFFFF4FD8.toInt(),
        0xFFFFD166.toInt(),
        0xFFB8FF5C.toInt(),
        0xFF8EA7FF.toInt()
    )

    private val longPressRunnable = Runnable {
        longPressed = true
        moodIndex = (moodIndex + 1) % moodColors.size
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.currentTimeMillis()
        val t = (now % 7000L) / 7000f
        val fast = (now % 1600L) / 1600f
        val blinkCycle = now % 3600L
        val blink = if (blinkCycle > 3300L) 0.16f else 1f

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val base = min(w, h) * 0.285f
        val pulse = 1f + 0.075f * sin(fast * PI.toFloat() * 2f)
        val r = base * pulse
        val mood = moodColors[moodIndex % moodColors.size]

        drawTrail(canvas, r)

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(cx, cy, r * 3.25f, intArrayOf(0xAA00E5FF.toInt(), 0x667C4DFF, 0x003A145F), floatArrayOf(0f, 0.48f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r * 2.55f, paint)
        paint.shader = null

        paint.color = 0x2236E7FF
        canvas.drawOval(RectF(cx - r * 2.65f, cy - r * 0.95f, cx - r * 0.25f, cy + r * 1.05f), paint)
        paint.color = 0x22FF4FD8
        canvas.drawOval(RectF(cx + r * 0.25f, cy - r * 0.95f, cx + r * 2.65f, cy + r * 1.05f), paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4.5f
        paint.color = mood
        canvas.drawCircle(cx, cy, r * 1.42f, paint)
        paint.strokeWidth = 2.6f
        paint.color = 0x9900E5FF.toInt()
        canvas.drawCircle(cx, cy, r * 1.86f, paint)
        paint.color = 0x77FF4FD8
        canvas.drawCircle(cx, cy, r * 2.18f, paint)

        paint.style = Paint.Style.FILL
        for (i in 0 until 14) {
            val dir = if (i % 2 == 0) 1f else -1f
            val a = (t * PI * 2.0 * dir + i * PI * 2.0 / 14.0).toFloat()
            val orbit = r * (1.48f + (i % 4) * 0.15f)
            paint.color = when (i % 4) {
                0 -> 0xFFFFD166.toInt()
                1 -> 0xDD00E5FF.toInt()
                2 -> 0xDDFF4FD8.toInt()
                else -> 0xCCB8FF5C.toInt()
            }
            canvas.drawCircle(cx + cos(a) * orbit, cy + sin(a) * orbit, 3.2f + (i % 3), paint)
        }

        paint.shader = RadialGradient(cx - r * 0.28f, cy - r * 0.42f, r * 1.25f, intArrayOf(Color.WHITE, 0xFFEDE7FF.toInt(), 0xFF7C4DFF.toInt(), 0xFF130D2B.toInt()), floatArrayOf(0f, 0.34f, 0.68f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        paint.color = 0xBBFFFFFF.toInt()
        canvas.drawCircle(cx - r * 0.34f, cy - r * 0.44f, r * 0.17f, paint)

        val eyeY = cy - r * 0.15f
        val eyeDx = r * 0.34f
        val eyeH = (r * 0.13f * blink).coerceAtLeast(2.2f)

        paint.color = Color.WHITE
        canvas.drawOval(RectF(cx - eyeDx - r * 0.13f, eyeY - eyeH, cx - eyeDx + r * 0.13f, eyeY + eyeH), paint)
        canvas.drawOval(RectF(cx + eyeDx - r * 0.13f, eyeY - eyeH, cx + eyeDx + r * 0.13f, eyeY + eyeH), paint)

        paint.color = 0xFF080713.toInt()
        canvas.drawCircle(cx - eyeDx, eyeY, r * 0.052f, paint)
        canvas.drawCircle(cx + eyeDx, eyeY, r * 0.052f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5.2f
        paint.color = mood
        val mouthTop = cy + r * 0.17f
        val mouthRect = RectF(cx - r * 0.35f, mouthTop, cx + r * 0.35f, mouthTop + r * 0.34f)
        when (moodIndex % moodColors.size) {
            0 -> canvas.drawArc(mouthRect, 12f, 156f, false, paint)
            1 -> canvas.drawArc(mouthRect, 0f, 180f, false, paint)
            2 -> canvas.drawOval(RectF(cx - r * 0.16f, cy + r * 0.25f, cx + r * 0.16f, cy + r * 0.42f), paint)
            3 -> canvas.drawLine(cx - r * 0.25f, cy + r * 0.31f, cx + r * 0.25f, cy + r * 0.31f, paint)
            else -> canvas.drawArc(mouthRect, 22f, 136f, false, paint)
        }

        paint.strokeWidth = 3.2f
        paint.color = 0xFFFFD166.toInt()
        canvas.drawLine(cx - r * 0.35f, cy - r * 0.97f, cx - r * 0.18f, cy - r * 1.29f, paint)
        canvas.drawLine(cx, cy - r * 1.03f, cx, cy - r * 1.42f, paint)
        canvas.drawLine(cx + r * 0.35f, cy - r * 0.97f, cx + r * 0.18f, cy - r * 1.29f, paint)

        paint.style = Paint.Style.FILL
        paint.color = 0xFFFFF1A6.toInt()
        canvas.drawCircle(cx, cy - r * 1.45f, r * 0.055f, paint)

        postInvalidateOnAnimation()
    }

    private fun drawTrail(canvas: Canvas, r: Float) {
        if (trail.isEmpty()) return
        paint.shader = null
        paint.style = Paint.Style.FILL
        trail.forEachIndexed { index, point ->
            val alpha = ((index + 1) * 150 / trail.size).coerceIn(20, 150)
            paint.color = Color.argb(alpha, 0, 229, 255)
            canvas.drawCircle(point.first, point.second, r * 0.08f * ((index + 1f) / trail.size), paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                startX = params.x
                startY = params.y
                downTime = System.currentTimeMillis()
                longPressed = false
                postDelayed(longPressRunnable, 520)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val distance = hypot(event.rawX - downRawX, event.rawY - downRawY)
                if (distance > 18f) removeCallbacks(longPressRunnable)
                params.x = startX + (event.rawX - downRawX).toInt()
                params.y = startY + (event.rawY - downRawY).toInt()
                clampToScreen()
                trail.add(width / 2f to height / 2f)
                if (trail.size > 10) trail.removeAt(0)
                runCatching { wm.updateViewLayout(this, params) }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
                val distance = hypot(event.rawX - downRawX, event.rawY - downRawY)
                val quick = System.currentTimeMillis() - downTime < 260

                if (longPressed) {
                    snapToEdge()
                    return true
                }

                if (quick && distance < 24f) onTap() else snapToEdge()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun clampToScreen() {
        val metrics = resources.displayMetrics
        params.x = params.x.coerceIn(0, metrics.widthPixels - width)
        params.y = params.y.coerceIn(0, metrics.heightPixels - height)
    }

    private fun snapToEdge() {
        val metrics = resources.displayMetrics
        val targetX = if (params.x + width / 2 < metrics.widthPixels / 2) 16 else metrics.widthPixels - width - 16
        val start = params.x
        val delta = targetX - start
        val interpolator = DecelerateInterpolator()
        val started = System.currentTimeMillis()

        post(object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - started).coerceAtMost(280)
                val f = interpolator.getInterpolation(elapsed / 280f)
                params.x = start + (delta * f).toInt()
                clampToScreen()
                runCatching { wm.updateViewLayout(this@NexisOverlayView, params) }
                if (elapsed < 280) postOnAnimation(this)
            }
        })
    }
}
