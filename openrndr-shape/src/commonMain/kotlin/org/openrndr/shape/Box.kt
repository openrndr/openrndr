package org.openrndr.shape

import org.openrndr.math.LinearType
import org.openrndr.math.Vector3
import kotlin.jvm.JvmName
import kotlin.jvm.JvmRecord
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * A 3D Box defined by an anchor point ([corner]), [width], [height] and [depth].
 */
@JvmRecord
data class Box(val corner: Vector3, val width: Double, val height: Double, val depth: Double) : LinearType<Box> {

    val xRange
        get() = min(corner.x, corner.x + width)..<max(corner.x, corner.x + width)

    val yRange
        get() = min(corner.y, corner.y + height)..<max(corner.y, corner.y + height)

    val zRange
        get() = min(corner.z, corner.z + depth)..<max(corner.z, corner.z + depth)

    override fun plus(right: Box): Box =
        Box(corner + right.corner, width + right.width, height + right.height, depth + right.depth)

    override fun minus(right: Box): Box =
        Box(corner - right.corner, width - right.width, height - right.height, depth - right.depth)

    override fun times(scale: Double): Box = Box(corner * scale, width * scale, height * scale, depth * scale)

    override fun div(scale: Double): Box = Box(corner / scale, width / scale, height / scale, depth / scale)

    val volume
        get() = width * height * depth

    val center
        get() = corner + Vector3(width / 2.0, height / 2.0, depth / 2.0)

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

    fun position(u: Double, v: Double, w: Double): Vector3 {
        return corner + Vector3(u * width, v * height, w * depth)
    }

    fun sub(
        u: ClosedFloatingPointRange<Double>,
        v: ClosedFloatingPointRange<Double>,
        w: ClosedFloatingPointRange<Double>
    ): Box {
        return sub(u.start, v.start, w.start, u.endInclusive, v.endInclusive, w.endInclusive)
    }

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

    fun toInt(): IntBox {
        return IntBox(corner.toInt(), width.toInt(), height.toInt(), depth.toInt())
    }
}

fun Box(x: Double, y: Double, z: Double, width: Double, height: Double = width, depth: Double = width) =
    Box(Vector3(x, y, z), width, height, depth)

/**
 * Calculate [Box]-bounds from a [List] of [Vector3] instances.
 *
 * The provided list should consist of more than one item for optimal results.
 */
val List<Vector3>.bounds: Box
    @JvmName("getVector2Bounds") get() {
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