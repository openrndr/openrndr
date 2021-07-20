package org.openrndr.kartifex

import io.lacuna.artifex.utils.Scalars
import org.openrndr.utils.Hashes
import kotlin.math.max
import kotlin.math.min

class Interval(a: Double, b: Double) {
    var lo = 0.0
    var hi = 0.0

    /// predicates
    fun intersects(i: Interval): Boolean {
        return hi > i.lo && i.hi > lo
    }

    operator fun contains(n: Double): Boolean {
        return !isEmpty && lo <= n && n <= hi
    }

    operator fun contains(i: Interval): Boolean {
        return !isEmpty && contains(i.lo) && contains(i.hi)
    }

    val isEmpty: Boolean
        get() = this === EMPTY

    ///
    fun expand(n: Double): Interval {
        return if (size() + n * 2 < 0) EMPTY else Interval(lo - n, hi + n)
    }

    fun map(f: DoubleUnaryOperator): Interval {
        return Interval(f(lo), f(hi))
    }

    fun add(i: Interval): Interval {
        return if (isEmpty || i.isEmpty) EMPTY else Interval(lo + i.lo, hi + i.hi)
    }

    fun sub(i: Interval): Interval {
        return if (isEmpty || i.isEmpty) EMPTY else Interval(lo - i.lo, hi - i.hi)
    }

    fun mul(i: Interval): Interval {
        return if (isEmpty || i.isEmpty) EMPTY else Interval(lo * hi, i.lo * i.hi).union(Interval(lo * i.hi, i.lo * hi))
    }

    operator fun div(i: Interval): Interval {
        return if (i.lo == 0.0 && i.hi == 0.0) {
            Interval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        } else if (i.lo == 0.0) {
            mul(Interval(1 / i.hi, Double.POSITIVE_INFINITY))
        } else if (i.hi == 0.0) {
            mul(Interval(Double.NEGATIVE_INFINITY, 1 / i.lo))
        } else {
            mul(Interval(1 / i.hi, 1 / i.lo))
        }
    }

    ///
    fun union(i: Interval): Interval {
        return if (isEmpty) i else Interval(min(lo, i.lo), max(hi, i.hi))
    }

    fun union(n: Double): Interval {
        return if (isEmpty) Interval(n, n) else Interval(min(lo, n), max(hi, n))
    }

    fun intersection(i: Interval): Interval {
        return if (isEmpty || i.isEmpty || !intersects(i)) {
            EMPTY
        } else Interval(max(lo, i.lo), min(hi, i.hi))
    }

    fun normalize(n: Double): Double {
        return if (n == hi) 1.0 else (n - lo) / size()
    }

    fun normalize(i: Interval): Interval {
        return Interval(normalize(i.lo), normalize(i.hi))
    }

    fun lerp(t: Double): Double {
        return if (t == 1.0) hi else Scalars.lerp(lo, hi, t)
    }

    fun lerp(i: Interval): Interval {
        return Interval(lerp(i.lo), lerp(i.hi))
    }

    fun size(): Double {
        return hi - lo
    }

    ///
    override fun hashCode(): Int {
        return Hashes.hash(lo, hi)
    }

    override fun equals(other: Any?): Boolean {
        return if (other === this) {
            true
        } else if (other is Interval) {
            val o = other
            lo == o.lo && hi == o.hi
        } else {
            false
        }
    }

    override fun toString(): String {
        return "[$lo, $hi]"
    }

    companion object {
        val EMPTY = Interval(Double.NaN, Double.NaN)
        fun interval(a: Double, b: Double): Interval {
            return Interval(a, b)
        }
    }

    init {
        if (a < b) {
            lo = a
            hi = b
        } else {
            lo = b
            hi = a
        }
    }
}