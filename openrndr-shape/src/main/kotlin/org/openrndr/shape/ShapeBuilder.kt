package org.openrndr.shape

import org.openrndr.math.Vector2
import kotlin.math.*

/**
 * Shape builder class, used by [shape]
 */
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

@Suppress("unused")
class ContourBuilder {
    var cursor = Vector2.INFINITY
    var anchor = Vector2.INFINITY
    internal var closed = false

    val segments = mutableListOf<Segment>()

    /**
     * Move pen without drawing
     * @param position coordinate to move pen to
     */
    fun moveTo(position: Vector2) {
        cursor = position
        anchor = position
    }

    /**
     * Move the pen to the given coordinates without drawing
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    fun moveTo(x: Double, y:Double) = moveTo(Vector2(x, y))

    /**
     * Move the pen or draw a line to the given coordinates.
     * The pen is moved without drawing when to prior moveTo instructions have been given.
     * @param position the coordinates to move the pen to
     */
    fun moveOrLineTo(position: Vector2) {
        if (anchor === Vector2.INFINITY) {
            moveTo(position)
        } else {
            lineTo(position)
        }
    }

    /**
     * Move the pen or draw a line to the given coordinates.
     * The pen is moved without drawing when to prior moveTo instructions have been given.
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    fun moveOrLineTo(x: Double, y: Double) = moveOrLineTo(Vector2(x, y))


    fun moveOrCurveTo(control: Vector2, position: Vector2) {
        if (anchor === Vector2.INFINITY) {
            moveTo(position)
        } else {
            curveTo(control, position)
        }
    }

    fun moveOrCurveTo(cx: Double, cy: Double, x: Double, y: Double) = moveOrCurveTo(Vector2(cx, cy), Vector2(x, y))

    fun moveOrCurveTo(control0: Vector2, control1: Vector2, position: Vector2) {
        if (anchor === Vector2.INFINITY) {
            moveTo(position)
        } else {
            curveTo(control0, control1, position)
        }
    }

    fun moveOrCurveTo(c0x: Double, c0y: Double, c1x: Double, c1y: Double, x: Double, y: Double) = moveOrCurveTo(Vector2(c0x, c0y), Vector2(c1x, c1y), Vector2(x, y))


    /**
     * Line to
     */
    fun lineTo(position: Vector2) {
        val segment = Segment(cursor, position)
        segments.add(segment)
        cursor = position
    }

    /**
     * Line to
     */
    fun lineTo(x: Double, y: Double) = lineTo(Vector2(x, y))

    /**
     * Quadratic curve to
     */
    fun curveTo(control: Vector2, position: Vector2) {
        val segment = Segment(cursor, control, position)
        segments.add(segment)
        cursor = position
    }

    /**
     * Quadratic curve to
     */
    fun curveTo(cx: Double, cy: Double, x: Double, y: Double) = curveTo(Vector2(cx, cy), Vector2(x, y))

    /**
     * Cubic curve to
     */
    fun curveTo(control0: Vector2, control1: Vector2, position: Vector2) {
        val segment = Segment(cursor, control0, control1, position)
        segments.add(segment)
        cursor = position
    }

    /**
     * Cubic curve to
     */
    fun curveTo(c0x: Double, c0y: Double, c1x: Double, c1y: Double, x: Double, y: Double)
            = curveTo(Vector2(c0x, c0y), Vector2(c1x, c1y), Vector2(x, y))

    /**
     * Closes the contour, adds a line segment to `anchor` when needed
     */
    fun close() {
        if ((anchor - cursor).length > 0.001) {
            segments.add(Segment(cursor, anchor))
        }
        closed = true
    }

    /**
     * Reverse all segments
     */
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

        var rx = abs(crx)
        var ry = abs(cry)
        val angleRad = Math.toRadians(angle % 360.0)

        val dx2 = (cursor.x - tx) / 2.0
        val dy2 = (cursor.y - ty) / 2.0

        val cosAngle = cos(angleRad)
        val sinAngle = sin(angleRad)
        val x1 = cosAngle * dx2 + sinAngle * dy2
        val y1 = -sinAngle * dx2 + cosAngle * dy2

        val radiiCheck = ((x1 * x1) / (rx * rx)) + ((y1 * y1) / (ry * ry))
        if (radiiCheck > 1) {
            rx *= sqrt(radiiCheck)
            ry *= sqrt(radiiCheck)
        }

