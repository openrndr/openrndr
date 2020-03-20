package org.openrndr.internal

import org.openrndr.draw.LineCap
import org.openrndr.draw.LineJoin
import org.openrndr.internal.PathPoint.Companion.BEVEL
import org.openrndr.internal.PathPoint.Companion.CORNER
import org.openrndr.internal.PathPoint.Companion.INNER_BEVEL
import org.openrndr.internal.PathPoint.Companion.LEFT
import org.openrndr.math.Vector2
import kotlin.math.*

internal enum class ExpansionType {
    STROKE,
    FILL,
    FRINGE,
    SKIP
}

internal class Expansion(val type: ExpansionType, val fb: FloatArray, val bufferStart: Int) {
    var vertexCount = 0
    var minx = Double.POSITIVE_INFINITY
    var maxx = Double.NEGATIVE_INFINITY
    var miny = Double.POSITIVE_INFINITY
    var maxy = Double.NEGATIVE_INFINITY
    var bufferPosition = bufferStart

    private fun chooseBevel(bevel: Boolean, p0: PathPoint, p1: PathPoint, w: Double): DoubleArray {
        val x0: Double
        val y0: Double
        val x1: Double
        val y1: Double

        if (bevel) {
            x0 = p1.x + p0.dy * w
            y0 = p1.y - p0.dx * w
            x1 = p1.x + p1.dy * w
            y1 = p1.y - p1.dx * w
        } else {
            x0 = p1.x + p1.dmx * w
            y0 = p1.y + p1.dmy * w
            x1 = p1.x + p1.dmx * w
            y1 = p1.y + p1.dmy * w
        }
        return doubleArrayOf(x0, y0, x1, y1)
    }

    fun bevelJoin(p0: PathPoint, p1: PathPoint,
                  lw: Double, rw: Double, lu: Double, ru: Double, fringe: Double, offset: Double) {

        val dlx0 = p0.dy
        val dly0 = -p0.dx
        val dlx1 = p1.dy
        val dly1 = -p1.dx

        if (p1.flags and LEFT != 0) {
            val r = chooseBevel(p1.flags and INNER_BEVEL != 0, p0, p1, lw)
            val lx0 = r[0]
            val ly0 = r[1]
            val lx1 = r[2]
            val ly1 = r[3]

            addVertex(lx0, ly0, lu, 1.0, offset)
            addVertex(p1.x - dlx0 * rw, p1.y - dly0 * rw, ru, 1.0, offset)

            if (p1.flags and BEVEL != 0) {
                addVertex(lx0, ly0, lu, 1.0, offset)
                addVertex(p1.x - dlx0 * rw, p1.y - dly0 * rw, ru, 1.0, offset)

                addVertex(lx1, ly1, lu, 1.0, offset)
                addVertex(p1.x - dlx1 * rw, p1.y - dly1 * rw, ru, 1.0, offset)
            } else {
                val rx0 = p1.x - p1.dmx * rw
                val ry0 = p1.y - p1.dmy * rw

                addVertex(p1.x, p1.y, 0.5, 1.0, offset)
                addVertex(p1.x - dlx0 * rw, p1.y - dly0 * rw, ru, 1.0, offset)

                addVertex(rx0, ry0, ru, 1.0, offset)
                addVertex(rx0, ry0, ru, 1.0, offset)

                addVertex(p1.x, p1.y, 0.5, 1.0, offset)
                addVertex(p1.x - dlx1 * rw, p1.y - dly1 * rw, ru, 1.0, offset)
            }

            addVertex(lx1, ly1, lu, 1.0, offset)
            addVertex(p1.x - dlx1 * rw, p1.y - dly1 * rw, ru, 1.0, offset)

        } else {
            val r = chooseBevel(p1.flags and INNER_BEVEL != 0, p0, p1, -rw)

            val rx0 = r[0]
            val ry0 = r[1]
            val rx1 = r[2]
            val ry1 = r[3]

            addVertex(p1.x + dlx0 * lw, p1.y + dly0 * lw, lu, 1.0, offset)
            addVertex(rx0, ry0, ru, 1.0, offset)

            if (p1.flags and BEVEL != 0) {
                addVertex(p1.x + dlx0 * lw, p1.y + dly0 * lw, lu, 1.0, offset)
                addVertex(rx0, ry0, ru, 1.0, offset)

                addVertex(p1.x + dlx1 * lw, p1.y + dly1 * lw, lu, 1.0, offset)
                addVertex(rx1, ry1, ru, 1.0, offset)
            } else {
                val lx0 = p1.x + p1.dmx * lw
                val ly0 = p1.y + p1.dmy * lw

                addVertex(p1.x + dlx0 * lw, p1.y + dly0 * lw, lu, 1.0, offset)
                addVertex(p1.x, p1.y, 0.5, 1.0, offset)

                addVertex(lx0, ly0, lu, 1.0, offset)
                addVertex(lx0, ly0, lu, 1.0, offset)

                addVertex(p1.x + dlx1 * lw, p1.y + dly1 * lw, lu, 1.0, offset)
                addVertex(p1.x, p1.y, 0.5, 1.0, offset)
            }

            addVertex(p1.x + dlx1 * lw, p1.y + dly1 * lw, lu, 1.0, offset)
            addVertex(rx1, ry1, ru, 1.0, offset)
        }
    }

