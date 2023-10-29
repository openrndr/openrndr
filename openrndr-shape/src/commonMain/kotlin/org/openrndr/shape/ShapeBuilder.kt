package org.openrndr.shape

import org.openrndr.math.*
import kotlin.math.*

/**
 * Shape builder class, used by [shape]
 */
class ShapeBuilder {
    internal val contours = mutableListOf<ShapeContour>()

    @Suppress("unused")
    fun boundary(f: ContourBuilder.() -> Unit) {
        val cb = ContourBuilder(false)
        cb.f()

        val contour = cb.result.first()
        require(contour.closed) { "boundary org.openrndr.shape.contours must be closed" }
        contours.add(contour.clockwise)
    }

    @Suppress("unused")
    fun hole(f: ContourBuilder.() -> Unit) {
        val cb = ContourBuilder(false)
        cb.f()
        val contour = cb.result.first()
        require(contour.closed) { "hole org.openrndr.shape.contours must be closed" }
        contours.add(contour.counterClockwise)
    }

    fun contour(f: ContourBuilder.() -> Unit) {
        val cb = ContourBuilder(false)
        cb.f()
        val c = cb.result.first()
        contours.add(if (contours.size == 0) c.clockwise else c.counterClockwise)
    }
}

@Suppress("unused")
class ContourBuilder(private val multipleContours: Boolean) {
    var cursor = Vector2.INFINITY
    var anchor = Vector2.INFINITY


    val segments = mutableListOf<Segment>()

    internal val contours = mutableListOf<ShapeContour>()


