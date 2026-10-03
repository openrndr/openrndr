package org.openrndr.kartifex.utils.regions

import org.openrndr.kartifex.Curve2
import org.openrndr.kartifex.Interval
import org.openrndr.kartifex.Vec2

/**
 * Represents an arc, which is a collection of curves that form a continuous shape.
 * This class implements the `List` interface and delegates behavior to the underlying list of `Curve2` objects.
 *
 * @property list The list of curves that constitute the arc.
 */
internal class Arc(val list: List<Curve2> = emptyList()) : List<Curve2> by list {

    private var length = Double.NaN
    private var area = Double.NaN
    fun length(): Double {
        if (length.isNaN()) {
            length = sumOf { c: Curve2 ->
                c.end().sub(c.start()).length()
            }
        }
        return length
    }

    fun signedArea(): Double {
        if (area.isNaN()) {
            area =
                sumOf { obj: Curve2 -> obj.signedArea() }
        }
        return area
    }

    fun head(): Vec2 {
        return first().start()
    }

    fun tail(): Vec2 {
        return last().end()
    }

    fun position(t: Double): Vec2 {
        val length = length()
        var offset = 0.0
        val threshold = length * t
        for (c in this) {
            val l: Double = c.end().sub(c.start()).length()
            val i = Interval(offset, offset + l)
            if (i.contains(threshold)) {
                return c.position(i.normalize(threshold))
            }
            offset = i.hi
        }
        throw IllegalStateException()
    }

    /**
     * @param t a parametric point along the whole arc, within [0, 1]
     * @return the (unnormalized) tangent direction at that point
     */
    fun direction(t: Double): Vec2 {
        val length = length()
        var offset = 0.0
        val threshold = length * t
        for (c in this) {
            val l: Double = c.end().sub(c.start()).length()
            val i = Interval(offset, offset + l)
            if (i.contains(threshold)) {
                return c.direction(i.normalize(threshold))
            }
            offset = i.hi
        }
        throw IllegalStateException()
    }

    fun reverse(): Arc {
        return Arc(reversed().map { it.reverse() }.toMutableList())
    }

    fun vertices(): List<Vec2> {
        return listOf(head()) + map { it.end() }
    }
}
