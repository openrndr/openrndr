package org.openrndr.kartifex

import org.openrndr.utils.hash
import kotlin.jvm.JvmRecord

@JvmRecord
data class Vec4(val x: Double, val y: Double, val z: Double, val w: Double) :
    Vec<Vec4> {
    override fun map(f: DoubleUnaryOperator): Vec4 {
        return Vec4(f(x), f(y), f(z), f(w))
    }

    override fun reduce(f: DoubleBinaryOperator, init: Double): Double {
        return f(f(f(f(init, x), y), z), w)
    }

    override fun reduce(f: DoubleBinaryOperator): Double {
        return f(f(f(x, y), z), w)
    }

    override fun zip(v: Vec4, f: DoubleBinaryOperator): Vec4 {
        return Vec4(f(x, v.x), f(y, v.y), f(z, v.z), f(w, v.w))
    }

    override fun every(f: DoublePredicate): Boolean {
        return f(x) && f(y) && f(z) && f(w)
    }

    override fun any(f: DoublePredicate): Boolean {
        return f(x) || f(y) || f(z) || f(w)
    }

    override fun nth(idx: Int): Double {
        return when (idx) {
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> throw IndexOutOfBoundsException()
        }
    }

    override fun dim(): Int {
        return 4
    }

    override fun array(): DoubleArray {
        return doubleArrayOf(x, y, z, w)
    }

    fun vec3(): Vec3 {
        return Vec3(x, y, z)
    }

    fun vec2(): Vec2 {
        return Vec2(x, y)
    }

    override fun hashCode(): Int {
        return hash(x, y, z, w)
    }

    override fun equals(other: Any?): Boolean {
        if (other is Vec4) {
            val v = other
            return v.x == x && v.y == y && v.z == z && v.w == w
        }
        return false
    }

    override fun toString(): String {
        return "[x=$x, y=$y, z=$z, w=$w]"
    }

    override operator fun compareTo(other: Vec4): Int {
        return COMPARATOR.compare(this, other)
    }

    companion object {
        val ORIGIN = Vec4(0.0, 0.0, 0.0, 0.0)
        val X_AXIS = Vec4(1.0, 0.0, 0.0, 0.0)
        val Y_AXIS = Vec4(0.0, 1.0, 0.0, 0.0)
        val Z_AXIS = Vec4(0.0, 0.0, 1.0, 0.0)
        val W_AXIS = Vec4(0.0, 0.0, 0.0, 1.0)
        val COMPARATOR = compareBy<Vec4>({ v -> v.x }, { v -> v.y }, { v -> v.z }, { v -> v.w })
    }
}