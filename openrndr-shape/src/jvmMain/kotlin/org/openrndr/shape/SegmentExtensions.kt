package org.openrndr.shape


/**
 * Calculates a [List] of all points where two [Segment]s intersect.
 */
@Suppress("unused")
fun Segment.intersections(other: Segment) = intersections(this, other)

/**
 * Calculates a [List] of all points of where a [Segment] and a [ShapeContour] intersect.
 */
@Suppress("unused")
fun Segment.intersections(other: ShapeContour) = intersections(this.contour, other)

/**
 * Calculates a [List] of all points of where a [Segment] and a [Shape] intersect.
 */
@Suppress("unused")
fun Segment.intersections(other: Shape) = intersections(this.contour.shape, other)