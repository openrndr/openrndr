package org.openrndr.shape.internal

import org.openrndr.math.Vector2
import org.openrndr.math.derivative
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import kotlin.math.ceil
import kotlin.math.pow

/**
 * Fraction of the tolerance spent on approximating a cubic by quadratics, the remainder is spent on
 * flattening those quadratics.
 */
private const val CUBIC_TO_QUADRATIC_FRACTION = 0.1

/**
 * Upper bound on the number of quadratics a cubic is split into, guards against (near) zero tolerances.
 */
private const val MAX_QUADRATICS = 256

internal fun flattenCubicTs(segment: Segment2D, tolerance: Double): List<Double> {
    val toleranceC = tolerance * CUBIC_TO_QUADRATIC_FRACTION
    val toleranceQ = tolerance - toleranceC
    val count = segment.numQuadratics(toleranceC)
    val result = ArrayList<Double>()
    var p0 = segment.start
    var d0 = derivative(segment.start, segment.control[0], segment.control[1], segment.end, 0.0)
    for (i in 0 until count) {
        val t0 = i.toDouble() / count
        val t1 = (i + 1.0) / count
        val p3 = if (i == count - 1) segment.end else segment.position(t1)
        val d3 = derivative(segment.start, segment.control[0], segment.control[1], segment.end, t1)
        // control point of the quadratic approximating the cubic span [t0, t1], see [Segment2D.quadratic]
        val s = (t1 - t0) / 3.0
        val c = ((p0 + d0 * s) * 3.0 + (p3 - d3 * s) * 3.0 - p0 - p3) * 0.25
        quadBezFlattenTs(p0, c, p3, toleranceQ, t0, t1, result, includeStart = i == 0)
        p0 = p3
        d0 = d3
    }
    return result
}

internal fun flattenCubicTs(segment: Segment3D, tolerance: Double): List<Double> {
    val toleranceC = tolerance * CUBIC_TO_QUADRATIC_FRACTION
    val toleranceQ = tolerance - toleranceC
    val count = segment.numQuadratics(toleranceC)
    val result = ArrayList<Double>()
    var p0 = segment.start
    var d0 = derivative(segment.start, segment.control[0], segment.control[1], segment.end, 0.0)
    for (i in 0 until count) {
        val t0 = i.toDouble() / count
        val t1 = (i + 1.0) / count
        val p3 = if (i == count - 1) segment.end else segment.position(t1)
        val d3 = derivative(segment.start, segment.control[0], segment.control[1], segment.end, t1)
        // control point of the quadratic approximating the cubic span [t0, t1], see [Segment3D.quadratic]
        val s = (t1 - t0) / 3.0
        val c = ((p0 + d0 * s) * 3.0 + (p3 - d3 * s) * 3.0 - p0 - p3) * 0.25
        val (c2, e2) = quadBez3dTo2d(p0, c, p3)
        quadBezFlattenTs(Vector2.ZERO, c2, e2, toleranceQ, t0, t1, result, includeStart = i == 0)
        p0 = p3
        d0 = d3
    }
    return result
}

internal fun Segment2D.toQuadratics(tolerance: Double): List<Segment2D> {
    val count = this.numQuadratics(tolerance)
    return List(count) { sub(it.toDouble() / count, (it + 1.0) / count).quadratic }
}

internal fun Segment3D.toQuadratics(tolerance: Double): List<Segment3D> {
    val count = this.numQuadratics(tolerance)
    return List(count) { sub(it.toDouble() / count, (it + 1.0) / count).quadratic }
}

internal fun Segment2D.cubicError(): Double {
    val x = start.x - 3.0 * control[0].x + 3.0 * control[1].x - end.x
    val y = start.y - 3.0 * control[0].y + 3.0 * control[1].y - end.y
    return x * x + y * y
}

internal fun Segment3D.cubicError(): Double {
    val x = start.x - 3.0 * control[0].x + 3.0 * control[1].x - end.x
    val y = start.y - 3.0 * control[0].y + 3.0 * control[1].y - end.y
    val z = start.z - 3.0 * control[0].z + 3.0 * control[1].z - end.z
    return x * x + y * y + z * z
}

private fun numQuadratics(cubicError: Double, tolerance: Double): Int {
    val result = ceil((cubicError / (432.0 * tolerance * tolerance)).pow(1.0 / 6.0))
    return if (result.isNaN()) 1 else result.coerceIn(1.0, MAX_QUADRATICS.toDouble()).toInt()
}

internal fun Segment2D.numQuadratics(tolerance: Double): Int =
    if (control.size == 2) numQuadratics(cubicError(), tolerance) else 1

internal fun Segment3D.numQuadratics(tolerance: Double): Int =
    if (control.size == 2) numQuadratics(cubicError(), tolerance) else 1
