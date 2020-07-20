@file:Suppress("unused")

package org.openrndr.shape

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.YPolarity
import org.openrndr.math.transforms.transform

data class OrientedRectangle(val corner: Vector2, val width: Double, val height: Double, val rotation: Double) {

    constructor(x: Double, y: Double, width: Double, height: Double, rotation: Double)
            : this(Vector2(x, y), width, height, rotation)

    /** the center of the rectangle */
    val center: Vector2
        get() = corner + Vector2(width / 2, height / 2)

    /** the unitless area covered by this rectangle*/
    val area: Double
        get() = width * height

    /** the dimensions of the rectangle */
    val dimensions: Vector2
        get() = Vector2(width, height)

    /** returns a position for parameterized coordinates [u] and [v] between 0 and 1 */
    fun position(u: Double, v: Double): Vector2 {
        return corner + Vector2(u * width, v * height)
    }

    val x: Double get() = corner.x
    val y: Double get() = corner.y

    /** [Shape] representation of the rectangle */
    val shape get() = Shape(listOf(contour))

    /** [ShapeContour] representation of the rectangle */
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

    /** creates a new [Rectangle] with dimensions scaled by [scale] and [scaleY] */
    fun scaled(scale: Double, scaleY: Double = scale): Rectangle {
        return Rectangle(corner, width * scale, height * scaleY)
    }

    /** creates a new [Rectangle] with width set to [fitWidth] and height scaled proportionally */
    fun widthScaledTo(fitWidth: Double): Rectangle {
        val scale = fitWidth / width
        return Rectangle(corner, fitWidth, height * scale)
    }

    /** creates a new [Rectangle] with height set to [fitHeight] and width scaled proportionally */
    fun heightScaledTo(fitHeight: Double): Rectangle {
        val scale = fitHeight / height
        return Rectangle(corner, width * scale, fitHeight)
    }

    /** creates a new [Rectangle] with its position offset by [offset] */
    fun moved(offset: Vector2): Rectangle {
        return Rectangle(corner + offset, width, height)
    }

    companion object {
        /** creates a [Rectangle] around [center] with dimensions [width] and [height] */
        fun fromCenter(center: Vector2, width: Double, height: Double) =
                Rectangle(center.x - width / 2.0, center.y - height / 2.0, width, height)
    }
}
