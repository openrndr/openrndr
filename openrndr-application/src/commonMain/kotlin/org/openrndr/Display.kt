package org.openrndr

import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2

class Display(
    val pointer: Long, val name: String?, val x: Int, val y: Int, val width: Int?, val height: Int?,
    val xScale: Double, val yScale: Double
) {
    val position: IntVector2 by lazy { IntVector2(x, y) }
    val dimensions: IntVector2? by lazy {
        if (width != null && height != null) {
            IntVector2(width, height)
        } else {
            null
        }
    }
    val contentScale: Vector2 by lazy { Vector2(xScale, yScale) }
}