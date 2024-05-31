package org.openrndr.math

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmRecord

/** Boolean 3D vector */
@Suppress("unused")
@Serializable
@JvmRecord
data class BooleanVector3(val x: Boolean, val y: Boolean, val z: Boolean) {
    companion object {
        val FALSE = BooleanVector3(false, false, false)
        val TRUE = BooleanVector3(true, true, true)
        val UNIT_X = BooleanVector3(true, false, false)
        val UNIT_Y = BooleanVector3(false, true, false)
        val UNIT_Z = BooleanVector3(false, false, true)
    }

    /** Casts to [Vector3]. */
    fun toVector3(
        x: Double = if (this.x) 1.0 else 0.0,
        y: Double = if (this.y) 1.0 else 0.0,
        z: Double = if (this.z) 1.0 else 0.0
    ) = Vector3(x, y, z)

    /** Casts to [IntVector3]. */
    fun toIntVector3(
        x: Int = if (this.x) 1 else 0,
        y: Int = if (this.y) 1 else 0,
        z: Int = if (this.z) 1 else 0
    ) = IntVector3(x, y, z)

}