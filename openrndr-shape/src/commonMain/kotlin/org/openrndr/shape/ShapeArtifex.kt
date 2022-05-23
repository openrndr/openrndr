package org.openrndr.shape


import org.openrndr.kartifex.*
import org.openrndr.kartifex.utils.Intersections
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import kotlin.jvm.JvmName
import kotlin.math.abs

private fun Vector2.toVec2(): Vec2 {
    return Vec2(x, y)
}

private fun Vec2.toVector2(): Vector2 {
    return Vector2(x, y)
}

internal fun Segment.toCurve2(): Curve2 {
    return when (control.size) {
        0 -> Line2.line(start.toVec2(), end.toVec2())
        1 -> Bezier2.curve(start.toVec2(), control[0].toVec2(), end.toVec2())
        2 -> Bezier2.curve(start.toVec2(), control[0].toVec2(), control[1].toVec2(), end.toVec2())
        else -> throw IllegalArgumentException("unsupported control count ${control.size}")
    }
}

private fun Region2.toShape(): Shape {
    return if (rings.isEmpty()) {
        Shape.EMPTY
    } else {
        Shape(rings.map { it.toShapeContour() }.filter { !it.empty })
    }
}

private fun Region2.toShapes(): List<Shape> {
    val shapes = mutableListOf<Shape>()
    if (rings.isNotEmpty()) {

        val contours = mutableListOf<ShapeContour>()
        rings.forEach { ring ->
            contours.add(ring.toShapeContour())

            if (!ring.isClockwise) {
                if (contours.isNotEmpty()) {
                    shapes.add(Shape(contours.reversed()))
                }
                contours.clear()
            }
        }
        if (contours.isNotEmpty()) {
            shapes.add(Shape(contours.reversed()))
        }

        if (rings.size != shapes.sumOf { it.contours.size }) {
            throw RuntimeException("conversion broken")
        }
    }
    return shapes
}

private fun Curve2.toSegment(): Segment {
    return when (this) {
        is Line2 -> Segment(this.start().toVector2(), this.end().toVector2())
        is Bezier2.QuadraticBezier2 -> Segment(this.p0.toVector2(), this.p1.toVector2(), this.p2.toVector2())
        is Bezier2.CubicBezier2 -> Segment(
            this.p0.toVector2(),
            this.p1.toVector2(),
            this.p2.toVector2(),
            this.p3.toVector2()
        )
        else -> throw IllegalArgumentException()
    }
}

private fun Ring2.toShapeContour(): ShapeContour {
    if (curves.isEmpty()) {
        return ShapeContour.EMPTY
    }
    return ShapeContour(this.curves.map { it.toSegment() }, true, YPolarity.CW_NEGATIVE_Y)
}

private fun List<Shape>.toRegion2(): Region2 {
    return Region2(flatMap { shape ->
        shape.contours.map { it.ring2 }
    })
}


/**
 * Applies a boolean difference operation between two [ShapeContour]s.
 */
fun difference(from: ShapeContour, subtract: ShapeContour): Shape {
    return if (from.closed) {
        val result = from.ring2.region().difference(subtract.ring2.region())
        result.toShape()
    } else {
        return if (subtract.closed) {
            val ints = intersections(from, subtract)
            return if (ints.isNotEmpty()) {
                val sortedInts = ints.sortedBy { it.a.contourT }.map { it.a.contourT }
                val weldedInts = (listOf(if (sortedInts.first() > 0.0) 0.0 else null) + sortedInts + (if (sortedInts.last() < 1.0) 1.0 else null)).filterNotNull().merge { a, b ->
                    abs(a - b) < 1E-6
                }
                val partitions = weldedInts.zipWithNext().mapNotNull {
                    val partition = from.sub(it.first, it.second)
                    if (partition.position(0.5) !in subtract) {
                        partition
                    } else {
                        null
                    }
                }
                Shape(partitions)
            } else {
                if (from.position(0.0) !in subtract) from.shape else Shape.EMPTY
            }
        } else {
            from.shape
        }
    }
}

/**
 * Applies a boolean difference operation between a [Shape] and a [ShapeContour].
 */
