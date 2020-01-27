package org.openrndr.math

import java.io.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Double precision vector 4
 */
data class Vector4(val x: Double, val y: Double, val z: Double, val w: Double) : Serializable {
    constructor(x: Double) : this(x, x, x, x)

    operator fun invoke(x: Double = this.x, y: Double = this.y, z: Double = this.z, w: Double = this.w) = Vector4(x, y, z, w)

    val xy: Vector2 get() = Vector2(x, y)
    val yx: Vector2 get() = Vector2(y, x)
    val xz: Vector2 get() = Vector2(x, z)
    val yz: Vector2 get() = Vector2(y, z)
    val zx: Vector2 get() = Vector2(z, x)
    val zy: Vector2 get() = Vector2(z, y)

    val xyz: Vector3 get() = Vector3(x, y, z)
    val div: Vector3 get() = Vector3(x / w, y / w, z / w)

    val length get() = sqrt(x * x + y * y + z * z + w * w)
    val squaredLength get() = x * x + y * y + z * z + w * w


    companion object {
        val UNIT_X = Vector4(1.0, 0.0, 0.0, 0.0)
        val UNIT_Y = Vector4(0.0, 1.0, 0.0, 0.0)
        val UNIT_Z = Vector4(0.0, 0.0, 1.0, 0.0)
        val UNIT_W = Vector4(0.0, 0.0, 0.0, 1.0)
        val ZERO = Vector4(0.0, 0.0, 0.0, 0.0)
        val ONE = Vector4(1.0, 1.0, 1.0, 1.0)
    }

    val normalized
        get() =
            length.let {
                Vector4(x / it, y / it, z / it, w / it)
            }

    operator fun unaryMinus() = Vector4(-x, -y, -z, -w)

    operator fun plus(v: Vector4) = Vector4(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun minus(v: Vector4) = Vector4(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun times(v: Vector4) = Vector4(x * v.x, y * v.y, z * v.z, w * v.w)
    operator fun times(s: Double) = Vector4(x * s, y * s, z * s, w * s)
    operator fun div(v: Vector4) = Vector4(x / v.x, y / v.y, z / v.z, w / v.w)
    operator fun div(s: Double) = Vector4(x / s, y / s, z / s, w / s)

    operator fun get(i: Int): Double {
        return when (i) {
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> throw IllegalArgumentException("unsupported index")
        }
    }

    fun toDoubleArray() = doubleArrayOf(x, y, z, w)

}

operator fun Double.times(v: Vector4) = v * this

fun min(a: Vector4, b: Vector4): Vector4 = Vector4(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z), min(a.w, b.w))
fun max(a: Vector4, b: Vector4): Vector4 = Vector4(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z), max(a.w, b.w))
