@file:Suppress("unused", "MemberVisibilityCanPrivate")

package org.openrndr.shape

import org.openrndr.math.*
import org.openrndr.shape.internal.BezierCubicRenderer
import org.openrndr.shape.internal.BezierQuadraticSampler

class SegmentProjection(val segment: Segment, val projection: Double, val distance: Double, val point: Vector2)
class ContourProjection(val segmentProjection: SegmentProjection, val projection: Double, val distance: Double, val point: Vector2)

class Segment {
    val start: Vector2
    val end: Vector2
    val control: Array<Vector2>

    val linear: Boolean get() = control.isEmpty()

    private var lut: List<Vector2>? = null

    constructor(start: Vector2, end: Vector2) {
        this.start = start
        this.end = end
        this.control = emptyArray()
    }

    constructor(start: Vector2, c0: Vector2, end: Vector2) {
        this.start = start
        this.control = arrayOf(c0)
        this.end = end
    }

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
        1 -> BezierQuadraticSampler().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], end)
        2 -> BezierCubicRenderer().apply { this.distanceTolerance = distanceTolerance }.sample(start, control[0], control[1], end)
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

    fun direction() : Vector2 {
        return (start - end).normalized
    }

    fun direction(t: Double): Vector2 {
        return if (linear) {
            direction()
        } else if (control.size == 1) {
            derivative(start, control[0], end, t).normalized
        } else if (control.size == 2) {
            derivative(start, control[0], control[1], end, t).normalized
        } else {
            throw RuntimeException("not implemented")
        }
    }

    fun normal(ut:Double):Vector2 {
        return direction(ut).let { it.copy(it.y*-1.0, it.x) }
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

                    val prx = rsm * px//px.multiply(rsm)
                    val pry = rsm * py//py.multiply(rsm)

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

                    val plx = lsm * px //px.multiply(lsm)
                    val ply = lsm * py //py.multiply(lsm)

                    val left = Segment(
                            Vector2(plx.x, ply.x),
                            Vector2(plx.y, ply.y),
                            Vector2(plx.z, ply.z))


                    val rsm = Matrix44(
                            iz2, 2.0 * iz * z, z2, 0.0,
                            0.0, iz, z, 0.0,
                            0.0, 0.0, 1.0, 0.0,
                            0.0, 0.0, 0.0, 0.0)

                    val prx = rsm * px //multiply(rsm)
                    val pry = rsm * py //.multiply(rsm)

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

    fun intersect(other: Segment) {
        if (control.size == 0 && other.control.size == 0) {
            // line line intersection
        }
    }
}

private fun sumDifferences(points: List<Vector2>): Double =
        (0 until points.size - 1).sumByDouble { (points[it] - points[it + 1]).length }

class ShapeContour(val segments: List<Segment>, val closed: Boolean) {

    companion object {
        fun fromPoints(points: List<Vector2>, closed: Boolean) =
                ShapeContour((0 until points.size - 1).map { Segment(points[it], points[it + 1]) }, closed)
    }

    val length get() = segments.sumByDouble { it.length }


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

    fun position(ut: Double): Vector2 {
        val t = ut.coerceIn(0.0, 1.0)
        val segment = (t * segments.size).toInt()
        val segmentOffset = (t * segments.size) - segment

        return segments[Math.min(segments.size - 1, segment)].position(segmentOffset)
    }

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

    fun equidistantPositions(pointCount: Int): List<Vector2> {
        return sampleEquidistant(adaptivePositions(), pointCount)
    }


    fun sampleLinear(distanceTolerance: Double = 0.5): ShapeContour {
        return fromPoints(adaptivePositions(distanceTolerance), closed)
    }

    fun sampleEquidistant(pointCount: Int): ShapeContour {
        val points = equidistantPositions(pointCount)
        val segments = (0 until points.size - 1).map { Segment(points[it], points[it + 1]) }
        return ShapeContour(segments, closed)
    }

    fun transform(transform: Matrix44): ShapeContour
            = ShapeContour(segments.map { it.transform(transform) }, closed)


    fun mod(a: Double, b: Double): Double {
        return ((a % b) + b) % b
    }

    fun sub(u0: Double, u1: Double): ShapeContour {
        var t0 = u0
        var t1 = u1

        if (closed && t0 < 0.0 || t1 > 1.0 || t1 < 0.0 || t1 > 1.0) {
            t0 = mod(t0, 1.0)
            t1 = mod(t1, 1.0)

            if (t0 > t1)
                return sub(t0, 1.0) + sub(0.0, t1)
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
        val newContour = ShapeContour(newSegments, false)

        return newContour
    }


    fun on(point: Vector2, error: Double = 5.0): Double? {
        for (i in 0 until segments.size) {
            val st = segments[i].on(point, error)
            if (st != null) {
                return (i + st) / segments.size
            }
        }
        return null
    }

    fun project(point: Vector2): ContourProjection {

        val nearest = segments.
                mapIndexed { index, it -> Pair(index, it.project(point)) }.
                minBy { it.second.distance }!!


        return ContourProjection(nearest.second, (nearest.first + nearest.second.projection) /
                segments.size, nearest.second.distance, nearest.second.point)

    }


    val reversed get() = ShapeContour(segments.map { it.reverse }, closed)
}


class Shape(val contours: List<ShapeContour>) {
    val linear: Boolean get() = contours.all { it.segments.all { it.linear } }
    fun polygon(distanceTolerance: Double = 0.5) = Shape(contours.map { it.sampleLinear(distanceTolerance) })

    val outline get() = contours[0]
    fun hole(index: Int) = contours[index + 1]

    fun transform(transform: Matrix44): Shape = Shape(contours.map { it.transform(transform) })

}