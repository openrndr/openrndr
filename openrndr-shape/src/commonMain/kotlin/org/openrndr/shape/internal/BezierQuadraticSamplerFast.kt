package org.openrndr.shape.internal

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import kotlin.jvm.JvmRecord
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.sqrt

/*
Based on https://minus-ze.ro/posts/flattening-bezier-curves-and-arcs/
and https://raphlinus.github.io/graphics/curves/2019/12/23/flatten-quadbez.html
 */

/**
 * Upper bound on the number of line segments a single quadratic is flattened into, guards against
 * (near) zero tolerances.
 */
private const val MAX_SUBDIVISIONS = 4096

private const val INTEGRAL_D = 0.67
private const val INTEGRAL_D4 = INTEGRAL_D * INTEGRAL_D * INTEGRAL_D * INTEGRAL_D

internal fun approxIntegral(x: Double): Double {
    return x / (1 - INTEGRAL_D + sqrt(sqrt(INTEGRAL_D4 + 0.25 * x * x)))
}

internal fun approxInvIntegral(x: Double): Double {
    val b = 0.39
    return x * (1 - b + sqrt(b * b + 0.25 * x * x))
}

@JvmRecord
internal data class ParabolaBasic(val x0: Double, val x2: Double, val scale: Double, val cross: Double)

/**
 * Maps a 3D quadratic Bézier onto the plane it spans. Collinear (or fully degenerate) curves are
 * mapped onto the x-axis.
 */
internal fun quadBez3dTo2d(q: Segment3D): Segment2D {
    val (c, e) = quadBez3dTo2d(q.start, q.control[0], q.end)
    return Segment2D(Vector2.ZERO, c, e)
}

/**
 * Maps the control and end point of a 3D quadratic Bézier onto the plane it spans, the start point
 * maps to the origin.
 */
internal fun quadBez3dTo2d(p0: Vector3, p1: Vector3, p2: Vector3): Pair<Vector2, Vector2> {
    val v0 = p1 - p0
    val v1 = p2 - p0
    val n = v0.cross(v1)

    return if (n.squaredLength <= 1E-24 * v0.squaredLength * v1.squaredLength) {
        val axis = if (v0.squaredLength >= v1.squaredLength) v0 else v1
        if (axis.squaredLength == 0.0) {
            Vector2.ZERO to Vector2.ZERO
        } else {
            val u = axis.normalized
            Vector2(v0.dot(u), 0.0) to Vector2(v1.dot(u), 0.0)
        }
    } else {
        val u = v0.normalized
        val w = n.normalized.cross(u)
        Vector2(v0.dot(u), v0.dot(w)) to Vector2(v1.dot(u), v1.dot(w))
    }
}

internal fun quadBezMapToBasic(p0: Vector2, p1: Vector2, p2: Vector2): ParabolaBasic {
    val dd = p1 * 2.0 - p0 - p2
    val u0 = (p1.x - p0.x) * dd.x + (p1.y - p0.y) * dd.y
    val u2 = (p2.x - p1.x) * dd.x + (p2.y - p1.y) * dd.y
    val cross = (p2.x - p0.x) * dd.y - (p2.y - p0.y) * dd.x
    val x0 = u0 / cross
    val x2 = u2 / cross
    val scale = abs(cross) / (hypot(dd.x, dd.y) * abs(x2 - x0))
    return ParabolaBasic(x0, x2, scale, cross)
}

/**
 * Appends the parameters at which the quadratic Bézier [p0], [p1], [p2] should be sampled such that the resulting polyline deviates
 * at most [tolerance] from the curve. Parameters are mapped linearly from [0, 1] onto [[t0], [t1]].
 *
 * @param includeStart whether [t0] is appended, pass false when continuing a previous span ending in [t0]
 */
internal fun quadBezFlattenTs(
    p0: Vector2,
    p1: Vector2,
    p2: Vector2,
    tolerance: Double,
    t0: Double,
    t1: Double,
    out: MutableList<Double>,
    includeStart: Boolean = true
) {
    val params = quadBezMapToBasic(p0, p1, p2)
    val sqrtTol = sqrt(tolerance)
    val a0 = approxIntegral(params.x0)
    val a2 = approxIntegral(params.x2)
    val value = if (params.scale.isFinite()) {
        val da = abs(a2 - a0)
        val sqrtScale = sqrt(params.scale)
        if ((params.x0 < 0.0) == (params.x2 < 0.0)) {
            da * sqrtScale
        } else {
            // the span contains the curvature maximum (cusp-like quadratics)
            val xMin = sqrtTol / sqrtScale
            sqrtTol * da / approxIntegral(xMin)
        }
    } else {
        0.0
    }
    val count = (0.5 * value / sqrtTol)
    val n = if (count.isNaN()) 1 else ceil(count).coerceIn(1.0, MAX_SUBDIVISIONS.toDouble()).toInt()

    if (includeStart) {
        out.add(t0)
    }
    if (n == 1) {
        // A collinear quadratic may run past one of its end points and back, the chord would cut off
        // that excursion. Sample the turning point, where the derivative vanishes.
        if (!value.isFinite() || !params.scale.isFinite() || params.cross == 0.0) {
            val d0 = p1 - p0
            val dd = d0 - (p2 - p1)
            val l2 = dd.squaredLength
            if (l2 > 0.0) {
                val t = d0.dot(dd) / l2
                if (t > 0.0 && t < 1.0) {
                    out.add(t0 + t * (t1 - t0))
                }
            }
        }
    } else {
        val u0 = approxInvIntegral(a0)
        val u2 = approxInvIntegral(a2)
        val uScale = 1.0 / (u2 - u0)
        for (i in 1 until n) {
            val u = approxInvIntegral(a0 + ((a2 - a0) * i) / n)
            val t = (u - u0) * uScale
            out.add(t0 + t * (t1 - t0))
        }
    }
    out.add(t1)
}

internal fun flattenQuadraticTs(q: Segment2D, tolerance: Double): List<Double> =
    ArrayList<Double>().also { quadBezFlattenTs(q.start, q.control[0], q.end, tolerance, 0.0, 1.0, it) }

internal fun flattenQuadraticTs(q: Segment3D, tolerance: Double): List<Double> {
    val (c, e) = quadBez3dTo2d(q.start, q.control[0], q.end)
    return ArrayList<Double>().also { quadBezFlattenTs(Vector2.ZERO, c, e, tolerance, 0.0, 1.0, it) }
}
