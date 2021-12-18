package org.openrndr.shape

import org.openrndr.math.*
import kotlin.jvm.JvmOverloads
import kotlin.math.abs
import kotlin.math.min

/**
 * A [List] for managing a collection of [Segment]s.
 */
data class ShapeContour @JvmOverloads constructor (
    val segments: List<Segment>,
    val closed: Boolean,
    val polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
) : ShapeContourProvider {
    companion object {
        /**
         * An empty [ShapeContour] object.
         *
         * It is advised to use this instance whenever an empty contour is needed.
         */
        val EMPTY = ShapeContour(emptyList(), false)

        @JvmOverloads
        fun fromSegments(
            segments: List<Segment>,
            closed: Boolean,
            polarity: YPolarity = YPolarity.CW_NEGATIVE_Y,
            distanceTolerance: Double = 1E-3,
        ): ShapeContour {
            if (segments.isEmpty()) {
                return EMPTY
            }
            return ShapeContour(
                segments.zipWithNext().map {
                    val distance = it.first.end.squaredDistanceTo(it.second.start)
                    require(distance < distanceTolerance) {
                        "distance between segment end and start is $distance (max: $distanceTolerance)"
                    }
                    it.first.copy(end = it.second.start)
                } + segments.last(), closed, polarity
            )
        }

        /** Creates a [ShapeContour] by converting [points] to [Segment]s. */
        @JvmOverloads
        fun fromPoints(
            points: List<Vector2>,
            closed: Boolean,
            polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
        ) =
            if (!closed) {
                ShapeContour((0 until points.size - 1).map {
                    Segment(
                        points[it],
                        points[it + 1]
                    )
                }, closed, polarity)
            } else {
                val d = (points.last() - points.first()).squaredLength
                val usePoints = if (d > 10E-6) points else points.dropLast(1)
                ShapeContour((usePoints.indices).map {
                    Segment(
                        usePoints[it],
                        usePoints[(it + 1) % usePoints.size]
                    )
                }, true, polarity)
            }
    }

    init {
        segments.zipWithNext().forEach {
            val d = (it.first.end - it.second.start).length
            require(d < 10E-6) {
                "points are too far away from each other ${it.first.end} ${it.second.start} $d"
            }
        }
    }

    /** Returns [Shape] representation. */
    val shape get() = Shape(listOf(this))

    /** Calculates approximate Euclidean length of the contour. */
    val length by lazy { segments.sumOf { it.length } }

    /** Calculates the bounding box of the contour as [Rectangle]. */
    val bounds by lazy {
        sampleLinear().segments.flatMap {
            listOf(
                it.start,
                it.end
            )
        }.bounds
    }

    /** Determines the winding order of the [ShapeContour]. */
    val winding: Winding
        get() {
            var sum = 0.0
            segments.forEachIndexed { i, v ->
                val after = segments[mod(i + 1, segments.size)].start
                sum += (after.x - v.start.x) * (after.y + v.start.y)
            }
            return when (polarity) {
                YPolarity.CCW_POSITIVE_Y -> if (sum < 0) {
                    Winding.COUNTER_CLOCKWISE
                } else {
                    Winding.CLOCKWISE
                }
                YPolarity.CW_NEGATIVE_Y -> if (sum < 0) {
                    Winding.CLOCKWISE
                } else {
                    Winding.COUNTER_CLOCKWISE
                }
            }
        }

    /** Converts to a [List] of single [Segment]s. */
    @Suppress("unused")
    val exploded: List<ShapeContour>
        get() = segments.map { ShapeContour(listOf(it), false, polarity) }

    /** Returns the [ShapeContour], but with a clockwise winding. */
    val clockwise: ShapeContour get() = if (winding == Winding.CLOCKWISE) this else this.reversed

    /** Returns the [ShapeContour], but with a counterclockwise winding. */
    val counterClockwise: ShapeContour get() = if (winding == Winding.COUNTER_CLOCKWISE) this else this.reversed

    operator fun plus(other: ShapeContour): ShapeContour {
        require(polarity == other.polarity) {
            """shapes have mixed polarities"""
        }
        if (segments.isEmpty() && other.segments.isEmpty()) {
            return EMPTY
        } else {
            if (segments.isEmpty()) {
                return other
            }
            if (other.segments.isEmpty()) {
                return this
            }
        }
        val epsilon = 0.001
        val segments = mutableListOf<Segment>()
        segments.addAll(this.segments)
        if ((this.segments[this.segments.size - 1].end - other.segments[0].start).length > epsilon) {
            segments.add(
                Segment(
                    this.segments[this.segments.size - 1].end,
                    other.segments[0].start
                )
            )
        }
        segments.addAll(other.segments)
        return ShapeContour(segments, false, polarity)
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


    /** Returns true if [ShapeContour] doesn't contain any [Segment]s. */
    val empty: Boolean
        get() {
            return this === EMPTY || segments.isEmpty()
        }

    /**
     * Returns a point on the path of the [ShapeContour].
     *
     * Also see: [Segment.position].
     *
     * @param ut unfiltered t parameter, will be clamped between 0.0 and 1.0.
     * @return [Vector2] that lies on the path of the [ShapeContour].
     */
    fun position(ut: Double): Vector2 {
        if (empty) {
            return Vector2.INFINITY
        }

        return when (val t = ut.clamp(0.0, 1.0)) {
            0.0 -> segments[0].start
            1.0 -> segments.last().end
            else -> {
                val segment = (t * segments.size).toInt()
                val segmentOffset = (t * segments.size) - segment
                segments[min(segments.size - 1, segment)].position(segmentOffset)
            }
        }
    }

    /** Calculates the normal for given value of `t`. */
    fun normal(ut: Double): Vector2 {
        if (empty) {
            return Vector2.ZERO
        }

        return when (val t = ut.coerceIn(0.0, 1.0)) {
            0.0 -> segments[0].normal(0.0, polarity)
            1.0 -> segments.last().normal(1.0, polarity)
            else -> {
                val segment = (t * segments.size).toInt()
                val segmentOffset = (t * segments.size) - segment
                segments[min(segments.size - 1, segment)].normal(segmentOffset, polarity)
            }
        }
    }

    /**
     * Calculates the pose [Matrix44] (i.e. translation and rotation) that describes an orthonormal basis
     * formed by normal and tangent of the contour at [t](https://pomax.github.io/bezierinfo/#explanation).
     *
     * Which means it returns a [Matrix44],
     * that you can use to orient an object
     * the same way the curve is oriented at
     * given value of *t*.
     *
     * @param t The value of t in the range of `0.0` to `1.0` at which to return the pose at.
     */
    @Suppress("unused")
    fun pose(t: Double): Matrix44 {
        val n = normal(t)
        val dx = n.perpendicular(polarity).xy0.xyz0
        val dy = n.xy0.xyz0
        val dt = position(t).xy01
        return Matrix44.fromColumnVectors(dx, dy, Vector4.UNIT_Z, dt)
    }

    /**
     * Recursively subdivides linear [Segment]s to approximate Bézier curves.
     *
     * Works similar to [adaptivePositionsAndCorners] but it only returns
     * the positions without the corners.
     */
    fun adaptivePositions(distanceTolerance: Double = 0.5): List<Vector2> {
        return adaptivePositionsAndCorners(distanceTolerance).first
    }

    /**
     * Recursively subdivides linear [Segment]s to approximate Bézier curves.
     *
     * Also see [Segment.adaptivePositions].
     *
     * @param distanceTolerance The square of the maximal distance of each point from curve.
     * @return A pair containing a list of points and a list of
     * respective boolean values for each point, indicating if the point is on a [Segment] boundary or not.
     */
    fun adaptivePositionsAndCorners(distanceTolerance: Double = 0.5): Pair<List<Vector2>, List<Boolean>> {
        if (empty) {
            return Pair(emptyList(), emptyList())
        }

        val adaptivePoints = mutableListOf<Vector2>()
        val corners = mutableListOf<Boolean>()
        for (segment in this.segments) {
            val samples = segment.adaptivePositions(distanceTolerance)
            val lastSampleIndex = samples.size - 1
            samples.forEachIndexed { index, it ->
                val last = adaptivePoints.lastOrNull()
                if (last == null || last.squaredDistanceTo(it) > 0.0) {
                    adaptivePoints.add(it)
                    if (index == 0 || index == lastSampleIndex) {
                        corners.add(segment.corner)
                    } else {
                        corners.add(false)
                    }
                }
            }
        }
        return Pair(adaptivePoints, corners)
    }

    /**
     * Recursively subdivides linear [Segment]s to approximate Bézier curves.
     *
     * Also see [Segment.adaptivePositions].
     *
     * @param distanceTolerance The square of the maximum distance of each point from curve.
     * @return A pair containing a list of points and a list of
     *      respective normal vectors.
     */
    @Suppress("unused")
    fun adaptivePositionsAndDirection(distanceTolerance: Double = 0.5): Pair<List<Vector2>, List<Vector2>> {
        if (empty) {
            return Pair(emptyList(), emptyList())
        }
        val adaptivePoints = mutableListOf<Vector2>()
        val adaptiveNormals = mutableListOf<Vector2>()
        var last: Vector2? = null
        for (segment in this.segments) {
            val samples = segment.adaptivePositionsAndNormals(distanceTolerance)
            if (samples.first.isNotEmpty()) {
                val r = samples.first[0]
                if (last == null || last.minus(r).length > 0.01) {
                    adaptivePoints.add(r)
                    adaptiveNormals.add(samples.second[0].normalized)
                }
                for (i in 1 until samples.first.size) {
                    adaptivePoints.add(samples.first[i])
                    adaptiveNormals.add(samples.second[i].normalized)
                    last = samples.first[i]
                }
            }
        }
        adaptivePoints.zipWithNext().forEach {
            require(it.first.squaredDistanceTo(it.second) > 0.0)
        }
        return Pair(adaptivePoints, adaptiveNormals)
    }

    /**
     * Returns specified amount of points of equal distance from each other.
     */
    fun equidistantPositions(pointCount: Int) =
        if (empty) {
            emptyList()
        } else {
            sampleEquidistant(
                adaptivePositions(),
                pointCount + if (closed) 1 else 0
            )
        }

    /**
     * Adaptively samples the contour into a new [ShapeContour] of
     * [linear][SegmentType.LINEAR] [Segment]s while still approximating the original contour.
     *
     * @param distanceTolerance Controls the precision of the approximation, higher values result in lower accuracy.
     * @return A [ShapeContour] composed of linear [Segment]s
     */
    fun sampleLinear(distanceTolerance: Double = 0.5) =
        if (empty) {
            EMPTY
        } else {
            fromPoints(adaptivePositions(distanceTolerance), closed, polarity)
        }

    /** Samples the [ShapeContour] into equidistant linear [Segment]s. */
    fun sampleEquidistant(pointCount: Int): ShapeContour {
        if (empty) {
            return EMPTY
        }
        val points = equidistantPositions(pointCount.coerceAtLeast(2))
        val segments = (0 until points.size - 1).map {
            Segment(
                points[it],
                points[it + 1]
            )
        }
        return ShapeContour(segments, closed, polarity)
    }

    /** Applies linear transformation to [ShapeContour]. */
    fun transform(transform: Matrix44) =
        if (empty) {
            EMPTY
        } else {
            if (transform === Matrix44.IDENTITY) {
                this
            } else {
                ShapeContour(
                    segments.map { it.transform(transform) },
                    closed,
                    polarity
                )
            }
        }

    /**
     * Samples a new [ShapeContour] from the current [ShapeContour] starting at [t0] and ending at [t1].
     *
     * @param t0 Starting point in range `0.0` to less than `1.0`.
     * @param t1 Ending point in range `0.0` to less than `1.0`.
     * @return Subcontour
     */
    fun sub(t0: Double, t1: Double): ShapeContour {
        if (empty) {
            return EMPTY
        }

        require(t0 == t0) { "t0 is NaN" }
        require(t1 == t1) { "t1 is NaN" }

        if (abs(t0 - t1) < 10E-6) {
            return EMPTY
        }

        var u0 = t0
        var u1 = t1

        if (closed && (u1 < u0 || u1 > 1.0 || u0 > 1.0 || u0 < 0.0 || u1 < 0.0)) {
            val diff = u1 - u0
            u0 = mod(u0, 1.0)
            if (abs(diff) < (1.0 - 10E-6)) {
                return if (diff > 0.0) {
                    u1 = u0 + diff
                    if (u1 > 1.0) {
                        sub(u0, 1.0) + sub(0.0, u1 - 1.0)
                    } else {
                        sub(u0, u1)
                    }
                } else {
                    u1 = u0 + diff
                    if (u1 < 0) {
                        sub(u1 + 1.0, 1.0) + sub(0.0, u0)
                    } else {
                        sub(u1, u0)
                    }
                }
            } else {
                u1 = if (diff < 0.0) {
                    u0 - 1.0
                } else {
                    u0 + 1.0
                }
                if (u1 > 1.0) {
                    return sub(u0, 1.0) + sub(0.0, u1 - 1.0)
                }
                if (u1 < 1.0) {
                    return sub(u0, 1.0) + sub(0.0, u1 + 1.0)
                }
            }
        }

        u0 = u0.coerceIn(0.0, 1.0)
        u1 = u1.coerceIn(0.0, 1.0)

        var z0 = u0
        var z1 = u1

        if (u0 > u1) {
            z0 = u1
            z1 = u0
        }

        val length = segments.size.toDouble()
        var segment0 = (z0 * length).toInt()
        val segmentOffset0 = if (segment0 < segments.size) z0 * length % 1.0 else 1.0
        var segment1 = (z1 * length).toInt()
        val segmentOffset1 = if (segment1 < segments.size) z1 * length % 1.0 else 1.0

        segment1 = min(segments.size - 1, segment1)
        segment0 = min(segments.size - 1, segment0)

        val newSegments = mutableListOf<Segment>()
        val epsilon = 10E-7

        for (s in segment0..segment1) {
            if (s == segment0 && s == segment1) {
                //if (Math.abs(segmentOffset0-segmentOffset1) > epsilon)
                newSegments.add(segments[s].sub(segmentOffset0, segmentOffset1))
            } else if (s == segment0) {
                if (segmentOffset0 < 1.0 - epsilon)
                    newSegments.add(segments[s].sub(segmentOffset0, 1.0))
            } else if (s == segment1) {
                if (segmentOffset1 > epsilon)
                    newSegments.add(segments[s].sub(0.0, segmentOffset1))
            } else {
                newSegments.add(segments[s])
            }
        }
        return ShapeContour(newSegments, false, polarity)
    }

    /**
     * Checks if given point lies on the path of the [ShapeContour].
     *
     * @param point The point to check.
     * @param error Maximum acceptable margin for error.
     * @return The contour parameter in the range 0 to less than 1 (inclusive of 0, but not 1) only if the point is within the margin of error.
     *      Otherwise `null` will be returned.
     */
    fun on(point: Vector2, error: Double = 5.0): Double? {
        for (i in segments.indices) {
            val st = segments[i].on(point, error)
            if (st != null) {
                return (i + st) / segments.size
            }
        }
        return null
    }

    /**
     * Projects a point on the [ShapeContour]
     * @param point The point to project.
     * @return a projected point that lies on the [ShapeContour].
     */
    fun nearest(point: Vector2): ContourPoint {
        val n = segments.map { it.nearest(point) }.minByOrNull { it.position.distanceTo(point) }
            ?: error("no segments")
        val segmentIndex = segments.indexOf(n.segment)
        val t = (segmentIndex + n.segmentT) / segments.size
        return ContourPoint(this, t, n.segment, n.segmentT, n.position)
    }

    //@Deprecated("Please use .open() instead")
    //val opened = open

    /**
     * Opens the path of the [ShapeContour].
     */
    val open
        get() = if (empty) EMPTY else
            ShapeContour(segments, false, polarity)

    /**
     * Closes the path of the [ShapeContour].
     *
     * The path is closed by creating a new connecting [Segment]
     * between the first and last [Segment] in the contour.
     * If the distance between the beginning of the first and the finish of the last point is negligible (`<0.001`),
     * then no new [Segment]s are added.
     */
    fun close() = if (empty) EMPTY else {
        if ((segments.last().end - segments.first().start).squaredLength < 10E-6)
            ShapeContour(segments, true, polarity)
        else
            ShapeContour(
                segments + Segment(
                    segments.last().end,
                    segments.first().start
                ), true, polarity
            )
    }

    /**
     * Reverses the direction of [Segment]s and their order.
     *
     * For more information, see [Segment.reverse].
     */
    val reversed
        get() = ShapeContour(
            segments.map { it.reverse }.reversed(),
            closed,
            polarity
        )
    override val contour: ShapeContour
        get() = this
}

/**
 * Converts a [List] of [ShapeContour]s to a single [Shape].
 */
val List<ShapeContour>.shape
    get() = Shape(this)

/**
 * Converts chain to a [ShapeContour].
 */
@Suppress("unused")
fun CatmullRomChain2.toContour(): ShapeContour =
    ShapeContour(segments.map { it.toSegment() }, this.loop)