@file:Suppress("unused", "MemberVisibilityCanPrivate", "MemberVisibilityCanBePrivate")

package org.openrndr.shape

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.openrndr.kartifex.Region2
import org.openrndr.math.GeometricPrimitive2D
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.utils.resettableLazy
import kotlin.random.Random

@Serializable
/**
 * A simple interface for managing a [List] of [ShapeContour].
 */
class Shape(val contours: List<ShapeContour>) : ShapeProvider, GeometricPrimitive2D {
    companion object {
        /**
         * An empty [Shape] object.
         *
         * It is advised to use this instance whenever an empty shape is needed.
         */
        val EMPTY: Shape = Shape(emptyList())

        /** Creates a [Shape] from combining a [List] of Shapes */
        fun compound(shapes: List<Shape>) = Shape(shapes.flatMap { it.contours })
    }

    /** Returns [Shape] bounding box. */
    @Transient
    private val boundsDelegate = resettableLazy {
        if (empty) {
            Rectangle(0.0, 0.0, 0.0, 0.0)
        } else {
            val result: List<Rectangle> = contours.mapNotNull {
                if (it.empty) {
                    null
                } else {
                    it.bounds
                }
            }
            result.bounds
        }
    }

    val bounds by boundsDelegate

    /** Indicates the [Shape] topology. */
    @Transient
    val topology = when {
        contours.isEmpty() -> ShapeTopology.OPEN
        contours.all { it.closed } -> ShapeTopology.CLOSED
        contours.all { !it.closed } -> ShapeTopology.OPEN
        else -> ShapeTopology.MIXED
    }

    @Transient
            /** Lists all [ShapeContour]s with an [open topology][ShapeTopology.OPEN]. */
    val openContours: List<ShapeContour> =
        when (topology) {
            ShapeTopology.OPEN -> contours
            ShapeTopology.CLOSED -> emptyList()
            ShapeTopology.MIXED -> contours.filter { !it.closed }
        }


    /** Lists all [ShapeContour]s with a [closed topology][ShapeTopology.CLOSED]. */
    @Transient
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
    fun nearest(point: Vector2): ContourPoint {
        require(!empty) {
            """cannot perform nearest point query on empty shape"""
        }
        return contours.map { it.nearest(point) }.minByOrNull { it.position.squaredDistanceTo(point) }
            ?: error("no nearest segment")
    }

    /**
     * Returns true if [Shape] consists solely of [ShapeContour]s,
     * where each [Segment2D] is a [line segment][SegmentType.LINEAR].
     */
    val linear get() = contours.all { it.segments.all { segment -> segment.linear } }
    fun polygon(distanceTolerance: Double = 0.5) =
        if (empty) {
            EMPTY
        } else {
            Shape(contours.map { it.sampleLinear(distanceTolerance) })
        }

    @Transient
    private val triangulationDelegate = resettableLazy {
        triangulate(this).windowed(3, 3).map {
            Triangle(it[0], it[1], it[2])
        }
    }


    /** Triangulates [Shape] into a [List] of [Triangle]s. */
    val triangulation by triangulationDelegate

    @Transient
    private val areaDelegate = resettableLazy {
        val closed = closedContours.filter { !it.empty }
        when {
            topology == ShapeTopology.OPEN || closed.isEmpty() -> 0.0
            !isSelfIntersecting() ->
                nestedArea(closed, if (topology == ShapeTopology.CLOSED) FillRule.NON_ZERO else FillRule.EVEN_ODD)
            // intersecting contours are resolved into the contours of the region they fill first
            topology == ShapeTopology.CLOSED -> resolvedArea()
            else -> triangulation.sumOf { it.area }
        }
    }

    /**
     * The area of a closed shape with intersecting contours, exact from the contours of the region they fill, or
     * through triangulation when that region cannot be found
     */
    private fun resolvedArea(): Double {
        val resolved = try {
            removeSelfIntersections(FillRule.NON_ZERO)
        } catch (e: Throwable) {
            null
        }
        val contours = resolved?.closedContours?.filter { !it.empty }
        return when {
            resolved == null || contours == null -> triangulation.sumOf { it.area }
            contours.isEmpty() -> 0.0
            resolved.isSelfIntersecting() -> triangulation.sumOf { it.area }
            // the resolved contours bound the filled region, which every second contour enters
            else -> nestedArea(contours, FillRule.EVEN_ODD)
        }
    }

    /**
     * Calculates the area of this shape, of the regions its closed contours fill by the [FillRule.NON_ZERO] rule, or
     * by [FillRule.EVEN_ODD] when it also has open contours, the rules its [triangulation] uses. The area is exact,
     * only a shape with open contours whose closed contours intersect is measured through triangulation.
     */
    val area by areaDelegate

    @Transient
    private val region2Delegate = resettableLazy {
        Region2(contours.map { it.ring2 })
    }

    internal val region2 by region2Delegate

    @Transient
    private val path2Delegate = resettableLazy {
        contours.map { it.path2 }
    }

    internal val path2 by path2Delegate

    fun resetCache() {
        boundsDelegate.reset()
        triangulationDelegate.reset()
        areaDelegate.reset()
        region2Delegate.reset()
        path2Delegate.reset()
        contours.forEach { it.resetCache() }
    }



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

    /** Splits an org.openrndr.shape.compound shape into separate shapes. */
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
}

/** Converts a [List] of [Shape] items into a single org.openrndr.shape.compound [Shape]. */
@Transient
val List<Shape>.compound
    get() = Shape.compound(this)

/**
 * The area filled by [contours], which do not intersect, by the [fillRule]. A contour lies inside another when it is
 * smaller and its start lies inside it, the face of a contour is the region inside it and outside the contours it
 * directly holds. A face is filled by the winding numbers of the contours around it.
 */
private fun nestedArea(contours: List<ShapeContour>, fillRule: FillRule): Double {
    val areas = contours.map { it.signedArea }
    // from large to small, so the contours around a contour come before it
    val order = contours.indices.sortedByDescending { kotlin.math.abs(areas[it]) }
    val parent = IntArray(contours.size) { -1 }
    val winding = IntArray(contours.size)
    val depth = IntArray(contours.size)
    val faceArea = DoubleArray(contours.size)
    for ((k, j) in order.withIndex()) {
        val start = contours[j].segments[0].start
        // the smallest contour around contour j
        for (m in k - 1 downTo 0) {
            val i = order[m]
            if (kotlin.math.abs(areas[i]) > kotlin.math.abs(areas[j]) && start in contours[i]) {
                parent[j] = i
                break
            }
        }
        val sign = if (areas[j] > 0.0) 1 else if (areas[j] < 0.0) -1 else 0
        winding[j] = sign + (if (parent[j] >= 0) winding[parent[j]] else 0)
        depth[j] = 1 + (if (parent[j] >= 0) depth[parent[j]] else 0)
        faceArea[j] += kotlin.math.abs(areas[j])
        if (parent[j] >= 0) {
            faceArea[parent[j]] -= kotlin.math.abs(areas[j])
        }
    }
    var sum = 0.0
    for (j in contours.indices) {
        val filled = when (fillRule) {
            FillRule.NON_ZERO -> winding[j] != 0
            FillRule.EVEN_ODD -> depth[j] % 2 == 1
        }
        if (filled) sum += faceArea[j]
    }
    return sum
}