    fun roundJoin(p0: PathPoint, p1: PathPoint,
                  lw: Double, rw: Double, lu: Double, ru: Double, ncap: Int, fringe: Double, offset: Double) {

        val dlx0 = p0.dy
        val dly0 = -p0.dx
        val dlx1 = p1.dy
        val dly1 = -p1.dx

        if (p1.flags and LEFT != 0) {

            val r = chooseBevel(p1.flags and INNER_BEVEL != 0, p0, p1, lw)
            val lx0 = r[0]
            val ly0 = r[1]
            val lx1 = r[2]
            val ly1 = r[3]

            var a0 = atan2(-dly0, -dlx0)
            var a1 = atan2(-dly1, -dlx1)
            if (a1 > a0) a0 += PI * 2

            if (a0 < 0 || a1 < 0) {
                a0 += PI * 2
                a1 += PI * 2
            }

            addVertex(lx0, ly0, lu, 1.0, offset)
            addVertex(p1.x - dlx0 * rw, p1.y - dly0 * rw, ru, 1.0, offset)

            val n = ncap.coerceAtMost(ceil((a0 - a1) / PI * ncap).toInt()).coerceAtLeast(2)
            for (i in 0 until n) {
                val u = i / (n - 1.0)
                val a = a0 + u * (a1 - a0)
                val rx = p1.x + cos(a) * rw
                val ry = p1.y + sin(a) * rw
                addVertex(p1.x, p1.y, 0.5, 1.0, offset)
                addVertex(rx, ry, ru, 1.0, offset)
            }

            addVertex(lx1, ly1, lu, 1.0, offset)
            addVertex(p1.x - dlx1 * rw, p1.y - dly1 * rw, ru, 1.0, offset)

        } else {
            val r = chooseBevel(p1.flags and INNER_BEVEL != 0, p0, p1, -rw)
            val rx0 = r[0]
            val ry0 = r[1]
            val rx1 = r[2]
            val ry1 = r[3]

            val a0 = atan2(dly0, dlx0)
            var a1 = atan2(dly1, dlx1)
            if (a1 < a0) {
                a1 += PI * 2
            }
            addVertex(p1.x + dlx0 * rw, p1.y + dly0 * rw, lu, 1.0, offset)
            addVertex(rx0, ry0, ru, 1.0, offset)

            val n = ncap.coerceAtMost(ceil((a1 - a0) / PI * ncap).toInt()).coerceAtLeast(2)

            for (i in 0 until n) {
                val a = a0 + i.toDouble() / (n - 1.0) * (a1 - a0)
                addVertex(p1.x + cos(a) * lw, p1.y + sin(a) * lw, lu, 1.0, offset)
                addVertex(p1.x, p1.y, 0.5, 1.0, offset)
            }
            addVertex(p1.x + dlx1 * rw, p1.y + dly1 * rw, lu, 1.0, offset)
            addVertex(rx1, ry1, ru, 1.0, offset)
        }
    }

