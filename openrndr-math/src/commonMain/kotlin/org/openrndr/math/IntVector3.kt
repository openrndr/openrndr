package org.openrndr.math

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/** Integer 3D vector, exclusively for integer calculations. */
@Suppress("unused")
@Serializable
data class IntVector3(val x: Int, val y: Int, val z: Int) {
    companion object {
        val ZERO = IntVector3(0, 0, 0)
        val UNIT_X = IntVector3(1, 0, 0)
        val UNIT_Y = IntVector3(0, 1, 0)
        val UNIT_Z = IntVector3(0, 0, 1)
    }

    /** The Euclidean length of the vector. */
    val length get() = sqrt(1.0 * x * x + y * y + z * z)

    /** The squared Euclidean length of the vector. */
    val squaredLength get() = x * x + y * y + z * z

    /** Calculates a dot product between this [Vector3] and [right]. */
    infix fun dot(right: IntVector3) = x * right.x + y * right.y + z * right.z
    val xy get() = IntVector2(x, y)
    val yx get() = IntVector2(y, x)
    val xx get() = IntVector2(x, x)
    val yy get() = IntVector2(y, y)
    operator fun plus(v: IntVector3) = IntVector3(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: IntVector3) = IntVector3(x - v.x, y - v.y, z - v.z)
    operator fun times(d: Int) = IntVector3(x * d, y * d, z * d)
    operator fun div(d: Int) = IntVector3(x / d, y / d, z / d)

    /** Casts to [Vector3]. */
    val vector3 get() = Vector3(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
}

operator fun Int.times(v: IntVector3) = v * this