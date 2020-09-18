package org.openrndr.shape

import org.openrndr.math.IntVector2

@Suppress("MemberVisibilityCanBePrivate", "unused")
data class IntRectangle(val corner: IntVector2, val width: Int, val height: Int) {
    constructor(x: Int, y: Int, width: Int, height: Int) : this(IntVector2(x, y), width, height)

    val x get() = corner.x
    val y get() = corner.y
    val center get() = corner + IntVector2(width / 2, height / 2)
    val area get() = width * height
    val rectangle get() = Rectangle(corner.vector2, width.toDouble(), height.toDouble())
}