@file:Suppress("unused", "MemberVisibilityCanPrivate", "MemberVisibilityCanBePrivate")

package org.openrndr.shape

import org.openrndr.math.*
import org.openrndr.shape.internal.BezierCubicSampler2D
import org.openrndr.shape.internal.BezierQuadraticSampler2D
import java.util.*
import kotlin.math.sign

class SegmentProjection(val segment: Segment, val projection: Double, val distance: Double, val point: Vector2)
class ContourProjection(val segmentProjection: SegmentProjection, val projection: Double, val distance: Double, val point: Vector2)


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

    private fun closest(points: List<Vector2>, query: Vector2): Pair<Int, Vector2> {
        var closestIndex = 0
        var closestValue = points[0]

        var closestDistance = Double.POSITIVE_INFINITY
        for (i in 0 until points.size) {
            val distance = (points[i] - query).squaredLength
            if (distance < closestDistance) {
                closestIndex = i
                closestValue = points[i]
                closestDistance = distance
            }
        }
        return Pair(closestIndex, closestValue)
    }

    fun project(point: Vector2): SegmentProjection {
        // based on bezier.js
        val lut = lut()
        val l = (lut.size - 1).toDouble()
        val closest = closest(lut, point)

        var closestDistance = (point - closest.second).squaredLength

        if (closest.first == 0 || closest.first == lut.size - 1) {
            val t = closest.first.toDouble() / l
            return SegmentProjection(this, t, closestDistance, closest.second)
        } else {
            val t1 = (closest.first - 1) / l
            val t2 = (closest.first + 1) / l
            val step = 0.1 / l

            var t = t1
            var ft = t1

            while (t < t2 + step) {
                val p = position(t)
                val d = (p - point).squaredLength
                if (d < closestDistance) {
                    closestDistance = d
                    ft = t
                }
                t += step
            }
            val p = position(ft)
            return SegmentProjection(this, ft, closestDistance, p)
        }
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


    fun sampleAdaptive(distanceTolerance: Double = 0.5): List<Vector2> = when (control.size) {
        0 -> listOf(start, end)
        1 -> BezierQuadraticSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], end).first
        2 -> BezierCubicSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], control[1], end).first
        else -> throw RuntimeException("unsupported number of control points")
    }

    fun sampleAdaptiveNormals(distanceTolerance: Double = 0.5): Pair<List<Vector2>, List<Vector2>> = when (control.size) {
        0 -> Pair(listOf(start, end), listOf(end - start, end - start))
        1 -> BezierQuadraticSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], end)
        2 -> BezierCubicSampler2D().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], control[1], end)
        else -> throw RuntimeException("unsupported number of control points")
    }


    val length: Double
        get() = when (control.size) {
            0 -> (end - start).length
            1, 2 -> sumDifferences(sampleAdaptive())
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
        return (start - end).normalized
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

    fun offset(distance: Double): List<Segment> {
        return if (linear) {
            val n = normal(0.0)
            listOf(Segment(start + distance * n, end + distance * n))
        } else {
            reduced.map { it.scale(distance) }
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
            val n1 = normal(0.0)
            val n2 = normal(1.0)
            val s = n1 dot n2
            return s >= 0.9

        }

    val reduced: List<Segment>
        get() {
            val step = 0.01
            var extrema = extrema()

            if (extrema.isEmpty() || extrema[0] != 0.0) {
                extrema = listOf(0.0) + extrema
            }

            if (extrema.last() != 1.0) {
                extrema = extrema + listOf(1.0)
            }

            val pass1 = extrema.zipWithNext().map {
                sub(it.first, it.second)
            }
            val pass2 = mutableListOf<Segment>()


            pass1.forEach {
                var t1 = 0.0
                var t2 = step

                while (t2 <= 1.0) {
                    val segment = it.sub(t1, t2)
                    if (!segment.simple) {
                        pass2.add(segment)
                        t1 = t2
                    }
                    t2 += step
                }

                if (t1 < 1.0) {
                    pass2.add(it.sub(t1, 1.0))
                }
            }

            return pass2.flatMap { it.split(0.5).toList() }
        }

    fun scale(scale: Double) = scale { scale }

    val clockwise
        get() = angle(start, end, control[0]) > 0

    fun scale(scale: (Double) -> Double): Segment {
        if (control.size == 1) {
            return cubic.scale(scale)
        }

        val newStart = start + normal(0.0) * scale(0.0)
        val newEnd = end + normal(1.0) * scale(1.0)

        val a = LineSegment(start + normal(0.0) * 10.0, start)
        val b = LineSegment(end + normal(1.0) * 10.0, end)

        val o = intersection(a, b, 1000000000.0)

        LineSegment(newStart, newEnd)

        if (o != Vector2.INFINITY) {
            val newControls = control.mapIndexed { index, it ->
                val d = it - o
                val rc = scale((index + 1.0) / 3.0)

                val s = normal(0.0).dot(d).sign

                val nd = d.normalized * s
                it + rc * nd
            }
            return Segment(newStart, newControls.toTypedArray(), newEnd)
        } else {
            val newControls = control.mapIndexed { index, it ->
                val rc = scale((index + 1.0) / 3.0)
                it + rc * normal(0.0) * if (clockwise) 1.0 else -1.0
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
            else -> throw RuntimeException("cannot convert to cubic segment")
        }

    fun derivative(t: Double): Vector2 = when {
        linear -> start - end
        control.size == 1 -> derivative(start, control[0], end, t)
        control.size == 2 -> derivative(start, control[0], control[1], end, t)
        else -> throw RuntimeException("not implemented")
    }

    fun normal(ut: Double): Vector2 {
        return direction(ut).let { it.copy(it.y * -1.0, it.x) }
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
            z1 == 1.0 -> split(z0)[1]
            else -> split(z0)[1].split(map(z0, 1.0, 0.0, 1.0, z1))[0]
        }
    }

    /**
     * Split the contour
     * @param t the point to split the contour at
     * @return array of parts, depending on the split point this is one or two entries long
     */
    fun split(t: Double): Array<Segment> {
        val u = t.coerceIn(0.0, 1.0)

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

data class ShapeContour(val segments: List<Segment>, val closed: Boolean) {

    companion object {
        fun fromPoints(points: List<Vector2>, closed: Boolean) =
                if (!closed)
                    ShapeContour((0 until points.size - 1).map { Segment(points[it], points[it + 1]) }, closed)
                else {
                    val d = (points.last() - points.first()).squaredLength
                    val usePoints = if (d > 0.001) points else points.dropLast(1)
                    ShapeContour((0 until usePoints.size).map { Segment(usePoints[it], usePoints[(it + 1) % usePoints.size]) }, true)
                }
    }

    val length get() = segments.sumByDouble { it.length }

    val bounds get() = vector2Bounds(sampleLinear().segments.flatMap { listOf(it.start, it.end) })

    val winding: Winding
        get() {
            var sum = 0.0
            segments.forEachIndexed { i, v ->
                val after = segments[mod(i + 1, segments.size)].start
                sum += (after.x - v.start.x) * (after.y + v.start.y)
            }
            return if (sum < 0) {
                Winding.COUNTER_CLOCKWISE
            } else {
                Winding.CLOCKWISE
            }
        }

    val exploded: List<ShapeContour>
        get() = segments.map { ShapeContour(listOf(it), false) }


    val clockwise: ShapeContour get() = if (winding == Winding.CLOCKWISE) this else this.reversed
    val counterClockwise: ShapeContour get() = if (winding == Winding.COUNTER_CLOCKWISE) this else this.reversed

    operator fun plus(other: ShapeContour): ShapeContour {
        val epsilon = 0.001
        val segments = mutableListOf<Segment>()
        segments.addAll(this.segments)
        if ((this.segments[this.segments.size - 1].end - other.segments[0].start).length > epsilon) {
            segments.add(Segment(this.segments[this.segments.size - 1].end, other.segments[0].start))
        }
        segments.addAll(other.segments)
        return ShapeContour(segments, false)
    }

    fun offset(distance: Double, joinType: SegmentJoin = SegmentJoin.ROUND): ShapeContour {
        if (segments.size == 1) {
            return ShapeContour(segments[0].offset(distance), false)
        }

        val joins = (segments + if (closed) listOf(segments.first()) else emptyList()).map {
            it.offset(distance)
        }.zipWithNext().flatMap {
            val end = it.first.last().end
            val start = it.second.first().start

            when (joinType) {
                SegmentJoin.ROUND -> {
                    val d = (end - start).length
                    val join = contour {
                        moveTo(end)
                        arcTo(d, d, 0.0, false, true, start.x, start.y)
                    }
                    it.first + join.segments
                }
                SegmentJoin.BEVEL -> {
                    val join = contour {
                        moveTo(end)
                        lineTo(start)
                    }
                    it.first + join.segments
                }
                SegmentJoin.MITER -> {
                    val endDir = it.first.last().direction(1.0)
                    val startDir = it.second.first().direction(0.0)
                    val endLine = LineSegment(end, end + endDir)
                    val startLine = LineSegment(start, start + startDir)
                    val i = intersection(endLine, startLine, 10000000.0)
                    val join = contour {
                        moveTo(end)
                        lineTo(i)
                        lineTo(start)
                    }
                    it.first + join.segments
                }
            }
        } + if (!closed) { segments.last().offset(distance) } else emptyList()
        return ShapeContour(joins, closed)
    }

    fun position(ut: Double): Vector2 {
        val t = ut.coerceIn(0.0, 1.0)
        val segment = (t * segments.size).toInt()
        val segmentOffset = (t * segments.size) - segment
        return segments[Math.min(segments.size - 1, segment)].position(segmentOffset)
    }

    /**
     * Evaluates the contour for the given position
     */
    fun normal(ut: Double): Vector2 {
        val t = ut.coerceIn(0.0, 1.0)
        val segment = (t * segments.size).toInt()
        val segmentOffset = (t * segments.size) - segment
        return segments[Math.min(segments.size - 1, segment)].normal(segmentOffset)
    }

    fun adaptivePositions(distanceTolerance: Double = 0.5): List<Vector2> {
        val adaptivePoints = mutableListOf<Vector2>()
        var last: Vector2? = null
        for (segment in this.segments) {
            val samples = segment.sampleAdaptive(distanceTolerance)
            if (samples.isNotEmpty()) {
                val r = samples[0]
                if (last == null || last.minus(r).length > 0.01) {
                    adaptivePoints.add(r)
                }
                for (i in 1 until samples.size) {
                    adaptivePoints.add(samples[i])
                    last = samples[i]
                }
            }
        }
        return adaptivePoints
    }

    fun adaptivePositionsAndDirection(distanceTolerance: Double = 0.5): Pair<List<Vector2>, List<Vector2>> {
        val adaptivePoints = mutableListOf<Vector2>()
        val adaptiveNormals = mutableListOf<Vector2>()
        var last: Vector2? = null
        for (segment in this.segments) {
            val samples = segment.sampleAdaptiveNormals(distanceTolerance)
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
        return Pair(adaptivePoints, adaptiveNormals)
    }

    /**
     *
     */
    fun equidistantPositions(pointCount: Int): List<Vector2> {
        return sampleEquidistant(adaptivePositions(), pointCount)
    }

    /**
     * Adaptively sample the contour into line segments while still approximating the original contour
     * @param distanceTolerance controls the quality of the approximation
     * @return a ShapeContour composed of linear segments
     */
    fun sampleLinear(distanceTolerance: Double = 0.5) =
            fromPoints(adaptivePositions(distanceTolerance), closed)

    /**
     * Sample the shape contour into line segments
     */
    fun sampleEquidistant(pointCount: Int): ShapeContour {
        val points = equidistantPositions(pointCount)
        val segments = (0 until points.size - 1).map { Segment(points[it], points[it + 1]) }
        return ShapeContour(segments, closed)
    }

    fun transform(transform: Matrix44) = ShapeContour(segments.map { it.transform(transform) }, closed)

    private fun mod(a: Double, b: Double) = ((a % b) + b) % b

    /**
     * Sample a sub contour
     * @param u0 starting point in [0, 1)
     * @param u1 ending point in [0, 1)
     * @return sub contour
     */
    fun sub(u0: Double, u1: Double): ShapeContour {
        var t0 = u0
        var t1 = u1

        if (closed && (t1 < t0 || t1 > 1.0 || t0 > 1.0 || t0 < 0.0 || t1 < 0.0)) {
            val diff = t1 - t0
            t0 = mod(t0, 1.0)
            if (Math.abs(diff) < 0.9999999999999998) {
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
        return ShapeContour(newSegments, false)
    }


    /**
     * Checks if a give point lies on the contour
     * @param point the point to check
     * @param error what is the allowed error (unitless, but likely in pixels)
     * @return the contour parameter in [0..1.0) if the point is within error `null` otherwise
     *
     */
    fun on(point: Vector2, error: Double = 5.0): Double? {
        for (i in 0 until segments.size) {
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
    fun project(point: Vector2): ContourProjection {
        val nearest = segments.mapIndexed { index, it -> Pair(index, it.project(point)) }.minBy { it.second.distance }!!

        return ContourProjection(nearest.second, (nearest.first + nearest.second.projection) /
                segments.size, nearest.second.distance, nearest.second.point)

    }

    val reversed get() = ShapeContour(segments.map { it.reverse }.reversed(), closed)

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

        return ShapeContour(if (segments.size > 1) fixedSegments else segments, closed)
    }
}

class Shape(val contours: List<ShapeContour>) {
    val linear get() = contours.all { it.segments.all { it.linear } }
    fun polygon(distanceTolerance: Double = 0.5) = Shape(contours.map { it.sampleLinear(distanceTolerance) })

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

    /**
     * Splits a compound shape into separate shapes.
     */
    fun splitCompounds(): List<Shape> {
        return if (contours.isEmpty()) {
            emptyList()
        } else {
            var split = Winding.COUNTER_CLOCKWISE
            if (contours[0].winding == Winding.CLOCKWISE) {
                split = Winding.CLOCKWISE
            }
            val (ccw, cw) = contours.partition { it.winding == split }
            ccw.map { Shape(listOf(it.counterClockwise) + cw.map { it.clockwise }) }
        }
    }

}

class Compound(val shapes: List<Shape>)