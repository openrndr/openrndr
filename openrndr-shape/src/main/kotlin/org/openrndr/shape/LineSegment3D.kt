package org.openrndr.shape

import org.openrndr.math.Vector3
import kotlin.math.max
import kotlin.math.min

class LineSegment3D(val start: Vector3, val end: Vector3) {

    constructor(x0: Double, y0: Double, z0:Double, x1: Double, y1: Double, z1:Double) : this(Vector3(x0, y0, z0), Vector3(x1, y1, z1))

    val direction get() = (end - start)

    fun nearest(query: Vector3): Vector3 {
        val l2 = end.minus(start).squaredLength
        if (l2 == 0.0) return start

        var t = ((query.x - start.x) * (end.x - start.x) + (query.y - start.y) * (end.y - start.y) + (query.z - start.z)* (end.z - start.z) ) / l2
        t = max(0.0, min(1.0, t))
        return Vector3(start.x + t * (end.x - start.x),start.y + t * (end.y - start.y), start.z + t * (end.z - start.z))
    }

    fun distance(query: Vector3): Double {
        return (nearest(query)-query).length
    }

    fun squaredDistance(query: Vector3): Double {
        return (nearest(query)-query).squaredLength
    }


    fun sub(t0: Double, t1: Double): LineSegment3D {
        var z0 = t0
        var z1 = t1

        if (t0 > t1) {
            z1 = t0
            z0 = t1
        }
        return when {
            z0 == 0.0 -> split(z1)[0]
            z1 == 1.0 -> split(z0)[1]
            else -> split(z0)[1].split(org.openrndr.math.map(z0, 1.0, 0.0, 1.0, z1))[0]
        }
    }

    fun split(t: Double): Array<LineSegment3D> {
        val u = t.coerceIn(0.0, 1.0)
        val cut = start + (end.minus(start) * u)
        return arrayOf(LineSegment3D(start, cut), LineSegment3D(cut, end))
    }

    fun position(t: Double) = start + (end.minus(start) * t)

    val path: Path3D
        get() = Path3D.fromPoints(listOf(start, end), false)

}