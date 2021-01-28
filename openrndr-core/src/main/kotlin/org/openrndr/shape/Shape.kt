@file:Suppress("unused", "MemberVisibilityCanPrivate", "MemberVisibilityCanBePrivate")

package org.openrndr.shape

import io.lacuna.artifex.Vec2
import org.openrndr.math.*
import kotlin.random.Random

class Shape(val contours: List<ShapeContour>) {
    companion object {
        /**
         * an empty shape object, advised to use this instance whenever an empty shape is needed
         */
        val EMPTY = Shape(emptyList())
        fun compound(shapes: List<Shape>) = Shape(shapes.flatMap { it.contours })
    }

    /**
     * bounding box [Rectangle]
     */
    val bounds by lazy {
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

    /**
     * indication of shape topology
     */
    val topology = when {
        contours.isEmpty() -> ShapeTopology.OPEN
        contours.all { it.closed } -> ShapeTopology.CLOSED
        contours.all { !it.closed } -> ShapeTopology.OPEN
        else -> ShapeTopology.MIXED
    }

    /**
     * list the open contours
     */
    val openContours: List<ShapeContour> =
            when (topology) {
                ShapeTopology.OPEN -> contours
                ShapeTopology.CLOSED -> emptyList()
                ShapeTopology.MIXED -> contours.filter { !it.closed }
            }

    /**
     * list the closed contours
     */
    val closedContours: List<ShapeContour> =
            when (topology) {
                ShapeTopology.OPEN -> emptyList()
                ShapeTopology.CLOSED -> contours
                ShapeTopology.MIXED -> contours.filter { it.closed }
            }

    val empty get() = this === EMPTY || contours.isEmpty()

    /**
     * indicates that the shape has only contours for which each segment is a line segment
     */
    val linear get() = contours.all { it.segments.all { segment -> segment.linear } }
    fun polygon(distanceTolerance: Double = 0.5) =
            if (empty) {
                EMPTY
            } else {
                Shape(contours.map { it.sampleLinear(distanceTolerance) })
            }

    /**
     * calculate triangulation for this shape
     */
    val triangulation by lazy {
        triangulate(this).windowed(3, 3).map {
            Triangle(it[0], it[1], it[2])
        }
    }

    /**
     * calculate (approximate) area for this shape (through triangulation)
     */
    val area by lazy {
        triangulation.sumByDouble { it.area }
    }

    /**
     * generate random points that lie inside the shape
     * @param pointCount the number of points to generate
     * @param random the [Random] number generator to use, default is [Random.Default]
     */
    fun randomPoints(pointCount: Int, random: Random = Random.Default): List<Vector2> {
        val randomValues = List(pointCount) { random.nextDouble() * area }.sortedDescending().toMutableList()
        var sum = 0.0
        val result = mutableListOf<Vector2>()
        for (triangle in triangulation) {
            sum += triangle.area
            if (randomValues.isEmpty()) {
                break
            }
            while (sum > randomValues.last()) {
                result.add(triangle.randomPoint())
                randomValues.removeLastOrNull()
                if (randomValues.isEmpty()) {
                    break
                }
            }
        }
        return result
    }

    operator fun contains(v: Vector2): Boolean {
        if (empty) {
            return false
        }
        return toRegion2().contains(Vec2(v.x, v.y))
    }

    /**
     * The outline of the shape
     */
    val outline get() = contours[0]

    /**
     * The indexed hole of the shape
     * @param index
     */
    fun hole(index: Int) = contours[index + 1]

    /**
     * Apply a transform to the shape
     * @param transform a Matrix44 that represents the transform
     * @return a transformed shape instance
     */
    fun transform(transform: Matrix44) = when {
        empty -> EMPTY
        transform === Matrix44.IDENTITY -> this
        else -> Shape(contours.map { it.transform(transform) })
    }

    /**
     * Apply a map to the shape. Maps every contour.
     */
    fun map(mapper: (ShapeContour) -> ShapeContour) = Shape(contours.map { mapper(it) })

    val compound: Boolean
        get() {
            return if (contours.isEmpty()) {
                false
            } else {
                contours.count { it.winding == Winding.CLOCKWISE } > 1
            }
        }

    /**
     * Splits a compound shape into separate shapes.
     */
    fun splitCompounds(winding: Winding = Winding.CLOCKWISE): List<Shape> {
        return if (contours.isEmpty()) {
            emptyList()
        } else {
            val (cw, ccw) = closedContours.partition { it.winding == winding }
            val candidates = cw.map { outer ->
                val cs = ccw.filter { intersects(it.bounds, outer.bounds) }
                listOf(outer) + cs
            }
            (candidates + openContours.map { listOf(it) }).map { Shape(it) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Shape

        if (contours != other.contours) return false

        return true
    }

    override fun hashCode(): Int {
        return contours.hashCode()
    }

    fun union(other: Shape): Shape = union(this, other)
    fun difference(other: Shape): Shape = difference(this, other)
    fun intersection(other: Shape): Shape = intersection(this, other)

    fun intersections(other: Shape) = intersections(this, other)
    fun intersections(other: ShapeContour) = intersections(this, other.shape)
    fun intersections(other: Segment) = intersections(this, other.contour.shape)
    override fun toString(): String {
        return "Shape(contours=$contours, topology=$topology)"
    }


}

/**
 * convert a list of [Shape] items into a single [Shape] compound
 */
val List<Shape>.compound
    get() = Shape.compound(this)

