package org.openrndr.math

import java.lang.Math.toDegrees
import kotlin.math.atan2

/**
 * Ref: https://en.wikipedia.org/wiki/Polar_coordinate_system
 * [theta] angle in degrees
 */
data class Polar(val theta: Double, val radius: Double = 1.0) {

    fun makeSafe() = Polar(
            radius,
            theta
    )

    companion object {
        fun fromVector(vector: Vector2): Polar {
            val r = vector.length
            return Polar(
                    if (r == 0.0) 0.0 else toDegrees(atan2(vector.y, vector.x)),
                    r
            )
        }
    }

    val cartesian: Vector2 get() {
        return Vector2.fromPolar(this)
    }

    operator fun plus(s: Polar) = Polar(radius + s.radius, theta + s.theta)
    operator fun minus(s: Polar) = Polar(radius - s.radius, theta - s.theta)
    operator fun times(s: Polar) = Polar(radius * s.radius, theta * s.theta)
    operator fun times(s: Double) = Polar(radius * s, theta * s)
    operator fun div(s: Double) = Polar(radius / s, theta / s)
}
