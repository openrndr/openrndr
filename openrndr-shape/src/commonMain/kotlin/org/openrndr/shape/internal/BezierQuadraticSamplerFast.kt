package org.openrndr.shape.internal

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.bezier
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Segment3D
import kotlin.jvm.JvmRecord
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

/*
Based on https://minus-ze.ro/posts/flattening-bezier-curves-and-arcs/
 */

internal fun approxIntegral(x: Double): Double {
    val d = 0.67
    return x / (1 - d + (d.pow(4) + 0.25 * x * x).pow(0.25))
}

internal fun approxInvIntegral(x: Double): Double {
    val b = 0.39
    return x * (1 - b + sqrt(b * b + 0.25 * x * x))
}

@JvmRecord
data class ParabolaBasic(val x0: Double, val x2: Double, val scale: Double, val cross: Double)

internal fun quadBez3dTo2d(q: Segment3D): Segment2D {
    val v0 = q.control[0] - q.start
    val v1 = q.end - q.start
    val n = v0.cross(v1).normalized

    val u = v0.normalized
    val w = n.cross(u).normalized

    return Segment2D(
        Vector2.ZERO,
        Vector2(v0.dot(u), v0.dot(w)),
        Vector2(v1.dot(u), v1.dot(w)),
    )

}

internal fun quadBezMapToBasic(q: Segment2D): ParabolaBasic {
    val p0 = q.start
    val p1 = q.control[0]
    val p2 = q.end
    val dd = p1 * 2.0 - p0 - p2
    val u0 = (p1.x - p0.x) * dd.x + (p1.y - p0.y) * dd.y
    val u2 = (p2.x - p1.x) * dd.x + (p2.y - p1.y) * dd.y
    val cross = (p2.x - p0.x) * dd.y - (p2.y - p0.y) * dd.x
    val x0 = u0 / cross
    val x2 = u2 / cross
    val scale = abs(cross) / (hypot(dd.x, dd.y) * abs(x2 - x0))
    return ParabolaBasic(x0, x2, scale, cross)
}

internal fun quadBezFlatten(q: Segment2D, tolerance: Double, tOffset:Double = 0.0, tScale: Double=1.0 ): List<Double> {
    val params = quadBezMapToBasic(q)
    val a0 = approxIntegral(params.x0)
    val a2 = approxIntegral(params.x2)
    val count = 0.5 * abs(a2 - a0) * sqrt(params.scale / tolerance)
    val n = ceil(count).toInt()

    val p0 = q.start
    val p1 = q.control[0]
    val p2 = q.end
    return if (count != count || n == 0 || n == 1) {
        val div = p0 + p2 - p1 * 2.0
        val t = (p0 - p1) / div
        val ts = ArrayList<Double>(4)
        ts.add(tOffset)
        if (t.x == t.x && t.x > 0 && t.x < 1) {
            ts.add(tOffset + t.x * tScale)
        }
        if (t.y == t.y && t.y > 0 && t.y < 1) {
            if (t.y > ts[ts.lastIndex]) {
                ts.add(tOffset + t.y * tScale)
            }
        }
        ts.add(tOffset + tScale)
        ts
    } else {
        val u0 = approxInvIntegral(a0)
        val u2 = approxInvIntegral(a2)
        //val result = mutableListOf(0.0)
        val result = ArrayList<Double>(n + 1)
        result.add(tOffset)

        for (i in 1 until n) {
            val u = approxInvIntegral(a0 + ((a2 - a0) * i) / n)
            val t = (u - u0) / (u2 - u0)
            result.add(tOffset + t * tScale)
        }
        result.add(tOffset + tScale)
        result
    }
}

 fun flattenQuadratic(q: Segment2D, qref: Segment2D, tolerance: Double, tOffset: Double = 0.0, tScale: Double = 1.0, outputLast: Boolean = true): List<Vector2> =
    if (outputLast) {
        quadBezFlatten(q, tolerance, tOffset, tScale).map {
            qref.position(it)
        }
    } else {
        quadBezFlatten(q, tolerance, tOffset, tScale).dropLast(1).map {
            qref.position(it)
        }
    }

fun flattenQuadraticWithT(q: Segment2D, qref: Segment2D, tolerance: Double, tOffset: Double = 0.0, tScale: Double = 1.0, outputLast: Boolean = true): List<Pair<Vector2, Double>> =
    if (outputLast) {
        quadBezFlatten(q, tolerance, tOffset, tScale).map {
            qref.position(it) to it
        }
    } else {
        quadBezFlatten(q, tolerance, tOffset, tScale).dropLast(1).map {
            qref.position(it) to it
        }
    }




 fun flattenQuadratic(q: Segment3D, qref: Segment3D, tolerance: Double, tOffset: Double = 0.0, tScale: Double = 1.0, outputLast: Boolean = true): List<Vector3> {
    val q2 = quadBez3dTo2d(q)
    return if (outputLast) {
        quadBezFlatten(q2, tolerance, tOffset, tScale).map {
            qref.position(it)
        }
    } else {
        quadBezFlatten(q2, tolerance, tOffset, tScale).dropLast(1).map {
            qref.position(it)
        }
    }
}

fun flattenQuadraticWithT(q: Segment3D, qref: Segment3D, tolerance: Double, tOffset: Double = 0.0, tScale: Double = 1.0, outputLast: Boolean = true): List<Pair<Vector3, Double>> {
    val q2 = quadBez3dTo2d(q)
    return if (outputLast) {
        quadBezFlatten(q2, tolerance, tOffset, tScale).map {
            qref.position(it) to it
        }
    } else {
        quadBezFlatten(q2, tolerance, tOffset, tScale).dropLast(1).map {
            qref.position(it) to it
        }
    }
}


