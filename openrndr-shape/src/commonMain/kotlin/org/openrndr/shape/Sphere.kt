package org.openrndr.shape

import kotlinx.serialization.Serializable
import org.openrndr.math.LinearType
import org.openrndr.math.Vector3
import kotlin.jvm.JvmRecord
import kotlin.math.PI

/**
 * Represents a 3D sphere defined by a center position and radius.
 *
 * This class supports basic arithmetic operations, such as addition, subtraction, scaling,
 * and division, as well as position manipulation in 3D space. It can also check if a given
 * point lies within the sphere.
 *
 * @property center The center of the sphere in 3D space, represented as a [Vector3].
 * @property radius The radius of the sphere.
 */
@Serializable
@JvmRecord
data class Sphere(val center: Vector3, val radius: Double) : LinearType<Sphere>, Movable3D, GeometricPrimitive3D {
    override fun plus(right: Sphere): Sphere = copy(center = center + right.center, radius = radius + right.radius)

    override fun minus(right: Sphere): Sphere = copy(center = center - right.center, radius = radius - right.radius)

    override fun times(scale: Double): Sphere = copy(center = center * scale, radius = radius * scale)

    override fun div(scale: Double): Sphere = copy(center = center / scale, radius = radius / scale)

    override fun movedBy(offset: Vector3): Sphere = copy(center = center + offset)

    override fun movedTo(position: Vector3): Sphere = copy(center = position)

    /**
     * Checks if the specified [point] is contained within the sphere.
     *
     * A point is considered to be inside the sphere if the squared distance
     * from the point to the sphere's center is less than the square of the sphere's radius.
     * If the sphere is defined as [INFINITY], this function always returns `true`.
     *
     * @param point The [Vector3] representing the point to check.
     * @return `true` if the point is inside the sphere or if the sphere is [INFINITY], `false` otherwise.
     */
    operator fun contains(point: Vector3): Boolean =
        if (this == INFINITY) true else point.squaredDistanceTo(center) < radius * radius

    /**
     * The volume of the sphere.
     *
     * This property calculates the volumetric size of the sphere using the formula (4/3) * π * radius³.
     */
    val volume: Double
        get() = (4.0 / 3.0) * PI * radius * radius * radius

    /**
     * The surface area of the sphere.
     *
     * This property calculates the total external area of the sphere using
     * the formula 4 * π * radius².
     */
    val surfaceArea: Double
        get() = 4.0 * PI * radius * radius

    companion object {
        /**
         * A constant representing a sphere with a zero radius, located at the origin.
         *
         * This value can be used as a reference for a "null" or "zero" sphere in computations.
         * The center of the sphere is `Vector3.ZERO`, and the radius is `0.0`.
         */
        val ZERO = Sphere(Vector3.ZERO, 0.0)

        /**
         * Represents a constant unit sphere with its center at the origin and a radius of 1.0.
         *
         * This sphere can be used as a standard reference or as a default instance for
         * operations involving spheres.
         */
        val UNIT = Sphere(Vector3.ZERO, 1.0)

        /**
         * This sphere has its center at the position defined by [Vector3.ONE]
         * and a radius of 1.0. It can be used as a predefined reference
         * or default sphere for various geometric or mathematical operations.
         */
        val ONE = Sphere(Vector3.ONE, 1.0)

        /**
         * Represents an infinite sphere with an infinite radius and a center at an infinite position.
         *
         * This sphere serves as a special-case constant for scenarios where an unbounded or all-encompassing sphere
         * is required. For example, when performing calculations related to containment or bounding regions,
         * an infinite sphere always contains any point or other finite sphere.
         *
         * The `INFINITY` sphere is defined to have a center at [Vector3.INFINITY] and a radius of
         * [Double.POSITIVE_INFINITY].
         */
        val INFINITY = Sphere(Vector3.INFINITY, Double.POSITIVE_INFINITY)
    }
}