        val rx_sq = rx * rx
        val ry_sq = ry * ry

        val y1_sq = y1 * y1
        val x1_sq = x1 * x1
        // Step 2 : Compute (cx1, cy1) - the transformed centre point
        var sign = if (largeArcFlag == sweepFlag) -1.0 else 1.0
        var sq = (rx_sq * ry_sq - rx_sq * y1_sq - ry_sq * x1_sq) / (rx_sq * y1_sq + ry_sq * x1_sq)
        sq = if (sq < 0) 0.0 else sq
        val coef = sign * sqrt(sq)
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
        n = sqrt(ux * ux + uy * uy)
        if (n == 0.0) {
            n = 0.000001
        }

        p = ux // (1 * ux) + (0 * uy)
        sign = if (uy < 0) -1.0 else 1.0
        var angleStart = Math.toDegrees(sign * acos(p / n))

        // Compute the angle extent
        n = sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy))
        p = ux * vx + uy * vy


        val ratio = if (n > 0.0) p / n else 0.0

        sign = if (ux * vy - uy * vx < 0) -1.0 else 1.0
        var angleExtent = if (ratio >= 0.0) Math.toDegrees(sign * acos(p / n)) else 180.0


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

    fun arcTo(crx: Double, cry: Double, angle: Double, largeArcFlag: Boolean, sweepFlag: Boolean, end: Vector2) =
            arcTo(crx, cry, angle, largeArcFlag, sweepFlag, end.x, end.y)

    fun continueTo(end: Vector2, tangentScale: Double = 1.0) {
        if (segments.isNotEmpty()) {
            val last = segments.last()
            val delta = last.control.last() - last.end
            curveTo(last.end - delta * tangentScale, end)
        } else {
            curveTo(cursor + (end-cursor) / 2.0, end)
        }
    }

    fun continueTo(x:Double, y:Double, tangentScale: Double = 1.0) = continueTo(Vector2(x,y), tangentScale)

    fun continueTo(control: Vector2, end: Vector2, tangentScale: Double = 1.0) {
        if (segments.isNotEmpty()) {
            val last = segments.last()
            val delta = last.control.last() - last.end
            curveTo(last.end - delta * tangentScale, control, end)
        } else {
            curveTo(cursor + (end-cursor)/3.0, control, end)
        }
    }

    fun continueTo(cx:Double, cy:Double, x:Double, y:Double, tangentScale: Double = 1.0) = continueTo(Vector2(cx, cy), Vector2(x, y), tangentScale)

    private fun arcToBeziers(angleStart: Double, angleExtent: Double): Array<Vector2> {
        val numSegments = ceil(abs(angleExtent) / 90.0).toInt()
        val angleStartRadians = Math.toRadians(angleStart)
        val angleExtentRadians = Math.toRadians(angleExtent)
        val angleIncrementRadians = (angleExtentRadians / numSegments)

        // The length of each control point vector is given by the following formula.
        val controlLength = 4.0 / 3.0 * sin(angleIncrementRadians / 2.0) / (1.0 + cos(angleIncrementRadians / 2.0))

        val coords = Array(numSegments * 3) { Vector2.ZERO }
        var pos = 0

        for (i in 0 until numSegments) {
            var angle = angleStartRadians + i * angleIncrementRadians
            // Calculate the control vector at this angle
            var dx = cos(angle)
            var dy = sin(angle)
            // First control point
            coords[pos] = Vector2(dx - controlLength * dy, dy + controlLength * dx)
            pos++
            // Second control point
            angle += angleIncrementRadians
            dx = cos(angle)
            dy = sin(angle)
            coords[pos] = Vector2(dx + controlLength * dy, dy - controlLength * dx)
            pos++
            // Endpoint of bezier
            coords[pos] = Vector2(dx, dy)
            pos++
        }
        return coords
    }
}

/**
 * Build a shape
 */
fun shape(f: ShapeBuilder.() -> Unit): Shape {
    val sb = ShapeBuilder()
    sb.f()
    return Shape(sb.contours)
}

/**
 * Build a contour
 */
fun contour(f: ContourBuilder.() -> Unit): ShapeContour {
    val cb = ContourBuilder()
    cb.f()
    return ShapeContour(cb.segments, cb.closed)
}

