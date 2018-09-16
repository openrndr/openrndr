package org.openrndr.shape

import org.openrndr.math.Vector2

class ShapeBuilder {
    internal val contours = mutableListOf<ShapeContour>()

    fun contour(shapeContour: ShapeContour) {
        contours.add(if (contours.size == 0) shapeContour.counterClockwise else shapeContour.clockwise)
    }

    fun contour(f: ContourBuilder.() -> Unit) {
        val cb = ContourBuilder()
        cb.f()
        val c = ShapeContour(cb.segments, cb.closed)
        contours.add(if (contours.size == 0) c.counterClockwise else c.clockwise)
    }
}

fun shape(f: ShapeBuilder.() -> Unit): Shape {
    val sb = ShapeBuilder()
    sb.f()
    return Shape(sb.contours)
}

fun contour(f: ContourBuilder.() -> Unit): ShapeContour {
    val cb = ContourBuilder()
    cb.f()
    return ShapeContour(cb.segments, cb.closed)
}

@Suppress("unused")
class ContourBuilder {
    var cursor = Vector2.INFINITY
    var anchor = Vector2.INFINITY
    internal var closed = false

    val segments = mutableListOf<Segment>()
    fun moveTo(position: Vector2) {
        cursor = position
        anchor = position
    }

    fun moveOrLineTo(position:Vector2) {
        if (anchor === Vector2.INFINITY) {
            moveTo(position)
        } else {
            lineTo(position)
        }
    }

    fun lineTo(position: Vector2) {
        val segment = Segment(cursor, position)
        segments.add(segment)
        cursor = position
    }

    fun curveTo(control: Vector2, position: Vector2) {
        val segment = Segment(cursor, control, position)
        segments.add(segment)
        cursor = position
    }

    fun curveTo(control0: Vector2, control1: Vector2, position: Vector2) {
        val segment = Segment(cursor, control0, control1, position)
        segments.add(segment)
        cursor = position
    }

    fun close() {
        closed = true
    }

    fun reverse() {
        segments.forEachIndexed { index, segment ->
            segments[index] = segment.reverse
        }
        segments.reverse()
    }