    fun buttCapStart(p: PathPoint, dx: Double, dy: Double, w: Double,
                     d: Double, aa: Double, offset: Double) {
        val px = p.x - dx * d
        val py = p.y - dy * d
        val dly = -dx

        addVertex(px + dy * w - dx * aa, py + dly * w - dy * aa, 0.0, 0.0, offset)
        addVertex(px - dy * w - dx * aa, py - dly * w - dy * aa, 1.0, 0.0, offset)
        addVertex(px + dy * w, py + dly * w, 0.0, 1.0, offset)
        addVertex(px - dy * w, py - dly * w, 1.0, 1.0, offset)
    }

    fun buttCapEnd(p: PathPoint, dx: Double, dy: Double, w: Double, d: Double, aa: Double, offset: Double) {
        val px = p.x - dx * d
        val py = p.y - dy * d
        val dly = -dx

        addVertex(px + dy * w, py + dly * w, 0.0, 1.0, offset)
        addVertex(px - dy * w, py - dly * w, 1.0, 1.0, offset)
        addVertex(px + dy * w + dx * aa, py + dly * w + dy * aa, 0.0, 0.0, offset)
        addVertex(px - dy * w + dx * aa, py - dly * w + dy * aa, 1.0, 0.0, offset)
    }

    fun roundCapStart(p: PathPoint,
                      dx: Double, dy: Double, w: Double, ncap: Int, aa: Double, offset: Double) {
        val px = p.x
        val py = p.y
        val dly = -dx

        for (i in 0 until ncap) {
            val a = i / (ncap - 1.0) * PI
            val ax = cos(a) * w
            val ay = sin(a) * w
            addVertex(px - dy * ax - dx * ay, py - dly * ax - dy * ay, 0.0, 1.0, offset)
            addVertex(px, py, 0.5, 1.0, offset)
        }
        addVertex(px + dy * w, py + dly * w, 0.0, 1.0, offset)
        addVertex(px - dy * w, py - dly * w, 1.0, 1.0, offset)
    }

    fun roundCapEnd(p: PathPoint,
                    dx: Double, dy: Double, w: Double, ncap: Int, aa: Double, offset: Double) {
        val px = p.x
        val py = p.y
        val dly = -dx

        addVertex(px + dy * w, py + dly * w, 0.0, 1.0, offset)
        addVertex(px - dy * w, py - dly * w, 1.0, 1.0, offset)
        for (i in 0 until ncap) {
            val a = i / (ncap - 1).toDouble() * PI
            val ax = cos(a) * w
            val ay = sin(a) * w
            addVertex(px, py, 0.5, 1.0, offset)
            addVertex(px - dy * ax + dx * ay, py - dly * ax + dy * ay, 0.0, 1.0, offset)
        }
    }

    fun vertex(idx: Int): Vector2 =
            Vector2(fb[bufferStart + idx * 5].toDouble(), fb[bufferStart + idx * 5 + 1].toDouble())

    fun addVertex(x: Double, y: Double, u: Double, v: Double, offset: Double) {

        if (x != x) {
            throw RuntimeException("$x $y $u $v")
        }

        minx = min(minx, x)
        maxx = max(maxx, x)
        miny = min(miny, y)
        maxy = max(maxy, y)

        fb[bufferPosition++] = x.toFloat()
        fb[bufferPosition++] = y.toFloat()
        fb[bufferPosition++] = u.toFloat()
        fb[bufferPosition++] = v.toFloat()
        fb[bufferPosition++] = offset.toFloat()

        vertexCount++
    }
}


internal class Path {
    var convex: Boolean = false
    var closed: Boolean = false
    var nbevel: Int = 0
    val contours = mutableListOf<List<PathPoint>>()


    companion object {
        fun fromLineStrip(segments: Iterable<Vector2>, closed: Boolean): Path {
            val sp = Path()
            val path = segments.map { PathPoint().apply { x = it.x; y = it.y; flags = CORNER } }.dropLast(if (closed) 1 else 0)

            if (path.isNotEmpty()) {
                if (!closed) {
                    path[0].flags = 0
                    path[path.size - 1].flags = 0
                }
                sp.contours.add(path)
            }
            sp.closed = closed
            return sp
        }

        fun fromLineLoops(contours: Iterable<Iterable<Vector2>>): Path {
            val sp = Path()
            contours.forEach { contour ->
                val path = contour.map { PathPoint().apply { x = it.x; y = it.y; flags = CORNER } }.dropLast(1)
                sp.contours.add(path)
            }
            sp.closed = true
            return sp
        }
    }

