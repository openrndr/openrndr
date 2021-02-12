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
 * A strictly linear 2D segment.
 *
 * Think of [LineSegment] as a more limited representation of the [Segment], .
 * While both [LineSegment] and [Segment] are capable of describing straight lines, [LineSegment] is only capable of dealing with straight lines
 * you'd generally only use [LineSegment] if you strictly
 * want to work with problems in the linear segment domain.
 *
 * [LineSegment]s are easy to extend in length
 * thanks to their simple two-point construction.
 *
 * @param start Start of the line segment.
 * @param end End of the line segment.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
data class LineSegment(val start: Vector2, val end: Vector2) : LinearType<LineSegment> {
    constructor(x0: Double, y0: Double, x1: Double, y1: Double) : this(Vector2(x0, y0), Vector2(x1, y1))

    /** Direction of the line segment */
    val direction get() = (end - start)

    /** The normal of the line segment, a unit vector. */
    val normal get() = (end - start).normalized.perpendicular(YPolarity.CW_NEGATIVE_Y)

    /**
     * Finds point on the [LineSegment] that is nearest to the given point.
     * @param query The query point.
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
     * Finds the shortest distance to the [LineSegment] from given point.
     * @param query The query point.
     */
    fun distance(query: Vector2): Double = nearest(query).distanceTo(query)

    /**
     * Calculates a new subsegment between two [t](https://pomax.github.io/bezierinfo/#explanation)
     * values of the [LineSegment].
     * @param t0 The [t](https://pomax.github.io/bezierinfo/#explanation) value marking the start of the subsegment, between `0.0` and `1.0`.
     * @param t1 The [t](https://pomax.github.io/bezierinfo/#explanation) value marking the end of the subsegment, between `0.0` and `1.0`.
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
     * Splits line segment at given [t](https://pomax.github.io/bezierinfo/#explanation) value.
     * @param t The [t](https://pomax.github.io/bezierinfo/#explanation) value between `0.0` and `1.0`.
     */
    fun split(t: Double): Array<LineSegment> {
        val u = t.coerceIn(0.0, 1.0)
        val cut = start + (end.minus(start) * u)
        return arrayOf(LineSegment(start, cut), LineSegment(cut, end))
    }

    /**
     * Calculates [Vector2] position at given [t](https://pomax.github.io/bezierinfo/#explanation) value.
     * @param t The [t](https://pomax.github.io/bezierinfo/#explanation) value between `0.0` and `1.0`.
     */
    fun position(t: Double) = start + (end.minus(start) * t)

    /**
     * Rotates the [LineSegment] around a point on the segment.
     * @param degrees The rotation in degrees.
     * @param t The [t](https://pomax.github.io/bezierinfo/#explanation) value
     *      of the point on the segment to rotate around, default is 0.5 (mid-point).
     */
    fun rotate(degrees: Double, t: Double = 0.5): LineSegment {
        val anchorPoint = end.mix(start, t.coerceIn(0.0, 1.0))

        return LineSegment(
                start.rotate(degrees, anchorPoint),
                end.rotate(degrees, anchorPoint)
        )
    }

    /** Extends the length of the segment by given multiplier. */
    fun extend(times: Double): LineSegment {
        return LineSegment(start - direction * times, end + direction * times)
    }

    /** Returns [Segment] representation of the [LineSegment]. */
    val segment: Segment
        get() = Segment(start, end)

    /** Returns [ShapeContour] representation of the [LineSegment]. */
    val contour: ShapeContour
        get() = ShapeContour.fromPoints(listOf(start, end), false, YPolarity.CW_NEGATIVE_Y)

    /** Returns [Shape] representation of the [LineSegment]. */
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
 * Finds the intersection point between two [LineSegment]s.
 * @param a The first line segment.
 * @param b The second line segment.
 * @param eps How far outside the [t](https://pomax.github.io/bezierinfo/#explanation) value are intersections considered.
 */
@JvmOverloads
fun intersection(a: LineSegment, b: LineSegment, eps: Double = 0.0): Vector2 =
        intersection(a.start, a.end, b.start, b.end, eps)

/**
 * Finds the intersection point between two [LineSegment]s.
 * @param a0 The start of the first line segment.
 * @param a1 The end of the first line segment.
 * @param b0 The start of the second line segment.
 * @param b1 The end of the second line segment.
 * @param eps How far outside the [t](https://pomax.github.io/bezierinfo/#explanation) value are intersections considered.
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