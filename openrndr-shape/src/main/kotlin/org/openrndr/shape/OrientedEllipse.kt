package org.openrndr.shape

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform

data class OrientedEllipse(val center: Vector2, val xRadius: Double, val yRadius: Double, val rotation: Double = 0.0) {
    constructor(x: Double, y: Double, xRadius: Double, yRadius: Double, rotation: Double) : this(Vector2(x, y), xRadius, yRadius, rotation)

    /** creates new [OrientedEllipse] with center offset by [offset] */
    fun moved(offset: Vector2): OrientedEllipse = OrientedEllipse(center + offset, xRadius, yRadius, rotation)

    /** creates new [OrientedEllipse] with center at [position] */
    fun movedTo(position: Vector2): OrientedEllipse = OrientedEllipse(position, xRadius, yRadius, rotation)

    /** creates new [OrientedEllipse] with radius scaled by [scale] */
    fun scaled(xScale: Double, yScale: Double = xScale): OrientedEllipse = OrientedEllipse(center, xRadius * xScale, yRadius * yScale, rotation)

    /** creates new [OrientedEllipse] with radius set to [fitRadius] */
    fun scaledTo(xFitRadius: Double, yFitRadius: Double = xFitRadius) = OrientedEllipse(center, xFitRadius, yFitRadius, rotation)

    /** creates [Shape] representation */
    val shape get() = Shape(listOf(contour))

    /** creates [ShapeContour] representation */
    val contour: ShapeContour
        get() {
            val x = center.x - xRadius
            val y = center.y - yRadius
            val width = xRadius * 2.0
            val height = yRadius * 2.0
            val kappa = 0.5522848
            val ox = width / 2 * kappa        // control point offset horizontal
            val oy = height / 2 * kappa        // control point offset vertical
            val xe = x + width        // x-end
            val ye = y + height        // y-end
            val xm = x + width / 2        // x-middle
            val ym = y + height / 2       // y-middle

            val t = transform {
                translate(center)
                rotate(Vector3.UNIT_Z, rotation)
                translate(-center)
            }

            return contour {
                moveTo(Vector2(x, ym))
                curveTo(Vector2(x, ym - oy), Vector2(xm - ox, y), Vector2(xm, y))
                curveTo(Vector2(xm + ox, y), Vector2(xe, ym - oy), Vector2(xe, ym))
                curveTo(Vector2(xe, ym + oy), Vector2(xm + ox, ye), Vector2(xm, ye))
                curveTo(Vector2(xm - ox, ye), Vector2(x, ym + oy), Vector2(x, ym))
                close()
            }.transform(t)
        }
}