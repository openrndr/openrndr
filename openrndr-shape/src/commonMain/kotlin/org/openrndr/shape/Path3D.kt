package org.openrndr.shape

import kotlinx.serialization.Serializable
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import kotlin.math.abs
import kotlin.math.min

class PathProjection3D(val segmentProjection: SegmentProjection3D, val projection: Double, val distance: Double, val point: Vector3)

@Serializable
class Path3D(val segments: List<Segment3D>, val closed: Boolean) {
    companion object {
        fun fromPoints(points: List<Vector3>, closed: Boolean) =
                if (!closed)
                    Path3D((0 until points.size - 1).map { Segment3D(points[it], points[it + 1]) }, closed)
                else
                    Path3D((points.indices).map { Segment3D(points[it], points[(it + 1) % points.size]) }, closed)
    }

    val exploded: List<Path3D>
        get() = segments.map { Path3D(listOf(it), false) }

    operator fun plus(other: Path3D): Path3D {
        val epsilon = 0.001
        val segments = mutableListOf<Segment3D>()
        segments.addAll(this.segments)
        if ((this.segments[this.segments.size - 1].end - other.segments[0].start).length > epsilon) {
            segments.add(Segment3D(this.segments[this.segments.size - 1].end, other.segments[0].start))
        }
        segments.addAll(other.segments)
        return Path3D(segments, false)
    }


    fun position(ut: Double): Vector3 {
        return when(val t = ut.coerceIn(0.0, 1.0)) {
            0.0 -> segments[0].start
            1.0 -> segments.last().end
            else -> {
                val segment = (t * segments.size).toInt()
                val segmentOffset = (t * segments.size) - segment
                segments[min(segments.size - 1, segment)].position(segmentOffset)
            }
        }
    }

    fun adaptivePositions(distanceTolerance: Double = 0.5): List<Vector3> {
        val adaptivePoints = mutableListOf<Vector3>()
        var last: Vector3? = null
        for (segment in this.segments) {
            val samples = segment.adaptivePositions(distanceTolerance)
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

    fun adaptivePositionsWithT(distanceTolerance: Double = 0.5): List<Pair<Vector3, Double>> {
        val adaptivePoints = mutableListOf<Pair<Vector3, Double>>()
        var last: Vector3? = null
        for (segment in this.segments) {
            val samples = segment.adaptivePositionsWithT(distanceTolerance)
            if (samples.isNotEmpty()) {
                val r = samples[0]
                if (last == null || last.minus(r.first).length > 0.01) {
                    adaptivePoints.add(r)
                }
                for (i in 1 until samples.size) {
                    adaptivePoints.add(samples[i])
                    last = samples[i].first
                }
            }
        }
        return adaptivePoints
    }



    /**
     *
     */
    fun equidistantPositions(pointCount: Int, distanceTolerance: Double = 0.5): List<Vector3> {
        return sampleEquidistant(adaptivePositions(distanceTolerance), pointCount)
    }

    fun equidistantPositionsWithT(pointCount: Int, distanceTolerance: Double = 0.5): List<Pair<Vector3, Double>> {
        return sampleEquidistantWithT(adaptivePositionsWithT(distanceTolerance), pointCount)
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
    fun sampleEquidistant(pointCount: Int, distanceTolerance: Double = 0.5): Path3D {
        val points = equidistantPositions(pointCount, distanceTolerance)
        val segments = (0 until points.size - 1).map { Segment3D(points[it], points[it + 1]) }
        return Path3D(segments, closed)
    }


    fun transform(transform: Matrix44) = Path3D(segments.map { it.transform(transform) }, closed)

    private fun mod(a: Double, b: Double) = ((a % b) + b) % b

    /**
     * Sample a sub contour
     * @param t0 starting point in [0, 1)
     * @param t1 ending point in [0, 1)
     * @return sub contour
     */
    fun sub(t0: Double, t1: Double): Path3D {
        var u0 = t0
        var u1 = t1

        if (closed && (u1 < u0 || u1 > 1.0 || u0 > 1.0 || u0 < 0.0 || u1 < 0.0)) {
            val diff = u1 - u0
            u0 = mod(u0, 1.0)
            if (abs(diff) < 1.0 - 2.0 * 10E-17) {
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


        val newSegments = mutableListOf<Segment3D>()
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
        return Path3D(newSegments, false)
    }


    /**
     * Checks if a give point lies on the contour
     * @param point the point to check
     * @param error what is the allowed error (unitless, but likely in pixels)
     * @return the contour parameter in [0..1.0) if the point is within error `null` otherwise
     *
     */
    fun on(point: Vector3, error: Double = 5.0): Double? {
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
    fun project(point: Vector3): PathProjection3D {
        val nearest = segments.mapIndexed { index, it -> Pair(index, it.project(point)) }.minByOrNull { it.second.distance }!!

        return PathProjection3D(
            nearest.second, (nearest.first + nearest.second.projection) /
                    segments.size, nearest.second.distance, nearest.second.point
        )

    }

    val reversed: Path3D get() = Path3D(segments.map { it.reverse }.reversed(), closed)

    val length get() = segments.sumOf { it.length }


    fun map(closed: Boolean = this.closed, mapper: (Segment3D) -> Segment3D): Path3D {
        val segments = segments.map(mapper)
        val fixedSegments = mutableListOf<Segment3D>()

        if (segments.size > 1) {
            for (i in 0 until segments.size - 1) {
                val left = segments[i]
                val right = segments[i + 1]
                val fixLeft = Segment3D(left.start, left.control, right.start)
                fixedSegments.add(fixLeft)
            }
            if (closed) {
                val left = segments.last()
                val right = segments.first()
                fixedSegments.add(Segment3D(left.start, left.control, right.start))
            } else {
                fixedSegments.add(segments.last())
            }
        }

        return Path3D(if (segments.size > 1) fixedSegments else segments, closed)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this::class != other!!::class)

        other as Path3D

        if (segments != (other as Path3D).segments) return false
        return closed == other.closed
    }

    override fun hashCode(): Int {
        var result = segments.hashCode()
        result = 31 * result + closed.hashCode()
        return result
    }
}

