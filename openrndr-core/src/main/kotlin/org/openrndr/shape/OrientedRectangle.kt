@file:Suppress("unused")

package org.openrndr.shape

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.YPolarity
import org.openrndr.math.transforms.transform

/**
 * Creates a new [OrientedRectangle].
 *
 * Also see [Rectangle] and [IntRectangle].
 * @param rotation The rotation in degrees.
 */
data class OrientedRectangle(val corner: Vector2, val width: Double, val height: Double, val rotation: Double) {

    constructor(x: Double, y: Double, width: Double, height: Double, rotation: Double)
            : this(Vector2(x, y), width, height, rotation)

    /** The center of the [OrientedRectangle]. */
    val center: Vector2
        get() = corner + Vector2(width / 2, height / 2)

    /** The unitless area covered by this [OrientedRectangle]. */
    val area: Double
        get() = width * height

    /** The dimensions of the [OrientedRectangle]. */
    val dimensions: Vector2
        get() = Vector2(width, height)

    /**
     * Returns a position for parameterized values [u] and [v] between `0.0` and `1.0`
     * where (`0.5`, `0.5`) is the center of the rectangle.
     */
    fun position(u: Double, v: Double): Vector2 {
        return corner + Vector2(u * width, v * height)
    }

    /** The [x]-coordinate of the top-left corner. */
    val x: Double get() = corner.x

    /** The [y]-coordinate of the top-left corner. */
    val y: Double get() = corner.y

    /** Returns [Shape] representation of the [OrientedRectangle]. */
    val shape get() = Shape(listOf(contour))

    /** Returns [ShapeContour] representation of the [OrientedRectangle]. */
    val contour
        get() =
            ShapeContour.fromPoints(listOf(corner, corner + Vector2(width, 0.0),
                    corner + Vector2(width, height),
                    corner + Vector2(0.0, height)), true, YPolarity.CW_NEGATIVE_Y).apply {

                val t = transform {
                    translate(center)
                    rotate(Vector3.UNIT_Z, rotation)
                    translate(-center)
                }

                transform(t)
            }

    /**
     * Creates a new [Rectangle] with dimensions scaled by [scale] and [scaleY].
     *
     * @param scale the x scale factor.
     * @param scaleY the y scale factor, default is [scale].
     */
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

    /** Creates a new [Rectangle] with the current position offset by [offset]. */
    fun moved(offset: Vector2): Rectangle {
        return Rectangle(corner + offset, width, height)
    }

    companion object {
        /** Creates a new [Rectangle] by specifying the [center] position with dimensions [width] and [height]. */
        fun fromCenter(center: Vector2, width: Double, height: Double) =
                Rectangle(center.x - width / 2.0, center.y - height / 2.0, width, height)
    }
}
