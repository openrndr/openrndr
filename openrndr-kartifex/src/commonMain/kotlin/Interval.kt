package org.openrndr.kartifex


import org.openrndr.kartifex.utils.lerp
import org.openrndr.utils.hash
import kotlin.math.max
import kotlin.math.min

class Interval(a: Double, b: Double) {
    var lo = 0.0
    var hi = 0.0

    /// predicates
    fun intersects(i: Interval) = hi > i.lo && i.hi > lo

    operator fun contains(n: Double) = !isEmpty && lo <= n && n <= hi

    operator fun contains(i: Interval) = !isEmpty && contains(i.lo) && contains(i.hi)

    val isEmpty
        get() = this === EMPTY

    ///
    fun expand(n: Double) = if (size() + n * 2 < 0) EMPTY else Interval(lo - n, hi + n)

    fun map(f: DoubleUnaryOperator) = Interval(f(lo), f(hi))

    fun add(i: Interval) = if (isEmpty || i.isEmpty) EMPTY else Interval(lo + i.lo, hi + i.hi)

    fun sub(i: Interval) = if (isEmpty || i.isEmpty) EMPTY else Interval(lo - i.lo, hi - i.hi)

    fun mul(i: Interval) =
        if (isEmpty || i.isEmpty) {
            EMPTY
        } else {
            Interval(lo * hi, i.lo * i.hi).union(Interval(lo * i.hi, i.lo * hi))
        }

    operator fun div(i: Interval) = when {
        i.lo == 0.0 && i.hi == 0.0 -> Interval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        i.lo == 0.0 -> mul(Interval(1 / i.hi, Double.POSITIVE_INFINITY))
        i.hi == 0.0 -> mul(Interval(Double.NEGATIVE_INFINITY, 1 / i.lo))
        else -> mul(Interval(1 / i.hi, 1 / i.lo))
    }

    ///
    fun union(i: Interval) = if (isEmpty) i else Interval(min(lo, i.lo), max(hi, i.hi))

    fun union(n: Double) = if (isEmpty) Interval(n, n) else Interval(min(lo, n), max(hi, n))

    fun intersection(i: Interval): Interval {
        return if (isEmpty || i.isEmpty || !intersects(i)) {
            EMPTY
        } else Interval(max(lo, i.lo), min(hi, i.hi))
    }

    fun normalize(n: Double) = if (n == hi) 1.0 else (n - lo) / size()

    fun lerp(t: Double) = if (t == 1.0) hi else lerp(lo, hi, t)

    fun lerp(i: Interval) = Interval(lerp(i.lo), lerp(i.hi))

    fun size() = hi - lo

    ///
    override fun hashCode() = hash(lo, hi)

    override fun equals(other: Any?) = when {
        other === this -> true
        other is Interval -> {
            val o = other
            lo == o.lo && hi == o.hi
        }
        else -> false
    }

    override fun toString(): String {
        return "Interval(lo=$lo, hi=$hi)"
    }


    companion object {
        val EMPTY = Interval(Double.NaN, Double.NaN)
        fun interval(a: Double, b: Double) = Interval(a, b)
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