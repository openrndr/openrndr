package org.openrndr.shape

import org.openrndr.kartifex.Vec2
import org.openrndr.math.Vector2
import kotlin.math.sqrt

/** Returns true if given [point] lies inside the [ShapeContour]. */
operator fun ShapeContour.contains(point: Vector2): Boolean = closed && this.toRing2().test(
    Vec2(point.x, point.y)
).inside


/** Splits a [ShapeContour] with another [ShapeContour]. */
fun ShapeContour.split(cutter: ShapeContour) = split(this, cutter)

/** Splits a [ShapeContour] with a [List] of [ShapeContour]s. */
fun ShapeContour.split(cutters: List<ShapeContour>) = split(this, cutters)


fun ShapeContour.removeLoops(attempts: Int = 0, reverseOrder: Boolean = false): ShapeContour {
    if (attempts > 10) {
        error("tried more than 10 times to remove loops")
    }

    if (this.closed) {
        return this
    } else {
        val ints = intersections(this, this)
        if (ints.isEmpty()) {
            return this
        } else {

            val toFix = ints.minByOrNull { it.a.contourT }!!
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


/**
 * Offsets a [ShapeContour]'s [Segment]s by given [distance].
 *
 * [Segment]s are moved outwards if [distance] is > 0 or inwards if [distance] is < 0.
 *
 * @param joinType Specifies how to join together the moved [Segment]s.
 */
fun ShapeContour.offset(distance: Double, joinType: SegmentJoin = SegmentJoin.ROUND): ShapeContour {
    val offsets =
        segments.map { it.offset(distance, yPolarity = polarity) }
            .filter { it.isNotEmpty() }
    val tempContours = offsets.map {
        ShapeContour.fromSegments(it, closed = false, distanceTolerance = 0.01)
    }
    val offsetContours = tempContours.map { it }.filter { it.length > 0.0 }.toMutableList()

    for (i in 0 until offsetContours.size) {
        offsetContours[i] = offsetContours[i].removeLoops()
    }

    for (i in 0 until if (this.closed) offsetContours.size else offsetContours.size - 1) {
        val i0 = i
        val i1 = (i + 1) % (offsetContours.size)
        val its = intersections(offsetContours[i0], offsetContours[i1])
        if (its.size == 1) {
            offsetContours[i0] = offsetContours[i0].sub(0.0, its[0].a.contourT)
            offsetContours[i1] = offsetContours[i1].sub(its[0].b.contourT, 1.0)
        }
    }

    if (offsets.isEmpty()) {
        return ShapeContour(emptyList(), false)
    }


    val startPoint = if (closed) offsets.last().last().end else offsets.first().first().start

    val candidateContour = contour {
        moveTo(startPoint)
        for (offsetContour in offsetContours) {
            val delta = (offsetContour.position(0.0) - cursor)
            val joinDistance = delta.length
            if (joinDistance > 10e-6) {
                when (joinType) {
                    SegmentJoin.BEVEL -> lineTo(offsetContour.position(0.0))
                    SegmentJoin.ROUND -> arcTo(
                        crx = joinDistance * 0.5 * sqrt(2.0),
                        cry = joinDistance * 0.5 * sqrt(2.0),
                        angle = 90.0,
                        largeArcFlag = false,
                        sweepFlag = true,
                        end = offsetContour.position(0.0)
                    )
                    SegmentJoin.MITER -> {
                        val ls = lastSegment ?: offsetContours.last().segments.last()
                        val fs = offsetContour.segments.first()
                        val i = intersection(
                            ls.end,
                            ls.end + ls.direction(1.0),
                            fs.start,
                            fs.start - fs.direction(0.0),
                            eps = 10E8
                        )
                        if (i !== Vector2.INFINITY) {
                            lineTo(i)
                            lineTo(fs.start)
                        } else {
                            lineTo(fs.start)
                        }
                    }
                }
            }
            for (offsetSegment in offsetContour.segments) {
                segment(offsetSegment)
            }

        }
        if (this@offset.closed) {
            close()
        }
    }

    val postProc = false

    var final = candidateContour.removeLoops()

    if (postProc && !final.empty) {
        val head = Segment(
            segments[0].start + segments[0].normal(0.0)
                .perpendicular(polarity) * 1000.0, segments[0].start
        ).offset(distance).firstOrNull()?.copy(end = final.segments[0].start)?.contour

        val tail = Segment(
            segments.last().end,
            segments.last().end - segments.last().normal(1.0)
                .perpendicular(polarity) * 1000.0
        ).offset(distance).firstOrNull()?.copy(start = final.segments.last().end)?.contour

        if (head != null) {
            val headInts = intersections(final, head)
            if (headInts.size == 1) {
                final = final.sub(headInts[0].a.contourT, 1.0)
            }
            if (headInts.size > 1) {
                val sInts = headInts.sortedByDescending { it.a.contourT }
                final = final.sub(sInts[0].a.contourT, 1.0)
            }
        }
//            final = head + final
//
        if (tail != null) {
            val tailInts = intersections(final, tail)
            if (tailInts.size == 1) {
                final = final.sub(0.0, tailInts[0].a.contourT)
            }
            if (tailInts.size > 1) {
                val sInts = tailInts.sortedBy { it.a.contourT }
                final = final.sub(0.0, sInts[0].a.contourT)
            }
        }

//            final = final + tail

    }

    return final
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

/** Calculates a [List] of all intersections between the [ShapeContour] and a [Segment]. */
@Suppress("unused")
fun  ShapeContour.intersections(other: Segment) = intersections(this, other.contour)

/** Calculates a [List] of all intersections between the [ShapeContour] and another [ShapeContour]. */
@Suppress("unused")
fun  ShapeContour.intersections(other: ShapeContour) = intersections(this, other)

/** Calculates a [List] of all intersections between the [ShapeContour] and a [Shape]. */
@Suppress("unused")
fun  ShapeContour.intersections(other: Shape) = intersections(this.shape, other)
