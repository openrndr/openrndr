package org.openrndr.shape

import org.openrndr.math.Vector2

class Circle(val center: Vector2, val radius: Double) {

    companion object {
        internal fun fromPoints(a: Vector2, b: Vector2): Circle {
            val center = (a + b) * 0.5
            return Circle(center, b.minus(center).length)
        }
    }

    fun contains(point: Vector2): Boolean {
        return point.minus(center).squaredLength < radius * radius
    }

    val shape: Shape
        get() {
            return Shape(listOf(contour))
        }

    val contour: ShapeContour
        get() {
            val x = center.x - radius
            val y = center.y - radius
            val width = radius * 2.0
            val height = radius * 2.0
            val kappa = 0.5522848
            val ox = width / 2 * kappa        // control point offset horizontal
            val oy = height / 2 * kappa        // control point offset vertical
            val xe = x + width        // x-end
            val ye = y + height        // y-end
            val xm = x + width / 2        // x-middle
            val ym = y + height / 2       // y-middle

            return contour {
                moveTo(Vector2(x, ym))
                curveTo(Vector2(x, ym - oy), Vector2(xm - ox, y), Vector2(xm, y))
                curveTo(Vector2(xm + ox, y), Vector2(xe, ym - oy), Vector2(xe, ym))
                curveTo(Vector2(xe, ym + oy), Vector2(xm + ox, ye), Vector2(xm, ye))
                curveTo(Vector2(xm - ox, ye), Vector2(x, ym + oy), Vector2(x, ym))
                close()
            }
        }

}