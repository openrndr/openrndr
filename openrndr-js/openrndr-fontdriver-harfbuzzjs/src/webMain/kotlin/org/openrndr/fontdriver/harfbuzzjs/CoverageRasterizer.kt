package org.openrndr.fontdriver.harfbuzzjs

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Rasterizes outlines to 8-bit coverage by accumulating the signed area of their edges per pixel, after font-rs by Raph
 * Levien (https://github.com/raphlinus/font-rs). The coverage of a pixel is the area of the outline inside it, the
 * absolute winding number saturates at 1.
 *
 * Edges are in pixel coordinates with y pointing down, within [0, width] x [0, height].
 */
internal class CoverageRasterizer(val width: Int, val height: Int) {
    private val accumulation = FloatArray(width * height + 4)
    private var lastX = 0.0
    private var lastY = 0.0
    private var startX = 0.0
    private var startY = 0.0

    fun moveTo(x: Double, y: Double) {
        lastX = x; lastY = y
        startX = x; startY = y
    }

    fun lineTo(x: Double, y: Double) {
        line(lastX, lastY, x, y)
        lastX = x; lastY = y
    }

    fun quadTo(cx: Double, cy: Double, x: Double, y: Double) {
        // the number of segments follows from the deviation of the control point from the chord
        val dx = lastX - 2.0 * cx + x
        val dy = lastY - 2.0 * cy + y
        val segments = 1 + floor(sqrt(sqrt(dx * dx + dy * dy) * 3.0)).toInt()
        val x0 = lastX
        val y0 = lastY
        for (i in 1..segments) {
            val t = i.toDouble() / segments
            val u = 1.0 - t
            lineTo(u * u * x0 + 2.0 * u * t * cx + t * t * x, u * u * y0 + 2.0 * u * t * cy + t * t * y)
        }
    }

    fun cubicTo(c1x: Double, c1y: Double, c2x: Double, c2y: Double, x: Double, y: Double) {
        val d1 = abs(lastX - 2.0 * c1x + c2x) + abs(lastY - 2.0 * c1y + c2y)
        val d2 = abs(c1x - 2.0 * c2x + x) + abs(c1y - 2.0 * c2y + y)
        val segments = 1 + floor(sqrt(max(d1, d2) * 3.0)).toInt()
        val x0 = lastX
        val y0 = lastY
        for (i in 1..segments) {
            val t = i.toDouble() / segments
            val u = 1.0 - t
            val a = u * u * u
            val b = 3.0 * u * u * t
            val c = 3.0 * u * t * t
            val d = t * t * t
            lineTo(a * x0 + b * c1x + c * c2x + d * x, a * y0 + b * c1y + c * c2y + d * y)
        }
    }

    fun close() {
        if (lastX != startX || lastY != startY) {
            lineTo(startX, startY)
        }
    }

    private fun line(x0: Double, y0: Double, x1: Double, y1: Double) {
        if (abs(y0 - y1) <= 1E-9) return
        val direction: Float
        val px0: Double
        val py0: Double
        val px1: Double
        val py1: Double
        if (y0 < y1) {
            direction = 1.0f; px0 = x0; py0 = y0; px1 = x1; py1 = y1
        } else {
            direction = -1.0f; px0 = x1; py0 = y1; px1 = x0; py1 = y0
        }
        val dxdy = (px1 - px0) / (py1 - py0)
        var x = px0
        val firstRow = max(0, floor(py0).toInt())
        if (py0 < 0.0) {
            x -= py0 * dxdy
        }
        val lastRow = min(height, ceil(py1).toInt())
        for (row in firstRow until lastRow) {
            val lineStart = row * width
            val dy = (min((row + 1).toDouble(), py1) - max(row.toDouble(), py0)).toFloat()
            val xNext = x + dxdy * dy
            val d = dy * direction
            val xa = min(x, xNext)
            val xb = max(x, xNext)
            val xaFloor = floor(xa)
            val xai = xaFloor.toInt()
            val xbCeil = ceil(xb)
            val xbi = xbCeil.toInt()
            val index = lineStart + xai
            if (index < 0) {
                x = xNext
                continue
            }
            if (xbi <= xai + 1) {
                val xmf = (0.5 * (x + xNext) - xaFloor).toFloat()
                accumulation[index] += d - d * xmf
                accumulation[index + 1] += d * xmf
            } else {
                val s = (1.0 / (xb - xa)).toFloat()
                val xaf = (xa - xaFloor).toFloat()
                val a0 = 0.5f * s * (1.0f - xaf) * (1.0f - xaf)
                val xbf = (xb - xbCeil + 1.0).toFloat()
                val am = 0.5f * s * xbf * xbf
                accumulation[index] += d * a0
                if (xbi == xai + 2) {
                    accumulation[index + 1] += d * (1.0f - a0 - am)
                } else {
                    val a1 = s * (1.5f - xaf)
                    accumulation[index + 1] += d * (a1 - a0)
                    for (xi in xai + 2 until xbi - 1) {
                        accumulation[lineStart + xi] += d * s
                    }
                    val a2 = a1 + (xbi - xai - 3) * s
                    accumulation[lineStart + xbi - 1] += d * (1.0f - a2 - am)
                }
                accumulation[lineStart + xbi] += d * am
            }
            x = xNext
        }
    }

    /**
     * The coverage of the pixels, row by row, as bytes from 0 to 255
     */
    fun coverage(write: (index: Int, value: Int) -> Unit) {
        var sum = 0.0f
        for (i in 0 until width * height) {
            sum += accumulation[i]
            write(i, (min(abs(sum), 1.0f) * 255.0f + 0.5f).toInt())
        }
    }
}