fun difference(from: Shape, subtract: ShapeContour): Shape {
    return when (from.topology) {
        ShapeTopology.CLOSED -> {
            if (subtract.closed) {
                val result = from.region2.difference(subtract.ring2.region())
                result.toShape()
            } else {
                return from
            }
        }
        ShapeTopology.OPEN -> {
            if (subtract.closed) {
                Shape.compound(from.contours.map {
                    difference(it, subtract)
                })
            } else {
                return from
            }
        }
        ShapeTopology.MIXED -> {
            if (subtract.closed) {
                Shape.compound(from.splitCompounds().map {
                    difference(it, subtract)
                })
            } else {
                return from
            }
        }
    }
}

/**
 * Applies a boolean difference operation between a [ShapeContour] and a [Shape].
 */
fun difference(from: ShapeContour, subtract: Shape): Shape {
    return if (from.closed) {
        val result = from.ring2.region().difference(subtract.region2)
        result.toShape()
    } else {
        when (subtract.topology) {
            ShapeTopology.CLOSED -> {
                val ints = subtract.contours.flatMap { intersections(from, it) }
                if (ints.isNotEmpty()) {
                    val sortedInts = ints.map { it.a.contourT }.sorted()
                    val weldedInts = (listOfNotNull(if (sortedInts.first() > 0.0) 0.0 else null) + sortedInts + (if (sortedInts.last() < 1.0) 1.0 else null)).filterNotNull().merge { a, b ->
                        abs(a - b) < 1E-6
                    }
                    val partitions = weldedInts.zipWithNext().mapNotNull {
                        val partition = from.sub(it.first, it.second)
                        if (partition.position(0.5) !in subtract) {
                            partition
                        } else {
                            null
                        }
                    }
                    Shape(partitions)
                } else {
                    if (from.position(0.0) !in subtract) from.shape else Shape.EMPTY
                }
            }
            ShapeTopology.OPEN -> {
                from.shape
            }
            ShapeTopology.MIXED -> {
                return difference(
                    from,
                    Shape(subtract.splitCompounds().filter { it.topology == ShapeTopology.OPEN }
                        .flatMap { it.contours })
                )
            }
        }
    }
}

/**
 * Applies a boolean difference operation between two [Shape]s.
 */
fun difference(from: Shape, subtract: Shape): Shape {
    if (from.empty) {
        return Shape.EMPTY
    }
    if (subtract.empty) {
        return from
    }
    return when (from.topology) {
        ShapeTopology.OPEN -> {
            when (subtract.topology) {
                ShapeTopology.OPEN -> from
                ShapeTopology.CLOSED -> {
                    Shape.compound(from.contours.map { difference(it, subtract) })
                }
                ShapeTopology.MIXED -> {
                    val closed = Shape(from.splitCompounds().filter { it.topology == ShapeTopology.CLOSED }
                        .flatMap { it.contours })
                    difference(closed, subtract)
                }
            }
        }
        ShapeTopology.CLOSED -> {
            when (subtract.topology) {
                ShapeTopology.OPEN -> from
                ShapeTopology.CLOSED -> {
                    val result = from.region2.difference(subtract.region2)
                    result.toShape()
                }
                ShapeTopology.MIXED -> {
                    val closed = Shape(from.splitCompounds().filter { it.topology == ShapeTopology.CLOSED }
                        .flatMap { it.contours })
                    difference(closed, subtract)
                }
            }
        }
        ShapeTopology.MIXED -> {
            val closed =
                Shape(from.splitCompounds().filter { it.topology == ShapeTopology.CLOSED }.flatMap { it.contours })
            val open = from.openContours
            Shape.compound(listOf(difference(closed, subtract)) + open.map { difference(it, subtract) })
        }
    }
}

/**
 * Applies a boolean difference operation between a [List] of [Shape]s and a [ShapeContour].
 */
fun difference(from: List<Shape>, subtract: ShapeContour): List<Shape> {
    return from.toRegion2().difference(subtract.ring2.region()).toShapes()
}

/**
 * Applies a boolean difference operation between a [List] of [Shape]s and a [Shape].
 */
