package org.openrndr.shape

import org.openrndr.math.Vector2

/**
 * representation of a point on a [Segment]
 * @param segment the [Segment] on which the point lies
 * @param segmentT the t-parameter value of the point on the [Segment]
 * @param position the position of the point
 */
data class SegmentPoint(
    val segment: Segment,
    val segmentT: Double,
    val position: Vector2
)

/**
 * representation of a point on a [ShapeContour]
 * @param contour the [ShapeContour] on which the point lies
 * @param contourT the t-parameter value of the point on the [ShapeContour]
 * @param segment the [Segment] on which the point lies
 * @param segmentT the t-parameter value of the point on the [Segment]
 * @param position the position of the point
 */
data class ContourPoint(
    val contour: ShapeContour,
    val contourT: Double,
    val segment: Segment,
    val segmentT: Double,
    val position: Vector2
)

/**
 * indication of the type of segment
 */
enum class SegmentType {
    /**
     * a segment with 2 control points
     */
    LINEAR,

    /**
     * a bezier segment with 3 control points
     */
    QUADRATIC,

    /**
     * a bezier segment with 4 control points
     */
    CUBIC
}

/**
 * indication of contour winding order
 */
enum class Winding {
    CLOCKWISE,
    COUNTER_CLOCKWISE
}

enum class SegmentJoin {
    ROUND,
    MITER,
    BEVEL
}

/**
 * indication of shape topology
 */
enum class ShapeTopology {
    /**
     * the shape contains closed contours only
     */
    CLOSED,

    /**
     * the shape contains open contours only
     */
    OPEN,

    /**
     * the shape contains a mix of open and closed contours
     */
    MIXED
}