package org.openrndr.kartifex

import org.openrndr.utils.Hashes
import kotlin.jvm.JvmRecord
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@JvmRecord
data class Vec2(val x: Double, val y: Double) : Vec<Vec2> {


    override fun map(f: DoubleUnaryOperator): Vec2 {
        return Vec2(f(x), f(y))
    }

    override fun reduce(f: DoubleBinaryOperator, init: Double): Double {
        return f(f(x, y), init)
    }

    override fun reduce(f: DoubleBinaryOperator): Double {
        return f(x, y)
    }

    override fun zip(v: Vec2, f: DoubleBinaryOperator): Vec2 {
        return Vec2(f(x, v.x), f(y, v.y))
    }

    override fun every(f: DoublePredicate): Boolean {
        return f(x) && f(y)
    }

    override fun any(f: DoublePredicate): Boolean {
        return f(x) || f(y)
    }

    override fun nth(idx: Int): Double {
        return when (idx) {
            0 -> x
            1 -> y
            else -> throw IndexOutOfBoundsException()
        }
    }

    override fun dim(): Int {
        return 2
    }

    override fun array(): DoubleArray {
        return doubleArrayOf(x, y)
    }

    fun add(x: Double, y: Double): Vec2 {
        return Vec2(this.x + x, this.y + y)
    }

    fun sub(x: Double, y: Double): Vec2 {
        return Vec2(this.x - x, this.y - y)
    }

    fun swap(): Vec2 {
        return Vec2(y, x)
    }

    fun vec3(z: Double): Vec3 {
        return Vec3(x, y, z)
    }

    fun vec4(z: Double, w: Double): Vec4 {
        return Vec4(x, y, z, w)
    }

    fun vec4(v: Vec2): Vec4 {
        return Vec4(x, y, v.x, v.y)
    }

    fun transform(m: Matrix3): Vec2 {
        return m.transform(this)
    }

    /**
     * @return a rotated vector
     */
    fun rotate(radians: Double): Vec2 {
        val s: Double = sin(radians)
        val c: Double = cos(radians)
        return Vec2(c * x + -s * y, s * x + c * y)
    }

    fun polar2(): Polar2 {
        return Polar2(atan2(y, x), length())
    }

    override fun hashCode(): Int {
        return Hashes.hash(x, y)
    }

    override fun equals(other: Any?): Boolean {
        if (other is Vec2) {
            val v = other
            return v.x == x && v.y == y
        }
        return false
    }

    override operator fun compareTo(other: Vec2): Int {
        return COMPARATOR.compare(this, other)
    }

    override fun add(v: Vec2): Vec2 {
        return Vec2(x + v.x, y + v.y)
    }

    override fun add(n: Double): Vec2 {
        return Vec2(x + n, y + n)
    }

    override fun negate(): Vec2 {
        return Vec2(-x, -y)
    }

    override fun sub(v: Vec2): Vec2 {
        return Vec2(x - v.x, y - v.y)
    }

    override fun sub(n: Double): Vec2 {
        return Vec2(x - n, y - n)
    }

    override fun mul(v: Vec2): Vec2 {
        return Vec2(x * v.x, y * v.y)
    }

    override fun mul(k: Double): Vec2 {
        return Vec2(x * k, y * k)
    }

    override operator fun div(v: Vec2): Vec2 {
        return Vec2(x / v.x, y / v.y)
    }

    override fun toString(): String {
        return "Vec2(x=$x, y=$y)"
    }

    companion object {
        val COMPARATOR = compareBy<Vec2>({ v -> v.x }, { v -> v.y })

        val ORIGIN = Vec2(0.0, 0.0)
        val X_AXIS = Vec2(1.0, 0.0)
        val Y_AXIS = Vec2(0.0, 1.0)

        fun cross(a: Vec2, b: Vec2): Double {
            return a.x * b.y - a.y * b.x
        }

        /**
         * @return the clockwise angle between the two vectors
         */
        fun angleBetween(oa: Vec2, ob: Vec2): Double {

            val a = oa.pseudoNorm()
            val b = ob.pseudoNorm()

            // from section 12 of https://people.eecs.berkeley.edu/~wkahan/Mindless.pdf
            var theta: Double = atan2(cross(a, b), Vec.dot(a, b))
            if (theta > 0) {
                theta -= PI * 2
            }
            return theta
        }

        /**
         * @return whether `b` sits between the vectors `a` and `b`
         */
        fun between(a: Vec2, b: Vec2, c: Vec2): Boolean {
            return cross(a, b) * cross(c, b) < 0
        }
    }
}