    fun arcTo(crx: Double, cry: Double, angle: Double, largeArcFlag: Boolean, sweepFlag: Boolean, tx: Double, ty: Double) {
        if (crx == 0.0 || cry == 0.0) {
            lineTo(Vector2(tx, ty))
        }

        var rx = Math.abs(crx)
        var ry = Math.abs(cry)
        val angleRad = Math.toRadians(angle % 360.0)

        val dx2 = (cursor.x - tx) / 2.0
        val dy2 = (cursor.y - ty) / 2.0

        val cosAngle = Math.cos(angleRad)
        val sinAngle = Math.sin(angleRad)
        val x1 = cosAngle * dx2 + sinAngle * dy2
        val y1 = -sinAngle * dx2 + cosAngle * dy2

        val radiiCheck = ((x1 * x1) / (rx * rx)) + ((y1 * y1) / (ry * ry))
        if (radiiCheck > 1) {
            rx = Math.sqrt(radiiCheck) * rx
            ry = Math.sqrt(radiiCheck) * ry
        }

        val rx_sq = rx * rx
        val ry_sq = ry * ry

        val y1_sq = y1 * y1
        val x1_sq = x1 * x1
        // Step 2 : Compute (cx1, cy1) - the transformed centre point
        var sign = (if (largeArcFlag == sweepFlag) -1 else 1).toDouble()
        var sq = (rx_sq * ry_sq - rx_sq * y1_sq - ry_sq * x1_sq) / (rx_sq * y1_sq + ry_sq * x1_sq)
        sq = if (sq < 0) 0.0 else sq
        val coef = sign * Math.sqrt(sq)
        val cx1 = coef * (rx * y1 / ry)
        val cy1 = coef * -(ry * x1 / rx)


        // Step 3 : Compute (cx, cy) from (cx1, cy1)
        val sx2 = (cursor.x + tx) / 2.0
        val sy2 = (cursor.y + ty) / 2.0
        val cx = sx2 + (cosAngle * cx1 - sinAngle * cy1)
        val cy = sy2 + (sinAngle * cx1 + cosAngle * cy1)

        // Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
        val ux = (x1 - cx1) / rx
        val uy = (y1 - cy1) / ry
        val vx = (-x1 - cx1) / rx
        val vy = (-y1 - cy1) / ry
        var p: Double
        var n: Double

        // Compute the angle start
        n = Math.sqrt(ux * ux + uy * uy)
        if (n == 0.0) {
            n = 0.000001
        }

        p = ux // (1 * ux) + (0 * uy)
        sign = if (uy < 0) -1.0 else 1.0
        var angleStart = Math.toDegrees(sign * Math.acos(p / n))

        // Compute the angle extent
        n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy))
        p = ux * vx + uy * vy


        val ratio = if (n > 0.0) p / n else 0.0

        sign = if (ux * vy - uy * vx < 0) -1.0 else 1.0
        var angleExtent = if (ratio >= 0.0) Math.toDegrees(sign * Math.acos(p / n)) else 180.0


        if (!sweepFlag && angleExtent > 0) {
            angleExtent -= 360.0
        } else if (sweepFlag && angleExtent < 0) {
            angleExtent += 360.0
        }
        angleExtent %= 360.0
        angleStart %= 360.0

        //angleExtent = Functions.mod(angleExtent, 360.0);
        //angleStart = Functions.mod(angleStart, 360.0);

        val coords = arcToBeziers(angleStart, angleExtent)

        for (i in coords.indices) {

            val x = coords[i].x
            val y = coords[i].y

            coords[i] = Vector2(cosAngle * rx * x + -sinAngle * ry * y + cx,
                    sinAngle * rx * x + cosAngle * ry * y + cy)
        }

        if (coords.isNotEmpty()) {
            coords[coords.size - 1] = Vector2(tx, ty)
            var i = 0
            while (i < coords.size) {
                curveTo(coords[i], coords[i + 1], coords[i + 2])
                i += 3
            }
        } else {
            //println("wot$angleStart $angleExtent")
        }

        cursor = Vector2(tx, ty)


    }

    fun continueTo(end: Vector2, tangentScale: Double = 1.0) {
        val last = segments.last()
        val delta = last.control.last() - last.end
        curveTo(last.end - delta * tangentScale, end)
    }

    fun continueTo(control: Vector2, end: Vector2, tangentScale: Double = 1.0) {
        val last = segments.last()
        val delta = last.control.last() - last.end
        curveTo(last.end - delta * tangentScale, control, end)
    }

    private fun arcToBeziers(angleStart: Double, angleExtent: Double): Array<Vector2> {
        val numSegments = Math.ceil(Math.abs(angleExtent) / 90.0).toInt()
        val angleStartRadians = Math.toRadians(angleStart)
        val angleExtentRadians = Math.toRadians(angleExtent)
        val angleIncrementRadians = (angleExtentRadians / numSegments).toFloat()

        // The length of each control point vector is given by the following formula.
        val controlLength = 4.0 / 3.0 * Math.sin(angleIncrementRadians / 2.0) / (1.0 + Math.cos(angleIncrementRadians / 2.0))

        val coords = Array(numSegments * 3) { Vector2.ZERO }
        var pos = 0

        for (i in 0 until numSegments) {
            var angle = angleStartRadians + i * angleIncrementRadians
            // Calculate the control vector at this angle
            var dx = Math.cos(angle)
            var dy = Math.sin(angle)
            // First control point
            coords[pos] = Vector2(dx - controlLength * dy, dy + controlLength * dx)
            pos++
            // Second control point
            angle += angleIncrementRadians.toDouble()
            dx = Math.cos(angle)
            dy = Math.sin(angle)
            coords[pos] = Vector2(dx + controlLength * dy, dy - controlLength * dx)
            pos++
            // Endpoint of bezier
            coords[pos] = Vector2(dx, dy)
            pos++
        }
        return coords
    }


}


