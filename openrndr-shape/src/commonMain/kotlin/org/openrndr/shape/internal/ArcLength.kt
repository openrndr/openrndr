package org.openrndr.shape.internal

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.abs
import kotlin.math.sqrt

/** The length of the derivative of a curve at t, without boxing */
internal fun interface Speed {
    fun at(t: Double): Double
}

/** The [Speed] of a quadratic Bézier curve, its derivative is 2 (p1 - p0) + 2 (p0 - 2 p1 + p2) t */
internal fun quadraticSpeed(p0: Vector2, p1: Vector2, p2: Vector2): Speed {
    val bx = 2.0 * (p1.x - p0.x)
    val by = 2.0 * (p1.y - p0.y)
    val ax = 2.0 * (p0.x - 2.0 * p1.x + p2.x)
    val ay = 2.0 * (p0.y - 2.0 * p1.y + p2.y)
    return Speed { t ->
        val x = bx + ax * t
        val y = by + ay * t
        sqrt(x * x + y * y)
    }
}

/** The [Speed] of a cubic Bézier curve, its derivative is 3 (p1 - p0) + 6 (p0 - 2 p1 + p2) t + 3 (p3 - 3 p2 + 3 p1 - p0) t^2 */
internal fun cubicSpeed(p0: Vector2, p1: Vector2, p2: Vector2, p3: Vector2): Speed {
    val cx = 3.0 * (p1.x - p0.x)
    val cy = 3.0 * (p1.y - p0.y)
    val bx = 6.0 * (p0.x - 2.0 * p1.x + p2.x)
    val by = 6.0 * (p0.y - 2.0 * p1.y + p2.y)
    val ax = 3.0 * (p3.x - 3.0 * p2.x + 3.0 * p1.x - p0.x)
    val ay = 3.0 * (p3.y - 3.0 * p2.y + 3.0 * p1.y - p0.y)
    return Speed { t ->
        val x = cx + (bx + ax * t) * t
        val y = cy + (by + ay * t) * t
        sqrt(x * x + y * y)
    }
}

/** The [Speed] of a quadratic Bézier curve in 3D */
internal fun quadraticSpeed(p0: Vector3, p1: Vector3, p2: Vector3): Speed {
    val b = (p1 - p0) * 2.0
    val a = (p0 - p1 * 2.0 + p2) * 2.0
    return Speed { t ->
        val x = b.x + a.x * t
        val y = b.y + a.y * t
        val z = b.z + a.z * t
        sqrt(x * x + y * y + z * z)
    }
}

/** The [Speed] of a cubic Bézier curve in 3D */
internal fun cubicSpeed(p0: Vector3, p1: Vector3, p2: Vector3, p3: Vector3): Speed {
    val c = (p1 - p0) * 3.0
    val b = (p0 - p1 * 2.0 + p2) * 6.0
    val a = (p3 - p2 * 3.0 + p1 * 3.0 - p0) * 3.0
    return Speed { t ->
        val x = c.x + (b.x + a.x * t) * t
        val y = c.y + (b.y + a.y * t) * t
        val z = c.z + (b.z + a.z * t) * t
        sqrt(x * x + y * y + z * z)
    }
}

/** Positive abscissae of the 8-point Gauss-Legendre rule on [-1, 1]. */
private val GL_X = doubleArrayOf(0.1834346424956498, 0.5255324099163290, 0.7966664774136267, 0.9602898564975363)

/** Weights belonging to [GL_X]. */
private val GL_W = doubleArrayOf(0.3626837833783620, 0.3137066458778873, 0.2223810344533745, 0.1012285362903763)

/** Relative accuracy of the arc length with respect to the size of the curve. */
private const val ARC_LENGTH_ACCURACY = 1E-10

/** Maximum subdivision depth of the arc length integration, only reached near cusps. */
private const val ARC_LENGTH_MAX_DEPTH = 16

private fun gaussLegendre(speed: Speed, t0: Double, t1: Double): Double {
    val mid = (t0 + t1) * 0.5
    val half = (t1 - t0) * 0.5
    var sum = 0.0
    for (i in GL_X.indices) {
        sum += GL_W[i] * (speed.at(mid + half * GL_X[i]) + speed.at(mid - half * GL_X[i]))
    }
    return sum * half
}

