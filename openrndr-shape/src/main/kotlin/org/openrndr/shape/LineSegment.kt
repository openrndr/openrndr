@file:Suppress("unused")

package org.openrndr.shape

import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.math.map
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Suppress("MemberVisibilityCanBePrivate", "unused")
class LineSegment(val start: Vector2, val end: Vector2) {

    constructor(x0: Double, y0: Double, x1: Double, y1: Double) : this(Vector2(x0, y0), Vector2(x1, y1))

    val direction get() = (end - start)
    val normal get() = (end - start).normalized.perpendicular(YPolarity.CW_NEGATIVE_Y)

    fun nearest(query: Vector2): Vector2 {
        val l2 = end.minus(start).squaredLength
        if (l2 == 0.0) return start

        var t = ((query.x - start.x) * (end.x - start.x) + (query.y - start.y) * (end.y - start.y)) / l2
        t = max(0.0, min(1.0, t))
        return Vector2(start.x + t * (end.x - start.x),
                start.y + t * (end.y - start.y))
    }

    fun distance(query: Vector2): Double {
        val l2 = end.minus(start).squaredLength
        if (l2 == 0.0) return query.minus(start).length

        var t = ((query.x - start.x) * (end.x - start.x) + (query.y - start.y) * (end.y - start.y)) / l2
        t = max(0.0, min(1.0, t))
        return (query - Vector2(start.x + t * (end.x - start.x),
                start.y + t * (end.y - start.y))).length
    }

    fun endingAtY(y: Double): LineSegment {
        val dy = end.y - start.y
        val y0 = y - start.y
        val t = y0 / dy
        return LineSegment(start, start + (end - start) * t)
    }

    fun startingAtY(y: Double): LineSegment {
        val dy = end.y - start.y
        val y0 = y - start.y
        val t = y0 / dy
        return LineSegment(start + (end - start) * t, end)
    }

    fun startingAtX(x: Double): LineSegment {
        val dx = end.x - start.x
        val x0 = x - start.x
        val t = x0 / dx
        return LineSegment(start + (end - start) * t, end)
    }

    fun endingAtX(x: Double): LineSegment {
        val dx = end.x - start.x
        val x0 = x - start.x
        val t = x0 / dx
        return LineSegment(start, start + (end - start) * t)
    }

    fun endingAtNearest(x: Double, y: Double): LineSegment {
        val dx = end.x - start.x
        val x0 = x - start.x
        val tx = x0 / dx

        val dy = end.y - start.y
        val y0 = y - start.y
        val ty = y0 / dy

        return if (tx == tx && tx < ty) {
            LineSegment(start, start + (end - start) * tx)
        } else {
            LineSegment(start, start + (end - start) * ty)
        }
    }

    fun sub(t0: Double, t1: Double): LineSegment {
        var z0 = t0
        var z1 = t1

        if (t0 > t1) {
            z1 = t0
            z0 = t1
        }
        return when {
            z0 == 0.0 -> split(z1)[0]
            z1 == 1.0 -> split(z0)[1]
            else -> split(z0)[1].split(map(z0, 1.0, 0.0, 1.0, z1))[0]
        }
    }

    fun split(t: Double): Array<LineSegment> {
        val u = t.coerceIn(0.0, 1.0)
        val cut = start + (end.minus(start) * u)
        return arrayOf(LineSegment(start, cut), LineSegment(cut, end))
    }

    fun position(t: Double) = start + (end.minus(start) * t)

    val contour: ShapeContour
        get() = ShapeContour.fromPoints(listOf(start, end), false, YPolarity.CW_NEGATIVE_Y)

    val shape: Shape
        get() = Shape(listOf(contour))
}

fun intersection(a: LineSegment, b: LineSegment, eps: Double = 0.0): Vector2 =
        intersection(a.start, a.end, b.start, b.end, eps)

fun intersection(a0: Vector2, a1: Vector2, b0: Vector2, b1: Vector2, eps: Double = 0.0): Vector2 {
    val x0 = a0.x
    val x1 = a1.x
    val x2 = b0.x
    val x3 = b1.x

    val y0 = a0.y
    val y1 = a1.y
    val y2 = b0.y
    val y3 = b1.y

    val den = (x0 - x1) * (y2 - y3) - (y0 - y1) * (x2 - x3)

    return if (abs(den) > 10E-6) {
        val px = ((x0 * y1 - y0 * x1) * (x2 - x3) - (x0 - x1) * (x2 * y3 - y2 * x3)) / den
        val py = ((x0 * y1 - y0 * x1) * (y2 - y3) - (y0 - y1) * (x2 * y3 - y2 * x3)) / den

        val s = (-(y1 - y0) * (x0 - x2) + (x1 - x0) * (y0 - y2)) / den
        val t = ((x3 - x2) * (y0 - y2) - (y3 - y2) * (x0 - x2)) / den

        if (t >= 0 - eps && t <= 1 + eps && s >= 0 - eps && s <= 1 + eps) {
            Vector2(px, py)
        } else {
            Vector2.INFINITY
        }
    } else {
        Vector2.INFINITY
    }
}