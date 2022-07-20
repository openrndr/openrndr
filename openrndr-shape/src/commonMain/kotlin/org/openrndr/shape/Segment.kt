package org.openrndr.shape

import org.openrndr.math.*
import org.openrndr.shape.internal.BezierCubicSampler2D
import org.openrndr.shape.internal.BezierQuadraticSampler2D
import kotlin.jvm.JvmOverloads
import kotlin.math.*

/**
 * Creates a new [Segment], which specifies a linear
 * or a Bézier curve path between two anchor points
 * (and up to two control points for curvature).
 */
class Segment : ShapeContourProvider {
    /** The start point of the [Segment]. */
    val start: Vector2

    /** The end point of the [Segment]. */
    val end: Vector2

    /**
     * Array of control points which control the curvature of the [Segment].
     *
     * Returns 0 if the [Segment] is linear.
     */
    val control: Array<Vector2>

    /**
     * Indicates whether or not the [Segment] is [linear][SegmentType.LINEAR].
     */
    val linear: Boolean get() = control.isEmpty()

    val corner: Boolean

    /**
     * Returns the type of the segment.
     */
    val type: SegmentType
        get() {
            return if (linear) {
                SegmentType.LINEAR
            } else {
                if (control.size == 1) {
                    SegmentType.QUADRATIC
                } else {
                    SegmentType.CUBIC
                }
            }
        }

    private var lut: List<Vector2>? = null

    /**
     * Linear segment constructor.
     *
     * @param start The starting anchor point.
     * @param end The ending anchor point.
     */
    @JvmOverloads constructor(start: Vector2, end: Vector2, corner: Boolean = true) {
        this.start = start
        this.end = end
        this.control = emptyArray()
        this.corner = corner
    }

    /**
     * Quadratic Bézier segment constructor.
     *
     * @param start The starting anchor point.
     * @param c0 The control point.
     * @param end The ending anchor point.
     */
    @JvmOverloads constructor(start: Vector2, c0: Vector2, end: Vector2, corner: Boolean = true) {
        this.start = start
        this.control = arrayOf(c0)
        this.end = end
        this.corner = corner
    }

    /**
     * Cubic Bézier segment constructor.
     *
     * @param start The starting anchor point.
     * @param c0 The first control point.
     * @param c1 The second control point
     * @param end The ending anchor point.
     */
    @JvmOverloads constructor(start: Vector2, c0: Vector2, c1: Vector2, end: Vector2, corner: Boolean = true) {
        this.start = start
        this.control = arrayOf(c0, c1)
        this.end = end
        this.corner = corner
    }

    @JvmOverloads constructor(start: Vector2, control: Array<Vector2>, end: Vector2, corner: Boolean = true) {
        this.start = start
        this.control = control
        this.end = end
        this.corner = corner
    }

    @Suppress("unused")
    fun lut(size: Int = 100): List<Vector2> {
        if (lut == null || lut!!.size != size) {
            lut = (0..size).map { position((it.toDouble() / size)) }
        }
        return lut!!
    }

    fun on(point: Vector2, error: Double = 5.0): Double? {
        val lut = lut()
        var hits = 0
        var t = 0.0
        for (i in lut.indices) {
            if ((lut[i] - point).squaredLength < error * error) {
                hits++
                t += i.toDouble() / lut.size
            }
        }
        return if (hits > 0) t / hits else null
    }

    /**
     * Estimate [t](https://pomax.github.io/bezierinfo/#explanation) value for a given length
     * @return A value between `0.0` and `1.0`.
     */
    fun tForLength(length: Double): Double {
        if (type == SegmentType.LINEAR) {
            return (length / this.length).coerceIn(0.0, 1.0)
        }

        val segmentLength = this.length
        val cLength = length.coerceIn(0.0, segmentLength)

        if (cLength == 0.0) {
            return 0.0
        }
        if (cLength >= segmentLength) {
            return 1.0
        }
        var summedLength = 0.0
        lut(100)
        val cLut = lut ?: error("no lut")
        val partitionCount = cLut.size - 1

        val dt = 1.0 / partitionCount
        for ((index, _ /*point*/) in lut!!.withIndex()) {
            if (index < lut!!.size - 1) {
                val p0 = cLut[index]
                val p1 = cLut[index + 1]
                val partitionLength = p0.distanceTo(p1)
                summedLength += partitionLength
                if (summedLength >= length) {
                    val localT = index.toDouble() / partitionCount
                    val overshoot = summedLength - length
                    return localT + (overshoot / partitionLength) * dt
                }
            }
        }
        return 1.0
    }

