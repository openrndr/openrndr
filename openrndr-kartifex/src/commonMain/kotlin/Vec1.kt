package org.openrndr.kartifex

import org.openrndr.utils.Hashes

class Vec1(val x: Double) : Vec<Vec1> {
    fun vec2(y: Double): Vec2 {
        return Vec2(x, y)
    }

    fun vec3(y: Double, z: Double): Vec3 {
        return Vec3(x, y, z)
    }

    fun vec3(v: Vec2): Vec3 {
        return Vec3(x, v.x, v.y)
    }

    fun vec4(y: Double, z: Double, w: Double): Vec4 {
        return Vec4(x, y, z, w)
    }

    fun vec4(v: Vec3): Vec4 {
        return Vec4(x, v.x, v.y, v.z)
    }

    override fun map(f: DoubleUnaryOperator): Vec1 {
        return Vec1(f(x))
    }

    override fun reduce(f: DoubleBinaryOperator, init: Double): Double {
        return f(init, x)
    }

    override fun reduce(f: DoubleBinaryOperator): Double {
        throw IllegalStateException()
    }

    override fun zip(v: Vec1, f: DoubleBinaryOperator): Vec1 {
        return Vec1(f(x, v.x))
    }

    override fun every(f: DoublePredicate): Boolean {
        return f(x)
    }

    override fun any(f: DoublePredicate): Boolean {
        return f(x)
    }

    override fun nth(idx: Int): Double {
        return if (idx == 0) {
            x
        } else {
            throw IndexOutOfBoundsException()
        }
    }

    override fun dim(): Int {
        return 1
    }

    override fun array(): DoubleArray {
        return doubleArrayOf(x)
    }

    override fun hashCode(): Int {
        return Hashes.hash(x)
    }

    override fun equals(other: Any?): Boolean {
        if (other is Vec1) {
            return x == other.x
        }
        return false
    }

    override operator fun compareTo(other: Vec1): Int {
        return x.compareTo(other.x)
    }

    override fun toString(): String {
        return "Vec1(x=$x)"
    }
}
