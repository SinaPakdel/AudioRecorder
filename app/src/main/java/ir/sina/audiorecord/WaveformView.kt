package ir.sina.audiorecord

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class WaveformView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {


    private var paint = Paint()

    private var amplitude = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()
    private val radius = 6f
    private var w = 9f
    private var d = 6f
    private var sw = 0f
    private var sh = 400f
    private var maxSpikes = 0

    init {
        paint.color = Color.rgb(224, 81, 30)
        sw = resources.displayMetrics.widthPixels.toFloat()
        maxSpikes = (sw / (w + d)).toInt()
    }


    fun addAmplitude(amp: Float) {
        amplitude.add(min(amp.toInt() / 7, 400).toFloat())

        spikes.clear()
        val amps = amplitude.takeLast(maxSpikes)
        for (i in amps.indices) {
            val left = sw - i * (w + d)
            val top = sh / 2 - amps[i] / 2
            val right = left + w
            val bottom = top + amps[i]
            spikes.add(RectF(left, top, right, bottom))

        }
        invalidate()
    }

    fun clear(): ArrayList<Float> {
        val amps = amplitude.clone() as ArrayList<Float>
        amplitude.clear()
        spikes.clear()
        invalidate()
        return amps
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        spikes.forEach { canvas.drawRoundRect(it, radius, radius, paint) }
    }
}