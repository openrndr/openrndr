package org.openrndr.kartifex

import kotlin.jvm.JvmRecord

@JvmRecord
data class Line2(
    private val ax: Double,
    private val ay: Double,
    private val bx: Double,
    private val by: Double
) : Curve2 {
    override fun transform(m: Matrix3) = line(start().transform(m), end().transform(m))

    override fun isFlat(epsilon: Double) = true

    override fun signedArea() = (ax * by - bx * ay) / 2

    override fun length() = end().sub(start()).length()

    override fun reverse() = Line2(bx, by, ax, ay)

    override fun inflections() = DoubleArray(0)

    override fun position(t: Double) = when (t) {
        0.0 -> start()
        1.0 -> end()
        else -> Vec2(ax + (bx - ax) * t, ay + (by - ay) * t)
    }

    override fun direction(t: Double) = Vec2(bx - ax, by - ay)

    override fun range(tMin: Double, tMax: Double) = line(position(tMin), position(tMax))

    override fun split(t: Double): Array<Curve2> {
        require(!(t <= 0 || t >= 1)) { "t must be within (0,1)" }
        val v = position(t)
        return arrayOf(line(start(), v), line(v, end()))
    }

    override fun nearestPoint(p: Vec2): Double {
        val bSa = end().sub(start())
        val pSa = p.sub(start())
        return Vec.dot(bSa, pSa) / bSa.lengthSquared()
    }

    override fun endpoints(start: Vec2, end: Vec2) = line(start, end)

    override fun start() = Vec2(ax, ay)

    override fun end() = Vec2(bx, by)

    override fun subdivide(error: Double) = arrayOf(start(), end())

    override fun bounds() = Box.box(start(), end())

    /**
     * @param p a point in 2D space
     * @return the distance from this segment to the point
     */
    fun distance(p: Vec2): Double {
        val t = nearestPoint(p)
        return when {
            t <= 0 -> p.sub(start()).length()
            t >= 1 -> p.sub(end()).length()
            else -> p.sub(end().sub(start()).mul(t)).length()
        }
    }

    override fun toString(): String {
        return "Line2(ax=$ax, ay=$ay, bx=$bx, by=$by)"
    }

    companion object {
        fun line(a: Vec2, b: Vec2): Line2 {
            require(a != b) { "segments must have non-zero length $a $b" }
            return Line2(a.x, a.y, b.x, b.y)
        }

        fun line(b: Box2) = Line2(b.lx, b.ly, b.ux, b.uy)
    }
}