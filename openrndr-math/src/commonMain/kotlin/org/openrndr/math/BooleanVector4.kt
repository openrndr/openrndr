package org.openrndr.math

import kotlinx.serialization.Serializable

/** Boolean 4D vector */
@Suppress("unused")
@Serializable
data class BooleanVector4(val x: Boolean, val y: Boolean, val z: Boolean, val w: Boolean) {
    companion object {
        val FALSE = BooleanVector4(false, false, false, false)
        val TRUE = BooleanVector4(true, true, true, true)
        val UNIT_X = BooleanVector4(true, false, false, false)
        val UNIT_Y = BooleanVector4(false, true, false, false)
        val UNIT_Z = BooleanVector4(false, false, true,  false)
        val UNIT_W = BooleanVector4(false, false, false, true)
    }

    /** Casts to [Vector4]. */
    fun toVector4(
        x: Double = if (this.x) 1.0 else 0.0,
        y: Double = if (this.y) 1.0 else 0.0,
        z: Double = if (this.z) 1.0 else 0.0,
        w: Double = if (this.x) 1.0 else 0.0
    ) = Vector4(x, y, z, w)

    /** Casts to [IntVector4]. */
    fun toIntVector4(
        x: Int = if (this.x) 1 else 0,
        y: Int = if (this.y) 1 else 0,
        z: Int = if (this.z) 1 else 0,
        w: Int = if (this.w) 1 else 0
    ) = IntVector4(x, y, z, w)
}