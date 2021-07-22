package org.openrndr.kartifex


import org.openrndr.kartifex.utils.Scalars
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


/**
 * @author ztellman
 */
abstract class Box<T : Vec<T>, U : Box<T, U>> {
    abstract fun lower(): T
    abstract fun upper(): T
    abstract val isEmpty: Boolean

    protected abstract fun construct(a: T, b: T): U
    protected abstract fun empty(): U
    fun distanceSquared(point: T): Double {
        val l: T = lower().sub(point)
        val u: T = point.sub(upper())
        return u.zip(
            l
        ) { a: Double, b: Double ->
            Scalars.max(
                0.0,
                a,
                b
            )
        }.lengthSquared()
    }

    fun distance(point: T): Double {
        return sqrt(distanceSquared(point))
    }

    fun union(b: U): U {
        if (isEmpty) {
            return b
        } else if (b.isEmpty) {
            @Suppress("UNCHECKED_CAST")
            return this as U
        }
        return construct(
            lower().zip(
                b.lower()
            ) { x: Double, y: Double -> min(x, y) }, upper().zip(
                b.upper()
            ) { x: Double, y: Double -> max(x, y) }
        )
    }

    fun union(v: T): U {
        return if (isEmpty) {
            construct(v, v)
        } else construct(
            lower().zip(v
            ) { a, b -> min(a, b) }, upper().zip(v
            ) { a, b -> max(a, b) }
        )
    }

    fun intersection(b: U): U {
        return if (isEmpty || b.isEmpty || !intersects(b)) {
            empty()
        } else construct(
            lower().zip(b.lower()) { u: Double, v: Double -> max(u, v) },
            upper().zip(b.upper()) { u: Double, v: Double -> min(u, v) }
        )
    }

    open fun intersects(b: U): Boolean {
        return if (isEmpty || b.isEmpty) {
            false
        } else b.upper().sub(lower()).every(NOT_NEGATIVE)
                && upper().sub(b.lower()).every(NOT_NEGATIVE)
    }

    operator fun contains(v: T): Boolean {
        return (v.sub(lower()).every(NOT_NEGATIVE)
                && upper().sub(v).every(NOT_NEGATIVE))
    }

    fun nth(idx: Int): Interval {
        return Interval.interval(lower().nth(idx), upper().nth(idx))
    }

    fun clamp(v: T): T {
        return v.zip(lower(), { a: Double, b: Double -> max(a, b) }).zip(upper(),
            { a: Double, b: Double -> min(a, b) })
    }

    fun size(): T {
        return upper().sub(lower())
    }

    fun normalize(v: T): T {
        return v.sub(lower()).div(size())
    }

    fun lerp(t: Double): T {
        return lower().add(size().mul(t))
    }

    fun lerp(v: T): T {
        return lower().add(size().mul(v))
    }

    fun translate(v: T): U {
        return construct(lower().add(v), upper().add(v))
    }

    fun scale(v: T): U {
        return construct(lower().mul(v), upper().mul(v))
    }

    fun expand(t: Double): U {
        if (isEmpty) {
            @Suppress("UNCHECKED_CAST")
            return this as U
        }
        val nLower: T = lower().map { n -> n - t }
        val nUpper: T = upper().map { n -> n + t }
        return if (nUpper.sub(nLower).every(NOT_NEGATIVE)) construct(nLower, nUpper) else empty()
    }

    fun expand(v: T): U {
        if (isEmpty) {
            @Suppress("UNCHECKED_CAST")
            return this as U
        }
        val nLower: T = lower().sub(v)
        val nUpper: T = upper().add(v)
        return if (nUpper.sub(nLower).every(NOT_NEGATIVE)) construct(nLower, nUpper) else empty()
    }

    override fun hashCode(): Int {
        return if (isEmpty) {
            0
        } else 31 * lower().hashCode() xor upper().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Box<*, *>) {
            val b = other
            return if (isEmpty) {
                b.isEmpty
            } else lower() == b.lower() && upper() == b.upper()
        }
        return false
    }

    override fun toString(): String {
        return "[" + lower() + ", " + upper() + "]"
    }

    companion object {
        fun box(a: Vec2, b: Vec2): Box2 {
            return Box2(a, b)
        }

        fun box(a: Interval, b: Interval): Box2 {
            return Box2(Vec2(a.lo, b.lo), Vec2(a.hi, b.hi))
        }

        fun box(a: Vec3, b: Vec3): Box3 {
            return Box3(a, b)
        }

        fun box(a: Vec4, b: Vec4): Box4 {
            return Box4(a, b)
        }

        private val POSITIVE: DoublePredicate = { d: Double -> d > 0 }
        private val NOT_NEGATIVE: DoublePredicate = { d: Double -> d >= 0 }

        fun <T : Vec<T>, U : Box<T, U>> equals(
            a: Box<T, U>,
            b: Box<T, U>,
            epsilon: Double
        ): Boolean {
            return Vec.equals(
                a.lower(),
                b.lower(),
                epsilon
            ) && Vec.equals(a.upper(), b.upper(), epsilon)
        }
    }
}