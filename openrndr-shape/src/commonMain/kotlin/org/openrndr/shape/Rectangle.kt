@file:Suppress("unused")

package org.openrndr.shape

import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.math.clamp
import kotlin.jvm.JvmName
import kotlin.math.max
import kotlin.math.min

/**
 * Creates a new [Rectangle].
 *
 * Also see [IntRectangle].
 */
data class Rectangle(val corner: Vector2, val width: Double, val height: Double = width) : Movable, Scalable2D, ShapeProvider, ShapeContourProvider {

    constructor(x: Double, y: Double, width: Double, height: Double = width) :
            this(Vector2(x, y), width, height)

    /** The center of the [Rectangle]. */
    val center: Vector2
        get() = corner + Vector2(width / 2, height / 2)

    /** The unitless area covered by this [Rectangle]. */
    val area: Double
        get() = width * height

    /** The dimensions of the [Rectangle]. */
    val dimensions: Vector2
        get() = Vector2(width, height)

    override val scale: Vector2
        get() = dimensions

    override fun position(u: Double, v: Double): Vector2 {
        return corner + Vector2(u * width, v * height)
    }

    /** The [x]-coordinate of the top-left corner. */
    val x: Double get() = corner.x

    /** The [y]-coordinate of the top-left corner. */
    val y: Double get() = corner.y

    /** Returns [Shape] representation of the [Rectangle]. */
    override val shape get() = Shape(listOf(contour))

    /** Returns [ShapeContour] representation of the [Rectangle]. */
    override val contour: ShapeContour
        get() {
            return if (corner == Vector2.INFINITY || corner.x != corner.x || corner.y != corner.y || width != width || height != height) {
                ShapeContour.EMPTY
            } else {
                ShapeContour.fromPoints(
                    listOf(
                        corner, corner + Vector2(width, 0.0),
                        corner + Vector2(width, height),
                        corner + Vector2(0.0, height)
                    ), true, YPolarity.CW_NEGATIVE_Y
                )
            }
        }

    /**
     * Creates a new [Rectangle] with sides offset both horizontally and vertically by specified amount.
     *
     * The [Rectangle] sides are shifted outwards if [offset] values are > 0 or inwards if the values are < 0.
     */
    fun offsetEdges(offset: Double, offsetY: Double = offset): Rectangle {
        return Rectangle(Vector2(corner.x - offset, corner.y - offsetY), width + 2 * offset, height + 2 * offsetY)
    }

    /**
     * Creates a new [Rectangle] with dimensions scaled by [scale] and [scaleY].
     *
     * @param scale the x scale factor
     * @param scaleY the y scale factor, default is [scale]
     * @param anchorU x coordinate of the scaling anchor in u parameter space, default is 0.5 (center)
     * @param anchorV y coordinate of the scaling anchor in v parameter space, default is 0.5 (center)
     */
    @Deprecated("Vague naming", ReplaceWith("scaledBy(scale, scaleY)"))
    fun scale(scale: Double, scaleY: Double = scale, anchorU: Double = 0.5, anchorV: Double = 0.5): Rectangle {
        return scaledBy(scale, scaleY, anchorU, anchorV)
    }

    @Deprecated("Doesn't account for anchor placement", ReplaceWith("scaledBy(scale, scaleY)"))
    fun scaled(scale: Double, scaleY: Double = scale): Rectangle {
        return Rectangle(corner, width * scale, height * scaleY)
    }

    /** Creates a new [Rectangle] with width set to [fitWidth] and height scaled proportionally. */
    fun widthScaledTo(fitWidth: Double): Rectangle {
        val scale = fitWidth / width
        return Rectangle(corner, fitWidth, height * scale)
    }

    /** Creates a new [Rectangle] with height set to [fitHeight] and width scaled proportionally. */
    fun heightScaledTo(fitHeight: Double): Rectangle {
        val scale = fitHeight / height
        return Rectangle(corner, width * scale, fitHeight)
    }

    /** Creates a new [Rectangle] with the same size but the current position offset by [offset] amount. */
    @Deprecated("Vague naming", ReplaceWith("movedBy(offset)"))
    fun moved(offset: Vector2): Rectangle {
        return Rectangle(corner + offset, width, height)
    }

    override fun movedBy(offset: Vector2): Rectangle = Rectangle(corner + offset, width, height)

    override fun movedTo(position: Vector2): Rectangle = Rectangle(position, width, height)

    override fun scaledBy(xScale: Double, yScale: Double, uAnchor: Double, vAnchor: Double): Rectangle {
        val anchorPosition = position(uAnchor, vAnchor)
        val d = corner - anchorPosition
        val nd = anchorPosition + d * Vector2(xScale, yScale)
        return Rectangle(nd, width * xScale, height * yScale)
    }