fun difference(from: List<Shape>, subtract: Shape): List<Shape> {
    return from.toRegion2().difference(subtract.region2).toShapes()
}

/**
 * Applies a boolean difference operation between two [List]s of [Shape]s.
 */
fun difference(from: List<Shape>, subtract: List<Shape>): List<Shape> {
    return from.toRegion2().difference(subtract.toRegion2()).toShapes()
}

/**
 * Applies a boolean difference operation iteratively between a [List] of [Shape]s and a two-dimensional [List] of [Shape]s.
 *
 * [subtract] is traversed and a boolean [union] is applied between [from] and each element.
 */
@JvmName("differenceIterative")
fun difference(from: List<Shape>, subtract: List<List<Shape>>): List<Shape> {
    var left = from
    for (subtractShapes in subtract) {
        left = difference(left, subtractShapes)
    }
    return left
}

/**
 * Applies a boolean org.openrndr.shape.union operation between two [ShapeContour]s.
 */
fun union(from: ShapeContour, add: ShapeContour): Shape {
    if (from === ShapeContour.EMPTY && add == ShapeContour.EMPTY) {
        return Shape.EMPTY
    }
    if (from === ShapeContour.EMPTY) {
        return add.shape
    }
    if (add === ShapeContour.EMPTY) {
        return from.shape
    }

    return if (from.closed) {
        val result = from.ring2.region().union(add.ring2.region())
        result.toShape()
    } else {
        from.shape
    }
}

/**
 * Applies a boolean org.openrndr.shape.union operation between a [Shape] and a [ShapeContour].
 */
fun union(from: Shape, add: ShapeContour): Shape {
    if (from === Shape.EMPTY && add === ShapeContour.EMPTY) {
        return Shape.EMPTY
    }
    if (from === Shape.EMPTY) {
        return add.shape
    }
    if (add === ShapeContour.EMPTY) {
        return from
    }
    val result = from.region2.union(add.ring2.region())
    return result.toShape()
}

/**
 * Applies a boolean org.openrndr.shape.union operation between two [Shape]s.
 */
fun union(from: Shape, add: Shape): Shape {
    return if (from.topology == ShapeTopology.CLOSED) {
        val result = from.region2.union(add.region2)
        result.toShape()
    } else {
        from
    }
}

/**
 * Applies a boolean org.openrndr.shape.union operation between a [List] of [Shape]s and a [ShapeContour].
 */
fun union(from: List<Shape>, add: ShapeContour): List<Shape> {
    return from.toRegion2().union(add.ring2.region()).toShapes()
}

/**
 * Applies a boolean org.openrndr.shape.union operation between a [List] of [Shape]s and a [Shape].
 */
fun union(from: List<Shape>, add: Shape): List<Shape> {
    return from.toRegion2().union(add.region2).toShapes()
}

/**
 * Applies a boolean org.openrndr.shape.union operation between two [List]s of [Shape]s.
 */
fun union(from: List<Shape>, add: List<Shape>): List<Shape> {
    return from.toRegion2().union(add.toRegion2()).toShapes()
}

/**
 * Applies a boolean org.openrndr.shape.union operation iteratively between a [List] of [Shape]s and a two-dimensional [List] of [Shape]s.
 *
 * [add] is traversed and a boolean [union] is applied between [from] and each element.
 */
@JvmName("unionIterative")
fun union(from: List<Shape>, add: List<List<Shape>>): List<Shape> {
    var left = from
    for (addShapes in add) {
        left = union(left, addShapes)
    }
    return left
}

fun List<Double>.merge(f: (Double, Double) -> Boolean): List<Double> {
    val result = mutableListOf<Double>()
    result.add(this[0])
    var last = this[0]
    for (i in 1 until size) {
        if (!f(last, this[i])) {
            result.add(this[i])
            last = this[i]
        }
    }
    return result
}

/**
 * Applies a boolean intersection operation between two [ShapeContour]s.
 */
