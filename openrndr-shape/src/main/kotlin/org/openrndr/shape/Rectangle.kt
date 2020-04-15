@file:Suppress("unused")

package org.openrndr.shape

import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity

data class Rectangle(val corner: Vector2, val width: Double, val height: Double) {

    constructor(x: Double, y: Double, width: Double, height: Double) : this(Vector2(x, y), width, height)

    /** the center of the rectangle */
    val center: Vector2
        get() = corner + Vector2(width / 2, height / 2)

    /** the unitless area covered by this rectangle*/
    val area: Double
        get() = width * height

    /** the center of the rectangle */
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
                    corner + Vector2(0.0, height)), true, YPolarity.CW_NEGATIVE_Y)

    /** create a new [Rectangle] instance with offset edges */
    fun offsetEdges(offset: Double, offsetY: Double = offset): Rectangle {
        return Rectangle(Vector2(corner.x - offset, corner.y - offsetY), width + 2 * offset, height + 2 * offsetY)
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

    operator fun contains(point: Vector2): Boolean {
        return (point.x >= corner.x &&
                point.x < corner.x + width &&
                point.y >= corner.y &&
                point.y < corner.y + height)
    }

    companion object {
        /** creates a [Rectangle] around [center] with dimensions [width] and [height] */
        fun fromCenter(center: Vector2, width: Double, height: Double) =
                Rectangle(center.x - width / 2.0, center.y - height / 2.0, width, height)
    }
}

/** calculates [Rectangle]-bounds for a list of [Vector2] instances */
fun vector2Bounds(points: List<Vector2>): Rectangle {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY

    points.forEach {
        minX = Math.min(minX, it.x)
        maxX = Math.max(maxX, it.x)
        minY = Math.min(minY, it.y)
        maxY = Math.max(maxY, it.y)
    }
    return Rectangle(Vector2(minX, minY), maxX - minX, maxY - minY)
}

/** calculates [Rectangle]-bounds for a list of [Rectangle] instances */
fun rectangleBounds(rectangles: List<Rectangle>): Rectangle {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY

    rectangles.forEach {
        minX = Math.min(minX, it.x)
        maxX = Math.max(maxX, it.x + it.width)
        minY = Math.min(minY, it.y)
        maxY = Math.max(maxY, it.y + it.height)
    }
    return Rectangle(Vector2(minX, minY), maxX - minX, maxY - minY)
}

/** determines of [a] and [b] intersect */
fun intersects(a: Rectangle, b: Rectangle): Boolean {
    val above = a.y + a.height < b.y
    val below = a.y > b.y + b.height
    val rightOf = a.x > b.x + b.width
    val leftOf = a.x + a.width < b.x

    return !(above || below || leftOf || rightOf)
}