    override fun scaledBy(scale: Double, uAnchor: Double, vAnchor: Double): Rectangle =
        scaledBy(scale, scale, uAnchor, vAnchor)

    override fun scaledTo(width: Double, height: Double): Rectangle = Rectangle(corner, width, height)

    override fun scaledTo(size: Double): Rectangle = scaledTo(size, size)

    /**
     * Returns true if given [point] is inside the [Rectangle].
     */
    operator fun contains(point: Vector2): Boolean {
        return (point.x >= corner.x &&
                point.x < corner.x + width &&
                point.y >= corner.y &&
                point.y < corner.y + height)
    }

    /**
     * Tests if the **areas** of two rectangles intersect.
     */
    fun intersects(other: Rectangle): Boolean {
        val above = y + height < other.y
        val below = y > other.y + other.height
        val rightOf = x > other.x + other.width
        val leftOf = x + width < other.x
        return !(above || below || leftOf || rightOf)
    }

    companion object {
        /** Creates a new [Rectangle] by specifying the [center] position with dimensions [width] and [height]. */
        fun fromCenter(center: Vector2, width: Double, height: Double = width) =
            Rectangle(center.x - width / 2.0, center.y - height / 2.0, width, height)

        /** A zero-length [Rectangle]. */
        val EMPTY = Rectangle(0.0, 0.0, 0.0, 0.0)
    }

    operator fun times(scale: Double) = Rectangle(corner * scale, width * scale, height * scale)

    operator fun div(scale: Double) = Rectangle(corner / scale, width / scale, height / scale)

    operator fun plus(right: Rectangle) =
        Rectangle(corner + right.corner, width + right.width, height + right.height)

    operator fun minus(right: Rectangle) =
        Rectangle(corner - right.corner, width - right.width, height - right.height)

    fun sub(u0: Double, v0: Double, u1: Double, v1: Double): Rectangle {
        val p0 = position(u0, v0)
        val p1 = position(u1, v1)
        val width = p1.x - p0.x
        val height = p1.y - p0.y
        return Rectangle(p0.x, p0.y, width, height)
    }

    /**
     * Casts to [IntRectangle].
     */
    fun toInt() = IntRectangle(x.toInt(), y.toInt(), width.toInt(), height.toInt())
}

/** calculates [Rectangle]-bounds for a list of [Vector2] instances */
@Deprecated("use List<Vector2>.bounds instead", ReplaceWith("points.bounds"))
fun vector2Bounds(points: List<Vector2>) = points.bounds

/**
 * Calculates [Rectangle]-bounds from a [List] of [Vector2] instances.
 *
 * The provided list should consist of more than one item for optimal results.
 */
val List<Vector2>.bounds: Rectangle
    @JvmName("getVector2Bounds") get() {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY

        this.forEach {
            minX = min(minX, it.x)
            maxX = max(maxX, it.x)
            minY = min(minY, it.y)
            maxY = max(maxY, it.y)
        }
        return Rectangle(Vector2(minX, minY), maxX - minX, maxY - minY)
    }

/** calculates [Rectangle]-bounds for a list of [Rectangle] instances */
@Deprecated("use List<Rectangle>.bounds instead",
    ReplaceWith("rectangles.bounds")
)
fun rectangleBounds(rectangles: List<Rectangle>): Rectangle {
    return rectangles.bounds
}

/** Calculates [Rectangle]-bounds for a list of [Rectangle] instances. */
val List<Rectangle>.bounds: Rectangle
    @JvmName("getRectangleBounds") get() {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY

        this.forEach {
            if (it != Rectangle.EMPTY) {
                minX = min(minX, it.x)
                maxX = max(maxX, it.x + it.width)
                minY = min(minY, it.y)
                maxY = max(maxY, it.y + it.height)
            }
        }
        return Rectangle(Vector2(minX, minY), maxX - minX, maxY - minY)
    }

/** Determines whether or not rectangles [a] and [b] intersect. */
@Deprecated("use Rectangle.intersects(Rectangle) instead",
    ReplaceWith("a.intersects(b)")
)
fun intersects(a: Rectangle, b: Rectangle) = a.intersects(b)

/**
 * Remaps [Vector2] from a position on the [sourceRectangle] to
 * a proportionally equivalent position on the [targetRectangle].
 *
 * @param clamp Clamps remapped value within the bounds of [targetRectangle].
 */
fun Vector2.map(sourceRectangle: Rectangle, targetRectangle: Rectangle, clamp: Boolean = false): Vector2 {
    val remapped = (this - sourceRectangle.corner) / sourceRectangle.dimensions * targetRectangle.dimensions + targetRectangle.corner
    return if (clamp) remapped.clamp(targetRectangle.corner, targetRectangle.corner + targetRectangle.dimensions) else remapped
}

