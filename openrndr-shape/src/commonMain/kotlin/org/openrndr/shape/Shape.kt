@file:Suppress("unused", "MemberVisibilityCanPrivate", "MemberVisibilityCanBePrivate")

package org.openrndr.shape

import org.openrndr.kartifex.Vec2
import org.openrndr.math.*
import org.openrndr.utils.resettableLazy

/**
 * A simple interface for managing a [List] of [ShapeContour].
 */
class Shape(val contours: List<ShapeContour>) : ShapeProvider {
    companion object {
        /**
         * An empty [Shape] object.
         *
         * It is advised to use this instance whenever an empty shape is needed.
         */
        val EMPTY = Shape(emptyList())

        /** Creates a [Shape] from combining a [List] of Shapes */
        fun compound(shapes: List<Shape>) = Shape(shapes.flatMap { it.contours })
    }

    /** Returns [Shape] bounding box. */
    private val boundsDelegate = resettableLazy {
        if (empty) {
            Rectangle(0.0, 0.0, 0.0, 0.0)
        } else {
            contours.mapNotNull {
                if (it.empty) {
                    null
                } else {
                    it.bounds
                }
            }.bounds
        }
    }
    val bounds by boundsDelegate

    /** Indicates the [Shape] topology. */
    val topology = when {
        contours.isEmpty() -> ShapeTopology.OPEN
        contours.all { it.closed } -> ShapeTopology.CLOSED
        contours.all { !it.closed } -> ShapeTopology.OPEN
        else -> ShapeTopology.MIXED
    }

    /** Lists all [ShapeContour]s with an [open topology][ShapeTopology.OPEN]. */
    val openContours: List<ShapeContour> =
            when (topology) {
                ShapeTopology.OPEN -> contours
                ShapeTopology.CLOSED -> emptyList()
                ShapeTopology.MIXED -> contours.filter { !it.closed }
            }

    /** Lists all [ShapeContour]s with a [closed topology][ShapeTopology.CLOSED]. */
    val closedContours: List<ShapeContour> =
            when (topology) {
                ShapeTopology.OPEN -> emptyList()
                ShapeTopology.CLOSED -> contours
                ShapeTopology.MIXED -> contours.filter { it.closed }
            }

    /** Returns true if [Shape] contains no [ShapeContour]s. */
    val empty get() = this === EMPTY || contours.isEmpty()


    /**
     * Projects a point on the contours of a [Shape]
     * @param point The point to project.
     * @return a projected point that lies on the [ShapeContour].
     */
    fun nearest(point: Vector2) : ContourPoint {
        require(!empty) {
            """cannot perform nearest point query on empty shape"""
        }
        return contours.map { it.nearest(point) }.minByOrNull { it.position.squaredDistanceTo(point) }
            ?: error("no nearest segment")
    }

    /**
     * Returns true if [Shape] consists solely of [ShapeContour]s,
     * where each [Segment] is a [line segment][SegmentType.LINEAR].
     */
    val linear get() = contours.all { it.segments.all { segment -> segment.linear } }
    fun polygon(distanceTolerance: Double = 0.5) =
            if (empty) {
                EMPTY
            } else {
                Shape(contours.map { it.sampleLinear(distanceTolerance) })
            }

//    private val triangulationDelegate = resettableLazy {
//        triangulate(this).windowed(3, 3).map {
//            Triangle(it[0], it[1], it[2])
//        }
//    }
//
//    /** Triangulates [Shape] into a [List] of [Triangle]s. */
//    val triangulation by triangulationDelegate

//    private val areaDelegate = resettableLazy {
//        triangulation.sumByDouble { it.area }
//    }

//    /** Calculates approximate area for this shape (through triangulation). */
//    val area by areaDelegate

//    fun resetCache() {
//        boundsDelegate.reset()
//        triangulationDelegate.reset()
//        areaDelegate.reset()
//    }
//
//    /**
//     * Generates specified amount of random points that lie inside the [Shape].
//     *
//     * @param pointCount The number of points to generate.
//     * @param random The [Random] number generator to use, defaults to [Random.Default].
//     */
//    fun randomPoints(pointCount: Int, random: Random = Random.Default): List<Vector2> {
//        val randomValues = List(pointCount) { random.nextDouble() * area }.sortedDescending().toMutableList()
//        var sum = 0.0
//        val result = mutableListOf<Vector2>()
//        for (triangle in triangulation) {
//            sum += triangle.area
//            if (randomValues.isEmpty()) {
//                break
//            }
//            while (sum > randomValues.last()) {
//                result.add(triangle.randomPoint())
//                randomValues.removeLastOrNull()
//                if (randomValues.isEmpty()) {
//                    break
//                }
//            }
//        }
//        return result
//    }
//


