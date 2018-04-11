package org.openrndr.shape

import org.openrndr.math.Vector2

data class Rectangle(val corner: Vector2, val width: Double, val height: Double) {

    constructor(x: Double, y: Double, width: Double, height: Double) : this(Vector2(x, y), width, height)

    val center: Vector2
        get() = corner + Vector2(width / 2.0, height / 2)
    val area: Double
        get() = width * height

    fun position(u: Double, v: Double): Vector2 {
        return corner + Vector2(u * width, v * height)
    }

    val x: Double get() = corner.x
    val y: Double get() = corner.y

    val shape: Shape
        get() {
            return Shape(listOf(contour))
        }

    val contour: ShapeContour
        get() {
            return ShapeContour.fromPoints(listOf(corner, corner + Vector2(width, 0.0), corner + Vector2(width, height), corner + Vector2(0.0, height), corner), true)
        }

    fun offsetEdges(offset: Double, offsetY: Double = offset): Rectangle {
        return Rectangle(Vector2(corner.x - offset, corner.y - offsetY), width + 2 * offset, height + 2 * offsetY)
    }

    operator fun contains(point: Vector2): Boolean {
        return (point.x >= corner.x &&
                point.x < corner.x + width &&
                point.y >= corner.y &&
                point.y < corner.y + height)
    }

}

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