    /**
     * Calculates the point at a given distance along this [Segment].
     * @param length the distance along the [Segment].
     * @param distanceTolerance the tolerance used for simplifying the [Segment], lower values
     * result in more accurate results, but slower calculation.
     *
     * @see [Segment.adaptivePositions]
     */
    fun pointAtLength(length: Double, distanceTolerance: Double = 0.5): Vector2 {
        return if (length <= 0.0) {
            start
        } else if (length >= this.length) {
            end
        } else if (isStraight(distanceTolerance)) {
            start + (end - start) / this.length * length
        } else {
            var remainingLength = length
            var currentPoint = start
            val points = adaptivePositions(distanceTolerance)
            for (point in points) {
                val segmentLength = currentPoint.distanceTo(point)
                if (remainingLength <= segmentLength) {
                    val currentVector = point - currentPoint
                    val tangent = currentVector / segmentLength
                    return currentPoint + tangent * remainingLength
                }
                remainingLength -= segmentLength
                currentPoint = point
            }
            end
        }
    }

    @Suppress("unused")
    private fun closest(points: List<Vector2>, query: Vector2): Pair<Int, Vector2> {
        var closestIndex = 0
        var closestValue = points[0]

        var closestDistance = Double.POSITIVE_INFINITY
        for (i in points.indices) {
            val distance = (points[i] - query).squaredLength
            if (distance < closestDistance) {
                closestIndex = i
                closestValue = points[i]
                closestDistance = distance
            }
        }
        return Pair(closestIndex, closestValue)
    }

    /**
     * Find point on segment nearest to given [point].
     * @param point The query point.
     */
    fun nearest(point: Vector2): SegmentPoint {
        val t = when (type) {
            SegmentType.LINEAR -> {
                val dir = end - start
                val relativePoint = point - start
                ((dir dot relativePoint) / dir.squaredLength).coerceIn(0.0, 1.0)
            }
            SegmentType.QUADRATIC -> {
                val qa = start - point
                val ab = control[0] - start
                val bc = end - control[0]
                val qc = end - point
                val ac = end - start
                val br = start + end - control[0] - control[0]

                var minDistance = sign(ab cross qa) * qa.length
                var param = -(qa dot ab) / (ab dot ab)

                val distance = sign(bc cross qc) * qc.length
                if (abs(distance) < abs(minDistance)) {
                    minDistance = distance
                    param =
                        max(1.0, ((point - control[0]) dot bc) / (bc dot bc))
                }

                val a = br dot br
                val b = 3.0 * (ab dot br)
                val c = (2.0 * (ab dot ab)) + (qa dot br)
                val d = qa dot ab
                val ts = solveCubic(a, b, c, d)

                for (t in ts) {
                    if (t > 0 && t < 1) {
                        val endpoint = position(t)
                        val distance2 = sign(ac cross (endpoint - point)) * (endpoint - point).length
                        if (abs(distance2) < abs(minDistance)) {
                            minDistance = distance2
                            param = t
                        }
                    }
                }
                param.coerceIn(0.0, 1.0)
            }
            SegmentType.CUBIC -> {
                fun sign(n: Double): Double {
                    val s = n.sign
                    return if (s == 0.0) -1.0 else s
                }

                val qa = start - point
                val ab = control[0] - start
                val bc = control[1] - control[0]
                val cd = end - control[1]
                val qd = end - point
                val br = bc - ab
                val ax = (cd - bc) - br

                var minDistance = sign(ab cross qa) * qa.length
                var param = -(qa dot ab) / (ab dot ab)

                var distance = sign(cd cross qd) * qd.length
                if (abs(distance) < abs(minDistance)) {
                    minDistance = distance
                    param = max(1.0, (point - control[1] dot cd) / (cd dot cd))
                }
                val searchStarts = 4
                val searchSteps = 8

                for (i in 0 until searchStarts) {
                    var t = i.toDouble() / (searchStarts - 1)
                    var step = 0
                    while (true) {
                        val qpt = position(t) - point
                        distance = sign(direction(t) cross qpt) * qpt.length
                        if (abs(distance) < abs(minDistance)) {
                            minDistance = distance
                            param = t
                        }
                        if (step == searchSteps) {
                            break
                        }
                        val d1 = (ax * (3 * t * t)) + br * (6 * t) + ab * 3.0
                        val d2 = (ax * (6 * t)) + br * 6.0
                        val dt = (qpt dot d1) / ((d1 dot d1) + (qpt dot d2))
                        if (abs(dt) < 1e-14) {
                            break
                        }
                        t -= dt
                        if (t < 0 || t > 1) {
                            break
                        }
                        step++
                    }
                }
                param.coerceIn(0.0, 1.0)
            }
        }
        val closest = position(t)
        return SegmentPoint(this, t, closest)
    }

