package org.openrndr.shape

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.openrndr.math.*
import org.openrndr.shape.internal.BezierCubicSamplerT
import org.openrndr.shape.internal.BezierQuadraticSamplerT
import org.openrndr.shape.internal.flattenSegment
import org.openrndr.shape.internal.flattenSegmentWithT
import org.openrndr.shape.internal.numQuadratics
import org.openrndr.shape.internal.Speed
import org.openrndr.shape.internal.arcLengthTable
import org.openrndr.shape.internal.cubicSpeed
import org.openrndr.shape.internal.quadraticSpeed
import org.openrndr.shape.internal.tsForLengths
import org.openrndr.shape.internal.unitIntervalRoots
import kotlin.math.*

/**
 * Represents a Bézier segment with a general implementation capable of handling linear, quadratic, and cubic segments.
 *
 * @param T The type of points used in the Bézier segment, which must extend [EuclideanVector].
 */
interface BezierSegment<T : EuclideanVector<T>> {

    /**
     * Represents the starting point of the [BezierSegment]. This is the first anchor point of the segment.
     */
    val start: T

    /**
     * Represents the list of control points that define the shape of a Bézier curve segment.
     * These points are used to calculate the position, direction, and other geometric properties of the curve.
     */
    val control: List<T>

    /**
     * The ending point of the Bézier segment.
     * This represents the final control point or anchor point of the curve.
     */
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

    fun derivative2(t: Double): T

    fun curvature(t: Double): Double

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
    val quadratic: BezierSegment<T>

    @Transient
    val cubic: BezierSegment<T>

    /**
     * Estimate [t](https://pomax.github.io/bezierinfo/#explanation) value for a given length
     * @return A value between `0.0` and `1.0`.
     */
    fun tForLength(length: Double): Double

    /**
     * Estimate the [t](https://pomax.github.io/bezierinfo/#explanation) values for ascending [lengths], faster than
     * calling [tForLength] for each of them
     * @return values between `0.0` and `1.0`.
     */
    fun tForLengths(lengths: List<Double>): List<Double> = lengths.map { tForLength(it) }

    /**
     * Calculates the point at a given distance along this [Segment2D].
     * @param length the distance along the [Segment2D].
     * @param distanceTolerance not used, the point is found on the curve itself
     */
    fun pointAtLength(length: Double, distanceTolerance: Double): T {
        return when {
            length <= 0.0 -> start
            length >= this.length -> end
            else -> position(tForLength(length))
        }
    }

    @Transient
    val reverse: BezierSegment<T>

    /**
     * Recursively subdivides [Segment2D] to approximate Bézier curve.
     *
     * @param distanceTolerance The square of the maximal distance of each point from curve.
     */
    fun adaptivePositions(distanceTolerance: Double = 0.5): List<T> {
        return adaptivePositionsWithT(distanceTolerance).map { it.first }
    }


    fun adaptivePositionsWithT(distanceTolerance: Double = 0.5): List<Pair<T, Double>> = when (control.size) {
        0 -> listOf(start to 0.0, end to 1.0)
        1 -> BezierQuadraticSamplerT<T>().apply { this.distanceTolerance = distanceTolerance }
            .sample(start, control[0], end)

        2 -> BezierCubicSamplerT<T>().apply { this.distanceTolerance = distanceTolerance }
            .sample(start, control[0], control[1], end)

        else -> throw RuntimeException("unsupported number of control points")
    }

    /**
     * Samples specified amount of points on the [Segment2D], spaced evenly by arc length, from start to end.
     * @param pointCount The number of points to sample.
     * @param distanceTolerance not used, the points are found on the curve itself
     */
    fun equidistantPositions(pointCount: Int, distanceTolerance: Double = 0.5): List<T> {
        return equidistantPositionsWithT(pointCount, distanceTolerance).map { it.first }
    }

