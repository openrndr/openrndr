package org.openrndr.shape

import kotlinx.serialization.Serializable
import org.openrndr.math.Vector2
import kotlin.jvm.JvmRecord

/**
 * Creates a new [Ellipse].
 *
 * Also see [Circle].
 *
 * @param xRadius Horizontal radius.
 * @param yRadius Vertical radius.
 */
@Serializable
@JvmRecord
data class Ellipse(val center: Vector2, val xRadius: Double, val yRadius: Double): Movable, Scalable2D, ShapeProvider, ShapeContourProvider, GeometricPrimitive2D {

    /** The top-left corner of the [Ellipse]. */
    val corner: Vector2
        get() = center - scale

    override val scale: Vector2
        get() = Vector2(xRadius, yRadius)

    /** Creates a new [Ellipse] with the current [center] offset by [offset] with the same radii. */
    override fun movedBy(offset: Vector2) = Ellipse(center + offset, xRadius, yRadius)

    /** Creates a new [Ellipse] with the center at [position] with the same radii. */
    override fun movedTo(position: Vector2) = Ellipse(position, xRadius, yRadius)

    /** Creates a new [Ellipse] with the scale specified as multipliers for the current radii. */
    override fun scaledBy(xScale: Double, yScale: Double, uAnchor: Double, vAnchor: Double): Ellipse {
        val anchorPosition = position(uAnchor, vAnchor)
        return Ellipse(anchorPosition, xRadius * xScale, yRadius * yScale)
    }

    override fun scaledBy(scale: Double, uAnchor: Double, vAnchor: Double) =
        scaledBy(scale, scale, uAnchor, vAnchor)

    /** Creates a new [Ellipse] at the same position with the given radii. */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun scaledTo(xRadius: Double, yRadius: Double) = Ellipse(center, xRadius, yRadius)

    /** Creates a new [Ellipse] at the same position with equal radii. */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun scaledTo(radius: Double) = scaledTo(radius, radius)

    operator fun times(scale: Double) = Ellipse(center * scale, xRadius * scale, yRadius * scale)

    operator fun div(scale: Double) = Ellipse(center / scale, xRadius / scale, yRadius / scale)

    operator fun plus(right: Ellipse) =
        Ellipse(center + right.center, xRadius + right.xRadius, yRadius + right.yRadius)

    operator fun minus(right: Ellipse) =
        Ellipse(center - right.center, xRadius - right.xRadius, yRadius - right.yRadius)

    override fun position(u: Double, v: Double): Vector2 {
        return corner + Vector2(u * 2 * xRadius, v * 2 * yRadius)
    }

    override val shape: Shape
        get() = Shape(listOf(contour))
    override val contour: ShapeContour
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

fun Ellipse(x: Double, y: Double, xRadius: Double, yRadius: Double) = Ellipse(Vector2(x, y), xRadius, yRadius)
