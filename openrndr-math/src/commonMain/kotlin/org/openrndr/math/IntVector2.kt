package org.openrndr.math

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmRecord
import kotlin.math.sqrt

/** Integer 2D vector, exclusively for integer calculations. */
@Suppress("unused")
@Serializable
@JvmRecord
data class IntVector2(val x: Int, val y: Int) {
    companion object {
        val ZERO = IntVector2(0, 0)
        val ONE = IntVector2(1, 1)
        val UNIT_X = IntVector2(1, 0)
        val UNIT_Y = IntVector2(0, 1)
    }

    /** The Euclidean length of the vector. */
    val length get() = sqrt(1.0 * x * x + y * y)

    /** The squared Euclidean length of the vector. */
    val squaredLength get() = x * x + y * y

    /** Calculates a dot product between this [Vector2] and [right]. */
    infix fun dot(right: IntVector2) = x * right.x + y * right.y

    val yx: IntVector2 get() = IntVector2(y, x)
    val xx: IntVector2 get() = IntVector2(x, x)
    val yy: IntVector2 get() = IntVector2(y, y)
    operator fun plus(v: IntVector2) = IntVector2(x + v.x, y + v.y)
    operator fun minus(v: IntVector2) = IntVector2(x - v.x, y - v.y)
    operator fun times(d: Int) = IntVector2(x * d, y * d)
    operator fun div(d: Int) = IntVector2(x / d, y / d)

    /** Casts to [Vector2]. */
    val vector2 get() = Vector2(this.x.toDouble(), this.y.toDouble())
}

operator fun Int.times(v: IntVector2) = v * this