package org.openrndr.math

/**
 * Created by voorbeeld on 4/17/17.
 */
data class Vector4(val x:Double, val y:Double, val z:Double, val w:Double) {
    val xyz:Vector3 get() = Vector3(x, y, z)
    val div:Vector3 get() = Vector3(x/w, y/w, z/w)

    val length: Double get() = Math.sqrt(x*x+y*y+z*z+w*w)

    companion object {
        val UNIT_X = Vector4(1.0, 0.0, 0.0, 0.0)
        val UNIT_Y = Vector4(0.0, 1.0, 0.0, 0.0)
        val UNIT_Z = Vector4(0.0, 0.0, 1.0, 0.0)
        val UNIT_W = Vector4(0.0, 0.0, 0.0, 1.0)
        val ZERO = Vector4(0.0, 0.0, 0.0, 0.0)
        val ONE = Vector4(1.0, 1.0, 1.0, 1.0)
    }

    val normalized get() =
        length.let {
           Vector4(x/it, y/it, z/it, w/it)
        }

    operator fun unaryMinus() = Vector4(-x, -y, -z, -w)
    operator fun times(s:Double):Vector4 {
        return Vector4(x*s, y*s, z*s, w*s)
    }
}
