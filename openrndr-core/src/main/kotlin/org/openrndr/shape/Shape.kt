@file:Suppress("unused", "MemberVisibilityCanPrivate", "MemberVisibilityCanBePrivate")

package org.openrndr.shape

import io.lacuna.artifex.Vec2
import org.openrndr.math.*
import org.openrndr.shape.internal.BezierCubicSampler2D
import org.openrndr.shape.internal.BezierQuadraticSampler2D
import java.util.*
import kotlin.math.*


data class SegmentPoint(val segment: Segment, val segmentT: Double, val position: Vector2)
data class ContourPoint(val contour: ShapeContour, val contourT: Double, val segment: Segment, val segmentT: Double, val position: Vector2)


enum class SegmentType {
    LINEAR,
    QUADRATIC,
    CUBIC
}

/**
 * Segment describes a linear or bezier path between two points
 */
class Segment {
    val start: Vector2
    val end: Vector2

    /**
     * control points, zero-length iff the segment is linear
     */
    val control: Array<Vector2>

    val linear: Boolean get() = control.isEmpty()

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
     * Linear segment constructor
     * @param start starting point of the segment
     * @param end end point of the segment
     */
    constructor(start: Vector2, end: Vector2) {
        this.start = start
        this.end = end
        this.control = emptyArray()
    }

    /**
     * Quadratic bezier segment constructor
     * @param start starting point of the segment
     * @param c0 control point
     * @param end end point of the segment
     */
    constructor(start: Vector2, c0: Vector2, end: Vector2) {
        this.start = start
        this.control = arrayOf(c0)
        this.end = end
    }

    /**
     * Cubic bezier segment constructor
     * @param start starting point of the segment
     * @param c0 first control point
     * @param c1 second control point
     * @param end end point of the segment
     */
    constructor(start: Vector2, c0: Vector2, c1: Vector2, end: Vector2) {
        this.start = start
        this.control = arrayOf(c0, c1)
        this.end = end
    }

