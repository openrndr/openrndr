package org.openrndr.shape

import kotlinx.serialization.Transient
import org.openrndr.math.EuclideanVector
import org.openrndr.math.Vector2

interface Path<T : EuclideanVector<T>> {
    val segments: List<BezierSegment<T>>
    val closed: Boolean

    @Transient
    val length get() = segments.sumOf { it.length }

    @Transient
    val infinity: T

    /**
     * Returns a point on the path of the [ShapeContour].
     *
     * To make the computation easier in the presence of non-linear Segments,
     * the result is derived first from the corresponding Segment in the ShapeContour,
     * and then within that Segment.
     *
     * For example, if the ShapeContour is composed of 10 Segments, asking for position ut=0.03
     * will return the point 30% of the along the 1st Segment, and ut=0.51 will return the point
     * 10% of the along the 6th.
     *
     *
     * If the component Segments are of wildly different lengths, the resulting point can be very
     * different from what would be arrived at if the ShapeContour were treated strictly as a whole.
     * In that case, consider using [ShapeContour.equidistantPositions] instead.
     *
     * Also see: [Segment2D.position].
     *
     * @param ut unfiltered t parameter, will be clamped between 0.0 and 1.0.
     * @return [Vector2] that lies on the path of the [ShapeContour].
     */
    fun position(ut: Double): T {
        if (empty) {
            return infinity
        }

        return when (val t = ut.coerceIn(0.0, 1.0)) {
            0.0 -> segments[0].start
            1.0 -> segments.last().end
            else -> {
                val (segment, segmentOffset) = segment(t)
                segments[segment].position(segmentOffset)
            }
        }
    }

    fun direction(ut: Double): T {
        if (empty) {
            return infinity
        }

        return when (val t = ut.coerceIn(0.0, 1.0)) {
            0.0 -> segments[0].direction(0.0)
            1.0 -> segments.last().direction(1.0)
            else -> {
                val (segment, segmentOffset) = segment(t)
                segments[segment].direction(segmentOffset)
            }
        }
    }

    fun curvature(ut: Double): Double {
        if (empty) {
            return 0.0
        }
        return when (val t = ut.coerceIn(0.0, 1.0)) {
            0.0 -> segments[0].curvature(0.0)
            1.0 -> segments.last().curvature(1.0)
            else -> {
                val (segment, segmentOffset) = segment(t)
                segments[segment].curvature(segmentOffset)
            }
        }
    }

    /**
     * Estimates the [t](https://pomax.github.io/bezierinfo/#explanation) value for a given length.
     *
     * @return The value of *t* between `0.0` and `1.0`.
     */
    @Suppress("unused")
    fun tForLength(length: Double): Double {
        var remaining = length
        if (length <= 0.0) {
            return 0.0
        }
        if (segments.size == 1) {
            return segments.first().tForLength(length)
        }
        for ((index, segment) in segments.withIndex()) {
            val segmentLength = segment.length
            if (segmentLength > remaining) {
                return (segment.tForLength(remaining) + index) / segments.size
            } else {
                remaining -= segmentLength
            }
        }
        return 1.0
    }

    /**
     * Returns segment number and segment offset
     * in a [ShapeContour] for the given [ut].
     * @param ut unfiltered t parameter, will be clamped between 0.0 and 1.0.
     */
    fun segment(ut: Double): Pair<Int, Double> {
        if (empty) {
            return Pair(0, 0.0)
        }

        return when (val t = ut.coerceIn(0.0, 1.0)) {
            0.0 -> Pair(0, 0.0)
            1.0 -> Pair(segments.size - 1, 1.0)
            else -> {
                val segment = (t * segments.size).toInt()
                val segmentOffset = (t * segments.size) - segment
                return Pair(segment, segmentOffset)
            }
        }
    }

    @Transient
    val empty: Boolean


