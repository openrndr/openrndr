package org.openrndr.math

import java.io.Serializable
import kotlin.math.sqrt


/**
 * Integer vector 4
 */
@Suppress("unused")
data class IntVector4(val x: Int, val y: Int, val z: Int, val w: Int) : Serializable {
    companion object {
        val ZERO = IntVector4(0, 0, 0, 0)
        val UNIT_X = IntVector4(1, 0, 0, 0)
        val UNIT_Y = IntVector4(0, 1, 0, 0)
        val UNIT_Z = IntVector4(0, 0, 1, 0)
        val UNIT_W = IntVector4(0, 0, 0, 1)
    }

    val length get() = sqrt(1.0 * x * x + y * y + z * z + w * w)
    val squaredLength get() = x * x + y * y + z * z + w * w
    infix fun dot(right: IntVector4) = x * right.x + y * right.y + z * right.z + w * right.w
    val xy get() = IntVector2(x, y)
    val yx get() = IntVector2(y, x)
    val xx get() = IntVector2(x, x)
    val yy get() = IntVector2(y, y)
    operator fun plus(v: IntVector4) = IntVector4(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: IntVector4) = IntVector4(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun times(d: Int) = IntVector4(x * d, y * d, z * d, w * d)
    operator fun div(d: Int) = IntVector4(x / d, y / d, z / d, w / d)
    val vector4 get() = Vector4(this.x.toDouble(), this.y.toDouble(), this.z.toDouble(), this.w.toDouble())
}

operator fun Int.times(v: IntVector4) = v * this