fun intersection(from: ShapeContour, with: ShapeContour): Shape {
    if (from.empty || with.empty)
        return Shape.EMPTY

    return if (from.closed) {
        val result = from.ring2.region().intersection(with.ring2.region())
        result.toShape()
    } else {
        return if (with.closed) {
            val ints = intersections(from, with)
            return if (ints.isNotEmpty()) {
                val sortedInts = ints.map { it.a.contourT }.sorted()
                val weldedInts = (listOf(if (sortedInts.first() > 0.0) 0.0 else null) + sortedInts + (if (sortedInts.last() < 1.0) 1.0 else null)).filterNotNull().merge { a, b ->
                    abs(a - b) < 1E-6
                }
                val partitions = weldedInts.zipWithNext().mapNotNull {
                    val partition = from.sub(it.first, it.second)
                    if (partition.position(0.5) in with) {
                        partition
                    } else {
                        null
                    }
                }
                Shape(partitions)
            } else {
                if (from.position(0.0) in with) from.shape else Shape.EMPTY
            }
        } else {
            from.shape
        }
    }
}

/**
 * Applies a boolean intersection operation between a [Shape] and a [ShapeContour].
 */
fun intersection(from: Shape, with: ShapeContour): Shape {
    if (from.empty || with.empty) {
        return Shape.EMPTY
    }

    return when (from.topology) {
        ShapeTopology.CLOSED -> {
            if (with.closed) {
                val result = from.region2.intersection(with.ring2.region())
                result.toShape()
            } else {
                return from
            }
        }
        ShapeTopology.OPEN -> {
            if (with.closed) {
                Shape.compound(from.contours.map {
                    intersection(it, with)
                })
            } else {
                return from
            }
        }
        ShapeTopology.MIXED -> {
            if (with.closed) {
                Shape.compound(from.splitCompounds().map {
                    intersection(it, with)
                })
            } else {
                return from
            }
        }
    }
}

/**
 * Applies a boolean intersection operation between a [ShapeContour] and [Shape].
 */
fun intersection(from: ShapeContour, with: Shape): Shape {
    if (from.empty || with.empty) {
        return Shape.EMPTY
    }

    return if (from.closed) {
        val result = from.ring2.region().intersection(with.region2)
        result.toShape()
    } else {
        when (with.topology) {
            ShapeTopology.CLOSED -> {
                val ints = with.contours.flatMap { intersections(from, it) }
                if (ints.isNotEmpty()) {
                    val sortedInts = ints.map { it.a.contourT }.sorted()
                    val weldedInts = (listOf(if (sortedInts.first() > 0.0) 0.0 else null) + sortedInts + (if (sortedInts.last() < 1.0) 1.0 else null)).filterNotNull().merge { a, b ->
                        abs(a - b) < 1E-6
                    }
                    val partitions = weldedInts.zipWithNext().mapNotNull {
                        val partition = from.sub(it.first, it.second)
                        if (partition.position(0.5) in with) {
                            partition
                        } else {
                            null
                        }
                    }
                    Shape(partitions)

                } else {
                    if (from.position(0.0) in with) from.shape else Shape.EMPTY
                }
            }
            ShapeTopology.OPEN -> {
                from.shape
            }
            ShapeTopology.MIXED -> {
                return intersection(
                    from,
                    Shape(with.splitCompounds().filter { it.topology == ShapeTopology.OPEN }.flatMap { it.contours })
                )
            }
        }
    }
}

/**
 * Applies a boolean intersection operation between two [Shape]s.
 */
fun intersection(from: Shape, with: Shape): Shape {
    return when (from.topology) {
        ShapeTopology.OPEN -> {
            when (with.topology) {
                ShapeTopology.OPEN -> from
                ShapeTopology.CLOSED -> {
                    Shape.compound(from.contours.map { intersection(it, with) })
                }
                ShapeTopology.MIXED -> {
                    val closed = Shape(from.splitCompounds().filter { it.topology == ShapeTopology.CLOSED }
                        .flatMap { it.contours })
                    intersection(closed, with)
                }
            }
        }
        ShapeTopology.CLOSED -> {
            when (with.topology) {
                ShapeTopology.OPEN -> from
                ShapeTopology.CLOSED -> {
                    val result = from.region2.intersection(with.region2)
                    result.toShape()
                }
                ShapeTopology.MIXED -> {
                    val closed = Shape(from.splitCompounds().filter { it.topology == ShapeTopology.CLOSED }
                        .flatMap { it.contours })
                    intersection(closed, with)
                }
            }
        }
        ShapeTopology.MIXED -> {
            val closed =
                Shape(from.splitCompounds().filter { it.topology == ShapeTopology.CLOSED }.flatMap { it.contours })
            val open = from.openContours
            Shape.compound(listOf(intersection(closed, with)) + open.map { intersection(it, with) })
        }
    }
}