    /**
     * Applies given linear transformation.
     */
    fun transform(transform: Matrix44): Segment {
        return if (transform === Matrix44.IDENTITY) {
            this
        } else {
            val tStart = (transform * (start.xy01)).div.xy
            val tEnd = (transform * (end.xy01)).div.xy
            val tControl = when (control.size) {
                2 -> arrayOf((transform * control[0].xy01).div.xy, (transform * control[1].xy01).div.xy)
                1 -> arrayOf((transform * control[0].xy01).div.xy)
                else -> emptyArray()
            }
            Segment(tStart, tControl, tEnd)
        }
    }

    /**
     * Recursively subdivides [Segment] to approximate Bézier curve.
     *
     * @param distanceTolerance The square of the maximal distance of each point from curve.
     */
    fun adaptivePositions(distanceTolerance: Double = 0.5): List<Vector2> = when (control.size) {
        0 -> listOf(start, end)
        1 -> BezierQuadraticSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], end).first
        2 -> BezierCubicSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], control[1], end).first
        else -> throw RuntimeException("unsupported number of control points")
    }

    /**
     * Recursively subdivides [Segment] to approximate Bézier curve.
     *
     * For a more detailed breakdown, see [http://agg.sourceforge.net/antigrain.com/research/adaptive_bezier/index.html].
     *
     * @param distanceTolerance The square of the maximal distance of each point from curve.
     * @return A pair of lists. The first list contains positions, the second list the points' normals.
     */
    fun adaptivePositionsAndNormals(distanceTolerance: Double = 0.5): Pair<List<Vector2>, List<Vector2>> = when (control.size) {
        0 -> Pair(listOf(start, end), listOf(end - start, end - start))
        1 -> BezierQuadraticSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], end)
        2 -> BezierCubicSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], control[1], end)
        else -> throw RuntimeException("unsupported number of control points")
    }

    /**
     * Samples specified amount of points on the [Segment].
     * @param pointCount The number of points to sample.
     */
    fun equidistantPositions(pointCount: Int): List<Vector2> {
        return sampleEquidistant(adaptivePositions(), pointCount)
    }

    // work around length-by-lazy property being initialized before the secondary constructor initializes the relevant fields
    private val internalLength: Double
        get() = when (control.size) {
            0 -> (end - start).length
            1, 2 -> sumDifferences(adaptivePositions())
            else -> throw RuntimeException("unsupported number of control points")
        }

    /** Calculates approximate Euclidean length of the [Segment]. */
    val length by lazy {
        internalLength
    }

    /**
     * Returns a point on the segment.
     *
     * @param ut unfiltered [t](https://pomax.github.io/bezierinfo/#explanation), will be clamped between 0.0 and 1.0.
     * @return [Vector2] that lies on the [Segment].
     */
    fun position(ut: Double): Vector2 {
        val t = ut.coerceIn(0.0, 1.0)
        return when (control.size) {
            0 -> Vector2(
                start.x * (1.0 - t) + end.x * t,
                start.y * (1.0 - t) + end.y * t
            )
            1 -> bezier(start, control[0], end, t)
            2 -> bezier(start, control[0], control[1], end, t)
            else -> error("unsupported number of control points")
        }
    }

    /** Returns the direction [Vector2] of between the [Segment] anchor points. */
    fun direction(): Vector2 = (end - start).normalized

    fun direction(t: Double): Vector2 = derivative(t).normalized

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
    fun pose(t: Double, polarity: YPolarity = YPolarity.CW_NEGATIVE_Y): Matrix44 {
        val dx = direction(t).xy0.xyz0
        val dy = direction(t).perpendicular(polarity).xy0.xyz0
        val dt = position(t).xy01
        return Matrix44.fromColumnVectors(dx, dy, Vector4.UNIT_Z, dt)
    }

    /**
     * Returns the [t](https://pomax.github.io/bezierinfo/#explanation)
     * values of the extrema for the current [Segment].
     *
     * Either one or two *t* values in which the curve
     * is the most distant from an imaginary
     * straight line between the two anchor points.
     */
    fun extrema(): List<Double> {
        val dPoints = dPoints()
        return when {
            linear -> emptyList()
            control.size == 1 -> {
                val xRoots = roots(dPoints[0].map { it.x })
                val yRoots = roots(dPoints[0].map { it.y })
                (xRoots + yRoots).distinct().sorted().filter { it in 0.0..1.0 }
            }
            control.size == 2 -> {
                val xRoots = roots(dPoints[0].map { it.x }) + roots(dPoints[1].map { it.x })
                val yRoots = roots(dPoints[0].map { it.y }) + roots(dPoints[1].map { it.y })
                (xRoots + yRoots).distinct().sorted().filter { it in 0.0..1.0 }
            }
            else -> throw RuntimeException("not supported")
        }
    }

    /** Returns the extrema points as [Vector2]s for current [Segment] */
    @Suppress("unused")
    fun extremaPoints(): List<Vector2> = extrema().map { position(it) }

    /** Returns the bounding box. */
    val bounds: Rectangle
        get() = (listOf(start, end) + extremaPoints()).bounds


    private fun dPoints(): List<List<Vector2>> {
        val points = listOf(start, *control, end)
        var d = points.size
        var c = d - 1
        val dPoints = mutableListOf<List<Vector2>>()
        var p = points
        while (d > 1) {
            val list = mutableListOf<Vector2>()
            for (j in 0 until c) {
                list.add(
                    Vector2(
                        c * (p[j + 1].x - p[j].x),
                        c * (p[j + 1].y - p[j].y)
                    )
                )
            }
            dPoints.add(list)
            p = list
            d--
            c--
        }
        return dPoints
    }

    fun offset(distance: Double, stepSize: Double = 0.01, yPolarity: YPolarity = YPolarity.CW_NEGATIVE_Y): List<Segment> {
        return if (linear) {
            val n = normal(0.0, yPolarity)
            if (distance > 0.0) {
                listOf(Segment(start + distance * n, end + distance * n))
            } else {
                val d = direction()
                val s = distance.coerceAtMost(length / 2.0)
                val candidate = Segment(
                    start - s * d + distance * n,
                    end + s * d + distance * n
                )
                if (candidate.length > 0.0) {
                    listOf(candidate)
                } else {
                    emptyList()
                }
            }
        } else {
            reduced(stepSize).map { it.scale(distance, yPolarity) }
        }
    }

    private fun angle(o: Vector2, v1: Vector2, v2: Vector2): Double {
        val dx1 = v1.x - o.x
        val dy1 = v1.y - o.y
        val dx2 = v2.x - o.x
        val dy2 = v2.y - o.y
        val cross = dx1 * dy2 - dy1 * dx2
        val dot = dx1 * dx2 + dy1 * dy2
        return atan2(cross, dot)
    }

    /**
     * Determines if the [Segment] forms a straight line.
     *
     * If the given [Segment] has control points,
     * the function verifies that they do not add any curvature to the path.
     *
     * @param epsilon The margin of error for what's considered a straight line.
     */
    @Suppress("unused")
    fun isStraight(epsilon: Double = 0.01): Boolean {
        return when (control.size) {
            2 -> {
                val dl = (end - start).normalized
                val d0 = (control[0] - start).normalized
                val d1 = (end - control[0]).normalized

                val dp0 = dl.dot(d0)
                val dp1 = (-dl).dot(d1)

                dp0 * dp0 + dp1 * dp1 > (2.0 - 2 * epsilon)
            }
            1 -> {
                val dl = (end - start).normalized
                val d0 = (control[0] - start).normalized

                val dp0 = dl.dot(d0)
                dp0 * dp0 > (1.0 - epsilon)
            }
            else -> {
                true
            }
        }

    }

    val simple: Boolean
        get() {
            if (linear) {
                return true
            }
            if (control.size == 2) {
                val a1 = angle(start, end, control[0])
                val a2 = angle(start, end, control[1])

                if ((a1 > 0 && a2 < 0) || (a1 < 0 && a2 > 0))
                    return false
            }
            val n1 = normal(0.0, YPolarity.CW_NEGATIVE_Y)
            val n2 = normal(1.0, YPolarity.CW_NEGATIVE_Y)
            val s = n1 dot n2
            return s >= 0.9
        }

    private fun splitOnExtrema(): List<Segment> {
        var extrema = extrema().toMutableList()

        if (isStraight(0.05)) {
            return listOf(this)
        }

        if (simple && extrema.isEmpty()) {
            return listOf(this)
        }

        if (extrema.isEmpty()) {
            return listOf(this)
        }
        if (extrema[0] <= 0.01) {
            extrema[0] = 0.0
        } else {
            extrema = (mutableListOf(0.0) + extrema).toMutableList()
        }

        if (extrema.last() < 0.99) {
            extrema = (extrema + listOf(1.0)).toMutableList()
        } else if (extrema.last() >= 0.99) {
            extrema[extrema.lastIndex] = 1.0
        }

        return extrema.zipWithNext().map {
            sub(it.first, it.second)
        }
    }

    private fun splitToSimple(step: Double): List<Segment> {
        var t1 = 0.0
        var t2 = 0.0
        val result = mutableListOf<Segment>()
        while (t2 <= 1.0) {
            t2 = t1 + step
            while (t2 <= 1.0 + step) {
                val segment = sub(t1, t2)
                if (!segment.simple) {
                    t2 -= step
                    if (abs(t1 - t2) < step) {
                        return listOf(this)
                    }
                    val segment2 = sub(t1, t2)
                    result.add(segment2)
                    t1 = t2
                    break
                }
                t2 += step
            }

        }
        if (t1 < 1.0) {
            result.add(sub(t1, 1.0))
        }
        if (result.isEmpty()) {
            result.add(this)
        }

        return result
    }

    fun reduced(stepSize: Double = 0.01): List<Segment> {
        val pass1 = splitOnExtrema()
        //return pass1
        return pass1.flatMap { it.splitToSimple(stepSize) }
    }

    fun scale(scale: Double, polarity: YPolarity) = scale(polarity) { scale }

    val clockwise
        get() = angle(start, end, control[0]) > 0

    fun scale(polarity: YPolarity, scale: (Double) -> Double): Segment {
        if (control.size == 1) {
            return cubic.scale(polarity, scale)
        }

        val newStart = start + normal(0.0, polarity) * scale(0.0)
        val newEnd = end + normal(1.0, polarity) * scale(1.0)

        val a = LineSegment(newStart, start)
        val b = LineSegment(newEnd, end)

        val o = intersection(a, b, 1E7)

        if (o != Vector2.INFINITY) {
            val newControls = control.mapIndexed { index, it ->
                val d = it - o
                val rc = scale((index + 1.0) / 3.0)
                val s = normal(0.0, polarity).dot(d).sign
                val nd = d.normalized * s
                it + rc * nd
            }
            return Segment(newStart, newControls.toTypedArray(), newEnd)
        } else {
            val newControls = control.mapIndexed { index, it ->
                val rc = scale((index + 1.0) / 3.0)
                it + rc * normal((index + 1.0), polarity)
            }
            return Segment(newStart, newControls.toTypedArray(), newEnd)
        }
    }

    /** Converts the [Segment] to a cubic Bézier curve. */
    val cubic: Segment
        get() = when {
            control.size == 2 -> this
            control.size == 1 -> {
                Segment(
                    start,
                    start * (1.0 / 3.0) + control[0] * (2.0 / 3.0),
                    control[0] * (2.0 / 3.0) + end * (1.0 / 3.0),
                    end
                )
            }
            linear -> {
                val delta = end - start
                Segment(
                    start,
                    start + delta * (1.0 / 3.0),
                    start + delta * (2.0 / 3.0),
                    end
                )
            }
            else -> error("cannot convert to cubic segment")
        }

    /** Converts the [Segment] to a quadratic Bézier curve. */
    val quadratic: Segment
        get() = when {
            control.size == 1 -> this
            linear -> {
                val delta = end - start
                Segment(start, start + delta * (1.0 / 2.0), end)
            }
            else -> error("cannot convert to quadratic segment")
        }


    fun derivative(t: Double): Vector2 = when {
        linear -> end - start
        control.size == 1 -> safeDerivative(start, control[0], end, t)
        control.size == 2 -> safeDerivative(
            start,
            control[0],
            control[1],
            end,
            t
        )
        else -> throw RuntimeException("not implemented")
    }

    /**
     * Returns a normal [Vector2] at given value of
     * [t](https://pomax.github.io/bezierinfo/#explanation)
     * in the range of `0.0` to `1.0`.
     */
    fun normal(ut: Double, polarity: YPolarity = YPolarity.CW_NEGATIVE_Y): Vector2 {
        return direction(ut).perpendicular(polarity)
    }

    /** Reverses the order of control points of the given path [Segment]. */
    val reverse: Segment
        get() {
            return when (control.size) {
                0 -> Segment(end, start)
                1 -> Segment(end, control[0], start)
                2 -> Segment(end, control[1], control[0], start)
                else -> throw RuntimeException("unsupported number of control points")
            }
        }

    /**
     * Samples a new [Segment] from the current [Segment] starting at [t0] and ending at [t1].
     *
     * @param t0 The starting value of [t](https://pomax.github.io/bezierinfo/#explanation) in the range of `0.0` to `1.0`.
     * @param t1 The ending value of *t* in the range of `0.0` to `1.0`.
     */
    fun sub(t0: Double, t1: Double): Segment {
        // ftp://ftp.fu-berlin.de/tex/CTAN/dviware/dvisvgm/src/Bezier.cpp
        var z0 = t0
        var z1 = t1

        if (t0 > t1) {
            z1 = t0
            z0 = t1
        }

        return when {
            z0 == 0.0 -> split(z1)[0]
            z1 == 1.0 -> split(z0).last()
            else -> split(z0).last().split(map(z0, 1.0, 0.0, 1.0, z1))[0]
        }
    }

    /**
     * Splits the path into one or two parts, depending on if the cut was successful.
     *
     * @param t The point at which to split the [Segment] at.
     * @return An array of parts, depending on the split point this is one or two entries long.
     */
    fun split(t: Double): Array<Segment> {
        val u = t.clamp(0.0, 1.0)
        val splitSigma = 10E-6

        if (u < splitSigma) {
            return arrayOf(Segment(start, start), this)
        }

        if (u >= 1.0 - splitSigma) {
            return arrayOf(this, Segment(end, end))
        }

        if (linear) {
            val cut = start + (end.minus(start) * u)
            return arrayOf(Segment(start, cut), Segment(cut, end))
        } else {
            when (control.size) {
                2 -> {
                    @Suppress("UnnecessaryVariable")
                    val z = u
                    val z2 = z * z
                    val z3 = z * z * z
                    val iz = 1 - z
                    val iz2 = iz * iz
                    val iz3 = iz * iz * iz

                    val lsm = Matrix44(
                        1.0, 0.0, 0.0, 0.0,
                        iz, z, 0.0, 0.0,
                        iz2, 2.0 * iz * z, z2, 0.0,
                        iz3, 3.0 * iz2 * z, 3.0 * iz * z2, z3
                    )

                    val px = Vector4(start.x, control[0].x, control[1].x, end.x)
                    val py = Vector4(start.y, control[0].y, control[1].y, end.y)

                    val plx = lsm * px//.multiply(lsm)
                    val ply = lsm * py// py.multiply(lsm)

                    val pl0 = Vector2(plx.x, ply.x)
                    val pl1 = Vector2(plx.y, ply.y)
                    val pl2 = Vector2(plx.z, ply.z)
                    val pl3 = Vector2(plx.w, ply.w)

                    val left = Segment(pl0, pl1, pl2, pl3)

                    val rsm = Matrix44(
                        iz3, 3.0 * iz2 * z, 3.0 * iz * z2, z3,
                        0.0, iz2, 2.0 * iz * z, z2,
                        0.0, 0.0, iz, z,
                        0.0, 0.0, 0.0, 1.0
                    )

                    val prx = rsm * px
                    val pry = rsm * py

                    val pr0 = Vector2(prx.x, pry.x)
                    val pr1 = Vector2(prx.y, pry.y)
                    val pr2 = Vector2(prx.z, pry.z)
                    val pr3 = Vector2(prx.w, pry.w)

                    val right = Segment(pr0, pr1, pr2, pr3)

                    return arrayOf(left, right)
                }
                1 -> {
                    @Suppress("UnnecessaryVariable")
                    val z = u
                    val iz = 1 - z
                    val iz2 = iz * iz
                    val z2 = z * z

                    val lsm = Matrix44(
                        1.0, 0.0, 0.0, 0.0,
                        iz, z, 0.0, 0.0,
                        iz2, 2.0 * iz * z, z2, 0.0,
                        0.0, 0.0, 0.0, 0.0
                    )

                    val px = Vector4(start.x, control[0].x, end.x, 0.0)
                    val py = Vector4(start.y, control[0].y, end.y, 0.0)

                    val plx = lsm * px
                    val ply = lsm * py

                    val left = Segment(
                        Vector2(plx.x, ply.x),
                        Vector2(plx.y, ply.y),
                        Vector2(plx.z, ply.z)
                    )

                    val rsm = Matrix44(
                        iz2, 2.0 * iz * z, z2, 0.0,
                        0.0, iz, z, 0.0,
                        0.0, 0.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 0.0
                    )

                    val prx = rsm * px
                    val pry = rsm * py

                    val rdx0 = prx.y - prx.x
                    val rdy0 = pry.y - pry.x

                    val rdx1 = prx.z - prx.y
                    val rdy1 = pry.z - pry.y


                    require(rdx0 * rdx0 + rdy0 * rdy0 > 0.0) {
                        "Q start/c0 overlap after split on $t $this"
                    }
                    require(rdx1 * rdx1 + rdy1 * rdy1 > 0.0) {
                        "Q end/c0 overlap after split on $t $this"
                    }

                    val right = Segment(
                        Vector2(prx.x, pry.x),
                        Vector2(prx.y, pry.y),
                        Vector2(prx.z, pry.z)
                    )

                    return arrayOf(left, right)
                }
                else -> error("unsupported number of control points")
            }
        }
    }

    override fun toString(): String {
        return "Segment(start=$start, end=$end, control=${control.contentToString()})"
    }

    /** Returns a shallow copy of the [Segment]. */
    fun copy(start: Vector2 = this.start, control: Array<Vector2> = this.control, end: Vector2 = this.end): Segment {
        return Segment(start, control, end)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as Segment

        if (start != other.start) return false
        if (end != other.end) return false
        if (!control.contentEquals(other.control)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + control.contentHashCode()
        return result
    }

    operator fun times(scale: Double): Segment {
        return when (type) {
            SegmentType.LINEAR -> Segment(start * scale, end * scale)
            SegmentType.QUADRATIC -> Segment(
                start * scale,
                control[0] * scale,
                end * scale
            )
            SegmentType.CUBIC -> Segment(
                start * scale,
                control[0] * scale,
                control[1] * scale,
                end * scale
            )
        }
    }

    operator fun div(scale: Double): Segment {
        return when (type) {
            SegmentType.LINEAR -> Segment(start / scale, end / scale)
            SegmentType.QUADRATIC -> Segment(
                start / scale,
                control[0] / scale,
                end / scale
            )
            SegmentType.CUBIC -> Segment(
                start / scale,
                control[0] / scale,
                control[1] / scale,
                end / scale
            )
        }
    }

    operator fun minus(right: Segment): Segment {
        return if (this.type == right.type) {
            when (type) {
                SegmentType.LINEAR -> Segment(
                    start - right.start,
                    end - right.end
                )
                SegmentType.QUADRATIC -> Segment(
                    start - right.start,
                    control[0] - right.control[0],
                    end - right.end
                )
                SegmentType.CUBIC -> Segment(
                    start - right.start,
                    control[0] - right.control[0],
                    control[1] - right.control[1],
                    end - right.end
                )
            }
        } else {
            if (this.type.ordinal > right.type.ordinal) {
                when (type) {
                    SegmentType.LINEAR -> error("impossible?")
                    SegmentType.QUADRATIC -> this - right.quadratic
                    SegmentType.CUBIC -> this - right.cubic
                }
            } else {
                when (right.type) {
                    SegmentType.LINEAR -> error("impossible?")
                    SegmentType.QUADRATIC -> this.quadratic - right
                    SegmentType.CUBIC -> this.cubic - right
                }
            }
        }
    }

    operator fun plus(right: Segment): Segment {
        return if (this.type == right.type) {
            when (type) {
                SegmentType.LINEAR -> Segment(
                    start + right.start,
                    end + right.end
                )
                SegmentType.QUADRATIC -> Segment(
                    start + right.start,
                    control[0] + right.control[0],
                    end + right.end
                )
                SegmentType.CUBIC -> Segment(
                    start + right.start,
                    control[0] + right.control[0],
                    control[1] + right.control[1],
                    end + right.end
                )
            }
        } else {
            if (this.type.ordinal > right.type.ordinal) {
                when (type) {
                    SegmentType.LINEAR -> error("impossible?")
                    SegmentType.QUADRATIC -> this + right.quadratic
                    SegmentType.CUBIC -> this + right.cubic
                }
            } else {
                when (right.type) {
                    SegmentType.LINEAR -> error("impossible?")
                    SegmentType.QUADRATIC -> this.quadratic + right
                    SegmentType.CUBIC -> this.cubic + right
                }
            }
        }
    }

    override val contour: ShapeContour
        get() = ShapeContour(listOf(this), false)

}

private fun sumDifferences(points: List<Vector2>) =
        (0 until points.size - 1).sumOf { (points[it] - points[it + 1]).length }

/** Converts spline to a [Segment]. */
fun CatmullRom2.toSegment(): Segment {
    val d1a2 = (p1 - p0).length.pow(2 * alpha)
    val d2a2 = (p2 - p1).length.pow(2 * alpha)
    val d3a2 = (p3 - p2).length.pow(2 * alpha)
    val d1a = (p1 - p0).length.pow(alpha)
    val d2a = (p2 - p1).length.pow(alpha)
    val d3a = (p3 - p2).length.pow(alpha)

    val b0 = p1
    val b1 = (p2 * d1a2 - p0 * d2a2 + p1 * (2 * d1a2 + 3 * d1a * d2a + d2a2)) / (3 * d1a * (d1a + d2a))
    val b2 = (p1 * d3a2 - p3 * d2a2 + p2 * (2 * d3a2 + 3 * d3a * d2a + d2a2)) / (3 * d3a * (d3a + d2a))
    val b3 = p2

    return Segment(b0, b1, b2, b3)
}