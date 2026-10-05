package org.openrndr.shape

import org.openrndr.kartifex.Vec2
import org.openrndr.math.Vector2

/** Returns true if given [point] lies inside the [ShapeContour]. */
operator fun ShapeContour.contains(point: Vector2): Boolean = closed && this.ring2.test(
    Vec2(point.x, point.y)
).inside


/** Splits a [ShapeContour] with another [ShapeContour]. */
fun ShapeContour.split(cutter: ShapeContour) = split(this, cutter)

/** Splits a [ShapeContour] with a [List] of [ShapeContour]s. */
fun ShapeContour.split(cutters: List<ShapeContour>) = split(this, cutters)

/** Applies a boolean org.openrndr.shape.union operation between the [ShapeContour] and a [Shape]. */
@Suppress("unused")
fun ShapeContour.union(other: Shape): Shape = union(this.shape, other)

/** Applies a boolean difference operation between the [ShapeContour] and another [Shape]. */
@Suppress("unused")
fun  ShapeContour.difference(other: Shape): Shape = difference(this, other)

/** Applies a boolean intersection operation between the [ShapeContour] and a [Shape]. */
@Suppress("unused")
fun  ShapeContour.intersection(other: Shape): Shape = intersection(this, other)

/** Calculates a [List] of all intersections between the [ShapeContour] and a [Segment2D]. */
@Suppress("unused")
fun  ShapeContour.intersections(other: Segment2D) = intersections(this, other.contour)

/** Calculates a [List] of all intersections between the [ShapeContour] and another [ShapeContour]. */
@Suppress("unused")
fun  ShapeContour.intersections(other: ShapeContour) = intersections(this, other)

/** Calculates a [List] of all intersections between the [ShapeContour] and a [Shape]. */
@Suppress("unused")
fun  ShapeContour.intersections(other: Shape) = intersections(this.shape, other)
