package org.openrndr.math

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmRecord
import kotlin.math.acos
import kotlin.math.atan2


private const val EPS = 0.000001

@Serializable
@JvmRecord
/**
 * Spherical coordinate. The poles (phi) are at the positive and negative y-axis. The equator starts at positive z.
 * @param theta the counterclockwise angle from the z-axis at which a point in the xz-plane lies.
 * @param phi the angle measured from the y-axis. The north-pole is at 0 degrees, the south-pole is at 180 degrees.
 * @param radius the radius of the sphere
 * @see <a href="https://en.wikipedia.org/wiki/Spherical_coordinate_system">Spherical coordinate system</a>
 */
data class Spherical(
    /**
     * the counterclockwise angle from the z-axis at which a point in the xz-plane lies.
     */
    val theta: Double,
    /**
     * the angle measured from the y-axis. The north-pole is at 0 degrees, the south-pole is at 180 degrees.
     */
    val phi: Double,
    /**
     * the radius of the sphere
     */
    val radius: Double) : LinearType<Spherical> {

    fun makeSafe() = Spherical(
            theta,
            clamp(phi, EPS, 180 - EPS),
            radius
    )

    companion object {
        /** A [Spherical] that points to [Vector3.UNIT_X] */
        val UNIT_X = Spherical(90.0, 90.0, 1.0)
        /** A [Spherical] that points to [Vector3.UNIT_Y] */
        val UNIT_Y = Spherical(0.0, 0.0, 1.0)
        /** A [Spherical] that points to [Vector3.UNIT_Z] */
        val UNIT_Z = Spherical(0.0, 90.0, 1.0)

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