    fun equidistantPositionsWithT(pointCount: Int, distanceTolerance: Double = 0.5): List<Pair<T, Double>> {
        if (pointCount <= 0) return emptyList()
        if (pointCount == 1) return listOf(start to 0.0)
        val total = length
        val ts = tForLengths(List(pointCount) { total * it / (pointCount - 1) })
        return ts.mapIndexed { i, t -> (if (i == pointCount - 1) end else position(t)) to t }
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
) : BezierSegment<Vector2>, ShapeContourProvider, LinearType<Segment2D>, GeometricPrimitive2D {

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

    /**
     * Checks if [point] lies on the segment
     * @return the [t](https://pomax.github.io/bezierinfo/#explanation) of the point on the segment nearest to
     * [point] when it is closer than [error], null otherwise
     */
    fun on(point: Vector2, error: Double = 5.0): Double? {
        val nearest = nearest(point)
        return if (nearest.position.squaredDistanceTo(point) < error * error) nearest.segmentT else null
    }

    override fun adaptivePositions(distanceTolerance: Double): List<Vector2> {
        return flattenSegment(this, distanceTolerance)
    }

    override fun adaptivePositionsWithT(distanceTolerance: Double): List<Pair<Vector2, Double>> {
        return flattenSegmentWithT(this, distanceTolerance)
    }

    override fun tForLength(length: Double): Double {
        if (type == SegmentType.LINEAR) {
            return (length / this.length).coerceIn(0.0, 1.0)
        }
        return tsForLengths(doubleArrayOf(length), arcLengthTable, polygonLength, speed)[0]
    }

    override fun tForLengths(lengths: List<Double>): List<Double> {
        if (type == SegmentType.LINEAR) {
            return lengths.map { (it / this.length).coerceIn(0.0, 1.0) }
        }
        return tsForLengths(lengths.toDoubleArray(), arcLengthTable, polygonLength, speed).asList()
    }

    /** Cumulative arc lengths, built with the [length] of a curved segment */
    @kotlinx.serialization.Transient
    private val arcLengthTable by lazy { arcLengthTable(polygonLength, speed) }

    /** The length of the control polygon, a measure of the size of the segment */
    private val polygonLength: Double
        get() = when (control.size) {
            0 -> start.distanceTo(end)
            1 -> start.distanceTo(control[0]) + control[0].distanceTo(end)
            else -> start.distanceTo(control[0]) + control[0].distanceTo(control[1]) + control[1].distanceTo(end)
        }

    /** The length of the derivative */
    private val speed: Speed
        get() = when (control.size) {
            0 -> Speed { start.distanceTo(end) }
            1 -> quadraticSpeed(start, control[0], end)
            else -> cubicSpeed(start, control[0], control[1], end)
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

    private fun nearestT(point: Vector2, projection: DoubleArray): Double {
        var bestT = 0.0
        var bestDistance = start.squaredDistanceTo(point)
        val endDistance = end.squaredDistanceTo(point)
        if (endDistance < bestDistance) {
            bestT = 1.0
            bestDistance = endDistance
        }
        for (t in unitIntervalRoots(projection)) {
            val distance = position(t).squaredDistanceTo(point)
            if (distance < bestDistance) {
                bestT = t
                bestDistance = distance
            }
        }
        return bestT
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
                // the nearest point is an end point or a root of (position(t) - point) . derivative(t)
                val qa = start - point
                val ab = control[0] - start
                val br = start + end - control[0] - control[0]
                nearestT(point, doubleArrayOf(qa dot ab, (qa dot br) + 2.0 * (ab dot ab), 3.0 * (ab dot br), br dot br))
            }

            SegmentType.CUBIC -> {
                // the nearest point is an end point or a root of (position(t) - point) . derivative(t), a quintic
                val qa = start - point
                val ab = control[0] - start
                val bc = control[1] - control[0]
                val cd = end - control[1]
                val br = bc - ab
                val ax = (cd - bc) - br
                nearestT(
                    point, doubleArrayOf(
                        qa dot ab,
                        3.0 * (ab dot ab) + 2.0 * (qa dot br),
                        9.0 * (br dot ab) + (qa dot ax),
                        4.0 * (ax dot ab) + 6.0 * (br dot br),
                        5.0 * (ax dot br),
                        ax dot ax
                    )
                )
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
            1, 2 -> arcLengthTable.total
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
        if (linear) {
            return emptyList()
        }
        val dPoints = dPoints()
        return when {
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
    val bounds by lazy {
        if (linear) {
            Rectangle(min(start.x, end.x), min(start.y, end.y), abs(end.x - start.x), abs(end.y - start.y))
        } else {
            (listOf(start, end) + extremaPoints()).bounds
        }
    }

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
    override val quadratic: Segment2D
        get() = when {
            control.size == 2 -> {
                val p0 = start
                val p1 = control[0]
                val p2 = control[1]
                val p3 = end

                val c1 = (p1 * 3.0 - p0) * 0.5
                val c2 = (p2 * 3.0 - p3) * 0.5
                val c = (c1 + c2) * 0.5
                Segment2D(p0, c, p3)
            }

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

    override fun derivative2(t: Double): Vector2 = when {
        linear -> Vector2.ZERO
        control.size == 1 -> cubic.derivative2(t)
        control.size == 2 -> derivative2(
            start,
            control[0],
            control[1],
            end,
            t
        )

        else -> throw RuntimeException("not implemented")
    }

    override fun curvature(t: Double): Double {
        val d = derivative(t)
        val dd = derivative2(t)
        val numerator = d.cross(dd)
        val denominator = d.length.pow(3.0)
        return numerator / denominator
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

        if (u <= 0.0) {
            return arrayOf(Segment2D(start, start), this)
        }

        if (u >= 1.0) {
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

    override operator fun times(scale: Double): Segment2D {
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

    override operator fun div(scale: Double): Segment2D {
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

    override operator fun minus(right: Segment2D): Segment2D {
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

    override operator fun plus(right: Segment2D): Segment2D {
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

fun Segment2D.toQuadratics(tolerance: Double): List<Segment2D> {
    val count = this.numQuadratics(tolerance)

    if (count == 1) {
        return listOf(this.quadratic)
    } else {

        val result = ArrayList<Segment2D>(count)
        for (i in 0 until count) {
            val t0 = i.toDouble() / count
            val t1 = (i + 1.0) / count
            result.add(sub(t0, t1).quadratic)
        }
        return result
    }
}

fun Segment2D.findIntersectionsX(x: Double): List<Double> {
    return when (control.size) {
        0 -> solveLinear(end.x - start.x, start.x - x).filter { it in 0.0..1.0 }
        1 -> {
            val a = start.x - 2.0 * control[0].x + end.x
            val b = 2.0 * (control[0].x - start.x)
            val c = start.x - x
            solveQuadratic(a, b, c).filter { it in 0.0..1.0 }.distinct()
        }
        2 -> {
            val a = -start.x + 3.0 * control[0].x - 3.0 * control[1].x + end.x
            val b = 3.0 * start.x - 6.0 * control[0].x + 3.0 * control[1].x
            val c = -3.0 * start.x + 3.0 * control[0].x
            val d = start.x - x
            solveCubic(a, b, c, d).filter { it in 0.0..1.0 }.distinct()
        }
        else -> emptyList()
    }
}

fun Segment2D.findIntersectionsY(y: Double): List<Double> {
    return when (control.size) {
        0 -> solveLinear(end.y - start.y, start.y - y).filter { it in 0.0..1.0 }
        1 -> {
            val a = start.y - 2.0 * control[0].y + end.y
            val b = 2.0 * (control[0].y - start.y)
            val c = start.y - y
            solveQuadratic(a, b, c).filter { it in 0.0..1.0 }.distinct()
        }
        2 -> {
            val a = -start.y + 3.0 * control[0].y - 3.0 * control[1].y + end.y
            val b = 3.0 * start.y - 6.0 * control[0].y + 3.0 * control[1].y
            val c = -3.0 * start.y + 3.0 * control[0].y
            val d = start.y - y
            solveCubic(a, b, c, d).filter { it in 0.0..1.0 }.distinct()
        }
        else -> emptyList()
    }
}

fun Segment2D.clip(mask: Rectangle): List<Segment2D> {
    val ts = mutableSetOf(0.0, 1.0)
    ts.addAll(findIntersectionsX(mask.corner.x))
    ts.addAll(findIntersectionsX(mask.corner.x + mask.width))
    ts.addAll(findIntersectionsY(mask.corner.y))
    ts.addAll(findIntersectionsY(mask.corner.y + mask.height))

    val sortedTs = ts.filter { it in 0.0..1.0 }.sorted()
    val result = mutableListOf<Segment2D>()
    for (i in 0 until sortedTs.size - 1) {
        val t0 = sortedTs[i]
        val t1 = sortedTs[i + 1]
        if (t1 - t0 > 1E-9) {
            val mid = position((t0 + t1) / 2.0)
            if (mask.contains(mid)) {
                result.add(sub(t0, t1))
            }
        }
    }
    return result
}

