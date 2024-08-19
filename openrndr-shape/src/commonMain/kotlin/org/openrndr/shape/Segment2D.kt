package org.openrndr.shape

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.openrndr.math.*

import org.openrndr.shape.internal.BezierCubicSamplerT
import org.openrndr.shape.internal.BezierQuadraticSamplerT
import kotlin.jvm.JvmRecord
import kotlin.math.*


interface BezierSegment<T : EuclideanVector<T>> {
    val start: T
    val control: List<T>
    val end: T

    /**
     * Indicates whether the [Segment2D] is [linear][SegmentType.LINEAR].
     */
    @Transient
    val linear: Boolean get() = control.isEmpty()

    /**
     * Returns the type of the segment.
     */
    @Transient
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

    /**
     * Returns a point on the segment.
     *
     * @param ut unfiltered [t](https://pomax.github.io/bezierinfo/#explanation), will be clamped between 0.0 and 1.0.
     * @return [T] that lies on the [BezierSegment].
     */
    fun position(ut: Double): T

    fun derivative(t: Double): T

    /** Returns the direction [T] of between the [Segment2D] anchor points. */
    fun direction(): T = (end - start).normalized

    fun direction(t: Double): T = derivative(t).normalized

    /**
     * Samples a new [Segment2D] from the current [Segment2D] starting at [t0] and ending at [t1].
     *
     * @param t0 The starting value of [t](https://pomax.github.io/bezierinfo/#explanation) in the range of `0.0` to `1.0`.
     * @param t1 The ending value of *t* in the range of `0.0` to `1.0`.
     */
    fun sub(t0: Double, t1: Double): BezierSegment<T> {
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
     * @param t The point at which to split the [Segment2D] at.
     * @return An array of parts, depending on the split point this is one or two entries long.
     */
    fun split(t: Double): Array<out BezierSegment<T>>

    @Transient
    val length: Double

    @Transient
    val cubic: BezierSegment<T>

    /**
     * Estimate [t](https://pomax.github.io/bezierinfo/#explanation) value for a given length
     * @return A value between `0.0` and `1.0`.
     */
    fun tForLength(length: Double): Double

    /**
     * Calculates the point at a given distance along this [Segment2D].
     * @param length the distance along the [Segment2D].
     * @param distanceTolerance the tolerance used for simplifying the [Segment2D], lower values
     * result in more accurate results, but slower calculation.
     *
     * @see [Segment2D.adaptivePositions]
     */
    fun pointAtLength(length: Double, distanceTolerance: Double): T {
        when {
            length <= 0.0 -> return start
            length >= this.length -> return end
        }
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
        return end
    }

    @Transient
    val reverse: BezierSegment<T>

    /**
     * Recursively subdivides [Segment2D] to approximate Bézier curve.
     *
     * @param distanceTolerance The square of the maximal distance of each point from curve.
     */
    fun adaptivePositions(distanceTolerance: Double = 0.5): List<T> =
        adaptivePositionsWithT(distanceTolerance).map { it.first }


    fun adaptivePositionsWithT(distanceTolerance: Double = 0.5): List<Pair<T, Double>> = when (control.size) {
        0 -> listOf(start to 0.0, end to 1.0)
        1 -> BezierQuadraticSamplerT<T>().apply { this.distanceTolerance = distanceTolerance }
            .sample(start, control[0], end)

        2 -> BezierCubicSamplerT<T>().apply { this.distanceTolerance = distanceTolerance }
            .sample(start, control[0], control[1], end)

        else -> throw RuntimeException("unsupported number of control points")
    }

    /**
     * Samples specified amount of points on the [Segment2D].
     * @param pointCount The number of points to sample.
     */
    fun equidistantPositions(pointCount: Int, distanceTolerance: Double = 0.5): List<T> {
        return sampleEquidistant(adaptivePositions(distanceTolerance), pointCount)
    }

    fun equidistantPositionsWithT(pointCount: Int, distanceTolerance: Double = 0.5): List<Pair<T, Double>> {
        return sampleEquidistantWithT(adaptivePositionsWithT(distanceTolerance), pointCount)
    }
}

/**
 * Creates a new [Segment2D], which specifies a linear
 * or a Bézier curve path between two anchor points
 * (and up to two control points for curvature).
 */
@Serializable
data class Segment2D(
    override val start: Vector2,
    override val control: List<Vector2>,
    override val end: Vector2,
    val corner: Boolean = false
) : BezierSegment<Vector2>, ShapeContourProvider {

    @Transient
    private var lut: List<Vector2>? = null

    @Suppress("unused")
    fun lut(size: Int = 100): List<Vector2> {
        if (lut == null || lut!!.size != size) {
            lut = (0..size).map { position((it.toDouble() / size)) }
        }
        return lut!!
    }

    override fun sub(t0: Double, t1: Double): Segment2D {
        return super.sub(t0, t1) as Segment2D
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


    override fun tForLength(length: Double): Double {
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
    fun transform(transform: Matrix44): Segment2D {
        return if (transform === Matrix44.IDENTITY) {
            this
        } else {
            val tStart = (transform * (start.xy01)).div.xy
            val tEnd = (transform * (end.xy01)).div.xy
            val tControl = when (control.size) {
                2 -> listOf((transform * control[0].xy01).div.xy, (transform * control[1].xy01).div.xy)
                1 -> listOf((transform * control[0].xy01).div.xy)
                else -> emptyList()
            }
            copy(start = tStart, control = tControl, end = tEnd)
        }
    }

    /** Calculates approximate Euclidean length of the [Segment2D]. */
    override val length by lazy {
        when (control.size) {
            0 -> (end - start).length
            1, 2 -> sumDifferences(adaptivePositions())
            else -> throw RuntimeException("unsupported number of control points")
        }
    }

    override fun position(ut: Double): Vector2 {
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
     * values of the extrema for the current [Segment2D].
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

    /** Returns the extrema points as [Vector2]s for current [Segment2D] */
    @Suppress("unused")
    fun extremaPoints(): List<Vector2> = extrema().map { position(it) }

    /** Returns the bounding box. */
    val bounds: Rectangle
        get() = (listOf(start, end) + extremaPoints()).bounds


    private fun dPoints(): List<List<Vector2>> {
        val points = listOf(start) + control + listOf(end)
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
     * Determines if the [Segment2D] forms a straight line.
     *
     * If the given [Segment2D] has control points,
     * the function verifies that they do not add any curvature to the path.
     *
     * @param tolerance The margin of error for what's considered a straight line.
     */
    @Suppress("unused")
    fun isStraight(tolerance: Double = 0.01): Boolean {
        return when (control.size) {
            2 -> {
                val dl = (end - start).normalized
                val d0 = (control[0] - start).normalized
                val d1 = (end - control[1]).normalized

                val dp0 = dl.dot(d0)
                val dp1 = (-dl).dot(d1)

                dp0 * dp0 + dp1 * dp1 > (2.0 - 2 * tolerance)
            }

            1 -> {
                val dl = (end - start).normalized
                val d0 = (control[0] - start).normalized

                val dp0 = dl.dot(d0)
                dp0 * dp0 > (1.0 - tolerance)
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


    val clockwise
        get() = angle(start, end, control[0]) > 0

    /** Converts the [Segment2D] to a cubic Bézier curve. */
    override val cubic: Segment2D
        get() = when {
            control.size == 2 -> this
            control.size == 1 -> {
                Segment2D(
                    start,
                    start * (1.0 / 3.0) + control[0] * (2.0 / 3.0),
                    control[0] * (2.0 / 3.0) + end * (1.0 / 3.0),
                    end,
                    corner
                )
            }

            linear -> {
                val delta = end - start
                Segment2D(
                    start,
                    start + delta * (1.0 / 3.0),
                    start + delta * (2.0 / 3.0),
                    end,
                    corner
                )
            }

            else -> error("cannot convert to cubic segment")
        }


    /** Converts the [Segment2D] to a quadratic Bézier curve. */
    val quadratic: Segment2D
        get() = when {
            control.size == 1 -> this
            linear -> {
                val delta = end - start
                Segment2D(start, start + delta * (1.0 / 2.0), end, corner)
            }

            else -> error("cannot convert to quadratic segment")
        }


    override fun derivative(t: Double): Vector2 = when {
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

    /** Reverses the order of control points of the given path [Segment2D]. */
    override val reverse: Segment2D
        get() {
            return when (control.size) {
                0 -> Segment2D(end, start)
                1 -> Segment2D(end, control[0], start)
                2 -> Segment2D(end, control[1], control[0], start)
                else -> throw RuntimeException("unsupported number of control points")
            }
        }


    override fun split(t: Double): Array<Segment2D> {
        val u = t.clamp(0.0, 1.0)
        val splitSigma = 10E-6

        if (u < splitSigma) {
            return arrayOf(Segment2D(start, start), this)
        }

        if (u >= 1.0 - splitSigma) {
            return arrayOf(this, Segment2D(end, end))
        }

        if (linear) {
            val cut = start + (end.minus(start) * u)
            return arrayOf(Segment2D(start, cut), Segment2D(cut, end))
        } else {
            when (control.size) {
                2 -> {
                    @Suppress("UnnecessaryVariable") val z = u
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

                    val left = Segment2D(pl0, pl1, pl2, pl3)

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

                    val right = Segment2D(pr0, pr1, pr2, pr3)

                    return arrayOf(left, right)
                }

                1 -> {
                    @Suppress("UnnecessaryVariable") val z = u
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

                    val left = Segment2D(
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

                    val right = Segment2D(
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
        return "Segment(start=$start, end=$end, control=${control})"
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as Segment2D

        if (start != other.start) return false
        if (end != other.end) return false
        return control == other.control
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + control.hashCode()
        return result
    }

    operator fun times(scale: Double): Segment2D {
        return when (type) {
            SegmentType.LINEAR -> Segment2D(start * scale, end * scale)
            SegmentType.QUADRATIC -> Segment2D(
                start * scale,
                control[0] * scale,
                end * scale
            )

            SegmentType.CUBIC -> Segment2D(
                start * scale,
                control[0] * scale,
                control[1] * scale,
                end * scale
            )
        }
    }

    operator fun div(scale: Double): Segment2D {
        return when (type) {
            SegmentType.LINEAR -> Segment2D(start / scale, end / scale)
            SegmentType.QUADRATIC -> Segment2D(
                start / scale,
                control[0] / scale,
                end / scale
            )

            SegmentType.CUBIC -> Segment2D(
                start / scale,
                control[0] / scale,
                control[1] / scale,
                end / scale
            )
        }
    }

    operator fun minus(right: Segment2D): Segment2D {
        return if (this.type == right.type) {
            when (type) {
                SegmentType.LINEAR -> Segment2D(
                    start - right.start,
                    end - right.end
                )

                SegmentType.QUADRATIC -> Segment2D(
                    start - right.start,
                    control[0] - right.control[0],
                    end - right.end
                )

                SegmentType.CUBIC -> Segment2D(
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

    operator fun plus(right: Segment2D): Segment2D {
        return if (this.type == right.type) {
            when (type) {
                SegmentType.LINEAR -> Segment2D(
                    start + right.start,
                    end + right.end
                )

                SegmentType.QUADRATIC -> Segment2D(
                    start + right.start,
                    control[0] + right.control[0],
                    end + right.end
                )

                SegmentType.CUBIC -> Segment2D(
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


/**
 * Linear segment constructor.
 *
 * @param start The starting anchor point.
 * @param end The ending anchor point.
 */

fun Segment2D(start: Vector2, end: Vector2, corner: Boolean = true) =
    Segment2D(start, emptyList(), end, corner)

/**
 * Quadratic Bézier segment constructor.
 *
 * @param start The starting anchor point.
 * @param c0 The control point.
 * @param end The ending anchor point.
 */
fun Segment2D(start: Vector2, c0: Vector2, end: Vector2, corner: Boolean = true) =
    Segment2D(start, listOf(c0), end, corner)

/**
 * Cubic Bézier segment constructor.
 *
 * @param start The starting anchor point.
 * @param c0 The first control point.
 * @param c1 The second control point
 * @param end The ending anchor point.
 */
fun Segment2D(start: Vector2, c0: Vector2, c1: Vector2, end: Vector2, corner: Boolean = true) =
    Segment2D(start, listOf(c0, c1), end, corner)
