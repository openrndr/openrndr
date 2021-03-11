package org.openrndr.shape

import io.lacuna.artifex.Vec2
import org.openrndr.math.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A [List] for managing a collection of [Segment]s.
 */
data class ShapeContour(
    val segments: List<Segment>,
    val closed: Boolean,
    val polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
) {
    companion object {
        /**
         * An empty [ShapeContour] object.
         *
         * It is advised to use this instance whenever an empty contour is needed.
         */
        val EMPTY = ShapeContour(emptyList(), false)

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

    /** Triangulates [ShapeContour] into a [List] of [Triangle]s. */
    @Suppress("unused")
    val triangulation by lazy {
        triangulate(Shape(listOf(this))).windowed(3, 3).map {
            Triangle(it[0], it[1], it[2])
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
    val length by lazy { segments.sumByDouble { it.length } }

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

    /** Returns true if given [point] lies inside the [ShapeContour]. */
    operator fun contains(point: Vector2): Boolean = closed && this.toRing2().test(
        Vec2(point.x, point.y)
    ).inside

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

    /** Splits a [ShapeContour] with another [ShapeContour]. */
    fun split(cutter: ShapeContour) = split(this, cutter)

    /** Splits a [ShapeContour] with a [List] of [ShapeContour]s. */
    fun split(cutters: List<ShapeContour>) = split(this, cutters)


    fun removeLoops(attempts: Int = 0, reverseOrder: Boolean = false): ShapeContour {
        if (attempts > 10) {
            //error("tried more than 10 times to remove loops")
            println("tried more than 10 times to remove loops")
            return this
        }

        if (this.closed) {
            return this
        } else {
            val ints = intersections(this, this)
            if (ints.isEmpty()) {
                return this
            } else {

                val toFix = ints.minByOrNull { it.a.contourT }!!
                val sorted = listOf(toFix.a.contourT, toFix.b.contourT).sorted()

                val head = this.sub(0.0, sorted[0])
                val tail = this.sub(sorted[1], 1.0)
                val tailSegments = tail.segments.toMutableList()


                if (head.segments.isEmpty()) {
                    return tail
                }
                if (tail.segments.isEmpty()) {
                    return head
                }

                tailSegments[0] = tailSegments.first().copy(start = head.segments.last().end)
                val fixedTail = ShapeContour(tailSegments, closed = false)
                return (head.removeLoops(attempts+1) + fixedTail.removeLoops(attempts+1))
            }
        }
    }


    /**
     * Offsets a [ShapeContour]'s [Segment]s by given [distance].
     *
     * [Segment]s are moved outwards if [distance] is > 0 or inwards if [distance] is < 0.
     *
     * @param joinType Specifies how to join together the moved [Segment]s.
     */
    fun offset(distance: Double, joinType: SegmentJoin = SegmentJoin.ROUND): ShapeContour {
        val offsets =
            segments.map { it.offset(distance, yPolarity = polarity) }
                .filter { it.isNotEmpty() }
        val tempContours = offsets.map {
            fromSegments(it, closed = false, distanceTolerance = 0.01)
        }
        val offsetContours = tempContours.map { it }.filter { it.length > 0.0 }.toMutableList()

        for (i in 0 until offsetContours.size) {
            offsetContours[i] = offsetContours[i].removeLoops()
        }

        for (i in 0 until if (this.closed) offsetContours.size else offsetContours.size - 1) {
            val i0 = i
            val i1 = (i + 1) % (offsetContours.size)
            val its = intersections(offsetContours[i0], offsetContours[i1])
            if (its.size == 1) {
                println(its[0])
                offsetContours[i0] = offsetContours[i0].sub(0.0, its[0].a.contourT)
                offsetContours[i1] = offsetContours[i1].sub(its[0].b.contourT, 1.0)
            }
        }

        if (offsets.isEmpty()) {
            return ShapeContour(emptyList(), false)
        }


        val startPoint = if (closed) offsets.last().last().end else offsets.first().first().start

        val candidateContour = contour {
            moveTo(startPoint)
            for (offsetContour in offsetContours) {
                val delta = (offsetContour.position(0.0) - cursor)
                val joinDistance = delta.length
                if (joinDistance > 10e-6) {
                    when (joinType) {
                        SegmentJoin.BEVEL -> lineTo(offsetContour.position(0.0))
                        SegmentJoin.ROUND -> arcTo(
                            crx = joinDistance * 0.5 * sqrt(2.0),
                            cry = joinDistance * 0.5 * sqrt(2.0),
                            angle = 90.0,
                            largeArcFlag = false,
                            sweepFlag = true,
                            end = offsetContour.position(0.0)
                        )
                        SegmentJoin.MITER -> {
                            val ls = lastSegment ?: offsetContours.last().segments.last()
                            val fs = offsetContour.segments.first()
                            val i = intersection(
                                ls.end,
                                ls.end + ls.direction(1.0),
                                fs.start,
                                fs.start - fs.direction(0.0),
                                eps = 10E8
                            )
                            if (i !== Vector2.INFINITY) {
                                lineTo(i)
                                lineTo(fs.start)
                            } else {
                                lineTo(fs.start)
                            }
                        }
                    }
                }
                for (offsetSegment in offsetContour.segments) {
                    segment(offsetSegment)
                }

            }
            if (this@ShapeContour.closed) {
                close()
            }
        }

        val postProc = false

        var final = candidateContour.removeLoops()

        if (postProc && !final.empty) {
            val head = Segment(
                segments[0].start + segments[0].normal(0.0)
                    .perpendicular(polarity) * 1000.0, segments[0].start
            ).offset(distance).firstOrNull()?.copy(end = final.segments[0].start)?.contour

            val tail = Segment(
                segments.last().end,
                segments.last().end - segments.last().normal(1.0)
                    .perpendicular(polarity) * 1000.0
            ).offset(distance).firstOrNull()?.copy(start = final.segments.last().end)?.contour

            if (head != null) {
                val headInts = intersections(final, head)
                if (headInts.size == 1) {
                    final = final.sub(headInts[0].a.contourT, 1.0)
                }
                if (headInts.size > 1) {
                    val sInts = headInts.sortedByDescending { it.a.contourT }
                    final = final.sub(sInts[0].a.contourT, 1.0)
                }
            }
//            final = head + final
//
            if (tail != null) {
                val tailInts = intersections(final, tail)
                if (tailInts.size == 1) {
                    final = final.sub(0.0, tailInts[0].a.contourT)
                }
                if (tailInts.size > 1) {
                    val sInts = tailInts.sortedBy { it.a.contourT }
                    final = final.sub(0.0, sInts[0].a.contourT)
                }
            }

//            final = final + tail

        }

        return final
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
     * Samples a new [ShapeContour] from the current [ShapeContour] starting at [startT] and ending at [endT].
     *
     * @param startT Starting point in range `0.0` to less than `1.0`.
     * @param endT Ending point in range `0.0` to less than `1.0`.
     * @return Subcontour
     */
    fun sub(startT: Double, endT: Double): ShapeContour {
        if (empty) {
            return EMPTY
        }

        require(startT == startT) { "u0 is NaN" }
        require(endT == endT) { "u1 is NaN" }

        if (abs(startT - endT) < 10E-6) {
            return EMPTY
        }

        var t0 = startT
        var t1 = endT

        if (closed && (t1 < t0 || t1 > 1.0 || t0 > 1.0 || t0 < 0.0 || t1 < 0.0)) {
            val diff = t1 - t0
            t0 = mod(t0, 1.0)
            if (abs(diff) < (1.0 - 10E-6)) {
                return if (diff > 0.0) {
                    t1 = t0 + diff
                    if (t1 > 1.0) {
                        sub(t0, 1.0) + sub(0.0, t1 - 1.0)
                    } else {
                        sub(t0, t1)
                    }
                } else {
                    t1 = t0 + diff
                    if (t1 < 0) {
                        sub(t1 + 1.0, 1.0) + sub(0.0, t0)
                    } else {
                        sub(t1, t0)
                    }
                }
            } else {
                t1 = if (diff < 0.0) {
                    t0 - 1.0
                } else {
                    t0 + 1.0
                }
                if (t1 > 1.0) {
                    return sub(t0, 1.0) + sub(0.0, t1 - 1.0)
                }
                if (t1 < 1.0) {
                    return sub(t0, 1.0) + sub(0.0, t1 + 1.0)
                }
            }
        }

        t0 = t0.coerceIn(0.0, 1.0)
        t1 = t1.coerceIn(0.0, 1.0)

        var z0 = t0
        var z1 = t1

        if (t0 > t1) {
            z0 = t1
            z1 = t0
        }

        val length = segments.size.toDouble()
        var segment0 = (z0 * length).toInt()
        val segmentOffset0 = if (segment0 < segments.size) z0 * length % 1.0 else 1.0
        var segment1 = (z1 * length).toInt()
        val segmentOffset1 = if (segment1 < segments.size) z1 * length % 1.0 else 1.0

        segment1 = min(segments.size - 1, segment1)
        segment0 = min(segments.size - 1, segment0)

        val newSegments = mutableListOf<Segment>()
        val epsilon = 0.000001

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
    val close
        get() = if (empty) EMPTY else {
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

    @Deprecated("complicated semantics")
    fun map(closed: Boolean = this.closed, mapper: (Segment) -> Segment): ShapeContour {
        val segments = segments.map(mapper)
        val fixedSegments = mutableListOf<Segment>()

        if (segments.size > 1) {
            for (i in 0 until segments.size - 1) {
                val left = segments[i]
                val right = segments[i + 1]
                val fixLeft = Segment(left.start, left.control, right.start)
                fixedSegments.add(fixLeft)
            }
            if (closed) {
                val left = segments.last()
                val right = segments.first()
                fixedSegments.add(
                    Segment(
                        left.start,
                        left.control,
                        right.start
                    )
                )
            } else {
                fixedSegments.add(segments.last())
            }
        }
        return ShapeContour(
            if (segments.size > 1) fixedSegments else segments,
            closed,
            polarity
        )
    }

    /** Applies a boolean union operation between the [ShapeContour] and a [Shape]. */
    @Suppress("unused")
    fun union(other: Shape): Shape = union(this.shape, other)

    /** Applies a boolean difference operation between the [ShapeContour] and another [Shape]. */
    @Suppress("unused")
    fun difference(other: Shape): Shape = difference(this, other)

    /** Applies a boolean intersection operation between the [ShapeContour] and a [Shape]. */
    @Suppress("unused")
    fun intersection(other: Shape): Shape = intersection(this, other)

    /** Calculates a [List] of all intersections between the [ShapeContour] and a [Segment]. */
    @Suppress("unused")
    fun intersections(other: Segment) = intersections(this, other.contour)

    /** Calculates a [List] of all intersections between the [ShapeContour] and another [ShapeContour]. */
    @Suppress("unused")
    fun intersections(other: ShapeContour) = intersections(this, other)

    /** Calculates a [List] of all intersections between the [ShapeContour] and a [Shape]. */
    @Suppress("unused")
    fun intersections(other: Shape) = intersections(this.shape, other)
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