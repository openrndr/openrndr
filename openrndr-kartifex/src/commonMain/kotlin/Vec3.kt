package org.openrndr.kartifex

import org.openrndr.utils.Hashes

class Vec3(val x: Double, val y: Double, val z: Double) : Vec<Vec3> {
    override fun map(f: DoubleUnaryOperator): Vec3 {
        return Vec3(f(x), f(y), f(z))
    }

    override fun reduce(f: DoubleBinaryOperator, init: Double): Double {
        return f(f(init, x), f(y, z))
    }

    override fun reduce(f: DoubleBinaryOperator): Double {
        return f(f(x, y), z)
    }

    override fun zip(v: Vec3, f: DoubleBinaryOperator): Vec3 {
        return Vec3(f(x, v.x), f(y, v.y), f(z, v.z))
    }

    override fun every(f: DoublePredicate): Boolean {
        return f(x) && f(y) && f(z)
    }

    override fun any(f: DoublePredicate): Boolean {
        return f(x) || f(y) || f(z)
    }

    override fun nth(idx: Int): Double {
        return when (idx) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw IndexOutOfBoundsException()
        }
    }

    override fun dim(): Int {
        return 3
    }

    override fun array(): DoubleArray {
        return doubleArrayOf(x, y, z)
    }

    fun vec2(): Vec2 {
        return Vec2(x, y)
    }

    fun vec4(w: Double): Vec4 {
        return Vec4(x, y, z, w)
    }

    override fun hashCode(): Int {
        return Hashes.hash(x, y, z)
    }

    override fun equals(other: Any?): Boolean {
        if (other is Vec3) {
            val v = other
            return v.x == x && v.y == y && v.z == z
        }
        return false
    }

    override fun toString(): String {
        return "[x=$x, y=$y, z=$z]"
    }

    override operator fun compareTo(other: Vec3): Int {
        return COMPARATOR.compare(this, other)
    }

    companion object {
        val ORIGIN = Vec3(0.0, 0.0, 0.0)
        val X_AXIS = Vec3(1.0, 0.0, 0.0)
        val Y_AXIS = Vec3(0.0, 1.0, 0.0)
        val Z_AXIS = Vec3(0.0, 0.0, 1.0)
        val COMPARATOR = compareBy<Vec3>({ v -> v.x }, { v -> v.y }, { v -> v.z })
        fun cross(a: Vec3, b: Vec3): Vec3 {
            return Vec3(
                a.y * b.z - a.z * b.y,
                a.x * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
            )
        }
    }
}