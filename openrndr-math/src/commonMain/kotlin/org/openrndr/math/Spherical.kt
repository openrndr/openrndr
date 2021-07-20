package org.openrndr.math

import kotlinx.serialization.Serializable
import kotlin.math.acos
import kotlin.math.atan2

/**
 * Ref: https://en.wikipedia.org/wiki/Spherical_coordinate_system
 *
 * The poles (phi) are at the positive and negative y axis.
 * The equator starts at positive z.
 */

private const val EPS = 0.000001

@Serializable
data class Spherical(val theta: Double, val phi: Double, val radius: Double) : LinearType<Spherical> {

    fun makeSafe() = Spherical(
            theta,
            clamp(phi, EPS, 180 - EPS),
            radius
    )

    companion object {
        fun fromVector(vector: Vector3): Spherical {
            val r = vector.length
            return Spherical(
                    if (r == 0.0) 0.0 else atan2(vector.x, vector.z).asDegrees,
                    if (r == 0.0) 0.0 else acos(clamp(vector.y / r, -1.0, 1.0)).asDegrees,
                    r)
        }
    }

    val cartesian: Vector3
        get() {
            return Vector3.fromSpherical(this)
        }

    override operator fun plus(right: Spherical) = Spherical(theta + right.theta, phi + right.phi, radius + right.radius)
    override operator fun minus(right: Spherical) = Spherical(theta - right.theta, phi - right.phi, radius - right.radius)
    operator fun times(scale: Spherical) = Spherical(theta * scale.theta, phi * scale.phi, radius * scale.radius)
    override operator fun times(scale: Double) = Spherical(theta * scale, phi * scale, radius * scale)
    override operator fun div(scale: Double) = Spherical(theta / scale, phi / scale, radius / scale)
}
