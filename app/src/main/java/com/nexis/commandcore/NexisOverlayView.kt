package com.nexis.commandcore

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class NexisOverlayView(
    context: Context,
    private val params: WindowManager.LayoutParams,
    private val onTap: () -> Unit
) : android.view.View(context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 24f * resources.displayMetrics.scaledDensity
        fakeBoldText = true
    }
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var downTime = 0L
    private var moodIndex = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = (System.currentTimeMillis() % 8000L) / 8000f
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val base = kotlin.math.min(w, h) * 0.31f
        val pulse = 1f + 0.08f * sin(t * PI.toFloat() * 2f)
        val r = base * pulse

        paint.shader = RadialGradient(cx, cy, r * 2.8f, intArrayOf(0xAA00E5FF.toInt(), 0x667C4DFF, 0x003A145F), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r * 2.15f, paint)

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = 0xBBFF4FD8.toInt()
        canvas.drawCircle(cx, cy, r * 1.35f, paint)
        paint.color = 0x8800E5FF.toInt()
        canvas.drawCircle(cx, cy, r * 1.72f, paint)

        paint.style = Paint.Style.FILL
        for (i in 0 until 9) {
            val a = (t * PI * 2.0 + i * PI * 2.0 / 9.0).toFloat()
            val orbit = r * (1.45f + (i % 3) * 0.18f)
            paint.color = if (i % 2 == 0) 0xDD00E5FF.toInt() else 0xDDFF4FD8.toInt()
            canvas.drawCircle(cx + cos(a) * orbit, cy + sin(a) * orbit, 4f + (i % 3), paint)
        }

        paint.shader = RadialGradient(cx - r * 0.25f, cy - r * 0.35f, r * 1.2f, intArrayOf(0xFFE9E2FF.toInt(), 0xFF7C4DFF.toInt(), 0xFF130D2B.toInt()), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        paint.color = Color.WHITE
        val eyeY = cy - r * 0.15f
        val eyeDx = r * 0.32f
        canvas.drawCircle(cx - eyeDx, eyeY, r * 0.12f, paint)
        canvas.drawCircle(cx + eyeDx, eyeY, r * 0.12f, paint)
        paint.color = 0xFF080713.toInt()
        canvas.drawCircle(cx - eyeDx, eyeY, r * 0.055f, paint)
        canvas.drawCircle(cx + eyeDx, eyeY, r * 0.055f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = when (moodIndex % 4) {
            0 -> 0xFFFFD166.toInt()
            1 -> 0xFF00E5FF.toInt()
            2 -> 0xFFFF4FD8.toInt()
            else -> 0xFFB8FF5C.toInt()
        }
        val smileTop = cy + r * 0.18f
        canvas.drawArc(cx - r * 0.34f, smileTop, cx + r * 0.34f, smileTop + r * 0.35f, 10f, 160f, false, paint)

        paint.style = Paint.Style.FILL
        canvas.drawText("N", cx, h - 18f, textPaint)
        postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                startX = params.x
                startY = params.y
                downTime = System.currentTimeMillis()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = startX + (event.rawX - downRawX).toInt()
                params.y = startY + (event.rawY - downRawY).toInt()
                wm.updateViewLayout(this, params)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val distance = hypot(event.rawX - downRawX, event.rawY - downRawY)
                val quick = System.currentTimeMillis() - downTime < 260
                if (quick && distance < 24f) {
                    moodIndex++
                    onTap()
                } else {
                    snapToEdge()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
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
                val elapsed = (System.currentTimeMillis() - started).coerceAtMost(260)
                val f = interpolator.getInterpolation(elapsed / 260f)
                params.x = start + (delta * f).toInt()
                runCatching { wm.updateViewLayout(this@NexisOverlayView, params) }
                if (elapsed < 260) postOnAnimation(this)
            }
        })
    }
}
