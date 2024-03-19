package org.openrndr.shape


/**
 * Calculates a [List] of all points where two [Segment2D]s intersect.
 */
@Suppress("unused")
fun Segment2D.intersections(other: Segment2D) = intersections(this, other)

/**
 * Calculates a [List] of all points of where a [Segment2D] and a [ShapeContour] intersect.
 */
@Suppress("unused")
fun Segment2D.intersections(other: ShapeContour) = intersections(this.contour, other)

/**
 * Calculates a [List] of all points of where a [Segment2D] and a [Shape] intersect.
 */
@Suppress("unused")
fun Segment2D.intersections(other: Shape) = intersections(this.contour.shape, other)