/**
 * Cumulative arc length of a curve at the ends of the intervals its adaptive integration settled on, the halves of
 * the intervals whose halves it accepted. On each of them the 8 point rule is accurate, also on a part of it.
 */
internal class ArcLengthTable(val ts: DoubleArray, val lengths: DoubleArray) {
    val total: Double get() = lengths[lengths.size - 1]
}

private fun collectIntervals(
    speed: Speed,
    t0: Double,
    t1: Double,
    whole: Double,
    accuracy: Double,
    depth: Int,
    ts: MutableList<Double>,
    lengths: MutableList<Double>
) {
    val mid = (t0 + t1) * 0.5
    val left = gaussLegendre(speed, t0, mid)
    val right = gaussLegendre(speed, mid, t1)
    if (depth == 0 || (ARC_LENGTH_MAX_DEPTH - depth >= TABLE_MIN_DEPTH && abs(left + right - whole) <= accuracy)) {
        ts.add(mid)
        lengths.add(lengths.last() + left)
        ts.add(t1)
        lengths.add(lengths.last() + right)
    } else {
        collectIntervals(speed, t0, mid, left, accuracy * 0.5, depth - 1, ts, lengths)
        collectIntervals(speed, mid, t1, right, accuracy * 0.5, depth - 1, ts, lengths)
    }
}

/**
 * Smallest number of halvings of [0, 1] in an [ArcLengthTable], its intervals are then short enough for the linear
 * guess of a t in them to be close
 */
private const val TABLE_MIN_DEPTH = 3

/**
 * Builds the [ArcLengthTable] of a curve by adaptive Gauss-Legendre integration of its [speed]
 *
 * @param size a measure of the size of the curve, such as the length of its control polygon, the arc lengths are
 * accurate to roughly `size * 1E-10`
 */
internal fun arcLengthTable(size: Double, speed: Speed): ArcLengthTable {
    val ts = mutableListOf(0.0)
    val lengths = mutableListOf(0.0)
    if (size > 0.0) {
        collectIntervals(
            speed, 0.0, 1.0, gaussLegendre(speed, 0.0, 1.0),
            size * ARC_LENGTH_ACCURACY, ARC_LENGTH_MAX_DEPTH, ts, lengths
        )
    } else {
        ts.add(1.0)
        lengths.add(0.0)
    }
    return ArcLengthTable(ts.toDoubleArray(), lengths.toDoubleArray())
}

/**
 * The parameters at which the arc length of a curve reaches each of the [lengths]. Each is found in its interval of
 * the [table] by Newton iteration that falls back to bisection, the arc length within an interval is integrated with
 * the 8 point rule from its start.
 */
internal fun tsForLengths(lengths: DoubleArray, table: ArcLengthTable, size: Double, speed: Speed): DoubleArray {
    val result = DoubleArray(lengths.size)
    val tolerance = size * ARC_LENGTH_ACCURACY
    val ts = table.ts
    val cumulative = table.lengths
    val total = table.total
    var interval = 0
    for (i in lengths.indices) {
        val target = lengths[i]
        if (target <= 0.0 || total <= 0.0) {
            result[i] = 0.0
            continue
        }
        if (target >= total) {
            result[i] = 1.0
            continue
        }
        // the interval holding the target, searched from the previous one for ascending lengths
        if (interval >= cumulative.size - 1 || cumulative[interval] > target) {
            interval = 0
        }
        while (interval < cumulative.size - 2 && cumulative[interval + 1] <= target) {
            interval++
        }
        val start = ts[interval]
        val startLength = cumulative[interval]
        var lo = start
        var hi = ts[interval + 1]
        val hiLength = cumulative[interval + 1]
        var x = if (hiLength > startLength) lo + (hi - lo) * (target - startLength) / (hiLength - startLength) else lo
        var iterations = 0
        while (iterations++ < 64) {
            val error = startLength + gaussLegendre(speed, start, x) - target
            if (abs(error) <= tolerance) break
            if (error < 0.0) lo = x else hi = x
            if (hi - lo <= 1E-15) break
            val s = speed.at(x)
            var next = if (s > 0.0) x - error / s else Double.NaN
            if (!(next > lo && next < hi)) {
                next = (lo + hi) * 0.5
            }
            val step = abs(next - x)
            x = next
            // Newton converges quadratically, after a step this small the error is far below the tolerance
            if (step <= 1E-12) break
        }
        result[i] = x
    }
    return result
}
