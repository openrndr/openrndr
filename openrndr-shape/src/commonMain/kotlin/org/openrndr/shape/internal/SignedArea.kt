package org.openrndr.shape.internal

import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.math.sqrt

/** Abscissae of the 3-point Gauss-Legendre rule on [0, 1], exact for the polynomials of degree 5 integrated here */
private val GL3_X = doubleArrayOf(0.5 - 0.5 * sqrt(0.6), 0.5, 0.5 + 0.5 * sqrt(0.6))

/** Weights belonging to [GL3_X] */
private val GL3_W = doubleArrayOf(5.0 / 18.0, 8.0 / 18.0, 5.0 / 18.0)

/**
 * The signed area enclosed by [segments] and the line from the end of the last segment back to the start of the
 * first, the integral of (x dy - y dx) / 2. It is exact for Bézier segments, and positive when the segments turn
 * clockwise on a screen, whose y-axis points down. Coordinates are taken relative to the first point, which keeps
 * the area precise far from the origin.
 */
internal fun signedArea(segments: List<Segment2D>): Double {
    if (segments.isEmpty()) return 0.0
    val origin = segments[0].start
    var sum = 0.0
    for (segment in segments) {
        if (segment.linear) {
            sum += (segment.start - origin).cross(segment.end - origin)
        } else {
            for (i in GL3_X.indices) {
                val t = GL3_X[i]
                val p = segment.position(t) - origin
                sum += GL3_W[i] * p.cross(segment.derivativeExact(t))
            }
        }
    }
    sum += (segments.last().end - origin).cross(segments[0].start - origin)
    return sum * 0.5
}

/** The derivative of the segment, without the substitutions [Segment2D.derivative] makes at degenerate ends */
private fun Segment2D.derivativeExact(t: Double): Vector2 = when (control.size) {
    1 -> org.openrndr.math.derivative(start, control[0], end, t)
    else -> org.openrndr.math.derivative(start, control[0], control[1], end, t)
}
