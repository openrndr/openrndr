package org.openrndr.shape

import org.openrndr.math.Vector2

/**
 * Representation of a point on a [Segment].
 *
 * @param segment The [Segment] on which the point lies.
 * @param segmentT The [t](https://pomax.github.io/bezierinfo/#explanation) value of the point on the [Segment].
 * @param position The position of the point.
 */
data class SegmentPoint(
    val segment: Segment,
    val segmentT: Double,
    val position: Vector2
)

/**
 * Representation of a point on a [ShapeContour].
 *
 * @param contour The [ShapeContour] on which the point lies.
 * @param contourT The [t](https://pomax.github.io/bezierinfo/#explanation) value of the point on the [ShapeContour].
 * @param segment The [Segment] on which the point lies.
 * @param segmentT The [t](https://pomax.github.io/bezierinfo/#explanation) value of the point on the [Segment].
 * @param position The position of the point.
 */
data class ContourPoint(
    val contour: ShapeContour,
    val contourT: Double,
    val segment: Segment,
    val segmentT: Double,
    val position: Vector2
)

/** Indicates the type of [Segment]. */
enum class SegmentType {
    /** A simple [Segment] with 2 control points. */
    LINEAR,

    /** A quadratic Bézier curve [Segment] with 3 control points. */
    QUADRATIC,

    /** A cubic Bézier curve [Segment] with 4 control points. */
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