    constructor(start: Vector2, control: Array<Vector2>, end: Vector2) {
        this.start = start
        this.control = control
        this.end = end
    }

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
        for (i in 0 until lut.size) {
            if ((lut[i] - point).squaredLength < error * error) {
                hits++
                t += i.toDouble() / lut.size
            }
        }
        return if (hits > 0) t / hits else null
    }

    /**
     * Estimate t parameter value for a given length
     * @return a value between 0 and 1
     */
    fun tForLength(length: Double): Double {
        if (type == SegmentType.LINEAR) {
            return (length / this.length).coerceIn(0.0, 1.0)
        }

        val segmentLength = this.length
        val clength = length.coerceIn(0.0, segmentLength)

        if (clength == 0.0) {
            return 0.0
        }
        if (clength >= segmentLength) {
            return 1.0
        }
        var summedLength = 0.0
        lut(100)
        val clut = lut ?: error("no lut")
        val partitionCount = clut.size - 1

        val dt = 1.0 / partitionCount
        for ((index, point) in lut!!.withIndex()) {
            if (index < lut!!.size - 1) {
                val p0 = clut[index]
                val p1 = clut[index + 1]
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
     *
     */

    fun nearest(point: Vector2): SegmentPoint {
        val t = when (type) {
            SegmentType.LINEAR -> {
                val dir = end - start
                val relativePoint = point - start
                (dir dot relativePoint) / dir.squaredLength
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
                    minDistance = distance;
                    param = max(1.0, ((point - control[0]) dot bc) / (bc dot bc))
                }

                val a = br dot br
                val b = 3.0 * (ab dot br)
                val c = (2.0 * (ab dot ab)) + (qa dot br);
                val d = qa dot ab
                val ts = solveCubic(a, b, c, d);

                for (t in ts) {
                    if (t > 0 && t < 1) {
                        val endpoint = position(t);
                        val distance = sign(ac cross (endpoint - point)) * (endpoint - point).length
                        if (abs(distance) < abs(minDistance)) {
                            minDistance = distance
                            param = t
                        }
                    }
                }
                param.coerceIn(0.0, 1.0)
            }
            SegmentType.CUBIC -> {
                fun sign(n: Double): Double {
                    val s = Math.signum(n)
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
                val SEARCH_STARTS = 4
                val SEARCH_STEPS = 8

                for (i in 0 until SEARCH_STARTS) {
                    var t = i.toDouble() / (SEARCH_STARTS - 1)
                    var step = 0
                    while (true) {
                        val qpt = position(t) - point
                        distance = sign(direction(t) cross qpt) * qpt.length
                        if (abs(distance) < abs(minDistance)) {
                            minDistance = distance
                            param = t
                        }
                        if (step == SEARCH_STEPS) {
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


    fun transform(transform: Matrix44): Segment {
        val tstart = (transform * (start.xy01)).div.xy
        val tend = (transform * (end.xy01)).div.xy
        val tcontrol = when (control.size) {
            2 -> arrayOf((transform * control[0].xy01).div.xy, (transform * control[1].xy01).div.xy)
            1 -> arrayOf((transform * control[0].xy01).div.xy)
            else -> emptyArray()
        }
        return Segment(tstart, tcontrol, tend)
    }


    fun adaptivePositions(distanceTolerance: Double = 0.5): List<Vector2> = when (control.size) {
        0 -> listOf(start, end)
        1 -> BezierQuadraticSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], end).first
        2 -> BezierCubicSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], control[1], end).first
        else -> throw RuntimeException("unsupported number of control points")
    }

    fun adaptivePositionsAndNormals(distanceTolerance: Double = 0.5): Pair<List<Vector2>, List<Vector2>> = when (control.size) {
        0 -> Pair(listOf(start, end), listOf(end - start, end - start))
        1 -> BezierQuadraticSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], end)
        2 -> BezierCubicSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], control[1], end)
        else -> throw RuntimeException("unsupported number of control points")
    }

    /**
     * Sample [pointCount] points on the segment
     * @param pointCount the number of points to sample on the segment
     */
    fun equidistantPositions(pointCount: Int): List<Vector2> {
        return sampleEquidistant(adaptivePositions(), pointCount)
    }

    val length: Double
        get() = when (control.size) {
            0 -> (end - start).length
            1, 2 -> sumDifferences(adaptivePositions())
            else -> throw RuntimeException("unsupported number of control points")
        }

    fun position(ut: Double): Vector2 {
        val t = ut.coerceIn(0.0, 1.0)
        return when (control.size) {
            0 -> Vector2(start.x * (1.0 - t) + end.x * t, start.y * (1.0 - t) + end.y * t)
            1 -> bezier(start, control[0], end, t)
            2 -> bezier(start, control[0], control[1], end, t)
            else -> throw RuntimeException("unsupported number of control points")
        }
    }

    fun direction(): Vector2 {
        return (end - start).normalized
    }

    fun direction(t: Double): Vector2 {
        return derivative(t).normalized
    }

    fun extrema(): List<Double> {
        val dpoints = dpoints()
        return when {
            linear -> emptyList()
            control.size == 1 -> {
                val xRoots = roots(dpoints[0].map { it.x })
                val yRoots = roots(dpoints[0].map { it.y })
                (xRoots + yRoots).distinct().sorted().filter { it in 0.0..1.0 }
            }
            control.size == 2 -> {
                val xRoots = roots(dpoints[0].map { it.x }) + roots(dpoints[1].map { it.x })
                val yRoots = roots(dpoints[0].map { it.y }) + roots(dpoints[1].map { it.y })
                (xRoots + yRoots).distinct().sorted().filter { it in 0.0..1.0 }
            }
            else -> throw RuntimeException("not supported")
        }
    }

    fun extremaPoints(): List<Vector2> = extrema().map { position(it) }

    val bounds: Rectangle get() = vector2Bounds(listOf(start, end) + extremaPoints())


    private fun dpoints(): List<List<Vector2>> {
        val points = listOf(start, *control, end)
        var d = points.size
        var c = d - 1
        val dpoints = mutableListOf<List<Vector2>>()
        var p = points
        while (d > 1) {
            val list = mutableListOf<Vector2>()
            for (j in 0 until c) {
                list.add(Vector2(c * (p[j + 1].x - p[j].x), c * (p[j + 1].y - p[j].y)))
            }
            dpoints.add(list)
            p = list
            d--
            c--
        }
        return dpoints
    }

    fun offset(distance: Double, stepSize: Double = 0.01, yPolarity: YPolarity = YPolarity.CW_NEGATIVE_Y): List<Segment> {
        return if (linear) {
            val n = normal(0.0, yPolarity)
            if (distance > 0.0) {
                listOf(Segment(start + distance * n, end + distance * n))
            } else {
                val d = direction()
                val s = distance.coerceAtMost(length / 2.0)


                val candidate = Segment(start - s * d + distance * n, end + s * d + distance * n)
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
        return Math.atan2(cross, dot)
    }


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

        val a = LineSegment(start + normal(0.0, polarity) * scale(0.0), start)
        val b = LineSegment(end + normal(1.0, polarity) * scale(1.0), end)

        val o = intersection(a, b, 1000000000.0)

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

    /**
     * Cubic version of segment
     */
    val cubic: Segment
        get() = when {
            control.size == 2 -> this
            control.size == 1 -> {
                Segment(start, start * (1.0 / 3.0) + control[0] * (2.0 / 3.0), control[0] * (2.0 / 3.0) + end * (1.0 / 3.0), end)
            }
            linear -> {
                val delta = end - start
                Segment(start, start + delta * (1.0 / 3.0), start + delta * (2.0 / 3.0), end)
            }
            else -> throw RuntimeException("cannot convert to cubic segment")
        }

    val quadratic: Segment
        get() = when {
            control.size == 1 -> this
            linear -> {
                val delta = end - start
                Segment(start, start + delta * (1.0 / 2.0), end)
            }
            else -> throw RuntimeException("cannot convert to cubic segment")
        }


    fun derivative(t: Double): Vector2 = when {
        linear -> end - start
        control.size == 1 -> safeDerivative(start, control[0], end, t)
        control.size == 2 -> safeDerivative(start, control[0], control[1], end, t)
        else -> throw RuntimeException("not implemented")
    }

    fun normal(ut: Double, polarity: YPolarity = YPolarity.CW_NEGATIVE_Y): Vector2 {
        return direction(ut).perpendicular(polarity)
    }

    val reverse: Segment
        get() {
            return when (control.size) {
                0 -> Segment(end, start)
                1 -> Segment(end, control[0], start)
                2 -> Segment(end, control[1], control[0], start)
                else -> throw RuntimeException("unsupported number of control points")
            }
        }

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
     * Split the contour
     * @param t the point to split the contour at
     * @return array of parts, depending on the split point this is one or two entries long
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
            when {
                control.size == 2 -> {
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
                            iz3, 3.0 * iz2 * z, 3.0 * iz * z2, z3)

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
                control.size == 1 -> {

                    val z = u
                    val iz = 1 - z
                    val iz2 = iz * iz
                    val z2 = z * z

                    val lsm = Matrix44(
                            1.0, 0.0, 0.0, 0.0,
                            iz, z, 0.0, 0.0,
                            iz2, 2.0 * iz * z, z2, 0.0,
                            0.0, 0.0, 0.0, 0.0)

                    val px = Vector4(start.x, control[0].x, end.x, 0.0)
                    val py = Vector4(start.y, control[0].y, end.y, 0.0)

                    val plx = lsm * px
                    val ply = lsm * py

                    val left = Segment(
                            Vector2(plx.x, ply.x),
                            Vector2(plx.y, ply.y),
                            Vector2(plx.z, ply.z))

                    val rsm = Matrix44(
                            iz2, 2.0 * iz * z, z2, 0.0,
                            0.0, iz, z, 0.0,
                            0.0, 0.0, 1.0, 0.0,
                            0.0, 0.0, 0.0, 0.0)

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
                            Vector2(prx.z, pry.z))

                    return arrayOf(left, right)

                }
                else -> throw RuntimeException("not implemented")
            }
        }
    }

    override fun toString(): String {
        return "Segment(start=$start, end=$end, control=${Arrays.toString(control)})"
    }

    fun copy(start: Vector2 = this.start, control: Array<Vector2> = this.control, end: Vector2 = this.end): Segment {
        return Segment(start, control, end)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

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


//    fun intersect(other: Segment) {
//        if (control.size == 0 && other.control.size == 0) {
//            // line line intersection
//        }
//    }
}

