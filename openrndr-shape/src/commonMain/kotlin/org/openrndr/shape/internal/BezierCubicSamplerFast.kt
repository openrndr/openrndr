package org.openrndr.shape.internal

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import kotlin.math.ceil
import kotlin.math.pow

fun flattenCubic(segment: Segment2D, toleranceC: Double, toleranceQ: Double): List<Vector2> {
    val quadratics = segment.toQuadratics(toleranceC)
    val tScale = 1.0 / quadratics.size

    return quadratics.flatMapIndexed { index, it ->
        flattenQuadratic(it, segment, toleranceQ, index * tScale, tScale, index == quadratics.lastIndex)
    }
}

fun flattenCubicWithT(segment: Segment2D, toleranceC: Double, toleranceQ: Double): List<Pair<Vector2, Double>> {
    val quadratics = segment.toQuadratics(toleranceC)
    val tScale = 1.0 / quadratics.size

    return quadratics.flatMapIndexed { index, it ->
        flattenQuadraticWithT(
            it,
            segment,
            toleranceQ,
            index * tScale,
            tScale,
            index == quadratics.lastIndex
        )
    }
}


fun flattenCubic(segment: Segment3D, toleranceC: Double, toleranceQ: Double): List<Vector3> {
    val quadratics = segment.toQuadratics(toleranceC)
    val tScale = 1.0 / quadratics.size

    return quadratics.flatMapIndexed { index, it ->
        flattenQuadratic(it, segment, toleranceQ, index * tScale, tScale, index == quadratics.lastIndex)
    }
}

fun flattenCubicWithT(segment: Segment3D, toleranceC: Double, toleranceQ: Double): List<Pair<Vector3, Double>> {
    val quadratics = segment.toQuadratics(toleranceC)
    val tScale = 1.0 / quadratics.size

    return quadratics.flatMapIndexed { index, it ->
        flattenQuadraticWithT(
            it,
            segment,
            toleranceQ,
            index * tScale,
            tScale,
            index == quadratics.lastIndex
        )
    }
}


fun Segment2D.toQuadratics(tolerance: Double): List<Segment2D> {
    val count = this.numQuadratics(tolerance)

    val result = ArrayList<Segment2D>(count)
    for (i in 0 until count) {
        val t0 = i.toDouble() / count
        val t1 = (i + 1.0) / count
        result.add(sub(t0, t1).quadratic)
    }
    return result
}

internal fun Segment3D.toQuadratics(tolerance: Double): List<Segment3D> {
    val count = this.numQuadratics(tolerance)

    val result = ArrayList<Segment3D>(count)
    for (i in 0 until count - 1) {
        val t0 = i.toDouble() / count
        val t1 = (i + 1.0) / count
        result.add(sub(t0, t1))
    }
    return result
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


internal fun Segment2D.numQuadratics(tolerance: Double): Int {
    return if (this.control.size == 2) {
        val x = start.x - 3.0 * control[0].x + 3.0 * control[1].x - end.x
        val y = start.y - 3.0 * control[0].y + 3.0 * control[1].y - end.y
        val err = cubicError()
        val result = err / (432.0 * tolerance * tolerance)
        ceil(result.pow(1.0 / 6.0)).coerceAtLeast(1.0).toInt()
    } else {
        1
    }
}

internal fun Segment3D.numQuadratics(tolerance: Double): Int {
    return if (this.control.size == 2) {
        val x = start.x - 3.0 * control[0].x + 3.0 * control[1].x - end.x
        val y = start.y - 3.0 * control[0].y + 3.0 * control[1].y - end.y
        val z = start.z - 3.0 * control[0].z + 3.0 * control[1].z - end.z
        val err = x * x + y * y + z * z
        val result = err / (432.0 * tolerance * tolerance)
        ceil(result.pow(1.0 / 6.0)).coerceAtLeast(1.0).toInt()
    } else {
        1
    }
}

