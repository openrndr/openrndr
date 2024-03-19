package org.openrndr.shape

import org.openrndr.math.EuclideanVector
import org.openrndr.math.Vector2

interface BezierSegmentPoint<T : EuclideanVector<T>> {
    val segment: BezierSegment<T>
    val segmentT: Double
    val position: T
}

/**
 * Representation of a point on a [Segment2D].
 *
 * @param segment The [Segment2D] on which the point lies.
 * @param segmentT The [t](https://pomax.github.io/bezierinfo/#explanation) value of the point on the [Segment2D].
 * @param position The position of the point.
 */
data class SegmentPoint(
    override val segment: Segment2D,
    override val segmentT: Double,
    override val position: Vector2
) : BezierSegmentPoint<Vector2>


/**
 * Representation of a point on a [ShapeContour].
 *
 * @param contour The [ShapeContour] on which the point lies.
 * @param contourT The [t](https://pomax.github.io/bezierinfo/#explanation) value of the point on the [ShapeContour] in the range of `0.0` to `1.0`.
 * @param segment The [Segment2D] on which the point lies.
 * @param segmentT The *t* value of the point on the [Segment2D] in the range of `0.0` to `1.0`.
 * @param position The position of the point.
 */
data class ContourPoint(
    val contour: ShapeContour,
    val contourT: Double,
    val segment: Segment2D,
    val segmentT: Double,
    val position: Vector2
)

/** Indicates the type of [Segment2D]. */
enum class SegmentType {
    /** A simple [Segment2D] with two anchor points. */
    LINEAR,

    /** A quadratic Bézier curve [Segment2D] with two anchor points and one control point. */
    QUADRATIC,

    /** A cubic Bézier curve [Segment2D] with two anchor points and two control points. */
    CUBIC
}

/** Indicates the winding order of the [ShapeContour]. */
enum class Winding {
    CLOCKWISE,
    COUNTER_CLOCKWISE
}

enum class SegmentJoin {
    ROUND,
    MITER,
    BEVEL
}

/** Indicates the [Shape] topology. */
enum class ShapeTopology {
    /** The [Shape] consists entirely of closed [ShapeContour]s. */
    CLOSED,

    /** The [Shape] consists entirely of open [ShapeContour]s. */
    OPEN,

    /** The [Shape] contains both open and closed [ShapeContour]s. */
    MIXED
}