package org.openrndr.kartifex


import org.openrndr.kartifex.utils.Scalars
import kotlin.math.*

interface Vec<T : Vec<T>> : Comparable<T> {
    companion object {
        val NEGATE: DoubleUnaryOperator = { n: Double -> -n }
        val ADD: DoubleBinaryOperator = { a: Double, b: Double -> a + b }
        val MUL: DoubleBinaryOperator = { a: Double, b: Double -> a * b }
        val SUB: DoubleBinaryOperator = { a: Double, b: Double -> a - b }
        val DIV: DoubleBinaryOperator = { a: Double, b: Double -> a / b }
        val DELTA: DoubleBinaryOperator = { a: Double, b: Double -> abs(a - b) }
        val MIN: DoubleBinaryOperator = { a: Double, b: Double -> min(a, b) }
        val MAX: DoubleBinaryOperator = { a: Double, b: Double -> max(a, b) }


        fun from(ary: DoubleArray) =
            when (ary.size) {
                2 -> Vec2(ary[0], ary[1])
                3 -> Vec3(ary[0], ary[1], ary[2])
                4 -> Vec4(ary[0], ary[1], ary[2], ary[3])
                else -> error("ary must have a length in [1,4]")
            }

        fun <T : Vec<T>> dot(a: T, b: T): Double {
            return a.mul(b).reduce(ADD)
        }

        fun dot(a: Vec2, b: Vec2): Double {
            return a.x * b.x + a.y * b.y
        }

        fun <T : Vec<T>> lerp(a: T, b: T, t: Double): T {
            return a.add(b.sub(a).mul(t))
        }

        fun lerp(a: Vec2, b: Vec2, t: Double): Vec2 {
            return Vec2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }

        fun <T : Vec<T>> lerp(a: T, b: T, t: T): T {
            return a.add(b.sub(a).mul(t))
        }

        fun lerp(
            a: Vec2,
            b: Vec2,
            t: Vec2
        ): Vec2 {
            return Vec2(a.x + (b.x - a.x) * t.x, a.y + (b.y - a.y) * t.y)
        }

        fun <T : Vec<T>> equals(a: T, b: T, tolerance: Double): Boolean {
            return a.zip(b, DELTA).every { i: Double -> i <= tolerance }
        }
    }

    fun map(f: DoubleUnaryOperator): T

    fun reduce(f: DoubleBinaryOperator, init: Double): Double

    fun reduce(f: DoubleBinaryOperator): Double

    fun zip(v: T, f: DoubleBinaryOperator): T

    fun every(f: DoublePredicate): Boolean

    fun any(f: DoublePredicate): Boolean

    fun nth(idx: Int): Double

    fun dim(): Int

    fun array(): DoubleArray

    fun negate(): T {
        return map(NEGATE)
    }

    fun add(v: T): T {
        return zip(v, ADD)
    }

    fun add(n: Double): T {
        return map { i: Double -> i + n }
    }

    fun sub(v: T): T {
        return zip(v, SUB)
    }

    fun sub(n: Double): T {
        return map { i: Double -> i - n }
    }

    fun mul(v: T): T {
        return zip(v, MUL)
    }

    fun mul(k: Double): T {
        return map { i: Double -> i * k }
    }

    operator fun div(v: T): T {
        return zip(v, DIV)
    }

    operator fun div(k: Double): T {
        return mul(1.0 / k)
    }

    fun abs(): T {
        return map { a: Double -> abs(a) }
    }

    fun lengthSquared(): Double {
        @Suppress("UNCHECKED_CAST")
        return dot(this as T, this)
    }

    fun length(): Double {
        return sqrt(lengthSquared())
    }

    fun norm(): T {
        val l = lengthSquared()
        return if (l == 1.0) {
            @Suppress("UNCHECKED_CAST")
            this as T
        } else {
            div(sqrt(l))
        }
    }

    fun pseudoNorm(): T {
        val exponent: Int = Scalars.getExponent(reduce(MAX))
        return if (exponent < -8.0 || exponent > 8.0)
            mul(2.0.pow(-exponent.toDouble()))
        else {
            @Suppress("UNCHECKED_CAST")
            this as T
        }

    }

    fun clamp(min: Double, max: Double): T {
        return map { x -> x.coerceIn(min, max) }
    }

    fun clamp(min: T, max: T): T {
        return zip(min, MAX).zip(max, MIN)
    }
}