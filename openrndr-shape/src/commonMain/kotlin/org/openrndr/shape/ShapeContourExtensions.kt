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


fun ShapeContour.removeLoops(attempts: Int = 0): ShapeContour {
    if (attempts > 10) {
        error("tried more than 10 times to remove loops")
    }

    if (this.closed) {
        return this
    } else {
        val selfIntersections = intersections(this, this)
        if (selfIntersections.isEmpty()) {
            return this
        } else {

            val toFix = selfIntersections.minByOrNull { it.a.contourT }!!
            val sorted = listOf(toFix.a.contourT, toFix.b.contourT).sorted()

            val head = this.sub(0.0, sorted[0])
            val tail = this.sub(sorted[1], 1.0)
            val tailSegments = tail.segments.toMutableList()


            if (head.segments.isEmpty()) {
                return tail
            }
            if (tail.segments.isEmpty()) {
                return head
            }

            tailSegments[0] = tailSegments.first().copy(start = head.segments.last().end)
            val fixedTail = ShapeContour(tailSegments, closed = false)
            return (head.removeLoops(attempts+1) + fixedTail.removeLoops(attempts+1))
        }
    }
}




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