    fun copy(source: ShapeContour, connectEpsilon: Double = 1E-6) {
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

    /**
     * Move pen without drawing
     * @param position coordinate to move pen to
     */
    fun moveTo(position: Vector2) {
        require(multipleContours || anchor === Vector2.INFINITY) {
            "pen only can only be moved once per contour, use 'org.openrndr.shape.contours {}' to create multiple org.openrndr.shape.contours"
        }
        if (multipleContours && segments.isNotEmpty()) {
            contours.add(ShapeContour(segments.map { it }, false))
            segments.clear()
        }
        cursor = position
        anchor = position
    }

    /**
     * Move the pen to the given coordinates without drawing
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    fun moveTo(x: Double, y: Double) = moveTo(Vector2(x, y))

    /**
     * Move the pen or draw a line to the given coordinates.
     * The pen is moved without drawing when to prior moveTo instructions have been given.
     * @param position the coordinates to move the pen to
     */
    @Suppress("unused")
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


    @Suppress("unused")
    fun moveOrCurveTo(control: Vector2, position: Vector2) {
        if (anchor === Vector2.INFINITY) {
            moveTo(position)
        } else {
            curveTo(control, position)
        }
    }

    fun moveOrCurveTo(cx: Double, cy: Double, x: Double, y: Double) = moveOrCurveTo(Vector2(cx, cy), Vector2(x, y))

    @Suppress("unused")
    fun moveOrCurveTo(control0: Vector2, control1: Vector2, position: Vector2) {
        if (anchor === Vector2.INFINITY) {
            moveTo(position)
        } else {
            curveTo(control0, control1, position)
        }
    }

    fun moveOrCurveTo(c0x: Double, c0y: Double, c1x: Double, c1y: Double, x: Double, y: Double) =
        moveOrCurveTo(Vector2(c0x, c0y), Vector2(c1x, c1y), Vector2(x, y))

    /**
     * Line to
     */
    fun lineTo(position: Vector2) {
        require(cursor !== Vector2.INFINITY) {
            "use moveTo first"
        }
        if ((position - cursor).length > 0.0) {
            val segment = Segment(cursor, position)
            segments.add(segment)
            cursor = position
        }
    }

    /**
     * Line to
     */
    fun lineTo(x: Double, y: Double) = lineTo(Vector2(x, y))

    /**
     * Quadratic curve to
     */
    @Suppress("unused")
    fun curveTo(control: Vector2, position: Vector2) {
        require(cursor !== Vector2.INFINITY) {
            "use moveTo first"
        }
        if ((position - cursor).squaredLength > 0.0) {
            val segment = Segment(cursor, control, position)
            segments.add(segment)
            cursor = position
        }
    }

    /**
     * Quadratic curve to
     */
    fun curveTo(cx: Double, cy: Double, x: Double, y: Double) = curveTo(Vector2(cx, cy), Vector2(x, y))

    /**
     * Cubic curve to
     */
    fun curveTo(control0: Vector2, control1: Vector2, position: Vector2) {
        require(cursor !== Vector2.INFINITY) {
            "use moveTo first"
        }
        if ((position - cursor).squaredLength > 0.0) {
            val segment = Segment(cursor, control0, control1, position)
            segments.add(segment)
            cursor = position
        }
    }

    /**
     * Cubic curve to
     */
    fun curveTo(c0x: Double, c0y: Double, c1x: Double, c1y: Double, x: Double, y: Double) =
        curveTo(Vector2(c0x, c0y), Vector2(c1x, c1y), Vector2(x, y))

    /**
     * Closes the contour, adds a line segment to `anchor` when needed
     */
    fun close() {
        require(segments.isNotEmpty()) {
            "cannot close contour with 0 segments"
        }

        if ((anchor - cursor).length > 0.001) {
            segments.add(Segment(cursor, anchor))
        }
        contours.add(ShapeContour(segments.map { it }, true))
        segments.clear()
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


    fun circularArcTo(through: Vector2, end: Vector2) {
        val circle = Circle.fromPoints(cursor, through, end)
        val side = LineSegment(cursor, end).side(through) < 0.0
        val centerSide = LineSegment(cursor, end).side(circle.center) < 0.0
        if (side == centerSide) {
            arcTo(circle.radius, circle.radius, 90.0, true, side, end)
        } else {
            arcTo(circle.radius, circle.radius, 90.0, false, side, end)
        }
    }

    fun arcTo(
        crx: Double,
        cry: Double,
        angle: Double,
        largeArcFlag: Boolean,
        sweepFlag: Boolean,
        tx: Double,
        ty: Double
    ) {
        // based on https://github.com/BigBadaboom/androidsvg/blob/master/androidsvg/src/main/java/com/caverock/androidsvg/SVGAndroidRenderer.java

        require(cursor !== Vector2.INFINITY) {
            "use moveTo first"
        }

        val angleRad = (angle.mod(360.0)).asRadians

        val tdx = cursor.x - tx
        val tdy = cursor.y - ty

        if (tdx * tdx + tdy * tdy == 0.0) {
            return
        }
        val radiiEpsilon = 10E-6

        if (abs(crx) <= radiiEpsilon || abs(cry) <= radiiEpsilon) {
            lineTo(Vector2(tx, ty))
            return
        }

        var rx = abs(crx)
        var ry = abs(cry)

        val cosAngle = cos(angleRad)
        val sinAngle = sin(angleRad)

        val dx2 = (cursor.x - tx) / 2.0
        val dy2 = (cursor.y - ty) / 2.0

        val x1 = cosAngle * dx2 + sinAngle * dy2
        val y1 = -sinAngle * dx2 + cosAngle * dy2

        val rxSqr = rx * rx
        val rySqr = ry * ry

        val y1Sqr = y1 * y1
        val x1Sqr = x1 * x1

        val radiiCheck = ((x1 * x1) / (rx * rx)) + ((y1 * y1) / (ry * ry))
        if (radiiCheck > 1) {
            rx *= sqrt(radiiCheck)
            ry *= sqrt(radiiCheck)
        }

        // Step 2 : Compute (cx1, cy1) - the transformed centre point
        val sign0 = if (largeArcFlag == sweepFlag) -1.0 else 1.0
        var sq = (rxSqr * rySqr - rxSqr * y1Sqr - rySqr * x1Sqr) / (rxSqr * y1Sqr + rySqr * x1Sqr)
        sq = if (sq < 0) 0.0 else sq
        val coefficient = sign0 * sqrt(sq)
        val cx1 = coefficient * (rx * y1 / ry)
        val cy1 = coefficient * -(ry * x1 / rx)

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

        // Compute the angle start
        val n0 = sqrt(ux * ux + uy * uy)
        //val p0 = (1 * ux) + (0 * uy)
        val sign1 = if (uy < 0) -1.0 else 1.0
        var angleStart = sign1 * acos(ux / n0) // ux was p0

        // Compute the angle extent
        val n1 = sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy))
        val p1 = ux * vx + uy * vy
        val sign2 = if (ux * vy - uy * vx < 0) -1.0 else 1.0

        fun checkedACos(v: Double): Double {
            return when {
                v < -1.0 -> PI
                v > 1.0 -> 0.0
                else -> acos(v)
            }
        }

        var angleExtent = sign2 * checkedACos(p1 / n1)

        if (angleExtent == 0.0) {
            lineTo(tx, ty)
            return
        }

        if (!sweepFlag && angleExtent > 0) {
            angleExtent -= PI * 2
        } else if (sweepFlag && angleExtent < 0) {
            angleExtent += PI * 2
        }
        angleExtent %= PI * 2
        angleStart %= PI * 2

        val bezierPoints = arcToBeziers(angleStart, angleExtent)
        if (bezierPoints.isEmpty()) {
            return
        }

        bezierPoints[bezierPoints.lastIndex] = Vector2(tx, ty)

        for (i in bezierPoints.indices) {
            val x = bezierPoints[i].x
            val y = bezierPoints[i].y
            bezierPoints[i] = Vector2(
                cosAngle * rx * x + -sinAngle * ry * y + cx,
                sinAngle * rx * x + cosAngle * ry * y + cy
            )
        }

        if (bezierPoints.isNotEmpty()) {
            bezierPoints[bezierPoints.size - 1] = Vector2(tx, ty)
            var i = 0
            while (i < bezierPoints.size) {
                try {
                    curveTo(bezierPoints[i], bezierPoints[i + 1], bezierPoints[i + 2])
                } catch (e: IllegalArgumentException) {
                    error("radii: $crx $cry, deltas: $tdx $tdy [$i] ${bezierPoints[i]}, ${bezierPoints[i + 1]}, ${bezierPoints[i + 2]}")
                }
                i += 3
            }
        }

        cursor = Vector2(tx, ty)
    }

