package org.openrndr.shape

import org.openrndr.math.Vector2

data class Ellipse(val center: Vector2, val xRadius: Double, val yRadius: Double) {
    constructor(x: Double, y: Double, xRadius: Double, yRadius: Double) : this(Vector2(x, y), xRadius, yRadius)

    /** creates new [Ellipse] with center offset by [offset] */
    fun moved(offset: Vector2): OrientedEllipse = OrientedEllipse(center + offset, xRadius, yRadius)

    /** creates new [Ellipse] with center at [position] */
    fun movedTo(position: Vector2): OrientedEllipse = OrientedEllipse(position, xRadius, yRadius)

    /** creates new [Ellipse] with radius scaled by [scale] */
    fun scaled(xScale: Double, yScale: Double = xScale): OrientedEllipse = OrientedEllipse(center, xRadius * xScale, yRadius * yScale)

    /** creates new [Ellipse] with radius set to [fitRadius] */
    fun scaledTo(xFitRadius: Double, yFitRadius: Double = xFitRadius) = OrientedEllipse(center, xFitRadius, yFitRadius)

    /** creates [Shape] representation */
    val shape get() = Shape(listOf(contour))

    fun withOrientation(degrees: Double): OrientedEllipse {
        return OrientedEllipse(center, xRadius, yRadius, degrees)
    }

    /** creates [ShapeContour] representation */
    val contour: ShapeContour
        get() {
            return withOrientation(0.0).contour
        }

    operator fun times(scale: Double) = Ellipse(center * scale, xRadius * scale, yRadius * scale)

    operator fun div(scale: Double) = Ellipse(center / scale, xRadius / scale, yRadius / scale)

    operator fun plus(right: Ellipse) =
            Ellipse(center + right.center, xRadius + right.xRadius, yRadius + right.yRadius)

    operator fun minus(right: Ellipse) =
            Ellipse(center - right.center, xRadius - right.xRadius, yRadius - right.yRadius)


}