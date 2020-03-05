package org.openrndr.shape

import org.openrndr.math.Vector2


data class RoundedRectangle(val corner: Vector2, val width: Double, val height: Double, val radius: Double) {
    constructor(x: Double, y: Double, width: Double, height: Double, radius: Double) : this(Vector2(x, y), width, height, radius)

    /** the center of the rounded rectangle */
    val center: Vector2
        get() = corner + Vector2(width / 2, height / 2)

    val x: Double get() = corner.x
    val y: Double get() = corner.y

    /** [ShapeContour] representation of the rounded rectangle */
    val contour
        get() = contour {
            // A higher radius than half the width/height makes it go weird
            var r = Math.min(Math.min(radius, width / 2), height / 2)

            moveTo(x + r, y)
            lineTo(x + width - r, y)

            curveTo(Vector2(x + width, y), Vector2(x + width, y + r))
            lineTo(x+width, y + height - r)

            curveTo(Vector2(x+width, y + height), Vector2(x + width - r, y + height))
            lineTo(x + r, y+height)

            curveTo(Vector2(x, y + height), Vector2(x, y + height - r))
            lineTo(x, y + r)

            curveTo(Vector2(x, y), Vector2(x + r, y))
            close()
        }
}