package org.openrndr.shape

import org.openrndr.math.Vector2
import kotlin.math.max
import kotlin.math.min


data class Triangle(val x1: Vector2, val x2: Vector2, val x3: Vector2) {
    operator fun contains(v: Vector2): Boolean {
        val x23 = x2 - x3
        val x32 = x3 - x2
        val x31 = x3 - x1
        val x13 = x1 - x3
        val det = x23.y * x13.x - x32.x * x31.y
        val minD = min(det, 0.0) - 10E-6
        val maxD = max(det, 0.0) + 10E-6

        val d = v - x3
        val a = x23.y * d.x + x32.x * d.y
        if (a < minD || a > maxD) {
            return false
        }
        val b = x31.y * d.x + x13.x * d.y
        if (b < minD || b > maxD) {
            return false
        }
        val c = det - a - b
        if (c < minD || c > maxD)
            return false

        return true
    }
    val contour: ShapeContour
        get() = ShapeContour.fromPoints(listOf(x1, x2, x3), closed = true)
}