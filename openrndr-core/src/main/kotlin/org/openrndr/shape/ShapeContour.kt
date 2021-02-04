package org.openrndr.shape

import io.lacuna.artifex.Vec2
import org.openrndr.math.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

data class ShapeContour(val segments: List<Segment>, val closed: Boolean, val polarity: YPolarity = YPolarity.CW_NEGATIVE_Y) {
    companion object {
        val EMPTY = ShapeContour(emptyList(), false)

        fun fromPoints(points: List<Vector2>, closed: Boolean, polarity: YPolarity = YPolarity.CW_NEGATIVE_Y) =
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

    /**
     * convert to [Shape]
     */
    val shape get() = Shape(listOf(this))

    /**
     * calculate approximate Euclidean length of the contour
     */
    val length by lazy { segments.sumByDouble { it.length } }

    /**
     * calculate bounding box [Rectangle] of the contour
     */
    val bounds by lazy {
        sampleLinear().segments.flatMap {
            listOf(
                it.start,
                it.end
            )
        }.bounds
    }

    /**
     * determine winding order of the contour
     */
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

    /**
     * convert to list of single segment [ShapeContour]s
     */
    @Suppress("unused")
    val exploded: List<ShapeContour>
        get() = segments.map { ShapeContour(listOf(it), false, polarity) }

    /**
     * convert to contour with clock wise winding
     */
    val clockwise: ShapeContour get() = if (winding == Winding.CLOCKWISE) this else this.reversed

    /**
     * convert to contour with contour-clockwise winding
     */
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

    operator fun contains(point: Vector2): Boolean = closed && this.toRing2().test(
        Vec2(point.x, point.y)
    ).inside

    /**
     * Estimate t parameter value for a given length
     * @return a value between 0 and 1
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
     * Split a ShapeContour with another ShapeContour.
     */
    fun split(cutter: ShapeContour) = split(this, cutter)
    fun split(cutters: List<ShapeContour>) = split(this, cutters)

    fun offset(distance: Double, joinType: SegmentJoin = SegmentJoin.ROUND): ShapeContour {
        if (segments.size == 1) {
            return ShapeContour(
                segments[0].offset(
                    distance,
                    yPolarity = polarity
                ), false, polarity
            )
        }

        val offsets = segments.map { it.offset(distance, yPolarity = polarity) }.filter { it.isNotEmpty() }

        if (offsets.isEmpty()) {
            return ShapeContour(emptyList(), false)
        }

        val startPoint = if (closed) offsets.last().last().end else offsets.first().first().start

        return contour {
            moveTo(startPoint)
            for (offset in offsets) {
                val mOffset = offset.toMutableList()
                lastSegment?.let { ls ->
                    val fs = offset.first()
                    if (ls.type == SegmentType.LINEAR && fs.type == SegmentType.LINEAR) {
                        val i = intersection(
                            ls.start,
                            ls.end,
                            offset.first().start,
                            offset.first().end
                        )
                        if (i != Vector2.INFINITY && (i - ls.end).squaredLength > 10E-6) {
                            undo()
                            lineTo(i)
                            lineTo(fs.end)
                            mOffset.removeAt(0)
                        }
                    }
                }
                if (mOffset.isNotEmpty()) {
                    val delta = (mOffset.first().start - cursor)
                    val joinDistance = delta.length
                    if (joinDistance > 10E-6) {
                        when (joinType) {
                            SegmentJoin.BEVEL -> lineTo(mOffset.first().start)
                            SegmentJoin.ROUND -> arcTo(
                                crx = joinDistance * 0.5 * sqrt(2.0),
                                cry = joinDistance * 0.5 * sqrt(2.0),
                                angle = 90.0,
                                largeArcFlag = false,
                                sweepFlag = true,
                                end = mOffset.first().start
                            )
                            SegmentJoin.MITER -> {
                                val ls = lastSegment ?: offsets.last().last()
                                val fs = mOffset.first()
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
                    for (segment in mOffset) {
                        val d = (segment.start - cursor).length
                        if (d > 1.0) {
                            // TODO: I am not sure if this should happen at all, and if it should, if this is
                            // the best way to deal with it.
                            lineTo(segment.start)
                        }
                        segment(segment)
                    }
                }
            }
            if (this@ShapeContour.closed) {
                close()
            }
        }
    }

    val empty: Boolean
        get() {
            return this === EMPTY || segments.isEmpty()
        }

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

    /**
     * Evaluates the contour for the given position
     */
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

    @Suppress("unused")
    fun pose(t: Double): Matrix44 {
        val n = normal(t)
        val dx = n.perpendicular(polarity).xy0.xyz0
        val dy = n.xy0.xyz0
        val dt = position(t).xy01
        return Matrix44.fromColumnVectors(dx, dy, Vector4.UNIT_Z, dt)
    }

    fun adaptivePositions(distanceTolerance: Double = 0.5): List<Vector2> {
        return adaptivePositionsAndCorners(distanceTolerance).first
    }

    fun adaptivePositionsAndCorners(distanceTolerance: Double = 0.5): Pair<List<Vector2>, List<Boolean>> {
        if (empty) {
            return Pair(emptyList(), emptyList())
        }

        val adaptivePoints = mutableListOf<Vector2>()
        val corners = mutableListOf<Boolean>()
        for (segment in this.segments) {
            val samples = segment.adaptivePositions(distanceTolerance)
            samples.forEachIndexed { index, _ ->
                if (index == 0) {
                    corners.add(segment.corner)
                } else {
                    corners.add(false)
                }
            }

            samples.forEach {
                val last = adaptivePoints.lastOrNull()
                if (last == null || last.squaredDistanceTo(it) > 0.0) {
                    adaptivePoints.add(it)
                }
            }
        }
        adaptivePoints.zipWithNext().forEach {
            require(it.first.squaredDistanceTo(it.second) > 0.0)
        }
        return Pair(adaptivePoints, corners)
    }


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
     *
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
     * Adaptively sample the contour into line segments while still approximating the original contour
     * @param distanceTolerance controls the quality of the approximation
     * @return a ShapeContour composed of linear segments
     */
    fun sampleLinear(distanceTolerance: Double = 0.5) =
            if (empty) {
                EMPTY
            } else {
                fromPoints(adaptivePositions(distanceTolerance), closed, polarity)
            }

    /**
     * Sample the shape contour into line segments
     */
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
     * Sample a sub contour
     * @param startT starting point in [0, 1)
     * @param endT ending point in [0, 1)
     * @return sub contour
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
     * Checks if a give point lies on the contour
     * @param point the point to check
     * @param error what is the allowed error (unit-less, but likely in pixels)
     * @return the contour parameter in [0..1.0) if the point is within error `null` otherwise
     *
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
     * Project a point on the contour
     * @param point the point to project
     * @return a projected point that lies on the contour
     */
    fun nearest(point: Vector2): ContourPoint {
        val n = segments.map { it.nearest(point) }.minByOrNull { it.position.distanceTo(point) } ?: error("no segments")
        val segmentIndex = segments.indexOf(n.segment)
        val t = (segmentIndex + n.segmentT) / segments.size
        return ContourPoint(this, t, n.segment, n.segmentT, n.position)
    }

    val opened get() = ShapeContour(segments, false, polarity)
    val reversed get() = ShapeContour(
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

    @Suppress("unused")
    fun union(other: Shape): Shape = union(this.shape, other)

    @Suppress("unused")
    fun difference(other: Shape): Shape = difference(this, other)

    @Suppress("unused")
    fun intersection(other: Shape): Shape = intersection(this, other)

    @Suppress("unused")
    fun intersections(other: Segment) = intersections(this, other.contour)

    @Suppress("unused")
    fun intersections(other: ShapeContour) = intersections(this, other)

    @Suppress("unused")
    fun intersections(other: Shape) = intersections(this.shape, other)
}

/**
 * convert a List of [ShapeContour] items to a [Shape]
 */
val List<ShapeContour>.shape
    get() = Shape(this)

/**
 * convert to [ShapeContour]
 */
@Suppress("unused")
fun CatmullRomChain2.toContour(): ShapeContour =
    ShapeContour(segments.map { it.toSegment() }, this.loop)