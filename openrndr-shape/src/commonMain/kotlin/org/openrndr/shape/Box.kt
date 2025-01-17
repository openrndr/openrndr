package org.openrndr.shape

import kotlinx.serialization.Serializable
import org.openrndr.math.LinearType
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import kotlin.jvm.JvmName
import kotlin.jvm.JvmRecord
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a 3D rectangular cuboid defined by its corner position, width, height, and depth.
 *
 * This class provides a number of geometric and mathematical operations, transformations,
 * and utility functions for working with 3D boxes. It implements `LinearType` for linear algebra
 * operations, as well as `Movable3D` and `Scalable3D` for 3D manipulation.
 *
 * @param corner The position of the corner of the box.
 * @param width The width of the box along the X-axis.
 * @param height The height of the box along the Y-axis.
 * @param depth The depth of the box along the Z-axis.
 */
@JvmRecord
@Serializable
data class Box(val corner: Vector3, val width: Double, val height: Double, val depth: Double) : LinearType<Box>, Movable3D, Scalable3D {

    /**
     * Represents the range along the X-axis for the box.
     *
     * The range is determined by the minimum and maximum values of the X-coordinates
     * based on the corner position and the width of the box. It is represented as
     * a half-open range, with the starting point inclusive and the end point exclusive.
     */
    val xRange
        get() = min(corner.x, corner.x + width)..<max(corner.x, corner.x + width)


    /**
     * Represents the range along the Y-axis for the box.
     *
     * The range is determined by the minimum and maximum values of the Y-coordinates
     * based on the corner position and the height of the box. It is represented as
     * a half-open range, with the starting point inclusive and the end point exclusive.
     */
    val yRange
        get() = min(corner.y, corner.y + height)..<max(corner.y, corner.y + height)


    /**
     * Represents the range along the Z-axis for the box.
     *
     * The range is determined by the minimum and maximum values of the Z-coordinates
     * based on the corner position and the depth of the box. It is represented as
     * a half-open range, with the starting point inclusive and the end point exclusive.
     */
    val zRange
        get() = min(corner.z, corner.z + depth)..<max(corner.z, corner.z + depth)


    /**
     * Determines the axis along which the box has its greatest dimension.
     *
     * The axis is selected based on the comparison of the dimensions of the box:
     * - If the width is the largest or equal to the largest dimension, the result is `Vector3.Axis.X`.
     * - If the height is the largest or equal to the largest dimension, the result is `Vector3.Axis.Y`.
     * - Otherwise, the result is `Vector3.Axis.Z`.
     *
     * This property provides a way to identify the major axis of a box for dimensional computations or direction-based operations.
     */
    val majorAxis: Vector3.Axis
        get() {
            return if (width >= height && width >= depth) {
                Vector3.Axis.X
            } else if (height >= width && height >= depth) {
                Vector3.Axis.Y
            } else {
                Vector3.Axis.Z
            }
        }

    /**
     * Represents the axis corresponding to the smallest dimension of the box.
     *
     * This property evaluates and returns the axis with the minimum size among
     * width, height, and depth. The comparison is performed as follows:
     * - If width is the smallest, the axis is `Vector3.Axis.X`.
     * - If height is the smallest, the axis is `Vector3.Axis.Y`.
     * - Otherwise, the axis is `Vector3.Axis.Z`.
     */
    val minorAxis: Vector3.Axis
        get() {
            return if (width <= height && width <= depth) {
                Vector3.Axis.X
            } else if (height <= width && height <= depth) {
                Vector3.Axis.Y
            } else {
                Vector3.Axis.Z
            }
        }

    /**
     * Provides the dimensions of the box as a [Vector3], where the x, y, and z components represent
     * the width, height, and depth of the box respectively.
     */
    val dimensions: Vector3 get() = Vector3(width, height, depth)

    fun ratio(axis: Vector3.Axis = majorAxis): Box {
        val scale = 1.0 / dimensions.dot(axis.direction)
        return fromCenter(Vector3.ZERO, width * scale, height * scale, depth * scale)
    }

    override fun plus(right: Box): Box =
        Box(corner + right.corner, width + right.width, height + right.height, depth + right.depth)

