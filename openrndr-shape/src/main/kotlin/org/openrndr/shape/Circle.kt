package org.openrndr.shape

import org.openrndr.math.Vector2

class Circle(val center: Vector2, val radius:Double) {

    companion object {
        internal fun fromPoints(a: Vector2, b: Vector2): Circle {
            val center = (a+b) * 0.5
            return Circle(center, b.minus(center).length)
        }
    }

    fun contains(point: Vector2):Boolean {
        return point.minus(center).squaredLength < radius * radius
    }

}