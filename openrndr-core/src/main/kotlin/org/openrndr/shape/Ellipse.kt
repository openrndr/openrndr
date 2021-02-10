package org.openrndr.shape

import org.openrndr.math.Vector2

/**
 * Creates a new [Ellipse].
 *
 * Also see [OrientedEllipse] and [Circle].
 *
 * @param xRadius Horizontal radius.
 * @param yRadius Vertical radius.
 */
data class Ellipse(val center: Vector2, val xRadius: Double, val yRadius: Double) {
    constructor(x: Double, y: Double, xRadius: Double, yRadius: Double) : this(Vector2(x, y), xRadius, yRadius)

    /** Creates a new [Ellipse] with the current [center] offset by [offset]. */
    fun moved(offset: Vector2): OrientedEllipse = OrientedEllipse(center + offset, xRadius, yRadius)

    /** Creates a new [Ellipse] with center at [position]. */
    fun movedTo(position: Vector2): OrientedEllipse = OrientedEllipse(position, xRadius, yRadius)

    /** Creates a new [Ellipse] with scale specified by multipliers for the current radii. */
    fun scaled(xScale: Double, yScale: Double = xScale): OrientedEllipse = OrientedEllipse(center, xRadius * xScale, yRadius * yScale)

    /** Creates a new [Ellipse] at the same position with given radius. */
    fun scaledTo(xFitRadius: Double, yFitRadius: Double = xFitRadius) = OrientedEllipse(center, xFitRadius, yFitRadius)

    /** Returns [Shape] representation of the [Ellipse]. */
    val shape get() = Shape(listOf(contour))

    /** Creates a new [Ellipse] with the given rotation in degrees. */
    fun withOrientation(degrees: Double): OrientedEllipse {
        return OrientedEllipse(center, xRadius, yRadius, degrees)
    }

    /** Returns [ShapeContour] representation of the [Ellipse]. */
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