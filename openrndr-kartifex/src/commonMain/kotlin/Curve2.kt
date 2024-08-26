package org.openrndr.kartifex

import org.openrndr.kartifex.utils.Intersections

interface Curve2 {
    /**
     * @param t a parametric point on the curve, not necessarily within [0, 1]
     * @return the interpolated position on the curve
     */
    fun position(t: Double): Vec2

    /**
     * Given a ring of curves, the sum of area() will be the area enclosed by that ring. For clockwise rings, the sum will
     * be negative, for counter-clockwise rings it will be positive.
     *
     * @return the signed area of the curve
     */
    fun signedArea(): Double
    fun length(): Double
    fun start() = position(0.0)

    fun end() = position(1.0)

    /**
     * @return an updated curve with the specified endpoints.
     */
    fun endpoints(start: Vec2, end: Vec2): Curve2

    /**
     * @param t a parametric point on the curve, not necessarily within [0, 1]
     * @return the tangent at the interpolated position on the curve, which is not normalized
     */
    fun direction(t: Double): Vec2

    /**
     * @param t a parametric point within the curve, which must be within (0, 1)
     * @return an array representing the lower and upper regions of the curve, split at `t`
     */
    fun split(t: Double): Array<Curve2>
    fun isFlat(epsilon: Double): Boolean

    /**
     * @param interval the parametric range
     * @return the curve within [interval.lo, interval.hi]
     */
    fun range(interval: Interval) = range(interval.lo, interval.hi)

    /**
     * @param tMin the lower parametric bound
     * @param tMax the upper parametric bound
     * @return the curve within [tMin, tMax]
     */
    fun range(tMin: Double, tMax: Double): Curve2 {
        require(tMin != tMax) { "range must be non-zero" }
        require(tMax >= tMin) { "tMin must be less than tMax" }

        return when {
            tMin == 0.0 && tMax == 1.0 -> this
            tMin == 0.0 -> split(tMax)[0]
            tMax == 1.0 -> split(tMin)[1]
            else -> split(tMin)[1].split((tMax - tMin) / (1 - tMin))[0].endpoints(position(tMin), position(tMax))
        }
    }

    /**
     * @param unsafeTs an array of parametric split points
     * @return an array of curves, split at the specified points.
     */
    fun split(unsafeTs: DoubleArray): Array<Curve2> {
        var previous = Double.POSITIVE_INFINITY
        var tCount = 0
        for (i in unsafeTs.indices) {
            if (unsafeTs[i] != previous) {
                tCount++
            }
            previous = unsafeTs[i]
        }
        var ts = DoubleArray(tCount)
        tCount = 0
        previous = Double.POSITIVE_INFINITY
        for (i in unsafeTs.indices) {
            if (unsafeTs[i] != previous) {
                ts[tCount] = unsafeTs[i]
                tCount++
            }
            previous = unsafeTs[i]
        }
        if (ts.isEmpty()) {
            return arrayOf(this)
        }
        //ts = ts.clone()
        //java.util.Arrays.sort(ts)

        ts = ts.map { it }.toDoubleArray()
        ts.sort()

        val offset = if (ts[0] == 0.0) 1 else 0
        val len = ts.size - offset - if (ts[ts.size - 1] == 1.0) 1 else 0

        ts.copyInto(ts, 0, offset, offset+len)


        if (len == 0) {
            return arrayOf(this)
        } else if (len == 1) {
            return split(ts[0])
        }
        val result = arrayOfNulls<Curve2>(len + 1)
        result[0] = range(0.0, ts[0])
        for (i in 0 until len - 1) {
            result[i + 1] = range(ts[i], ts[i + 1])
        }
        result[len] = range(ts[len - 1], 1.0)
        return result.filterNotNull().toTypedArray()
    }

    /**
     * @param p a point in 2D space
     * @return the `t` parameter representing the closest point on the curve, not necessarily within [0,1]
     */
    fun nearestPoint(p: Vec2): Double
    fun bounds(): Box2 {
        var bounds: Box2 = Box.box(start(), end())
        for (t in inflections()) {
            bounds = bounds.union(position(t))
        }
        return bounds
    }

    fun subdivide(error: Double): Array<Vec2>
    fun transform(m: Matrix3): Curve2
    fun reverse(): Curve2
    fun inflections(): DoubleArray

    fun intersections(c: Curve2) = Intersections.intersections(this, c)
}