/**
 * Applies a boolean intersection operation between a [List] of [Shape]s and a [ShapeContour].
 */
fun intersection(from: List<Shape>, with: ShapeContour): List<Shape> {
    return from.toRegion2().intersection(with.ring2.region()).toShapes()
}

/**
 * Applies a boolean intersection operation between a [List] of [Shape]s and a [Shape].
 */
fun intersection(from: List<Shape>, with: Shape): List<Shape> {
    return from.toRegion2().intersection(with.region2).toShapes()
}

/**
 * Applies a boolean intersection operation between two [List]s of [Shape]s.
 */
fun intersection(from: List<Shape>, with: List<Shape>): List<Shape> {
    return from.toRegion2().intersection(with.toRegion2()).toShapes()
}

/**
 * Applies a boolean intersection operation iteratively between a [List] of [Shape]s and a two-dimensional [List] of [Shape]s.
 *
 * [with] is traversed and a boolean [intersection] is applied between [from] and each element.
 */
@JvmName("intersectionIterative")
fun intersection(from: List<Shape>, with: List<List<Shape>>): List<Shape> {
    var left = from
    for (withShapes in with) {
        left = intersection(left, withShapes)
    }
    return left
}

class SegmentIntersection(val a: SegmentPoint, val b: SegmentPoint, val position: Vector2)

/** Calculates a [List] of all points where two [Segment]s intersect. */
fun intersections(a: Segment, b: Segment, vertexThreshold: Double = 1E-5): List<SegmentIntersection> {

    if ((a.linear && a.length == 0.0) || (b.linear && b.length == 0.0)) {
        return emptyList()
    }


    // Test if checking against self. This test should be improved such that it is not based on object identity
    val selfTest = a === b
    val ca = a.toCurve2()
    val cb = b.toCurve2()

    return if (!selfTest) {
        Intersections.intersections(ca, cb).map {
            val at = when {
                it.x < vertexThreshold -> 0.0
                it.x >= 1.0 - vertexThreshold -> 1.0
                else -> it.x
            }
            val bt = when {
                it.y < vertexThreshold -> 0.0
                it.y >= 1.0 - vertexThreshold -> 1.0
                else -> it.y
            }
            val pointA = SegmentPoint(a, at, ca.position(it.x).toVector2())
            val pointB = SegmentPoint(b, bt, pointA.position)
            SegmentIntersection(pointA, pointB, pointA.position)
        }
    } else {
        // Here we should handle self-intersections properly
        emptyList()
    }
}

data class ContourIntersection(val a: ContourPoint, val b: ContourPoint, val position: Vector2)

/**
 * Calculates a [List] of all points of where paths intersect between two [ShapeContour]s.
 */
