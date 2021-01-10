package org.openrndr.math

import kotlin.math.abs
import kotlin.math.pow

private const val almostZero = 0.00000001
private const val almostOne = 0.99999999

/**
 * 1D Catmull-Rom segment
 */
class CatmulRom1(val p0: Double, val p1: Double, val p2: Double, val p3: Double, val alpha: Double = 0.5) {
    val t0: Double = 0.0
    val t1: Double = calculateT(t0, p0, p1)
    val t2: Double = calculateT(t1, p1, p2)
    val t3: Double = calculateT(t2, p2, p3)

    private fun f(x: Double): Double = if (abs(x) < almostZero) 1.0 else x

    /**
     * @param rt segment parameter value in [0, 1]
     * @return a position on the segment
     */
    fun position(rt: Double): Double {
        val t = (t2 - t1) * rt + t1

        val a1 = p0 * ((t1 - t) / f(t1 - t0)) + p1 * ((t - t0) / f(t1 - t0))
        val a2 = p1 * ((t2 - t) / f(t2 - t1)) + p2 * ((t - t1) / f(t2 - t1))
        val a3 = p2 * ((t3 - t) / f(t3 - t2)) + p3 * ((t - t2) / f(t3 - t2))

        val b1 = a1 * ((t2 - t) / f(t2 - t0)) + a2 * ((t - t0) / f(t2 - t0))
        val b2 = a2 * ((t3 - t) / f(t3 - t1)) + a3 * ((t - t1) / f(t3 - t1))

        val c = b1 * ((t2 - t) / f(t2 - t1)) + b2 * ((t - t1) / f(t2 - t1))
        return c
    }

    private fun calculateT(t: Double, p0: Double, p1: Double): Double {
        val a = (p1 - p0).pow(2.0)
        val b = a.pow(0.5)
        val c = b.pow(alpha)
        return c + t
    }
}

/**
 * 1D Catmull-Rom chain
 */

class CatmullRomChain1(points: List<Double>, alpha: Double = 0.5, val loop: Boolean = false) {
    val segments = if (!loop) points.windowed(4, 1).map {
        CatmulRom1(it[0], it[1], it[2], it[3], alpha)
    } else {
        val cleanPoints = if (loop && abs(points.first() - (points.last())) <= 1.0E-6) {
            points.dropLast(1)
        } else {
            points
        }
        (cleanPoints + cleanPoints.take(3)).windowed(4, 1).map {
            CatmulRom1(it[0], it[1], it[2], it[3], alpha)
        }
    }

    fun position(rt: Double): Double {
        val st = if (loop) mod(rt, 1.0) else rt.coerceIn(0.0, 1.0)
        val segmentIndex = (kotlin.math.min(almostOne, st) * segments.size).toInt()
        val t = (kotlin.math.min(almostOne, st) * segments.size) - segmentIndex
        return segments[segmentIndex].position(t)
    }
}

class CatmullRom2(val p0: Vector2, val p1: Vector2, val p2: Vector2, val p3: Vector2, val alpha: Double = 0.5) {
    val t0: Double = 0.0
    val t1: Double = calculateT(t0, p0, p1)
    val t2: Double = calculateT(t1, p1, p2)
    val t3: Double = calculateT(t2, p2, p3)

    fun position(rt: Double): Vector2 {
        val t = t1 + rt * (t2 - t1)
        val a1 = p0 * ((t1 - t) / (t1 - t0)) + p1 * ((t - t0) / (t1 - t0))
        val a2 = p1 * ((t2 - t) / (t2 - t1)) + p2 * ((t - t1) / (t2 - t1))
        val a3 = p2 * ((t3 - t) / (t3 - t2)) + p3 * ((t - t2) / (t3 - t2))

        val b1 = a1 * ((t2 - t) / (t2 - t0)) + a2 * ((t - t0) / (t2 - t0))
        val b2 = a2 * ((t3 - t) / (t3 - t1)) + a3 * ((t - t1) / (t3 - t1))

        val c = b1 * ((t2 - t) / (t2 - t1)) + b2 * ((t - t1) / (t2 - t1))
        return c
    }

    private fun calculateT(t: Double, p0: Vector2, p1: Vector2): Double {
        val a = (p1.x - p0.x).pow(2.0) + (p1.y - p0.y).pow(2.0)
        val b = a.pow(0.5)
        val c = b.pow(alpha)
        return c + t
    }
}

