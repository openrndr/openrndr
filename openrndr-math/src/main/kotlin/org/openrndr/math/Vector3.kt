package org.openrndr.math

data class Vector3(val x: Double, val y: Double, val z: Double) {

    companion object {
        val ZERO = Vector3(0.0, 0.0, 0.0)
        val UNIT_X = Vector3(1.0, 0.0, 0.0)
        val UNIT_Y = Vector3(0.0, 1.0, 0.0)
        val UNIT_Z = Vector3(0.0, 0.0, 1.0)

        fun fromSpherical(s: Spherical): Vector3 {

            val sinPhiRadius = Math.sin(s.phi) * s.radius

            return Vector3(
                    sinPhiRadius * Math.sin(s.theta),
                    Math.cos(s.phi) * s.radius,
                    sinPhiRadius * Math.cos(s.theta))
        }

    }

    val xyz0: Vector4 get() = Vector4(x, y, z, 0.0)
    val xyz1: Vector4 get() = Vector4(x, y, z, 1.0)

    val xy: Vector2 get() = Vector2(x, y)
    val yx: Vector2 get() = Vector2(y, x)
    val zx: Vector2 get() = Vector2(z, x)
    val xz: Vector2 get() = Vector2(x, z)

    val normalized: Vector3 get() {

        val l = 1.0 / length

        if (l.isNaN() || l.isInfinite()) {
            return ZERO
        }

        return this * l
    }


    private operator fun get(i: Int): Double {
        return when (i) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw RuntimeException("unsupported index")
        }
    }

    operator fun plus(v: Vector3): Vector3 = Vector3(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vector3): Vector3 = Vector3(x - v.x, y - v.y, z - v.z)
    operator fun times(v: Vector3): Vector3 = Vector3(x * v.x, y * v.y, z * v.z)
    operator fun times(s: Double): Vector3 = Vector3(x * s, y * s, z * s)
    operator fun div(s: Double): Vector3 = Vector3(x / s, y / s, z / s)

    infix fun dot(v: Vector3): Double = x * v.x + y * v.y + z * v.z

    infix fun cross(v: Vector3): Vector3 = Vector3(
            y * v.z - z * v.y,
            -(x * v.z - z * v.x),
            x * v.y - y * v.x)

    infix fun projectedOn(v: Vector3): Vector3 {
        return (this dot v) / (v dot v) * v
    }

    val length: Double get() = Math.sqrt(x * x + y * y + z * z)

    fun toDoubleArray(): DoubleArray = doubleArrayOf(x, y, z)



}

operator fun Double.times(v: Vector3): Vector3 {
    return v * this
}
