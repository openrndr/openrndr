package org.openrndr.math

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/** Boolean 2D vector */
@Suppress("unused")
@Serializable
data class BooleanVector2(val x: Boolean, val y: Boolean) {
    companion object {
        val FALSE = BooleanVector2(false, false)
        val TRUE = BooleanVector2(true, true)
        val UNIT_X = BooleanVector2(true, false)
        val UNIT_Y = BooleanVector2(false, true)
    }

    val yx: BooleanVector2 get() = BooleanVector2(y, x)
    val xx: BooleanVector2 get() = BooleanVector2(x, x)
    val yy: BooleanVector2 get() = BooleanVector2(y, y)

    /** Casts to [Vector2]. */
    fun toVector2(
        x: Double = if (this.x) 1.0 else 0.0,
        y: Double = if (this.y) 1.0 else 0.0,
    ) = Vector2(x, y)

    /** Casts to [IntVector2]. */
    fun toIntVector2(
        x: Int = if (this.x) 1 else 0,
        y: Int = if (this.y) 1 else 0,
    ) = IntVector2(x, y)
}