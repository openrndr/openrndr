@file:Suppress("unused")

package org.openrndr.shape

import org.openrndr.math.LinearType
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.math.map
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * a 2D line segment
 * @param start start of the line segment
 * @param end end of the line segment
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
data class LineSegment(val start: Vector2, val end: Vector2) : LinearType<LineSegment> {
    constructor(x0: Double, y0: Double, x1: Double, y1: Double) : this(Vector2(x0, y0), Vector2(x1, y1))
    /**
     * direction of the line segment
     */
    val direction get() = (end - start)

    /**
     * normal of the line segment, a unit length vector
     */
    val normal get() = (end - start).normalized.perpendicular(YPolarity.CW_NEGATIVE_Y)

    /**
     * find point on the line segment that is nearest to the query point
     * @param query the query point
     */
    fun nearest(query: Vector2): Vector2 {
        val l2 = end.minus(start).squaredLength
        if (l2 == 0.0) return start

        var t = ((query.x - start.x) * (end.x - start.x) + (query.y - start.y) * (end.y - start.y)) / l2
        t = max(0.0, min(1.0, t))
        return Vector2(start.x + t * (end.x - start.x),
                start.y + t * (end.y - start.y))
    }

    /**
     * find distance between the query point and the point on the line segment that is nearest to the query point
     * @param query the query point
     */
    fun distance(query: Vector2): Double = nearest(query).distanceTo(query)

    /**
     * calculate a sub line segment from t0 to t1
     * @param t0 t-parameter value at start of line segment
     * @param t1 t-parameter value at end of line segment
     */
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

    /**
     * split line segment at t-parameter value
     * @param t the t-parameter value between 0 and 1
     */
    fun split(t: Double): Array<LineSegment> {
        val u = t.coerceIn(0.0, 1.0)
        val cut = start + (end.minus(start) * u)
        return arrayOf(LineSegment(start, cut), LineSegment(cut, end))
    }

    /**
     * calculate point on the line segment
     * @param t t-parameter value between 0 and 1
     */
    fun position(t: Double) = start + (end.minus(start) * t)

    /**
     * rotate line segment around a point on the line segment
     * @param degrees rotation in degrees
     * @param t t-parameter value of point on line segment to rotate around, default is 0.5 (mid-point)
     */
    fun rotate(degrees: Double, t: Double = 0.5): LineSegment {
        val anchorPoint = end.mix(start, t.coerceIn(0.0, 1.0))

        return LineSegment(
                start.rotate(degrees, anchorPoint),
                end.rotate(degrees, anchorPoint)
        )
    }

    fun extend(times: Double): LineSegment {
        return LineSegment(start - direction * times, end + direction * times)
    }

    /**
     * convert line segment to [Segment]
     */
    val segment: Segment
        get() = Segment(start, end)

    /**
     * convert line segment to [ShapeContour]
     */
    val contour: ShapeContour
        get() = ShapeContour.fromPoints(listOf(start, end), false, YPolarity.CW_NEGATIVE_Y)

    /**
     * convert line segment to [Shape]
     */
    val shape: Shape
        get() = Shape(listOf(contour))

    override operator fun times(scale: Double): LineSegment {
        return LineSegment(start * scale, end * scale)
    }

    override operator fun div(scale: Double): LineSegment {
        return LineSegment(start / scale, end / scale)
    }

    override operator fun plus(right: LineSegment): LineSegment {
        return LineSegment(start + right.start, end + right.end)
    }

    override operator fun minus(right: LineSegment): LineSegment {
        return LineSegment(start - right.start, end - right.end)
    }
}

/**
 * find intersection between two line segments
 * @param a the first line segment
 * @param b the second line segment
 * @param eps how far outside the t-parameter range of 0 .. 1 are intersections considered
 */
@JvmOverloads
fun intersection(a: LineSegment, b: LineSegment, eps: Double = 0.0): Vector2 =
        intersection(a.start, a.end, b.start, b.end, eps)

/**
 * find intersection between two line segments
 * @param a0 the start of the first line segment
 * @param a1 the end of the first line segment
 * @param b0 the start of the second line segment
 * @param b1 the end of the second line segment
 * @param eps how far outside the t-parameter range of 0 .. 1 are intersections considered, default is 0.0
 */
@JvmOverloads
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