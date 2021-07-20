package org.openrndr.kartifex

import io.lacuna.artifex.utils.Scalars

class Path2 {
    private val curves: Array<Curve2>
    private val bounds: Box2
    val isRing: Boolean

    internal constructor(ring: Ring2) {
        curves = ring.curves
        bounds = ring.bounds
        isRing = true
    }

    constructor(cs: Iterable<Curve2>) {
        val l = ArrayDeque<Curve2>()
        var bounds: Box2 = Box2.EMPTY
        for (a in cs) {
            for (b in a.split(a.inflections())) {
                l.addLast(b)
                bounds = bounds.union(b.start()).union(b.end())
            }
        }
        this.bounds = bounds
        isRing = Vec.equals(l.first().start(), l.last().end(), Scalars.EPSILON)
        curves = l.toTypedArray()
        for (i in 0 until curves.size - 1) {
            curves[i] = curves[i].endpoints(curves[i].start(), curves[i + 1].start())
        }
        if (isRing) {
            val lastIdx = curves.size - 1
            curves[lastIdx] = curves[lastIdx].endpoints(curves[lastIdx].start(), curves[0].start())
        }
    }

    fun reverse(): Path2 {
        return Path2(curves.map {
            it.reverse()
        }.reversed())
    }

    fun curves(): Array<Curve2> {
        return curves
    }

    fun bounds(): Box2 {
        return bounds
    }

    fun vertices(error: Double): Iterable<Vec2> {
        val result: MutableList<Vec2> = mutableListOf()
        for (c in curves) {
            val segments: Array<Vec2> = c.subdivide(error)
            if (result.isEmpty()) {
                result.addAll(segments)
            } else {
                val t1: Vec2 = result[result.size - 1].sub(result[result.size - 2]).norm()
                val t2: Vec2 = segments[1].sub(segments[0]).norm()
                if (Vec.equals(t1, t2, Scalars.EPSILON)) {
                    result.removeAt(result.size - 1)
                }
                for (i in 1 until segments.size) {
                    result.add(segments[i])
                }
            }
        }
        return result
    }

    companion object {
        fun of(vararg curves: Curve2): Path2 {
            return Path2(curves.toList())
        }

        fun linear(vararg vertices: Vec2): Path2 {
            val segments = mutableListOf<Curve2>()
            for (i in 0 until vertices.size - 1) {
                val a = vertices[i]
                val b = vertices[i + 1]
                if (!Vec.equals(a, b, Scalars.EPSILON)) {
                    segments.add(Line2.line(vertices[i], vertices[i + 1]))
                }
            }
            return Path2(segments)
        }
    }
}
