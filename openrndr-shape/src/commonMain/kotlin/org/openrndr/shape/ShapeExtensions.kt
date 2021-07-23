package org.openrndr.shape

import org.openrndr.kartifex.Vec2
import org.openrndr.math.Vector2


/** Applies a boolean org.openrndr.shape.union operation between two [Shape]s. */
fun Shape.union(other: Shape): Shape = union(this, other)

/** Applies a boolean difference operation between two [Shape]s. */
fun Shape.difference(other: Shape): Shape = difference(this, other)

/** Applies a boolean intersection operation between two [Shape]s. */
fun Shape.intersection(other: Shape): Shape = intersection(this, other)

/** Calculates a [List] of all points where two [Shape]s intersect. */
fun Shape.intersections(other: Shape) = intersections(this, other)

/** Calculates a [List] of all points where the [Shape] and a [ShapeContour] intersect. */
fun Shape.intersections(other: ShapeContour) = intersections(this, other.shape)

/** Calculates a [List] of all points where the [Shape] and a [Segment] intersect. */
fun Shape.intersections(other: Segment) = intersections(this, other.contour.shape)

/** Returns true if given [Vector2] is inside the [Shape]. */
operator fun Shape.contains(v: Vector2): Boolean {
    if (empty) {
        return false
    }
    return toRegion2().contains(Vec2(v.x, v.y))
}
