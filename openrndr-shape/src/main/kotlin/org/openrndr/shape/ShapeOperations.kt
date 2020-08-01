@file:Suppress("unused")

package org.openrndr.shape

import io.lacuna.artifex.*
import io.lacuna.artifex.utils.Intersections
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity

private fun Vector2.toVec2(): Vec2 {
    return Vec2(x, y)
}

private fun Vec2.toVector2(): Vector2 {
    return Vector2(x, y)
}

private fun Segment.toCurve2(): Curve2 {
    return when (control.size) {
        0 -> Line2.line(start.toVec2(), end.toVec2())
        1 -> Bezier2.curve(start.toVec2(), control[0].toVec2(), end.toVec2())
        2 -> Bezier2.curve(start.toVec2(), control[0].toVec2(), control[1].toVec2(), end.toVec2())
        else -> throw IllegalArgumentException("unsupported control count ${control.size}")
    }
}

private fun ShapeContour.toPath2(): Path2 {
    return Path2(segments.map { it.toCurve2() })
}

private fun ShapeContour.toRing2(): Ring2 {
    return Ring2(segments.map { it.toCurve2() })
}

private fun Shape.toRegion2(): Region2 {
    return Region2(contours.map { it.toRing2() })
}

private fun Region2.toShapes(): List<Shape> {
    val shapes = mutableListOf<Shape>()
    if (rings.isNotEmpty()) {

        val contours = mutableListOf<ShapeContour>()
        rings.forEach { it ->
            contours.add(it.toShapeContour())

            if (!it.isClockwise) {
                if (contours.isNotEmpty()) {
                    shapes.add(Shape(contours.reversed()))
                }
                contours.clear()
            }
        }
        if (contours.isNotEmpty()) {
            shapes.add(Shape(contours.reversed()))
        }

        if (rings.size != shapes.sumBy { it.contours.size }) {
            throw RuntimeException("conversion broken")
        }
    }
    return shapes
}

private fun Curve2.toSegment(): Segment {
    return when (this) {
        is Line2 -> Segment(this.start().toVector2(), this.end().toVector2())
        is Bezier2.QuadraticBezier2 -> Segment(this.p0.toVector2(), this.p1.toVector2(), this.p2.toVector2())
        is Bezier2.CubicBezier2 -> Segment(this.p0.toVector2(), this.p1.toVector2(), this.p2.toVector2(), this.p3.toVector2())
        else -> throw IllegalArgumentException()
    }
}

private fun Ring2.toShapeContour(): ShapeContour {
    return ShapeContour(this.curves.map { it.toSegment() }, true, YPolarity.CW_NEGATIVE_Y)
}

private fun List<Shape>.toRegion2(): Region2 {
    return Region2(flatMap { shape ->
        shape.contours.map { it.toRing2() }
    })
}

fun difference(from: ShapeContour, subtract: ShapeContour): List<Shape> {
    val result = from.toRing2().region().difference(subtract.toRing2().region())
    return result.toShapes()
}

fun difference(from: Shape, subtract: ShapeContour): List<Shape> {
    val result = from.toRegion2().difference(subtract.toRing2().region())
    return result.toShapes()
}

fun difference(from: Shape, subtract: Shape): List<Shape> {
    val result = from.toRegion2().difference(subtract.toRegion2())
    return result.toShapes()
}

fun difference(from: List<Shape>, subtract: ShapeContour): List<Shape> {
    return from.toRegion2().difference(subtract.toRing2().region()).toShapes()
}

fun difference(from: List<Shape>, subtract: Shape): List<Shape> {
    return from.toRegion2().difference(subtract.toRegion2()).toShapes()
}

fun difference(from: List<Shape>, subtract: List<Shape>): List<Shape> {
    return from.toRegion2().difference(subtract.toRegion2()).toShapes()
}

@JvmName("differenceIterative")
fun difference(from: List<Shape>, subtract: List<List<Shape>>): List<Shape> {
    var left = from
    for (subtractShapes in subtract) {
        left = difference(left, subtractShapes)
    }
    return left
}


fun union(from: ShapeContour, add: ShapeContour): List<Shape> {
    val result = from.toRing2().region().union(add.toRing2().region())
    return result.toShapes()
}

fun union(from: Shape, add: ShapeContour): List<Shape> {
    val result = from.toRegion2().union(add.toRing2().region())
    return result.toShapes()
}

fun union(from: Shape, add: Shape): List<Shape> {
    val result = from.toRegion2().union(add.toRegion2())
    return result.toShapes()
}

fun union(from: List<Shape>, add: ShapeContour): List<Shape> {
    return from.toRegion2().union(add.toRing2().region()).toShapes()
}

fun union(from: List<Shape>, add: Shape): List<Shape> {
    return from.toRegion2().union(add.toRegion2()).toShapes()
}

fun union(from: List<Shape>, add: List<Shape>): List<Shape> {
    return from.toRegion2().union(add.toRegion2()).toShapes()
}

@JvmName("unionIterative")
fun union(from: List<Shape>, add: List<List<Shape>>): List<Shape> {
    var left = from
    for (addShapes in add) {
        left = union(left, addShapes)
    }
    return left
}


fun intersection(from: ShapeContour, with: ShapeContour): List<Shape> {
    val result = from.toRing2().region().intersection(with.toRing2().region())
    return result.toShapes()
}

