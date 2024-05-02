package org.openrndr.shape

import org.openrndr.math.Matrix33
import org.openrndr.math.Vector3

@Suppress("unused")
class Path3DBuilder {
    var cursor = Vector3.INFINITY
    var anchor = Vector3.INFINITY
    internal var closed = false

    val segments = mutableListOf<Segment3D>()

    fun copy(source: Path3D, connectEpsilon: Double = 1E-6) {
        if (this.segments.isEmpty() && !source.empty) {
            segments.addAll(source.segments)
            anchor = segments.first().start
            cursor = segments.last().end
        } else if (!source.empty) {
            val d = cursor - source.segments.first().start
            if (d.squaredLength > connectEpsilon * connectEpsilon) {
                lineTo(source.segments.first().start)
            }
            for (segment in source.segments) {
                segment(segment)
            }
        }
    }

    fun segment(segment: Segment3D) {
        if (cursor !== Vector3.INFINITY) {
            require((segment.start - cursor).length < 10E-3) {
                "segment is disconnected: cursor: ${cursor}, segment.start: ${segment.start}, distance: ${(cursor - segment.start).length}"
            }
        }
        if (cursor === Vector3.INFINITY) {
            moveTo(segment.start)
        }

        if (segment.linear) {
            lineTo(segment.end)
        } else {
            if (segment.control.size == 1) {
                curveTo(segment.control[0], segment.end)
            } else {
                curveTo(segment.control[0], segment.control[1], segment.end)
            }
        }
    }


    fun moveTo(position: Vector3) {
        cursor = position
        anchor = position
    }

    fun moveTo(x: Double, y: Double, z: Double) = moveTo(Vector3(x, y, z))


    fun moveOrLineTo(position: Vector3) {
        if (anchor === Vector3.INFINITY) {
            moveTo(position)
        } else {
            lineTo(position)
        }
    }

    fun moveOrLineTo(x: Double, y: Double, z: Double) = moveOrLineTo(Vector3(x, y, z))


    fun moveOrCurveTo(control: Vector3, position: Vector3) {
        if (anchor === Vector3.INFINITY) {
            moveTo(position)
        } else {
            curveTo(control, position)
        }
    }

    fun moveOrCurveTo(cx: Double, cy: Double, cz: Double, x: Double, y: Double, z: Double) =
        moveOrCurveTo(Vector3(cx, cy, cz), Vector3(x, y, z))

    fun moveOrCurveTo(control0: Vector3, control1: Vector3, position: Vector3) {
        if (anchor === Vector3.INFINITY) {
            moveTo(position)
        } else {
            curveTo(control0, control1, position)
        }
    }

    fun moveOrCurveTo(
        c0x: Double, c0y: Double, c0z: Double,
        c1x: Double, c1y: Double, c1z: Double,
        x: Double, y: Double, z: Double
    ) = moveOrCurveTo(Vector3(c0x, c0y, c0z), Vector3(c1x, c1y, c1z), Vector3(x, y, z))

    fun lineTo(position: Vector3) {
        val segment = Segment3D(cursor, position)
        segments.add(segment)
        cursor = position
    }

    fun lineTo(x: Double, y: Double, z: Double) = lineTo(Vector3(x, y, z))

    fun curveTo(control: Vector3, position: Vector3) {
        val segment = Segment3D(cursor, control, position)
        segments.add(segment)
        cursor = position
    }

    fun curveTo(cx: Double, cy: Double, cz: Double, x: Double, y: Double, z: Double) =
        curveTo(Vector3(cx, cy, cz), Vector3(x, y, z))

    fun curveTo(control0: Vector3, control1: Vector3, position: Vector3) {
        val segment = Segment3D(cursor, control0, control1, position)
        segments.add(segment)
        cursor = position
    }

    fun curveTo(
        c0x: Double, c0y: Double, c0z: Double,
        c1x: Double, c1y: Double, c1z: Double,
        x: Double, y: Double, z: Double
    ) = curveTo(Vector3(c0x, c0y, c0z), Vector3(c1x, c1y, c1z), Vector3(x, y, z))

    fun close() {
        closed = true
    }

    fun reverse() {
        segments.forEachIndexed { index, segment ->
            segments[index] = segment.reverse
        }
        segments.reverse()
    }

    fun continueTo(end: Vector3, tangentScale: Double = 1.0) {
        if (segments.isNotEmpty()) {
            val last = segments.last()
            val delta = last.control.last() - last.end
            curveTo(last.end - delta * tangentScale, end)
        } else {
            curveTo(cursor, end)
        }
    }

    fun continueTo(x: Double, y: Double, z: Double, tangentScale: Double = 1.0) =
        continueTo(Vector3(x, y, z), tangentScale)

    fun continueTo(control: Vector3, end: Vector3, tangentScale: Double = 1.0) {
        if (segments.isNotEmpty()) {
            val last = segments.last()
            val delta = last.control.last() - last.end
            curveTo(last.end - delta * tangentScale, control, end)
        } else {
            curveTo(cursor, control, end)
        }
    }

    fun continueTo(cx: Double, cy: Double, cz: Double, x: Double, y: Double, z: Double, tangentScale: Double = 1.0) =
        continueTo(Vector3(cx, cy, cz), Vector3(x, y, z), tangentScale)

    /**
     * @param sr sphere radius
     * @param angle
     * @param largeArcFlag
     * @param sweepFlag
     * @param tx
     * @param ty
     * @param tz
     */
    fun arcTo(
        sr: Double,
        angle: Double,
        largeArcFlag: Boolean,
        sweepFlag: Boolean,
        tx: Double,
        ty: Double,
        tz: Double
    ) {
        val d = Vector3(tx, ty, tz) - cursor

        // find a plane P that contains the triangle (0, cursor, target)
        val xn = d.normalized
        val zn = Vector3(tx, ty, tz).normalized.cross(cursor.normalized)
        val yn = zn.cross(xn).normalized

        // construct an othonormal basis
        val mat = Matrix33.fromColumnVectors(xn, yn, zn)

        // invert the basis (this is just a transpose)
        val imat = mat.transposed

        // find d inside xy-plane
        val d2 = (imat * d)

        // construct the arc in xy-plane and transform back to P
        val s2 = contour {
            moveTo(0.0, 0.0)
            arcTo(sr, sr, angle, largeArcFlag, sweepFlag, d2.x, d2.y)
        }.segments.map {
            Segment3D(mat * it.start.xy0 + cursor, it.control.map { mat * it.xy0 + cursor }, mat * it.end.xy0 + cursor)
        }

        // emit segments
        for (s in s2) {
            segment(s)
        }

        cursor = Vector3(tx, ty, tz)
    }


    fun arcTo(
        cr: Double,
        angle: Double,
        largeArcFlag: Boolean,
        sweepFlag: Boolean,
        end: Vector3
    ) =
        arcTo(cr, angle, largeArcFlag, sweepFlag, end.x, end.y, end.z)

}

fun path3D(f: Path3DBuilder.() -> Unit): Path3D {
    val cb = Path3DBuilder()
    cb.f()
    return Path3D(cb.segments, cb.closed)
}