    /**
     * Calculates the point at a given distance along this [ShapeContour].
     * @param length the distance along the [ShapeContour]
     * @param distanceTolerance not used, the point is found on the curves themselves
     *
     * @return Resulting [Vector2] or [Vector2.INFINITY] for an empty [ShapeContour].
     *
     * @see [Segment2D.pointAtLength]
     */
    fun pointAtLength(length: Double, distanceTolerance: Double = 0.5): T {
        when {
            empty -> return infinity
            length <= 0.0 -> return segments.first().start
            length >= this.length -> return segments.last().end
        }
        var remainingLength = length
        for (segment in segments) {
            val segmentLength = segment.length
            if (segmentLength > remainingLength) {
                return segment.pointAtLength(remainingLength, distanceTolerance)
            }
            remainingLength -= segmentLength
        }
        return segments.last().end
    }

    /**
     * Recursively subdivides linear [Segment2D]s to approximate Bézier curves.
     *
     * Works similar to [adaptivePositionsAndCorners] but it only returns
     * the positions without the corners.
     */
    fun adaptivePositions(distanceTolerance: Double = 0.5): List<T> {
        return adaptivePositionsWithT(distanceTolerance).map { it.first }
    }

    fun adaptivePositionsWithT(distanceTolerance: Double = 0.5): List<Pair<T, Double>> {
        val adaptivePoints = mutableListOf<Pair<T, Double>>()
        val segmentCount = segments.size
        for ((segmentIndex, segment) in this.segments.withIndex()) {
            val samples = segment.adaptivePositionsWithT(distanceTolerance)
            samples.forEach {
                val last = adaptivePoints.lastOrNull()
                if (last == null || last.first.squaredDistanceTo(it.first) > 0.0) {
                    adaptivePoints.add(it.copy(second = (it.second + segmentIndex) / segmentCount))
                }
            }
        }
        return adaptivePoints
    }

    /**
     * Returns specified amount of points of equal distance from each other, measured along the path. The first point
     * is the start of the path, the last point is the end of an open path.
     *
     * @param distanceTolerance not used, the points are found on the curves themselves
     */
    fun equidistantPositions(pointCount: Int, distanceTolerance: Double = 0.5): List<T> =
        equidistantPositionsWithT(pointCount, distanceTolerance).map { it.first }

    /**
     * Returns specified amount of points of equal distance from each other, measured along the path, with their
     * path parameter. The first point is the start of the path, the last point is the end of an open path.
     *
     * @param distanceTolerance not used, the points are found on the curves themselves
     */
    fun equidistantPositionsWithT(pointCount: Int, distanceTolerance: Double = 0.5): List<Pair<T, Double>> {
        if (empty || pointCount <= 0) {
            return emptyList()
        }
        val lengths = segments.map { it.length }
        val total = lengths.sum()
        val intervals = if (closed) pointCount else pointCount - 1
        fun target(i: Int) = if (intervals == 0) 0.0 else total * i / intervals

        val result = ArrayList<Pair<T, Double>>(pointCount)
        var segmentIndex = 0
        var segmentStart = 0.0
        var i = 0
        while (i < pointCount) {
            while (segmentIndex < segments.size - 1 && target(i) > segmentStart + lengths[segmentIndex]) {
                segmentStart += lengths[segmentIndex]
                segmentIndex++
            }
            // all targets on this segment are found in one ascending search
            val local = mutableListOf<Double>()
            var j = i
            while (j < pointCount &&
                (segmentIndex == segments.size - 1 || target(j) <= segmentStart + lengths[segmentIndex])
            ) {
                local.add(target(j) - segmentStart)
                j++
            }
            val segment = segments[segmentIndex]
            for (t in segment.tForLengths(local)) {
                result.add(segment.position(t) to (segmentIndex + t) / segments.size)
            }
            i = j
        }
        if (!closed && pointCount >= 2) {
            result[result.lastIndex] = segments.last().end to 1.0
        }
        return result
    }

    fun sub(t0: Double, t1: Double): Path<T>
}