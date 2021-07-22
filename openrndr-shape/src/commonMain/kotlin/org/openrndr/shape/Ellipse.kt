package org.openrndr.shape

import org.openrndr.math.Vector2

/**
 * Creates a new [Ellipse].
 *
 * Also see [Circle].
 *
 * @param xRadius Horizontal radius.
 * @param yRadius Vertical radius.
 */
data class Ellipse(val center: Vector2, val xRadius: Double, val yRadius: Double) {
    constructor(x: Double, y: Double, xRadius: Double, yRadius: Double) : this(Vector2(x, y), xRadius, yRadius)

    /** Creates a new [Ellipse] with the current [center] offset by [offset] with the same radii. */
    fun moved(offset: Vector2): Ellipse = Ellipse(center + offset, xRadius, yRadius)

    /** Creates a new [Ellipse] with center at [position] with the same radii. */
    fun movedTo(position: Vector2): Ellipse = Ellipse(position, xRadius, yRadius)

    /** Creates a new [Ellipse] with scale specified by multipliers for the current radii. */
    fun scaled(xScale: Double, yScale: Double = xScale): Ellipse =
        Ellipse(center, xRadius * xScale, yRadius * yScale)

    /** Creates a new [Ellipse] at the same position with given radii. */
    fun scaledTo(xFitRadius: Double, yFitRadius: Double = xFitRadius) = Ellipse(center, xFitRadius, yFitRadius)

    operator fun times(scale: Double) = Ellipse(center * scale, xRadius * scale, yRadius * scale)

    operator fun div(scale: Double) = Ellipse(center / scale, xRadius / scale, yRadius / scale)

    operator fun plus(right: Ellipse) =
        Ellipse(center + right.center, xRadius + right.xRadius, yRadius + right.yRadius)

    operator fun minus(right: Ellipse) =
        Ellipse(center - right.center, xRadius - right.xRadius, yRadius - right.yRadius)

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