package org.openrndr.shape

import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.asDegrees
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.PI

/**
 * Creates a [Circle].
 *
 * Alternatively, see [Ellipse].
 */
data class Circle(val center: Vector2, val radius: Double): Movable, Scalable1D, ShapeProvider, ShapeContourProvider {
    constructor(x: Double, y: Double, radius: Double) : this(Vector2(x, y), radius)

    companion object {
        val INVALID = Circle(Vector2.INFINITY, 0.0)

        /**
         * Creates a [Circle] passing through two points.
         *
         * The diameter of the circle equals the distance between the points.
         */
        fun fromPoints(a: Vector2, b: Vector2): Circle {
            val center = (a + b) * 0.5
            return Circle(center, b.minus(center).length)
        }

        /**
         * Constructs a [Circle] where the perimeter passes through the three points.
         */
        fun fromPoints(a: Vector2, b: Vector2, c: Vector2): Circle {
            val det = (a.x - b.x) * (b.y - c.y) - (b.x - c.x) * (a.y - b.y)

            if (abs(det) < 1E-7) {
                return INVALID
            }

            val offset = b.x * b.x + b.y * b.y
            val bc = (a.x * a.x + a.y * a.y - offset) / 2
            val cd = (offset - c.x * c.x - c.y * c.y) / 2
            val x = (bc * (b.y - c.y) - cd * (a.y - b.y)) / det
            val y = (cd * (a.x - b.x) - bc * (b.x - c.x)) / det
            val radius = sqrt(
                (b.x - x) * (b.x - x) + (b.y - y) * (b.y - y)
            )

            return Circle(x, y, radius)
        }
    }

    /** The top-left corner of the [Circle]. */
    val corner: Vector2
        get() = center - scale

    override val scale: Vector2
        get() = Vector2(radius)

    /** Creates a new [Circle] with the current [center] offset by [offset]. */
    @Deprecated("Vague naming", ReplaceWith("movedBy(offset)"))
    fun moved(offset: Vector2): Circle = Circle(center + offset, radius)

    override fun movedBy(offset: Vector2): Circle = Circle(center + offset, radius)

    /** Creates a new [Circle] with center at [center]. */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun movedTo(center: Vector2) = Circle(center, radius)

    /** Creates a new [Circle] with the [scale] specified as a multiplier for the current radius. */
    @Deprecated("Vague naming", ReplaceWith("scaledBy(scale)"))
    fun scaled(scale: Double): Circle = Circle(center, radius * scale)

    override fun scaledBy(scale: Double, uAnchor: Double, vAnchor: Double): Circle {
        val anchorPosition = position(uAnchor, vAnchor)
        return Circle(anchorPosition, radius * scale)
    }

    /** Creates a new [Circle] at the same position with the given [radius]. */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun scaledTo(radius: Double) = Circle(center, radius)

    override fun position(u: Double, v: Double): Vector2 {
        return corner + Vector2(u * 2 * radius, v * 2 * radius)
    }

    /** Returns true if given [point] lies inside the [Shape]. */
    operator fun contains(point: Vector2): Boolean = point.minus(center).squaredLength < radius * radius

    val area: Double
        get() = radius * radius * PI

    /** Returns [Shape] representation of the [Circle]. */
    override val shape get() = Shape(listOf(contour))

    /** Returns [ShapeContour] representation of the [Circle]. */
    override val contour: ShapeContour
        get() {
            if(this == INVALID) {
                return ShapeContour.EMPTY
            }
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

    /**
     * Calculates the tangent lines between two [Circle]s.
     *
     * Defaults to returning the outer tangents.
     *
     * @param isInner If true, returns the inner tangents instead.
     */
    fun tangents(c: Circle, isInner: Boolean = false): List<Pair<Vector2, Vector2>>? {
        if(this == INVALID || c == INVALID) {
            return listOf()
        }
        val r1 = radius
        val r2 = if (isInner) -c.radius else c.radius

        val w = (c.center - center) // hypotenuse
        val d = w.length
        val dr = r1 - r2 // adjacent
        val d2 = sqrt(d)
        val h = sqrt(d.pow(2.0) - dr.pow(2.0))

        if (d2 == 0.0) return null

        return listOf(-1.0, 1.0).map { sign ->
            val v = (w * dr + w.perpendicular() * h * sign) / d.pow(2.0)

            Pair(center + v * r1, c.center + v * r2)
        }
    }

    /** Calculates the tangent lines through an external point. **/
    fun tangents(point: Vector2): Pair<Vector2, Vector2> {
        if(this == INVALID) {
            return Pair(Vector2.INFINITY, Vector2.INFINITY)
        }
        val v = Polar.fromVector(point - center)
        val b = v.radius
        val theta = (acos(radius / b)).asDegrees
        val d1 = v.theta + theta
        val d2 = v.theta - theta

        val tp = center + Polar(d1, radius).cartesian
        val tp2 = center + Polar(d2, radius).cartesian

        return Pair(tp, tp2)
    }

    operator fun times(scale: Double) = Circle(center * scale, radius * scale)

    operator fun div(scale: Double) = Circle(center / scale, radius / scale)

    operator fun plus(right: Circle) =
        Circle(center + right.center, radius + right.radius)

    operator fun minus(right: Circle) =
        Circle(center - right.center, radius - right.radius)

}