fun intersections(a: ShapeContour, b: ShapeContour, vertexThreshold: Double = 1E-5): List<ContourIntersection> {
    val selfTest = a === b
    val result = mutableListOf<ContourIntersection>()

    if (a.empty || b.empty) {
        return emptyList()
    }

    val lastA = a.segments.lastIndex
    val lastB = b.segments.lastIndex
    // this is where we should use a sweepline approach
    for ((ia, sa) in a.segments.withIndex()) {
        for ((ib, sb) in b.segments.withIndex()) {
            if (selfTest && ib > ia) {
                continue
            }
            val segmentIntersections = intersections(sa, sb, vertexThreshold).let {
                if (selfTest) {
                    it.filterNot { intersection -> intersection.a.segmentT == 1.0 && intersection.b.segmentT == 0.0 || intersection.a.segmentT == 0.0 && intersection.b.segmentT == 1.0 }
                } else {
                    it
                }
            }
            result.addAll(segmentIntersections.map {
                val at = if (it.a.segmentT == 1.0 && ia != lastA) 0.0 else it.a.segmentT
                val ai = if (it.a.segmentT == 1.0 && ia != lastA) ia + 1 else ia
                val bt = if (it.b.segmentT == 1.0 && ib != lastB) 0.0 else it.b.segmentT
                val bi = if (it.b.segmentT == 1.0 && ib != lastB) ib + 1 else ib

                ContourIntersection(
                    ContourPoint(a, (ai + at) / a.segments.size, a.segments[ai], at, it.position),
                    ContourPoint(b, (bi + bt) / b.segments.size, b.segments[bi], bt, it.position),
                    it.position
                )
            })
        }
    }


    return result.let {
        if (selfTest) {
            it.distinctBy { intersection -> Pair(intersection.a.contourT.toString().take(7), intersection.b.contourT.toString().take(7)) }
        } else {
            it
        }
    }
}

/**
 * Calculates a [List] of all points of where paths intersect between the two [Shape]s.
 */
fun intersections(a: Shape, b: Shape): List<ContourIntersection> {
    return a.contours.flatMap { ac ->
        b.contours.flatMap { bc ->
            intersections(ac, bc)
        }
    }
}


/**
 * Splits a [Shape] into two separate [Shape]s from given [LineSegment].
 *
 * [LineSegment] doesn't necessarily need to cover the full length of the [Shape],
 * as it will be extended on both ends to ensure it splits the whole [Shape].
 *
 * @return A pair containing two partial [Shape]s.
 */
fun split(shape: Shape, cutter: LineSegment): Pair<Shape, Shape> {
    val center = (cutter.end + cutter.start) / 2.0
    val direction = (cutter.end - cutter.start).normalized
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
    val leftShape = difference(shape, leftContour)
    val rightShape = difference(shape, rightContour)
    return Pair(leftShape, rightShape)
}

/**
 * Splits a [ShapeContour] with another [ShapeContour].
 *
 * If there is no intersection, the original contour is returned.
 */
fun split(from: ShapeContour, cutter: ShapeContour): List<ShapeContour> {
    if (from.empty) return listOf()
    if (cutter.empty) return listOf(from)

    val ints = intersections(from, cutter)
    return performSplit(from, ints)
}

/**
 * Splits a [ShapeContour] with all other [ShapeContour] in a [List].
 */
fun split(from: ShapeContour, cutters: List<ShapeContour>): List<ShapeContour> {
    if (from.empty) return listOf()

    // Do `it != from` because we may want to split all org.openrndr.shape.contours
    // in a collection at their intersections.
    // We would iterate over each element and cut against
    // "all other items". Building a collection with "all other items"
    // for each item is tedious. Easier to just discard if equal.
    val validCutters = cutters.filter { !it.empty && it != from }
    if (validCutters.isEmpty()) return listOf(from)

    val ints = validCutters.map { cutter ->
        intersections(from, cutter)
    }.flatten()

    return performSplit(from, ints)
}

/**
 * Performs the actual ShapeContour cutting. Receive the shape to be cut
 * and a list of all the cut points.
 */
private fun performSplit(from: ShapeContour, ints: List<ContourIntersection>):
        List<ShapeContour> {
    return if (ints.isNotEmpty()) {
        val sortedInts = ints.map { it.a.contourT }.sorted()
        val weldedInts = (if (from.closed) {
            sortedInts + (if (sortedInts.first() > 0.0) 1 +
                    sortedInts.first() else null)
        } else {
            listOf(if (sortedInts.first() > 0.0) 0.0 else null) +
                    sortedInts + (if (sortedInts.last() < 1.0) 1.0 else null)
        }).filterNotNull().merge { a, b -> abs(a - b) < 1E-6 }
        weldedInts.zipWithNext().map { from.sub(it.first, it.second) }
    } else {
        listOf(from)
    }
}