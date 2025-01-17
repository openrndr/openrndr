package org.openrndr.shape

import kotlinx.serialization.Serializable
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import kotlin.math.abs
import kotlin.math.min

class PathProjection3D(val segmentProjection: SegmentProjection3D, val projection: Double, val distance: Double, val point: Vector3)

/**
 * Represents a 3D path composed of multiple segments. The class supports operations on the path,
 * such as transformations, sampling, sub-path extraction, and various geometric calculations.
 * It also provides methods for creating paths from points or segments and combines functionality
 * for both open and closed paths.
 *
 * @property segments A list of the segments forming the path.
 * @property closed Indicates whether the path is closed (i.e., the last point connects to the first point).
 */
@Serializable
class Path3D(override val segments: List<Segment3D>, override val closed: Boolean) : Path<Vector3> {
    companion object {

        val EMPTY: Path3D = Path3D(emptyList(), false)

        /**
         * Constructs a Path3D object from a list of 3D points, optionally closing the path.
         *
         * @param points A list of [Vector3] representing the points that define the path.
         * @param closed A boolean indicating whether the path should be closed, connecting the last point back to the first.
         */
        fun fromPoints(points: List<Vector3>, closed: Boolean) =
                if (!closed)
                    Path3D((0 until points.size - 1).map { Segment3D(points[it], points[it + 1]) }, closed)
                else
                    Path3D((points.indices).map { Segment3D(points[it], points[(it + 1) % points.size]) }, closed)

        /**
         * Constructs a Path3D instance from a list of 3D segments.
         *
         * @param segments the list of 3D segments that define the path
         * @param closed a boolean indicating whether the path is closed
         * @param distanceTolerance the maximum allowable squared distance between segment endpoints for the path, default is 1E-3
         * @return a Path3D instance constructed from the segments
         * @throws IllegalArgumentException if the distance between consecutive segment ends and starts exceeds the specified tolerance
         */
        fun fromSegments(
            segments: List<Segment3D>,
            closed: Boolean,
            distanceTolerance: Double = 1E-3,
        ): Path3D {
            if (segments.isEmpty()) {
                return EMPTY
            }
            return Path3D(
                segments.zipWithNext().map {
                    val distance = it.first.end.squaredDistanceTo(it.second.start)
                    require(distance < distanceTolerance) {
                        "distance between segment end and start is $distance (max: $distanceTolerance)"
                    }
                    it.first.copy(end = it.second.start)
                } + segments.last(), closed
            )
        }
    }

    /**
     * A computed property that calculates and returns the bounding box of the `Path3D`.
     * The bounding box is determined by mapping and aggregating the individual bounding boxes
     * of all segments in the path.
     */
    val bounds: Box get() {
        val b = segments.map { it.bounds }
        return b.bounds
    }

    /**
     * A computed property that provides a list of individual `Path3D` instances,
     * each wrapping a single segment from the `segments` property of the current `Path3D`.
     * Each of these paths is not closed.
     */
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

    override val infinity: Vector3 = Vector3.INFINITY

    override val empty: Boolean
        get() {
            return segments.isEmpty()
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


    /**
     * Transforms the current Path3D using the provided transformation matrix.
     *
     * @param transform the transformation matrix of type Matrix44 to apply to the Path3D.
     * @return a new Path3D instance with its segments transformed by the provided matrix.
     */
    fun transform(transform: Matrix44) = Path3D(segments.map { it.transform(transform) }, closed)

    private fun mod(a: Double, b: Double) = ((a % b) + b) % b

    /**
     * Sample a sub contour
     * @param t0 starting point in [0, 1)
     * @param t1 ending point in [0, 1)
     * @return sub contour
     */
    override fun sub(t0: Double, t1: Double): Path3D {
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
                newSegments.add(segments[s].sub(segmentOffset0, segmentOffset1) as Segment3D)
            } else if (s == segment0) {
                if (segmentOffset0 < 1.0 - epsilon)
                    newSegments.add(segments[s].sub(segmentOffset0, 1.0) as Segment3D)
            } else if (s == segment1) {
                if (segmentOffset1 > epsilon)
                    newSegments.add(segments[s].sub(0.0, segmentOffset1) as Segment3D)
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

    /**
     * Retrieves a new `Path3D` instance where the order of the segments is reversed and
     * each segment is individually reversed. The closed property of the path is preserved.
     */
    val reversed: Path3D get() = Path3D(segments.map { it.reverse }.reversed(), closed)


    /**
     * Transforms the segments of the current Path3D by applying a given mapping function, and optionally adjusts
     * the connections between the segments to maintain continuity.
     *
     * @param closed Specifies whether the resulting Path3D should be closed or open. Defaults to the current Path3D's closed state.
     * @param mapper A lambda function that applies a transformation to each segment of the Path3D.
     * @return A new Path3D instance with its segments transformed by the provided mapping function.
     */
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

