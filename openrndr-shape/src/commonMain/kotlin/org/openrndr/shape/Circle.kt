package org.openrndr.shape

import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.asDegrees
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.abs
/**
 * Creates a [Circle].
 *
 * Alternatively, see [Ellipse].
 */
data class Circle(val center: Vector2, val radius: Double) {
    constructor(x: Double, y: Double, radius: Double) : this(Vector2(x, y), radius)

    companion object {
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
            val epsilon = 1E-7
            val dyba = b.y - a.y
            val dxba = b.x - a.x
            val dycb = c.y - b.y
            val dxcb = c.x - b.x

            if (abs(dxba) <= epsilon && abs(dycb) <= epsilon) {
                val center = (b + c) * 0.5
                val radius = center.distanceTo(a)
                return Circle(center, radius)
            }

            val baSlope = dyba / dxba
            val cbSlope = dycb / dxcb
            if (abs(baSlope - cbSlope) <= epsilon) {
                return Circle((a + b + c) / 3.0, 0.0)
            }

            val cx = (baSlope * cbSlope * (a.y - c.y) + cbSlope * (a.x + b.x)
                    - baSlope * (b.x + c.x)) / (2 * (cbSlope - baSlope))
            val cy = -1 * (cx - (a.x + b.x) / 2) / baSlope + (a.y + b.y) / 2

            val center = Vector2(cx, cy)
            return Circle(center, center.distanceTo(a))
        }
    }

    /** Creates a new [Circle] with the current [center] offset by [offset]. */
    fun moved(offset: Vector2): Circle = Circle(center + offset, radius)

    /** Creates a new [Circle] with center at [position]. */
    fun movedTo(position: Vector2): Circle = Circle(position, radius)

    /** Creates a new [Circle] with scale specified by a multiplier for the current radius. */
    fun scaled(scale: Double): Circle = Circle(center, radius * scale)

    /** Creates a new [Circle] at the same position with given radius. */
    fun scaledTo(fitRadius: Double) = Circle(center, fitRadius)

    /** Returns true if given [point] lies inside the [Shape]. */
    fun contains(point: Vector2): Boolean = point.minus(center).squaredLength < radius * radius

    /** Returns [Shape] representation of the [Circle]. */
    val shape get() = Shape(listOf(contour))

    /** Returns [ShapeContour] representation of the [Circle]. */
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

    /**
     * Calculates the tangent lines between two [Circle]s.
     *
     * Defaults to returning the outer tangents.
     *
     * @param isInner If true, returns the inner tangents instead.
     */
    fun tangents(c: Circle, isInner: Boolean = false): List<Pair<Vector2, Vector2>>? {
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