fun intersection(from: Shape, with: ShapeContour): List<Shape> {
    val result = from.toRegion2().intersection(with.toRing2().region())
    return result.toShapes()
}

fun intersection(from: ShapeContour, with: Shape): List<Shape> {
    val result = from.toRing2().region().intersection(with.toRegion2())
    return result.toShapes()
}

fun intersection(from: Shape, with: Shape): List<Shape> {
    val result = from.toRegion2().intersection(with.toRegion2())
    return result.toShapes()
}

fun intersection(from: List<Shape>, with: ShapeContour): List<Shape> {
    return from.toRegion2().intersection(with.toRing2().region()).toShapes()
}

fun intersection(from: List<Shape>, with: Shape): List<Shape> {
    return from.toRegion2().intersection(with.toRegion2()).toShapes()
}

fun intersection(from: List<Shape>, with: List<Shape>): List<Shape> {
    return from.toRegion2().intersection(with.toRegion2()).toShapes()
}

@JvmName("intersectionIterative")
fun intersection(from: List<Shape>, with: List<List<Shape>>): List<Shape> {
    var left = from
    for (withShapes in with) {
        left = intersection(left, withShapes)
    }
    return left
}

class SegmentIntersection(val segmentA: Segment, val segmentTA: Double,
                          val segmentB: Segment, val segmentTB: Double,
                          val position: Vector2)

/** Find intersections between two Segments **/
fun intersections(a: Segment, b: Segment): List<SegmentIntersection> {
    // Test if checking against self. This test should be improved such that it is not based on object identity
    val selfTest = a === b
    val ca = a.toCurve2()
    val cb = b.toCurve2()

    return if (!selfTest) {
        Intersections.intersections(ca, cb).map {
            SegmentIntersection(a, it.x, b, it.y, ca.position(it.x).toVector2())
        }
    } else {
        // Here we should handle self-intersections properly
        emptyList()
    }
}

data class ContourIntersection(val contourA: ShapeContour, val contourTA: Double, val segmentA: Segment, val segmentTA: Double,
                               val contourB: ShapeContour, val contourTB: Double, val segmentB: Segment, val segmentTB: Double,
                               val position: Vector2)

/**
 * Find intersections between two ShapeContours
 */
fun intersections(a: ShapeContour, b: ShapeContour): List<ContourIntersection> {
    val selfTest = a === b
    val result = mutableListOf<ContourIntersection>()
    for ((ia, sa) in a.segments.withIndex()) {
        for ((ib, sb) in b.segments.withIndex()) {
            if (selfTest && ib > ia) {
                continue
            }
            val segmentIntersections = intersections(sa, sb).let {
                if (selfTest) {
                    it.filterNot { intersection -> intersection.segmentTA == 1.0 && intersection.segmentTB == 0.0 || intersection.segmentTA == 0.0 && intersection.segmentTB == 1.0 }
                } else {
                    it
                }
            }
            result.addAll(segmentIntersections.map {
                ContourIntersection(
                        a, (ia + it.segmentTA) / a.segments.size, it.segmentA, it.segmentTA,
                        b, (ib + it.segmentTB) / b.segments.size, it.segmentB, it.segmentTB,
                        it.position
                )
            })
        }
    }
    return result.let {
        if (selfTest) {
            it.distinctBy { intersection -> Pair(intersection.contourTA, intersection.contourTB) }
        } else {
            it
        }
    }
}

fun split(shape: Shape, line: LineSegment): Pair<List<Shape>, List<Shape>> {
    val center = (line.end + line.start) / 2.0
    val direction = (line.end - line.start).normalized
    val perpendicular = direction.perpendicular(shape.contours.first().polarity)
    val extend = 50000.0

    val splitLine = LineSegment(center - direction * extend, center + direction * extend)

    val leftContour = shape {
        contour {
            moveTo(splitLine.start)
            lineTo(cursor + perpendicular * extend)
            lineTo(cursor + direction * extend)
            lineTo(splitLine.end)
            lineTo(splitLine.start)
            close()
        }
    }

    val rightContour = shape {
        contour {
            moveTo(splitLine.start)
            lineTo(cursor - perpendicular * extend)
            lineTo(cursor + direction * extend)
            lineTo(splitLine.end)
            lineTo(splitLine.start)
            close()
        }
    }
    val leftShapes = difference(shape, leftContour)
    val rightShapes = difference(shape, rightContour)
    return Pair(leftShapes, rightShapes)
}


data class SegmentPoint(val segment: Segment, val segmentT: Double, val position: Vector2)

fun Segment.nearest(point: Vector2): SegmentPoint {
    val c2 = this.toCurve2()
    val t = c2.nearestPoint(point.toVec2()).coerceIn(0.0, 1.0)
    val p = c2.position(t).toVector2()
    return SegmentPoint(this, t, p)
}

data class ContourPoint(val contour: ShapeContour, val contourT: Double, val segment: Segment, val segmentT: Double, val position: Vector2)

fun ShapeContour.nearest(point: Vector2): ContourPoint {
    val n = segments.map { it.nearest(point) }.minBy { it.position.distanceTo(point) } ?: error("no segments")
    val segmentIndex = segments.indexOf(n.segment)
    val t = (segmentIndex + n.segmentT) / segments.size
    return ContourPoint(this, t, n.segment, n.segmentT, n.position)
}
