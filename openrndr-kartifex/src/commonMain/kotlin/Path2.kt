package org.openrndr.kartifex

import org.openrndr.kartifex.utils.SCALAR_EPSILON
import kotlin.jvm.JvmRecord

@JvmRecord
data class Path2(
    private val curves: Array<Curve2>,
    private val bounds: Box2,
    val isRing: Boolean
) {
    fun reverse() = Path2(curves.map {
        it.reverse()
    }.reversed())

    fun curves(): Array<Curve2> = curves

    fun bounds(): Box2 = bounds

    companion object {
        fun of(vararg curves: Curve2) = Path2(curves.toList())

        fun linear(vararg vertices: Vec2): Path2 {
            val segments = mutableListOf<Curve2>()
            for (i in 0 until vertices.size - 1) {
                val a = vertices[i]
                val b = vertices[i + 1]
                if (!Vec.equals(a, b, SCALAR_EPSILON)) {
                    segments.add(Line2.line(vertices[i], vertices[i + 1]))
                }
            }
            return Path2(segments)
        }
    }
}


fun Path2(cs: Iterable<Curve2>): Path2 {
    val l = ArrayDeque<Curve2>()
    var bounds = Box2.EMPTY
    for (a in cs) {
        for (b in a.split(a.inflections())) {
            l.addLast(b)
            bounds = bounds.union(b.start()).union(b.end())
        }
    }
    val isRing = Vec.equals(l.first().start(), l.last().end(), SCALAR_EPSILON)
    val curves = l.toTypedArray()
    for (i in 0 until curves.size - 1) {
        curves[i] = curves[i].endpoints(curves[i].start(), curves[i + 1].start())
    }
    if (isRing) {
        val lastIdx = curves.size - 1
        curves[lastIdx] = curves[lastIdx].endpoints(curves[lastIdx].start(), curves[0].start())
    }

    return Path2(curves, bounds, isRing)
}