    /** The outline of the shape. */
    val outline get() = contours[0]

    /**
     * The indexed hole of the shape.
     * @param index
     */
    fun hole(index: Int) = contours[index + 1]

    /**
     * Applies a linear transformation to the [Shape].
     *
     * @param transform A [Matrix44] that represents the transform.
     * @return A transformed [Shape] instance
     */
    fun transform(transform: Matrix44) = when {
        empty -> EMPTY
        transform === Matrix44.IDENTITY -> this
        else -> Shape(contours.map { it.transform(transform) })
    }

    /** Applies a map to the shape. Maps every contour. */
    fun map(mapper: (ShapeContour) -> ShapeContour) = Shape(contours.map { mapper(it) })

    /**
     * Checks whether the [Shape] is org.openrndr.shape.compound or not.
     *
     * Returns true when there's multiple [ShapeContour]s with a [clockwise winding][Winding.CLOCKWISE]
     * because the function only counts non-hole ShapeContours (which are clockwise).
     */
    val compound: Boolean
        get() {
            return if (contours.isEmpty()) {
                false
            } else {
                contours.count { it.winding == Winding.CLOCKWISE } > 1
            }
        }

    /** Splits a org.openrndr.shape.compound shape into separate shapes. */
    fun splitCompounds(winding: Winding = Winding.CLOCKWISE): List<Shape> {
        return if (contours.isEmpty()) {
            emptyList()
        } else {
            val (cw, ccw) = closedContours.partition { it.winding == winding }
            val candidates = cw.map { outer ->
                val cs = ccw.filter { it.bounds.intersects(outer.bounds) }
                listOf(outer) + cs
            }
            (candidates + openContours.map { listOf(it) }).map { Shape(it) }
        }
    }

    override val shape: Shape
        get() = this

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shape) return false

        if (contours != other.contours) return false

        return true
    }

    override fun hashCode(): Int {
        return contours.hashCode()
    }

    override fun toString(): String {
        return "Shape(org.openrndr.shape.contours=$contours, topology=$topology)"
    }

    /** Applies a boolean org.openrndr.shape.union operation between two [Shape]s. */
    fun union(other: Shape): Shape = union(this, other)

    /** Applies a boolean difference operation between two [Shape]s. */
    fun difference(other: Shape): Shape = difference(this, other)

    /** Applies a boolean intersection operation between two [Shape]s. */
    fun intersection(other: Shape): Shape = intersection(this, other)

    /** Calculates a [List] of all points where two [Shape]s intersect. */
    fun intersections(other: Shape) = intersections(this, other)

    /** Calculates a [List] of all points where the [Shape] and a [ShapeContour] intersect. */
    fun intersections(other: ShapeContour) = intersections(this, other.shape)

    /** Calculates a [List] of all points where the [Shape] and a [Segment] intersect. */
    fun intersections(other: Segment) = intersections(this, other.contour.shape)

    /** Returns true if given [Vector2] is inside the [Shape]. */
    operator fun Shape.contains(v: Vector2): Boolean {
        if (empty) {
            return false
        }
        return toRegion2().contains(Vec2(v.x, v.y))
    }
}

/** Converts a [List] of [Shape] items into a single org.openrndr.shape.compound [Shape]. */
val List<Shape>.compound
    get() = Shape.compound(this)
