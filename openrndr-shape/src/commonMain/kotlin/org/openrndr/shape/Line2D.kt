package org.openrndr.shape

import kotlinx.serialization.Serializable
import org.openrndr.math.LinearType
import org.openrndr.math.Vector2
import kotlin.jvm.JvmRecord

/**
 * Represents a two-dimensional line in space, defined by an origin point and a direction vector.
 *
 * The `Line2D` class facilitates mathematical operations such as addition, subtraction,
 * scaling, and division. It also includes methods for constructing a line based on specific
 * parameters and operations to compute derived properties or positions along the line.
 *
 * @property origin The starting point of the line in 2D space.
 * @property direction The direction vector defining the orientation of the line.
 */
@Serializable
@JvmRecord
data class Line2D(val origin: Vector2, val direction: Vector2) : LinearType<Line2D> {
    override fun plus(right: Line2D): Line2D {
        return copy(origin = origin + right.origin, direction = direction + right.direction)
    }

    override fun minus(right: Line2D): Line2D {
        return copy(origin = origin - right.origin, direction = direction - right.direction)
    }

    override fun times(scale: Double): Line2D {
        return copy(origin = origin * scale, direction = direction * scale)
    }

    override fun div(scale: Double): Line2D {
        return copy(origin = origin / scale, direction = direction / scale)
    }

    /**
     * Provides a normalized representation of the line.
     *
     * This property returns a new instance of `Line2D` where the direction vector
     * has been normalized to have a unit length while maintaining the same origin
     * and overall direction.
     */
    val normalized: Line2D get() = Line2D(origin, direction.normalized)

    /**
     * Calculates a position on the line at a given parameter `t`.
     * The position is determined by extending or retracting the line
     * along its direction vector by `t` units from the origin.
     *
     * @param t The parameter along the line, where 0 represents the origin,
     *          positive values extend in the direction of the line, and
     *          negative values extend in the opposite direction.
     * @return The computed position as a `Vector2` object.
     */
    fun position(t: Double) = origin + direction * t


    /**
     * Creates a line segment between two positions on the line, determined by the parameters `t0` and `t1`.
     *
     * @param t0 The parameter representing the first position on the line.
     * @param t1 The parameter representing the second position on the line.
     * @return A `LineSegment` object defined between the positions calculated at `t0` and `t1`.
     */
    fun segment(t0: Double, t1: Double) : LineSegment = LineSegment(position(t0), position(t1))

    companion object {
        /**
         * Creates a line in 2D space defined by two points.
         *
         * @param a The starting point of the line.
         * @param b The ending point of the line.
         * @return A line represented by the defined origin point and a normalized direction vector.
         */
        fun fromPoints(a: Vector2, b: Vector2): Line2D = Line2D(a, (b - a).normalized)

        /**
         * Constructs a `Line2D` object from a given `LineSegment`.
         *
         * @param segment The line segment to create the line from, defined by start and end points.
         * @return A line represented by the origin set to the start of the segment and a direction vector from the start to the end of the segment.
         */
        fun fromLineSegment(segment: LineSegment) = fromPoints(segment.start, segment.end)
        fun fromInterceptForm(a: Double, b: Double): Line2D = fromPoints(Vector2(0.0, b), Vector2(a, 0.0))
    }
}