    override fun minus(right: Box): Box =
        Box(corner - right.corner, width - right.width, height - right.height, depth - right.depth)

    override fun times(scale: Double): Box = Box(corner * scale, width * scale, height * scale, depth * scale)

    override fun div(scale: Double): Box = Box(corner / scale, width / scale, height / scale, depth / scale)

    /**
     * The calculated volume of the box.
     * This value is computed as the product of the box's width, height, and depth.
     * Represents the 3D space occupied by the box in cubic units.
     */
    val volume
        get() = width * height * depth

    /**
     * Represents the center of the box.
     *
     * The center is calculated as the position derived by adding
     * half the width, height, and depth of the box to its corner position.
     */
    val center
        get() = corner + Vector3(width / 2.0, height / 2.0, depth / 2.0)

    /**
     * Returns a normalized version of the current box.
     *
     * A normalized box is defined by ensuring all dimensions (width, height, depth) are positive values.
     * If any dimension is negative, the corresponding coordinate is adjusted such that the box maintains
     * the same physical space but has non-negative dimensions. This ensures consistent representation.
     *
     * @return A new instance of the box with adjusted position and absolute values for dimensions.
     */
    val normalized: Box
        get() {
            var nx = corner.x
            var ny = corner.y
            var nz = corner.z
            if (width < 0) {
                nx += width
            }
            if (height < 0) {
                ny += height
            }
            if (depth < 0) {
                nz += depth
            }
            return Box(nx, ny, nz, width.absoluteValue, height.absoluteValue, depth.absoluteValue)
        }

    /**
     * Return a copy of the box with sides offset
     *
     * The [Box] sides are shifted outwards if [offset] values are > 0 or inwards if the values are < 0.
     */
    fun offsetSides(offset: Double, offsetY: Double = offset, offsetZ: Double = offset): Box {
        return Box(
            Vector3(corner.x - offset, corner.y - offsetY, corner.z - offsetZ),
            width + 2 * offset,
            height + 2 * offsetY,
            depth + 2 * offsetZ
        )
    }

    /**
     * Calculates the position within the box based on the given relative coordinates.
     *
     * @param u The relative position along the width of the box, ranging from 0.0 to 1.0.
     * @param v The relative position along the height of the box, ranging from 0.0 to 1.0.
     * @param w The relative position along the depth of the box, ranging from 0.0 to 1.0.
     * @return A [Vector3] representing the position within the box for the given relative coordinates.
     */
    fun position(u: Double, v: Double, w: Double): Vector3 {
        return corner + Vector3(u * width, v * height, w * depth)
    }

    /**
     * Extract a sub-box
     */
    fun sub(
        u: ClosedFloatingPointRange<Double>,
        v: ClosedFloatingPointRange<Double>,
        w: ClosedFloatingPointRange<Double>
    ): Box = sub(u.start, v.start, w.start, u.endInclusive, v.endInclusive, w.endInclusive)

    /**
     * Extract a sub-box
     */
    fun sub(u0: Double, v0: Double, w0: Double, u1: Double, v1: Double, w1: Double): Box {
        val p0 = position(u0, v0, w0)
        val p1 = position(u1, v1, w1)
        val width = p1.x - p0.x
        val height = p1.y - p0.y
        val depth = p1.z - p0.z
        return Box(p0, width, height, depth)
    }

    /**
     * Return true if given [point] is inside the [Box].
     */
    operator fun contains(point: Vector3): Boolean {
        return (point.x >= corner.x &&
                point.x < corner.x + width &&
                point.y >= corner.y &&
                point.y < corner.y + height &&
                point.z >= corner.z &&
                point.z < corner.z + depth)
    }

    /**
     * Return true if the **volumes** of two boxes intersect.
     */
    fun intersects(other: Box): Boolean {
        val above = corner.y + height < other.corner.y
        val below = corner.y > other.corner.y + other.height
        val rightOf = corner.x > other.corner.x + other.width
        val leftOf = corner.x + width < other.corner.x
        val inFrontOf = corner.z + depth < other.corner.z
        val behind = corner.z > other.corner.z + other.depth
        return !(above || below || leftOf || rightOf || inFrontOf || behind)
    }

