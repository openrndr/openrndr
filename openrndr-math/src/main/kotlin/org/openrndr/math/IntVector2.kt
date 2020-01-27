package org.openrndr.math

import java.io.Serializable
import kotlin.math.sqrt

/**
 * Integer vector 2
 */
@Suppress("unused")
data class IntVector2(val x: Int, val y: Int) : Serializable {
    companion object {
        val ZERO = IntVector2(0, 0)
        val UNIT_X = IntVector2(1, 0)
        val UNIT_Y = IntVector2(0, 1)
    }

    val length get() = sqrt(1.0 * x * x + y * y)
    val squaredLength get() = x * x + y * y
    infix fun dot(right: IntVector2) = x * right.x + y * right.y
    val yx get() = IntVector2(y, x)
    val xx get() = IntVector2(x, x)
    val yy get() = IntVector2(y, y)
    operator fun plus(v: IntVector2) = IntVector2(x + v.x, y + v.y)
    operator fun minus(v: IntVector2) = IntVector2(x - v.x, y - v.y)
    operator fun times(d: Int) = IntVector2(x * d, y * d)
    operator fun div(d: Int) = IntVector2(x / d, y / d)
    val vector2 get() = Vector2(this.x.toDouble(), this.y.toDouble())
}

operator fun Int.times(v: IntVector2) = v * this