    private fun calculateJoins(points: List<PathPoint>, w: Double, lineJoin: LineJoin, miterLimit: Double) {
        nbevel = 0

        val iw = if (w > 0.0) 1.0 / w else 0.0
        var nleft = 0

        var p0 = points[points.size - 1]
        var p1 = points[0]
        var p1ptr = 0

        for (j in points.indices) {
            val dlx0 = p0.dy
            val dly0 = -p0.dx
            val dlx1 = p1.dy
            val dly1 = -p1.dx

            p1.dmx = (dlx0 + dlx1) * 0.5
            p1.dmy = (dly0 + dly1) * 0.5
            val dmr2 = p1.dmx * p1.dmx + p1.dmy * p1.dmy
            if (dmr2 > 0.000001f) {
                var scale = 1.0 / dmr2
                if (scale > 600.0) {
                    scale = 600.0
                }
                p1.dmx *= scale
                p1.dmy *= scale
            }
            p1.flags = if (p1.flags and CORNER != 0) CORNER else 0
            val cross = p1.dx * p0.dy - p0.dx * p1.dy
            if (cross > 0.0) {
                nleft += 1
                p1.flags = p1.flags or LEFT
            }

            // Calculate if we should use bevel or miter for inner join.
            val limit = max(1.01, min(p0.length, p1.length) * iw)
            if (dmr2 * limit * limit < 1.0f) {
                p1.flags = p1.flags or INNER_BEVEL
            }

            // Check to see if the corner needs to be beveled.
            if (p1.flags and CORNER != 0) {
                if (dmr2 * miterLimit * miterLimit < 1.0f || lineJoin === LineJoin.BEVEL || lineJoin === LineJoin.ROUND) {
                    p1.flags = p1.flags or BEVEL
                }
            }

            if (p1.flags and (BEVEL or INNER_BEVEL) != 0) {
                nbevel++
            }

            p0 = p1
            p1ptr++
            if (p1ptr < points.size) {
                p1 = points[p1ptr]
            }
        }
        convex = nleft == points.size
    }

    fun prepare(points: List<PathPoint>) {
        var p0 = points[points.size - 1]
        var p1 = points[0]
        var p1ptr = 0
        for (i in points.indices) {
            // Calculate segment direction and length
            p0.dx = p1.x - p0.x
            p0.dy = p1.y - p0.y

            val distanceSquared = p0.dx * p0.dx + p0.dy * p0.dy

            require(distanceSquared > 0.0 || (i == 0 && !closed)) { "consecutive point duplication in input geometry at ($i and ${i + 1}) (${p0.x},${p0.y})" }

            p0.length = sqrt(distanceSquared)
            if (p0.length > 0) {
                p0.dx /= p0.length
                p0.dy /= p0.length
            } else {
                p0.dx = 0.0
                p0.dy = 0.0
                //throw RuntimeException("duplicate points within epsilon")
            }
            p0 = p1
            p1ptr += 1
            if (p1ptr < points.size) {
                p1 = points[p1ptr]
            }
        }
    }