private fun sumDifferences(points: List<Vector2>) =
        (0 until points.size - 1).sumByDouble { (points[it] - points[it + 1]).length }


enum class Winding {
    CLOCKWISE,
    COUNTER_CLOCKWISE
}

enum class SegmentJoin {
    ROUND,
    MITER,
    BEVEL
}

data class ShapeContour(val segments: List<Segment>, val closed: Boolean, val polarity: YPolarity = YPolarity.CW_NEGATIVE_Y) {
    companion object {
        val EMPTY = ShapeContour(emptyList(), false)

        fun fromPoints(points: List<Vector2>, closed: Boolean, polarity: YPolarity = YPolarity.CW_NEGATIVE_Y) =
                if (!closed) {
                    ShapeContour((0 until points.size - 1).map { Segment(points[it], points[it + 1]) }, closed, polarity)
                } else {
                    val d = (points.last() - points.first()).squaredLength
                    val usePoints = if (d > 10E-6) points else points.dropLast(1)
                    ShapeContour((usePoints.indices).map { Segment(usePoints[it], usePoints[(it + 1) % usePoints.size]) }, true, polarity)
                }
    }

    val triangulation by lazy {
        triangulate(Shape(listOf(this))).windowed(3, 3).map {
            Triangle(it[0], it[1], it[2])
        }
    }

