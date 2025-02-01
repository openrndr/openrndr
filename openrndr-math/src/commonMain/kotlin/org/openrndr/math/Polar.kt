package org.openrndr.math

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmRecord
import kotlin.math.atan2

/**
 * A 2D point defined in the
 * [Polar coordinate system](https://en.wikipedia.org/wiki/Polar_coordinate_system).
 *
 * @param theta The angle in degrees.
 */
@Serializable
@JvmRecord
data class Polar(val theta: Double, val radius: Double = 1.0) : LinearType<Polar> {

    /**
     * make a safe version by bringing [theta] between 0 and 360
     */
    fun makeSafe() = Polar(
        theta.mod(360.0),
            radius
    )

    companion object {
        /** Constructs equivalent polar coordinates from the Cartesian coordinate system. */
        fun fromVector(vector: Vector2): Polar {
            val r = vector.length
            return Polar(
                    if (r == 0.0) 0.0 else atan2(vector.y, vector.x).asDegrees,
                    r
            )
        }
    }

    /** Constructs equivalent Cartesian coordinates from the polar representation. */
    val cartesian: Vector2
        get() {
            return Vector2.fromPolar(this)
        }

    override operator fun plus(right: Polar) = Polar(theta + right.theta, radius + right.radius)
    override operator fun minus(right: Polar) = Polar(theta - right.theta, radius - right.radius)
    operator fun times(scale: Polar) = Polar(theta * scale.theta, radius * scale.radius)

    override operator fun times(scale: Double) = Polar(theta * scale, radius * scale)
    override operator fun div(scale: Double) = Polar(theta / scale, radius / scale)
}
