package org.openrndr.math

/**
 * Ref: https://en.wikipedia.org/wiki/Spherical_coordinate_system
 *
 * The poles (phi) are at the positive and negative y axis.
 * The equator starts at positive z.
 */

private const val EPS = 0.000001

data class Spherical(val radius: Double, val theta: Double, val phi: Double) {

    fun makeSafe() = Spherical(
            radius,
            theta,
            clamp(phi, EPS, Math.PI - EPS)
    )

    companion object {
        fun fromVector(vector: Vector3): Spherical {
            val r = vector.length
            return Spherical(
                    r,
                    if (r == 0.0) 0.0 else Math.atan2(vector.x, vector.z),
                    if (r == 0.0) 0.0 else Math.acos(clamp(vector.y / r, -1.0, 1.0)))
        }
    }

    val cartesian: Vector3 get() {
        return Vector3.fromSpherical(this)
    }

    operator fun plus(s: Spherical) = Spherical(radius + s.radius, theta + s.theta, phi + s.phi)
    operator fun minus(s: Spherical) = Spherical(radius - s.radius, theta - s.theta, phi - s.phi)
    operator fun times(s: Spherical) = Spherical(radius * s.radius, theta * s.theta, phi * s.phi)
    operator fun times(s: Double) = Spherical(radius * s, theta * s, phi * s)
    operator fun div(s: Double) = Spherical(radius / s, theta / s, phi / s)
}
