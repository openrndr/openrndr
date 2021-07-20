package org.openrndr.kartifex.utils.regions

import org.openrndr.kartifex.Curve2
import org.openrndr.kartifex.Interval
import org.openrndr.kartifex.Vec2

internal class Arc(val list:MutableList<Curve2> = mutableListOf()) : MutableList<Curve2> by list {

    private var hashProxy = 0.5

    private var length = Double.NaN
    private var area = Double.NaN
    fun length(): Double {
        if (length.isNaN()) {
            length = map {  c: Curve2 ->
                c.end().sub(c.start()).length()
            }.sum()
        }
        return length
    }

    fun signedArea(): Double {
        if (area.isNaN()) {
            area =
                map{  obj: Curve2 -> obj.signedArea() }
                    .sum()
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
            val i: Interval = Interval(offset, offset + l)
            if (i.contains(threshold)) {
                return c.position(i.normalize(threshold))
            }
            offset = i.hi
        }
        throw IllegalStateException()
    }

    fun reverse(): Arc {
        return Arc(reversed().map { it.reverse() }.toMutableList())
    }

    fun vertices(): List<Vec2> {
        return listOf(head()) + map { it.end()}
    }

    override fun hashCode(): Int {
        return hashProxy.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }
}