package org.openrndr.shape

import kotlinx.serialization.Serializable
import org.openrndr.math.LinearType
import org.openrndr.math.Vector3
import kotlin.jvm.JvmRecord

/**
 * Represents a 3-dimensional line defined by an origin point and a direction vector.
 *
 * A `Line3D` describes an infinite line in 3D space that extends through the `origin`
 * in the direction specified by the `direction` vector. The line may be manipulated
 * through arithmetic operations such as addition, subtraction, scaling, and division.
 *
 * @property origin The starting point of the line.
 * @property direction The direction vector of the line, which represents the orientation
 *                     of the line in 3D space. Should ideally be normalized for consistency.
 */
@Serializable
@JvmRecord
data class Line3D(val origin: Vector3, val direction: Vector3) : LinearType<Line3D>, GeometricPrimitive3D {
    override fun plus(right: Line3D): Line3D {
        return copy(origin = origin + right.origin, direction = direction + right.direction)
    }

    override fun minus(right: Line3D): Line3D {
        return copy(origin = origin - right.origin, direction = direction - right.direction)
    }

    override fun times(scale: Double): Line3D {
        return copy(origin = origin * scale, direction = direction * scale)
    }

    override fun div(scale: Double): Line3D {
        return copy(origin = origin / scale, direction = direction / scale)
    }

    /**
     * Provides a normalized representation of the line.
     *
     * This property returns a new instance of `Line3D` where the direction vector
     * has been normalized to have a unit length while maintaining the same origin
     * and overall direction.
     */
    val normalized: Line3D get() = Line3D(origin, direction.normalized)

    /**
     * Calculates a position on the line at a given parameter `t`.
     * The position is determined by extending or retracting the line
     * along its direction vector by `t` units from the origin.
     *
     * @param t The parameter along the line, where 0 represents the origin,
     *          positive values extend in the direction of the line, and
     *          negative values extend in the opposite direction.
     * @return The computed position as a `Vector3` object.
     */
    fun position(t: Double) = origin + direction * t

    /**
     * Creates a line segment between two positions on the line, determined by the parameters `t0` and `t1`.
     *
     * @param t0 The parameter representing the first position on the line.
     * @param t1 The parameter representing the second position on the line.
     * @return A `LineSegment` object defined between the positions calculated at `t0` and `t1`.
     */
    fun segment(t0: Double, t1: Double) : LineSegment3D = LineSegment3D(position(t0), position(t1))

    companion object {
        /**
         * Creates a line in 3D space defined by two points.
         *
         * @param a The starting point of the line.
         * @param b The ending point of the line.
         * @return A line represented by the defined origin point and a normalized direction vector.
         */
        fun fromPoints(a: Vector3, b: Vector3): Line3D = Line3D(a, (b - a).normalized)

        /**
         * Constructs a `Line3D` object from a given `LineSegment`.
         *
         * @param segment The line segment to create the line from, defined by start and end points.
         * @return A line represented by the origin set to the start of the segment and a direction vector from the start to the end of the segment.
         */
        fun fromLineSegment(segment: LineSegment3D) = fromPoints(segment.start, segment.end)
    }
}