    fun arcTo(crx: Double, cry: Double, angle: Double, largeArcFlag: Boolean, sweepFlag: Boolean, end: Vector2) =
        arcTo(crx, cry, angle, largeArcFlag, sweepFlag, end.x, end.y)

    fun continueTo(end: Vector2, tangentScale: Double = 1.0) {
        if ((cursor - end).squaredLength > 0.0) {
            if (segments.isNotEmpty() && segments.last().control.isNotEmpty()) {
                val last = segments.last()
                val delta = last.control.last() - last.end
                curveTo(last.end - delta * tangentScale, end)
            } else {
                curveTo(cursor + (end - cursor) / 2.0, end)
            }
        }
    }

    fun continueTo(x: Double, y: Double, tangentScale: Double = 1.0) = continueTo(Vector2(x, y), tangentScale)

    @Suppress("unused")
    fun continueTo(control: Vector2, end: Vector2, tangentScale: Double = 1.0) {
        if (segments.isNotEmpty() && segments.last().control.isNotEmpty()) {
            val last = segments.last()
            val delta = last.control.last() - last.end
            curveTo(last.end - delta * tangentScale, control, end)
        } else {
            curveTo(cursor + (end - cursor) / 3.0, control, end)
        }
    }

    fun continueTo(cx: Double, cy: Double, x: Double, y: Double, tangentScale: Double = 1.0) =
        continueTo(Vector2(cx, cy), Vector2(x, y), tangentScale)

    private fun arcToBeziers(angleStart: Double, angleExtent: Double): Array<Vector2> {
        val numSegments = ceil(abs(angleExtent) * 2.0 / PI).toInt()
        val angleIncrement = (angleExtent / numSegments)

        // The length of each control point vector is given by the following formula.
        val controlLength = 4.0 / 3.0 * sin(angleIncrement / 2.0) / (1.0 + cos(angleIncrement / 2.0))

        val coordinates = Array(numSegments * 3) { Vector2.ZERO }
        var pos = 0

        for (i in 0 until numSegments) {
            var angle = angleStart + i * angleIncrement
            // Calculate the control vector at this angle
            var dx = cos(angle)
            var dy = sin(angle)
            // First control point
            coordinates[pos] = Vector2(dx - controlLength * dy, dy + controlLength * dx)
            pos++
            // Second control point
            angle += angleIncrement
            dx = cos(angle)
            dy = sin(angle)
            coordinates[pos] = Vector2(dx + controlLength * dy, dy - controlLength * dx)
            pos++
            // Endpoint of bezier
            coordinates[pos] = Vector2(dx, dy)
            pos++
        }
        return coordinates
    }

    fun segment(segment: Segment) {
        if (cursor !== Vector2.INFINITY) {
            require((segment.start - cursor).length < 10E-3) {
                "segment is disconnected: cursor: ${cursor}, segment.start: ${segment.start}, distance: ${(cursor - segment.start).length}"
            }
        }
        if (cursor === Vector2.INFINITY) {
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

    fun undo(): Segment? {
        return if (segments.isNotEmpty()) {
            val r = segments.removeAt(segments.lastIndex)
            cursor = r.start
            r
        } else {
            null
        }
    }

    val lastSegment: Segment?
        get() = segments.lastOrNull()


    val result: List<ShapeContour>
        get() {
            return contours + if (segments.isNotEmpty()) listOf(
                ShapeContour(
                    segments.map { it },
                    false
                )
            ) else emptyList()
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
    val cb = ContourBuilder(false)
    cb.f()
    return if (cb.result.isEmpty()) {
        ShapeContour.EMPTY
    } else {
        cb.result.first()
    }
}

/**
 * Build multiple org.openrndr.shape.contours
 */
fun contours(f: ContourBuilder.() -> Unit): List<ShapeContour> {
    val clb = ContourBuilder(true)
    clb.f()
    return clb.result
}
