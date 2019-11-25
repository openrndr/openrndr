package org.openrndr.math

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2

/**
 * Ref: https://en.wikipedia.org/wiki/Spherical_coordinate_system
 *
 * The poles (phi) are at the positive and negative y axis.
 * The equator starts at positive z.
 */

private const val EPS = 0.000001

data class Spherical(val theta: Double, val phi: Double, val radius: Double) {

    fun makeSafe() = Spherical(
            radius,
            theta,
            clamp(phi, EPS, 180 - EPS)
    )

    companion object {
        fun fromVector(vector: Vector3): Spherical {
            val r = vector.length
            return Spherical(
                    if (r == 0.0) 0.0 else Math.toDegrees(atan2(vector.x, vector.z)),
                    if (r == 0.0) 0.0 else Math.toDegrees(acos(clamp(vector.y / r, -1.0, 1.0))),
                    r)
        }
    }

    val cartesian: Vector3
        get() {
            return Vector3.fromSpherical(this)
        }

    operator fun plus(s: Spherical) = Spherical(radius + s.radius, theta + s.theta, phi + s.phi)
    operator fun minus(s: Spherical) = Spherical(radius - s.radius, theta - s.theta, phi - s.phi)
    operator fun times(s: Spherical) = Spherical(radius * s.radius, theta * s.theta, phi * s.phi)
    operator fun times(s: Double) = Spherical(radius * s, theta * s, phi * s)
    operator fun div(s: Double) = Spherical(radius / s, theta / s, phi / s)
}