    fun expandStroke(fringeWidth: Double, weight: Double, lineCap: LineCap, lineJoin: LineJoin, miterLimit: Double): Expansion {

        if (contours.isNotEmpty() && contours[0].size >= 2) {
            val points = contours[0]
            val tessTol = 0.1
            val capSteps = curveDivs(weight, PI, tessTol)

            prepare(points)

            calculateJoins(points, weight, lineJoin, miterLimit)

            var cverts = 0
            cverts += if (lineJoin == LineJoin.ROUND) {
                (points.size + nbevel * (capSteps + 2) + 1) * 2 // plus one for loop
            } else {
                (points.size + nbevel * 5 + 1) * 2 // plus one for loop
            }

            if (!closed) {
                cverts += if (lineCap === LineCap.ROUND) {
                    (capSteps * 2 + 2) * 2
                } else {
                    (3 + 3) * 2
                }
            }

            val expansion = Expansion(ExpansionType.STROKE, FloatArray(cverts * 5), 0)

            var offset = 0.0
            val aa = fringeWidth

            var p0 = if (closed) points[points.size - 1] else points[0]
            var p1 = if (closed) points[0] else points[1]
            val start = if (closed) 0 else 1
            val end = if (closed) points.size else points.size - 1
            var p1ptr = if (closed) 0 else 1

            // -- start, optional cap
            if (!closed) {
                var dx = p1.x - p0.x
                var dy = p1.y - p0.y
                val length = sqrt(dx * dx + dy * dy)

                if (length > 0) {
                    dx /= length
                    dy /= length
                }

                when (lineCap) {
                    LineCap.BUTT -> expansion.buttCapStart(p0, dx, dy, weight, -aa * 0.5, aa, offset)
                    LineCap.SQUARE -> expansion.buttCapStart(p0, dx, dy, weight, weight - aa, aa, offset)
                    LineCap.ROUND -> expansion.roundCapStart(p0, dx, dy, weight, capSteps, aa, offset)
                }
            }


            // -- middle
            for (j in start until end) {
                offset += p0.length
                if (p1.flags and (BEVEL or INNER_BEVEL) != 0) {
                    if (lineJoin === LineJoin.ROUND) {
                        expansion.roundJoin(p0, p1, weight, weight, 0.0, 1.0, capSteps, aa, offset)
                    } else {
                        expansion.bevelJoin(p0, p1, weight, weight, 0.0, 1.0, aa, offset)
                    }
                } else {
                    expansion.addVertex(p1.x + p1.dmx * weight, p1.y + p1.dmy * weight, 0.0, 1.0, offset)
                    expansion.addVertex(p1.x - p1.dmx * weight, p1.y - p1.dmy * weight, 1.0, 1.0, offset)
                }
                p0 = p1
                p1ptr += 1
                if (p1ptr < points.size) {
                    p1 = points[p1ptr]
                }
            }

            if (points.size == 2) {
                val dx = p1.x - p0.x
                val dy = p1.y - p0.y
                val length = sqrt(dx * dx + dy * dy)
                offset = length
            }
            // -- end
            if (closed) {
                // Loop it
                val v0 = expansion.vertex(0)
                val v1 = expansion.vertex(1)
                expansion.addVertex(v0.x, v0.y, 0.0, 1.0, offset)
                expansion.addVertex(v1.x, v1.y, 1.0, 1.0, offset)
            } else {
                // Add cap
                var dx = p1.x - p0.x
                var dy = p1.y - p0.y
                val l = sqrt(dx * dx + dy * dy)
                if (l > 0) {
                    dx /= l
                    dy /= l
                }
                when (lineCap) {
                    LineCap.BUTT -> expansion.buttCapEnd(p1, dx, dy, weight, -aa * 0.5, aa, offset)
                    LineCap.SQUARE -> expansion.buttCapEnd(p1, dx, dy, weight, weight - aa, aa, offset)
                    LineCap.ROUND -> expansion.roundCapEnd(p1, dx, dy, weight, capSteps, aa, offset)
                }
            }
            return expansion
        } else {
            return Expansion(ExpansionType.SKIP, FloatArray(0), 0)
        }
    }