class CatmullRomChain2(points: List<Vector2>, alpha: Double = 0.5, val loop: Boolean = false) {
    val segments = if (!loop) {
        val startPoints = points.take(2)
        val endPoints = points.takeLast(2)
        val mirrorStart = startPoints.first() - (startPoints.last() - startPoints.first()).normalized
        val mirrorEnd = endPoints.last() + (endPoints.last() - endPoints.first()).normalized

        (listOf(mirrorStart) + points + listOf(mirrorEnd)).windowed(4, 1).map {
            CatmullRom2(it[0], it[1], it[2], it[3], alpha)
        }
    } else {
        val cleanPoints = if (loop && points.first().distanceTo(points.last()) <= 1.0E-6) {
            points.dropLast(1)
        } else {
            points
        }
        (cleanPoints + cleanPoints.take(3)).windowed(4, 1).map {
            CatmullRom2(it[0], it[1], it[2], it[3], alpha)
        }
    }

    fun positions(steps: Int = segments.size * 4): List<Vector2> {
        return (0..steps).map {
            position(it.toDouble() / steps)
        }
    }

    fun position(rt: Double): Vector2 {
        val st = if (loop) mod(rt, 1.0) else rt.coerceIn(0.0, 1.0)
        val segmentIndex = (kotlin.math.min(almostOne, st) * segments.size).toInt()
        val t = (kotlin.math.min(almostOne, st) * segments.size) - segmentIndex
        return segments[segmentIndex].position(t)
    }
}

class CatmullRom3(val p0: Vector3, val p1: Vector3, val p2: Vector3, val p3: Vector3, val alpha: Double = 0.5) {
    val t0: Double = 0.0
    val t1: Double = calculateT(t0, p0, p1)
    val t2: Double = calculateT(t1, p1, p2)
    val t3: Double = calculateT(t2, p2, p3)

    fun position(rt: Double): Vector3 {
        val t = t1 + rt * (t2 - t1)
        val a1 = p0 * ((t1 - t) / (t1 - t0)) + p1 * ((t - t0) / (t1 - t0))
        val a2 = p1 * ((t2 - t) / (t2 - t1)) + p2 * ((t - t1) / (t2 - t1))
        val a3 = p2 * ((t3 - t) / (t3 - t2)) + p3 * ((t - t2) / (t3 - t2))

        val b1 = a1 * ((t2 - t) / (t2 - t0)) + a2 * ((t - t0) / (t2 - t0))
        val b2 = a2 * ((t3 - t) / (t3 - t1)) + a3 * ((t - t1) / (t3 - t1))

        val c = b1 * ((t2 - t) / (t2 - t1)) + b2 * ((t - t1) / (t2 - t1))
        return c
    }

    private fun calculateT(t: Double, p0: Vector3, p1: Vector3): Double {
        val a = (p1.x - p0.x).pow(2.0) + (p1.y - p0.y).pow(2.0) + (p1.z - p0.z).pow(2.0)
        val b = a.pow(0.5)
        val c = b.pow(alpha)
        return c + t
    }
}

class CatmullRomChain3(points: List<Vector3>, alpha: Double = 0.5, val loop: Boolean = false) {
    val segments = if (!loop) {
        val startPoints = points.take(2)
        val endPoints = points.takeLast(2)
        val mirrorStart = startPoints.first() - (startPoints.last() - startPoints.first()).normalized
        val mirrorEnd = endPoints.last() + (endPoints.last() - endPoints.first()).normalized

        (listOf(mirrorStart) + points + listOf(mirrorEnd)).windowed(4, 1).map {
            CatmullRom3(it[0], it[1], it[2], it[3], alpha)
        }
    } else {
        val cleanPoints = if (loop && points.first().distanceTo(points.last()) <= 1.0E-6) {
            points.dropLast(1)
        } else {
            points
        }
        (cleanPoints + cleanPoints + cleanPoints.take(3)).windowed(4, 1).map {
            CatmullRom3(it[0], it[1], it[2], it[3], alpha)
        }
    }

    fun positions(steps: Int = segments.size * 4): List<Vector3> {
        return (0..steps).map {
            position(it.toDouble() / steps)
        }
    }

    fun position(rt: Double): Vector3 {
        val st = if (loop) mod(rt, 1.0) else rt.coerceIn(0.0, 1.0)
        val segmentIndex = (kotlin.math.min(almostOne, st) * segments.size).toInt()
        val t = (kotlin.math.min(almostOne, st) * segments.size) - segmentIndex
        return segments[segmentIndex].position(t)
    }
}