    /**
     * Convert to [IntBox]
     */
    fun toInt(): IntBox {
        return IntBox(corner.toInt(), width.toInt(), height.toInt(), depth.toInt())
    }

    companion object {
        /** Creates a new [Box] by specifying the [center] position with dimensions [width], [height] and [depth]. */
        fun fromCenter(center: Vector3, width: Double, height: Double = width, depth: Double = width) =
            Box.fromAnchor(Vector3(0.5, 0.5, 0.5), center, width, height)

        /** Create a new [Box] by specifying the [anchorUVW], [anchor] positions with dimensions [width] and [height]. */
        fun fromAnchor(
            anchorUVW: Vector3,
            anchor: Vector3,
            width: Double,
            height: Double = width,
            depth: Double = width
        ) =
            Box(
                anchor.x - width * anchorUVW.x,
                anchor.y - height * anchorUVW.y,
                anchor.z - depth * anchorUVW.z,
                width, height, depth
            )

        /**
         * A constant representing an empty [Box] with zero width, height, and depth, and positioned at the origin.
         *
         * It can be used as a default or placeholder value when a non-populated [Box] is required.
         */
        val EMPTY = Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

    override fun movedBy(offset: Vector3): Box {
        return copy(corner = corner + offset)
    }

    override fun movedTo(position: Vector3): Box {
        return copy(corner = position)
    }

    override fun scaledBy(scale: Double, uAnchor: Double, vAnchor: Double, wAnchor: Double): Box {
        return scaledBy(scale, scale, scale, uAnchor, vAnchor, wAnchor)
    }

    override fun scaledBy(
        xScale: Double,
        yScale: Double,
        zScale: Double,
        uAnchor: Double,
        vAnchor: Double,
        wAnchor: Double
    ): Box {
        val anchorPosition = position(uAnchor, vAnchor, wAnchor)
        val d = corner - anchorPosition
        val nd = anchorPosition + d * Vector3(xScale, yScale, zScale)
        return Box(nd, width * xScale, height * yScale, depth * zScale)
    }

    fun mapTo(target: Box): Matrix44 {
        return buildTransform {
            translate(target.corner - corner)
            scale(target.dimensions / dimensions)
        }
    }
}

/**
 * Creates a Box instance using the specified position and dimensions.
 *
 * @param x The x-coordinate of the box's position.
 * @param y The y-coordinate of the box's position.
 * @param z The z-coordinate of the box's position.
 * @param width The width of the box.
 * @param height The height of the box. Defaults to the value of `width`.
 * @param depth The depth of the box. Defaults to the value of `width`.
 */
fun Box(x: Double, y: Double, z: Double, width: Double, height: Double = width, depth: Double = width) =
    Box(Vector3(x, y, z), width, height, depth)

/**
 * Calculate [Box]-bounds from a [List] of [Vector3] instances.
 *
 * The provided list should consist of more than one item for optimal results.
 */
val List<Vector3>.bounds: Box
    @JvmName("getVector3Bounds") get() {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY

        this.forEach {
            minX = min(minX, it.x)
            maxX = max(maxX, it.x)
            minY = min(minY, it.y)
            maxY = max(maxY, it.y)
            minZ = min(minZ, it.z)
            maxZ = max(maxZ, it.z)
        }
        return Box(Vector3(minX, minY, minZ), maxX - minX, maxY - minY, maxZ - minZ)
    }

/**
 * Calculate [Box]-bounds from a [List] of [Box] instances.
 *
 * The provided list should consist of more than one item for optimal results.
 */
val List<Box>.bounds: Box
    @JvmName("getBoxBounds") get() {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY

        this.forEach {
            minX = min(minX, it.corner.x)
            maxX = max(maxX, it.corner.x + it.width)
            minY = min(minY, it.corner.y)
            maxY = max(maxY, it.corner.y + it.height)
            minZ = min(minZ, it.corner.z)
            maxZ = max(maxZ, it.corner.z + it.depth)
        }
        return Box(Vector3(minX, minY, minZ), maxX - minX, maxY - minY, maxZ - minZ)
    }