    fun expandFill(fringeWidth: Double, w: Double, lineJoin: LineJoin, miterLimit: Double): List<Expansion> {

        if (contours.isNotEmpty()) {
            val result = mutableListOf<Expansion>()

            contours.forEach { prepare(it) }
            contours.forEach { calculateJoins(it, w, lineJoin, miterLimit) }

            if (contours.size > 1) {
                convex = false
            }
            val aa = fringeWidth
            val woff = 0.5 * aa
            val generateFringe = w > 0.0
            val offset = 0.0


            contours.forEach { points ->
                var size = 4
                points.forEach { point ->
                    size += if ((point.flags and BEVEL) != 0) {
                        12
                    } else {
                        4
                    }
                }

                val fill = Expansion(ExpansionType.FILL, FloatArray(size * 5), 0)


                if (generateFringe) {
                    var p0 = points.last()
                    var p1 = points[0]
                    var p1ptr = 0

                    for (j in points.indices) {
                        if (p1.flags and BEVEL != 0) {
                            if (p1.flags and LEFT != 0) {
                                fill.addVertex(p1.x + p1.dmx * woff, p1.y + p1.dmy * woff, 0.5, 1.0, offset)
                            } else {

                                val dlx0 = p0.dy
                                val dly0 = -p0.dx
                                val dlx1 = p1.dy
                                val dly1 = -p1.dx

                                val lx0 = p1.x + dlx0 * woff
                                val ly0 = p1.y + dly0 * woff
                                val lx1 = p1.x + dlx1 * woff
                                val ly1 = p1.y + dly1 * woff

                                fill.addVertex(lx0, ly0, 0.5, 1.0, offset)
                                fill.addVertex(lx1, ly1, 0.5, 1.0, offset)
                            }
                        } else {
                            fill.addVertex(p1.x + p1.dmx * woff, p1.y + p1.dmy * woff, 0.5, 1.0, offset)
                        }
                        p0 = p1
                        p1ptr++
                        if (p1ptr < points.size) {
                            p1 = points[p1ptr]
                        }
                    }

                } else {
                    for (j in points.indices) {
                        fill.addVertex(points[j].x, points[j].y, 0.5, 1.0, offset)
                    }
                }
                result.add(fill)
            }


            // Calculate fringe
            if (generateFringe) {
                for (points in contours) {
                    var size = 2
                    for (point in points) {
                        size += if (point.flags and (BEVEL or INNER_BEVEL) != 0) {
                            10
                        } else {
                            4
                        }
                    }
                    val fringe = Expansion(ExpansionType.FRINGE, FloatArray(size * 5), 0)

                    var lw = w + woff
                    val rw = w - woff
                    var lu = 0.0
                    val ru = 1.0

                    // Create only half a fringe for convex shapes so that
                    // the shape can be rendered without stenciling.
                    if (convex) {
                        lw = woff    // This should generate the same vertex as fill inset above.
                        lu = 0.5    // Set outline fade at middle.
                    }

                    // Looping
                    var p0 = points[points.size - 1]
                    var p1 = points[0]

                    var p1ptr = 0
                    for (j in points.indices) {
                        if (p1.flags and (BEVEL or INNER_BEVEL) != 0) {
                            fringe.bevelJoin(p0, p1, lw, rw, lu, ru, fringeWidth, offset)
                        } else {
                            fringe.addVertex(p1.x + (p1.dmx * lw), p1.y + (p1.dmy * lw), lu, 1.0, offset)
                            fringe.addVertex(p1.x - (p1.dmx * rw), p1.y - (p1.dmy * rw), ru, 1.0, offset)
                        }
                        p0 = p1
                        p1ptr++
                        if (p1ptr < points.size) {
                            p1 = points[p1ptr]
                        }
                    }
                    // Loop it
                    val v0 = fringe.vertex(0)
                    val v1 = fringe.vertex(1)

                    fringe.addVertex(v0.x, v0.y, lu, 1.0, offset)
                    fringe.addVertex(v1.x, v1.y, ru, 1.0, offset)
                    result.add(fringe)
                }

            }
            return result

        } else {
            return emptyList()
        }
    }
}

internal class PathPoint {
    var x: Double = 0.0
    var y: Double = 0.0
    var dx: Double = 0.0
    var dy: Double = 0.0
    var length: Double = 0.0
    var dmx: Double = 0.0
    var dmy: Double = 0.0
    var flags: Int = 0

    companion object {
        const val CORNER = 0x01
        const val LEFT = 0x02
        const val BEVEL = 0x04
        const val INNER_BEVEL = 0x08
    }

    override fun toString(): String {
        return "PathPoint(x=$x, y=$y, flags=$flags)"
    }
}

internal fun curveDivs(r: Double, arc: Double, tol: Double): Int {
    val da = acos(r / (r + tol)) * 2.0
    return max(2, ceil(arc / da).toInt())
}