    init {
        segments.zipWithNext().forEach {

            val d = (it.first.end - it.second.start).length
            require(d < 10E-6) {
                "points are to far away from each other ${it.first.end} ${it.second.start} $d"
            }

        }
    }

    val shape get() = Shape(listOf(this))

    val length get() = segments.sumByDouble { it.length }
    val bounds get() = vector2Bounds(sampleLinear().segments.flatMap { listOf(it.start, it.end) })

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
    val exploded: List<ShapeContour>
        get() = segments.map { ShapeContour(listOf(it), false, polarity) }

    val clockwise: ShapeContour get() = if (winding == Winding.CLOCKWISE) this else this.reversed
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
            segments.add(Segment(this.segments[this.segments.size - 1].end, other.segments[0].start))
        }
        segments.addAll(other.segments)
        return ShapeContour(segments, false, polarity)
    }

    operator fun contains(point: Vector2): Boolean = closed && this.toRing2().test(Vec2(point.x, point.y)).inside

    /*
    operator fun contains(point: Vector2) : Boolean {
        return this.toRing2().test(Vec2(point.x, point.y)).inside
    }
     */

    /**
     * Estimate t parameter value for a given length
     * @return a value between 0 and 1
     */
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

    fun offset(distance: Double, joinType: SegmentJoin = SegmentJoin.ROUND): ShapeContour {
        if (segments.size == 1) {
            return ShapeContour(segments[0].offset(distance, yPolarity = polarity), false, polarity)
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
                        val i = intersection(ls.start, ls.end, offset.first().start, offset.first().end)
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
                                val i = intersection(ls.end, ls.end + ls.direction(1.0), fs.start, fs.start - fs.direction(0.0), eps = 10E8)
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

    fun position(ut: Double): Vector2 {
        require(segments.isNotEmpty())
        val t = ut.clamp(0.0, 1.0)
        return when (t) {
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
        val t = ut.coerceIn(0.0, 1.0)
        return when (t) {
            0.0 -> segments[0].normal(0.0, polarity)
            1.0 -> segments.last().normal(1.0, polarity)
            else -> {
                val segment = (t * segments.size).toInt()
                val segmentOffset = (t * segments.size) - segment
                segments[Math.min(segments.size - 1, segment)].normal(segmentOffset, polarity)
            }
        }
    }

    fun adaptivePositions(distanceTolerance: Double = 0.5): List<Vector2> {
        val adaptivePoints = mutableListOf<Vector2>()

        for (segment in this.segments) {
            val samples = segment.adaptivePositions(distanceTolerance)

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
        return adaptivePoints
    }

    fun adaptivePositionsAndDirection(distanceTolerance: Double = 0.5): Pair<List<Vector2>, List<Vector2>> {
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
    fun equidistantPositions(pointCount: Int) = sampleEquidistant(adaptivePositions(), pointCount)

    /**
     * Adaptively sample the contour into line segments while still approximating the original contour
     * @param distanceTolerance controls the quality of the approximation
     * @return a ShapeContour composed of linear segments
     */
    fun sampleLinear(distanceTolerance: Double = 0.5) =
            fromPoints(adaptivePositions(distanceTolerance), closed, polarity)

    /**
     * Sample the shape contour into line segments
     */
    fun sampleEquidistant(pointCount: Int): ShapeContour {
        val points = equidistantPositions(pointCount.coerceAtLeast(2))
        val segments = (0 until points.size - 1).map { Segment(points[it], points[it + 1]) }
        return ShapeContour(segments, closed, polarity)
    }

    fun transform(transform: Matrix44) = ShapeContour(segments.map { it.transform(transform) }, closed, polarity)

    /**
     * Sample a sub contour
     * @param u0 starting point in [0, 1)
     * @param u1 ending point in [0, 1)
     * @return sub contour
     */
    fun sub(u0: Double, u1: Double): ShapeContour {
        if (segments.isEmpty()) {
            return EMPTY
        }

        require(u0 == u0) { "u0 is NaN" }
        require(u1 == u1) { "u1 is NaN" }

        if (abs(u0 - u1) < 10E-6) {
            return EMPTY
        }

        var t0 = u0
        var t1 = u1

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

        segment1 = Math.min(segments.size - 1, segment1)
        segment0 = Math.min(segments.size - 1, segment0)

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
     * @param error what is the allowed error (unitless, but likely in pixels)
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
        val n = segments.map { it.nearest(point) }.minBy { it.position.distanceTo(point) } ?: error("no segments")
        val segmentIndex = segments.indexOf(n.segment)
        val t = (segmentIndex + n.segmentT) / segments.size
        return ContourPoint(this, t, n.segment, n.segmentT, n.position)
    }

    val reversed get() = ShapeContour(segments.map { it.reverse }.reversed(), closed, polarity)

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
                fixedSegments.add(Segment(left.start, left.control, right.start))
            } else {
                fixedSegments.add(segments.last())
            }
        }

        return ShapeContour(if (segments.size > 1) fixedSegments else segments, closed, polarity)
    }


}

enum class ShapeTopology {
    CLOSED,
    OPEN,
    MIXED
}

class Shape(val contours: List<ShapeContour>) {

    companion object {
        fun compound(shapes: List<Shape>) = Shape(shapes.flatMap { it.contours })
    }

    val topology = when {
        contours.isEmpty() -> ShapeTopology.OPEN
        contours.all { it.closed } -> ShapeTopology.CLOSED
        contours.all { !it.closed } -> ShapeTopology.OPEN
        else -> ShapeTopology.MIXED
    }

    val openContours: List<ShapeContour> =
            when (topology) {
                ShapeTopology.OPEN -> contours
                ShapeTopology.CLOSED -> emptyList()
                ShapeTopology.MIXED -> contours.filter { !it.closed }
            }

    val closedContours: List<ShapeContour> =
            when (topology) {
                ShapeTopology.OPEN -> emptyList()
                ShapeTopology.CLOSED -> contours
                ShapeTopology.MIXED -> contours.filter { it.closed }
            }

    val linear get() = contours.all { it.segments.all { it.linear } }
    fun polygon(distanceTolerance: Double = 0.5) = Shape(contours.map { it.sampleLinear(distanceTolerance) })


    val triangulation by lazy {
        triangulate(this).windowed(3, 3).map {
            Triangle(it[0], it[1], it[2])
        }
    }

    //operator fun contains(v: Vector2): Boolean = triangulation.any { v in it }

    operator fun contains(v: Vector2) : Boolean {
        val v = Vec2(v.x, v.y)
        return toRegion2().contains(v)
    }


    /**
     * The outline of the shape
     */
    val outline get() = contours[0]

    /**
     * The indexed hole of the shape
     * @param index
     */
    fun hole(index: Int) = contours[index + 1]

    /**
     * Apply a tranform to the shape
     * @param transform a Matrix44 that represents the transform
     * @return a transformed shape instance
     */
    fun transform(transform: Matrix44) = Shape(contours.map { it.transform(transform) })

    /**
     * Apply a map to the shape. Maps every contour.
     */
    fun map(mapper: (ShapeContour) -> ShapeContour) = Shape(contours.map { mapper(it) })

    val compound: Boolean
        get() {
            return if (contours.isEmpty()) {
                false
            } else {
                contours.count { it.winding == Winding.CLOCKWISE } > 1
            }
        }

    /**
     * Splits a compound shape into separate shapes.
     */
    fun splitCompounds(winding: Winding = Winding.CLOCKWISE): List<Shape> {
        return if (contours.isEmpty()) {
            emptyList()
        } else {
            val (cw, ccw) = closedContours.partition { it.winding == winding }
            val candidates = cw.map { outer ->
                val cs = ccw.filter { intersects(it.bounds, outer.bounds) }
                listOf(outer) + cs
            }
            (candidates + openContours.map { listOf(it) }).map { Shape(it) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Shape

        if (contours != other.contours) return false

        return true
    }

    override fun hashCode(): Int {
        return contours.hashCode()
    }
}

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

fun CatmullRomChain2.toContour(): ShapeContour = ShapeContour(segments.map { it.toSegment() }, this.loop)