package org.openrndr.internal

import org.openrndr.draw.LineCap
import org.openrndr.draw.LineJoin
import org.openrndr.internal.PathPoint.Companion.BEVEL
import org.openrndr.internal.PathPoint.Companion.CORNER
import org.openrndr.internal.PathPoint.Companion.INNER_BEVEL
import org.openrndr.internal.PathPoint.Companion.CLIP
import org.openrndr.internal.PathPoint.Companion.LEFT
import org.openrndr.math.Vector2
import kotlin.math.*

private val List<PathPoint>.renderable: Boolean
    get() = when (size) {
        0, 1 -> false
        2 -> {
            val dx = this[1].x - this[0].x
            val dy = this[1].y - this[0].y
            dx * dx + dy * dy > 10E-6
        }
        else -> true
    }

internal enum class ExpansionType {
    STROKE,
    FILL,
    FRINGE,
    SKIP
}

/**
 * Represents an expansion operation for a specific vertex buffer,
 * used for rendering paths with strokes, joins, caps, or other geometric operations.
 *
 * @param type The type of expansion operation, defined by the `ExpansionType` enumeration.
 * @param fb The FloatArray serving as the vertex buffer to store calculated geometry.
 * @param bufferStart The starting index (in floats) from which geometry data will be written in `fb`
 */
internal class Expansion(
    val type: ExpansionType,
    val fb: FloatArray,
    val bufferStart: Int,
    private val bufferEnd: Int = fb.size
) {
    var vertexCount = 0
    var minx = Double.POSITIVE_INFINITY
    var maxx = Double.NEGATIVE_INFINITY
    var miny = Double.POSITIVE_INFINITY
    var maxy = Double.NEGATIVE_INFINITY
    var bufferPosition = bufferStart

    /** the corners that [chooseBevel] chose */
    private var bevelX0 = 0.0
    private var bevelY0 = 0.0
    private var bevelX1 = 0.0
    private var bevelY1 = 0.0

    private fun chooseBevel(bevel: Boolean, p0: PathPoint, p1: PathPoint, w: Double) {
        if (bevel) {
            bevelX0 = p1.x + p0.dy * w
            bevelY0 = p1.y - p0.dx * w
            bevelX1 = p1.x + p1.dy * w
            bevelY1 = p1.y - p1.dx * w
        } else {
            bevelX0 = p1.x + p1.dmx * w
            bevelY0 = p1.y + p1.dmy * w
            bevelX1 = p1.x + p1.dmx * w
            bevelY1 = p1.y + p1.dmy * w
        }
    }

    /**
     * Emits the bevel triangle (outer corner [o0], inner miter point [inner], outer corner [o1])
     * with its own vertices, so it can be anti-aliased along the bevel edge [o0]-[o1] alone.
     *
     * The regular per-vertex across-stroke coordinate (0 at the outer corners, 1 at the inner miter
     * point) fades the triangle linearly from the bevel edge all the way to the inner miter point,
     * which for a sharp join lies many stroke widths back, smearing the bevel edge out over that
     * distance. Instead, the bevel is treated like a butt cap:
     * - u runs across the stroke, along the bevel edge, centered on its middle and in the units of
     *   the rest of the stroke, which is [width] wide from u = 0 to u = 1. The ends of the bevel edge
     *   lie on the sides of the stroke, the inner miter point's projection onto the bevel edge lies
     *   in between, so the sides fade as they do along the rest of the stroke;
     * - v fades the bevel edge itself: 0 on the edge, reaching 1 at [aa] from it (the stroke shader
     *   saturates it with `min(1, v)`, which keeps the fade sharp however far the inner point is).
     *
     * The surrounding vertices are repeated so the extra triangles in the strip are degenerate.
     */
    private fun bevelTriangle(o0x: Double, o0y: Double, ix: Double, iy: Double, o1x: Double, o1y: Double, aa: Double, width: Double, offset: Double) {
        val ex = o1x - o0x
        val ey = o1y - o0y
        val el2 = ex * ex + ey * ey
        val d = if (el2 > 0.0) abs((ix - o0x) * ey - (iy - o0y) * ex) / sqrt(el2) else 0.0
        val t = if (el2 > 0.0) (((ix - o0x) * ex + (iy - o0y) * ey) / el2).coerceIn(0.0, 1.0) else 0.5
        // the bevel edge spans this part of the width of the stroke
        val span = if (width > 0.0) (sqrt(el2) / width).coerceAtMost(1.0) else 1.0
        val u0 = 0.5 - 0.5 * span
        val u1 = 0.5 + 0.5 * span
        addVertex(o0x, o0y, u0, 0.0, offset)
        addVertex(ix, iy, u0 + t * (u1 - u0), d / aa, offset)
        addVertex(o1x, o1y, u1, 0.0, offset)
        addVertex(o1x, o1y, u1, 0.0, offset)
    }

    /**
     * @param aa the fringe width; when positive, a beveled join with an inner miter point is
     * anti-aliased along its bevel edge only (see [bevelTriangle])
     */
    fun bevelJoin(
        p0: PathPoint, p1: PathPoint,
        lw: Double, rw: Double, lu: Double, ru: Double,
        offset: Double,
        aa: Double = 0.0,
        mu: Double = 0.5,
        miterLimit: Double = Double.POSITIVE_INFINITY
    ) {
        val dlx0 = p0.dy
        val dly0 = -p0.dx
        val dlx1 = p1.dy
        val dly1 = -p1.dx

        if (dlx0 != dlx0 || dly0 != dly0 || dlx1 != dlx1 || dly1 != dly1 || lw != lw || rw != rw || p0.x != p0.x || p0.y != p0.y || p1.x != p1.x || p1.y != p1.y)
            return

        if (p1.flags and LEFT != 0) {
            chooseBevel(p1.flags and INNER_BEVEL != 0, p0, p1, lw)
            val lx0 = bevelX0
            val ly0 = bevelY0
            val lx1 = bevelX1
            val ly1 = bevelY1

            addVertex(lx0, ly0, lu, 1.0, offset)
            addVertex(p1.x - dlx0 * rw, p1.y - dly0 * rw, ru, 1.0, offset)

            if (p1.flags and BEVEL != 0 && p1.flags and INNER_BEVEL == 0 && aa > 0.0) {
                // turning left: the bevel is on the right, the inner miter point (lx0 == lx1) on the left
                bevelTriangle(p1.x - dlx0 * rw, p1.y - dly0 * rw, lx0, ly0, p1.x - dlx1 * rw, p1.y - dly1 * rw, aa, lw + rw, offset)
            } else if (p1.flags and BEVEL != 0) {
                addVertex(lx0, ly0, lu, 1.0, offset)
                addVertex(p1.x - dlx0 * rw, p1.y - dly0 * rw, ru, 1.0, offset)

                addVertex(lx1, ly1, lu, 1.0, offset)
                addVertex(p1.x - dlx1 * rw, p1.y - dly1 * rw, ru, 1.0, offset)
            } else if (p1.flags and CLIP != 0) {
                // the clipped miter on the right
                clippedMiter(p0, p1, -1.0, rw, lx0, ly0, lx1, ly1, aa, miterLimit, offset)
            } else {
                val rx0 = p1.x - p1.dmx * rw
                val ry0 = p1.y - p1.dmy * rw

                addVertex(p1.x, p1.y, mu, 1.0, offset)
                addVertex(p1.x - dlx0 * rw, p1.y - dly0 * rw, ru, 1.0, offset)

                addVertex(rx0, ry0, ru, 1.0, offset)
                addVertex(rx0, ry0, ru, 1.0, offset)

                addVertex(p1.x, p1.y, mu, 1.0, offset)
                addVertex(p1.x - dlx1 * rw, p1.y - dly1 * rw, ru, 1.0, offset)
            }

            addVertex(lx1, ly1, lu, 1.0, offset)
            addVertex(p1.x - dlx1 * rw, p1.y - dly1 * rw, ru, 1.0, offset)

        } else {
            chooseBevel(p1.flags and INNER_BEVEL != 0, p0, p1, -rw)

            val rx0 = bevelX0
            val ry0 = bevelY0
            val rx1 = bevelX1
            val ry1 = bevelY1

            addVertex(p1.x + dlx0 * lw, p1.y + dly0 * lw, lu, 1.0, offset)
            addVertex(rx0, ry0, ru, 1.0, offset)

            if (p1.flags and BEVEL != 0 && p1.flags and INNER_BEVEL == 0 && aa > 0.0) {
                // turning right: the bevel is on the left, the inner miter point (rx0 == rx1) on the right
                bevelTriangle(p1.x + dlx0 * lw, p1.y + dly0 * lw, rx0, ry0, p1.x + dlx1 * lw, p1.y + dly1 * lw, aa, lw + rw, offset)
            } else if (p1.flags and BEVEL != 0) {
                addVertex(p1.x + dlx0 * lw, p1.y + dly0 * lw, lu, 1.0, offset)
                addVertex(rx0, ry0, ru, 1.0, offset)

                addVertex(p1.x + dlx1 * lw, p1.y + dly1 * lw, lu, 1.0, offset)
                addVertex(rx1, ry1, ru, 1.0, offset)
            } else if (p1.flags and CLIP != 0) {
                // the clipped miter on the left
                clippedMiter(p0, p1, 1.0, lw, rx0, ry0, rx1, ry1, aa, miterLimit, offset)
            } else {
                val lx0 = p1.x + p1.dmx * lw
                val ly0 = p1.y + p1.dmy * lw

                addVertex(p1.x + dlx0 * lw, p1.y + dly0 * lw, lu, 1.0, offset)
                addVertex(p1.x, p1.y, mu, 1.0, offset)

                addVertex(lx0, ly0, lu, 1.0, offset)
                addVertex(lx0, ly0, lu, 1.0, offset)

                addVertex(p1.x + dlx1 * lw, p1.y + dly1 * lw, lu, 1.0, offset)
                addVertex(p1.x, p1.y, mu, 1.0, offset)
            }

            addVertex(p1.x + dlx1 * lw, p1.y + dly1 * lw, lu, 1.0, offset)
            addVertex(rx1, ry1, ru, 1.0, offset)
        }
    }

    /**
     * Emits a clipped miter: the polygon of the inner corners ([i0x], [i0y]) and ([i1x], [i1y]), the outer corners of
     * the segments at [p1], and the points where their outer edges reach the clip line, perpendicular to the bisector
     * of the join at the miter limit times the width of the stroke on that side from the vertex. [side] is 1 when the
     * outer side is the left side, -1 when it is the right side, [w] is the width of that side including half of the
     * fringe [aa].
     *
     * u runs from one outer edge to the other, so they fade like the sides of the stroke, and is 0.5 at the inner
     * corners. v is the distance to the clip line in fringes, so the clip line fades like the edge of a bevel. The
     * polygon is connected by degenerate triangles, the strip continues at the corners after the join.
     */
    private fun clippedMiter(
        p0: PathPoint, p1: PathPoint, side: Double, w: Double,
        i0x: Double, i0y: Double, i1x: Double, i1y: Double,
        aa: Double, miterLimit: Double, offset: Double
    ) {
        val o0x = p1.x + side * p0.dy * w
        val o0y = p1.y - side * p0.dx * w
        val o1x = p1.x + side * p1.dy * w
        val o1y = p1.y - side * p1.dx * w
        // the outward bisector
        var bx = side * p1.dmx
        var by = side * p1.dmy
        val bl = sqrt(bx * bx + by * by)
        if (bl < 1E-12) return
        bx /= bl
        by /= bl
        val clipDistance = miterLimit * (w - aa * 0.5) + aa * 0.5
        fun distance(x: Double, y: Double) = clipDistance - ((x - p1.x) * bx + (y - p1.y) * by)
        fun v(x: Double, y: Double) = if (aa > 0.0) distance(x, y) / aa else 1.0

        // the outer edge before continues forwards, the outer edge after backwards, up to the clip line
        val t0 = p0.dx * bx + p0.dy * by
        val t1 = -(p1.dx * bx + p1.dy * by)
        val d0 = distance(o0x, o0y)
        val d1 = distance(o1x, o1y)
        val clipped = t0 > 1E-12 && t1 > 1E-12 && d0 > 0.0 && d1 > 0.0
        val c0x = if (clipped) o0x + p0.dx * d0 / t0 else o0x
        val c0y = if (clipped) o0y + p0.dy * d0 / t0 else o0y
        val c1x = if (clipped) o1x - p1.dx * d1 / t1 else o1x
        val c1y = if (clipped) o1y - p1.dy * d1 / t1 else o1y

        // the corners before the join, then the bevel
        addVertex(i0x, i0y, 0.5, v(i0x, i0y), offset)
        addVertex(o0x, o0y, 0.0, v(o0x, o0y), offset)
        addVertex(i1x, i1y, 0.5, v(i1x, i1y), offset)
        addVertex(o1x, o1y, 1.0, v(o1x, o1y), offset)
        // the clipped miter beyond the bevel
        addVertex(o1x, o1y, 1.0, v(o1x, o1y), offset)
        addVertex(o0x, o0y, 0.0, v(o0x, o0y), offset)
        addVertex(c1x, c1y, 1.0, v(c1x, c1y), offset)
        addVertex(c0x, c0y, 0.0, v(c0x, c0y), offset)
        // back to the outer corner after the join
        addVertex(c0x, c0y, 0.0, v(c0x, c0y), offset)
        addVertex(o1x, o1y, 1.0, v(o1x, o1y), offset)
        addVertex(o1x, o1y, 1.0, v(o1x, o1y), offset)
    }

    fun roundJoin(
        p0: PathPoint, p1: PathPoint,
        lw: Double, rw: Double, lu: Double, ru: Double, ncap: Int, offset: Double,
        mu: Double = 0.5
    ) {

        val dlx0 = p0.dy
        val dly0 = -p0.dx
        val dlx1 = p1.dy
        val dly1 = -p1.dx

        if (p1.flags and LEFT != 0) {

            chooseBevel(p1.flags and INNER_BEVEL != 0, p0, p1, lw)
            val lx0 = bevelX0
            val ly0 = bevelY0
            val lx1 = bevelX1
            val ly1 = bevelY1

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
                addVertex(p1.x, p1.y, mu, 1.0, offset)
                addVertex(rx, ry, ru, 1.0, offset)
            }

            addVertex(lx1, ly1, lu, 1.0, offset)
            addVertex(p1.x - dlx1 * rw, p1.y - dly1 * rw, ru, 1.0, offset)

        } else {
            chooseBevel(p1.flags and INNER_BEVEL != 0, p0, p1, -rw)
            val rx0 = bevelX0
            val ry0 = bevelY0
            val rx1 = bevelX1
            val ry1 = bevelY1

            val a0 = atan2(dly0, dlx0)
            var a1 = atan2(dly1, dlx1)
            if (a1 < a0) {
                a1 += PI * 2
            }
            addVertex(p1.x + dlx0 * lw, p1.y + dly0 * lw, lu, 1.0, offset)
            addVertex(rx0, ry0, ru, 1.0, offset)

            val n = ncap.coerceAtMost(ceil((a1 - a0) / PI * ncap).toInt()).coerceAtLeast(2)

            for (i in 0 until n) {
                val a = a0 + i.toDouble() / (n - 1.0) * (a1 - a0)
                addVertex(p1.x + cos(a) * lw, p1.y + sin(a) * lw, lu, 1.0, offset)
                addVertex(p1.x, p1.y, mu, 1.0, offset)
            }
            addVertex(p1.x + dlx1 * lw, p1.y + dly1 * lw, lu, 1.0, offset)
            addVertex(rx1, ry1, ru, 1.0, offset)
        }
    }

    fun buttCapStart(
        p: PathPoint, dx: Double, dy: Double, w: Double,
        d: Double, aa: Double, offset: Double
    ) {
        if (dx == dx && dy == dy && d == d) {
            val px = p.x - dx * d
            val py = p.y - dy * d
            val dly = -dx

            addVertex(px + dy * w - dx * aa, py + dly * w - dy * aa, 0.0, 0.0, offset)
            addVertex(px - dy * w - dx * aa, py - dly * w - dy * aa, 1.0, 0.0, offset)
            addVertex(px + dy * w, py + dly * w, 0.0, 1.0, offset)
            addVertex(px - dy * w, py - dly * w, 1.0, 1.0, offset)
        }
    }


    fun buttCapEnd(p: PathPoint, dx: Double, dy: Double, w: Double, d: Double, aa: Double, offset: Double) {
        if (dx == dx && dy == dy && d == d) {
            val px = p.x - dx * d
            val py = p.y - dy * d
            val dly = -dx

            addVertex(px + dy * w, py + dly * w, 0.0, 1.0, offset)
            addVertex(px - dy * w, py - dly * w, 1.0, 1.0, offset)
            addVertex(px + dy * w + dx * aa, py + dly * w + dy * aa, 0.0, 0.0, offset)
            addVertex(px - dy * w + dx * aa, py - dly * w + dy * aa, 1.0, 0.0, offset)
        }
    }

    fun roundCapStart(
        p: PathPoint,
        dx: Double, dy: Double, w: Double, ncap: Int, offset: Double
    ) {
        if (dx == dx && dy == dy) {
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
    }

    fun roundCapEnd(
        p: PathPoint,
        dx: Double, dy: Double, w: Double, ncap: Int, offset: Double
    ) {
        if (dx == dx && dy == dy) {
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
    }

    /**
     * Maps the positions of the vertices by the linear map ([a], [b], [c], [d]): x' = a x + b y, y' = c x + d y, the
     * bounds are those of the mapped positions. Vertices with the same position have the same mapped position.
     */
    fun mapPositions(a: Double, b: Double, c: Double, d: Double) {
        minx = Double.POSITIVE_INFINITY
        maxx = Double.NEGATIVE_INFINITY
        miny = Double.POSITIVE_INFINITY
        maxy = Double.NEGATIVE_INFINITY
        for (i in 0 until vertexCount) {
            val index = bufferStart + i * 5
            val x = fb[index].toDouble()
            val y = fb[index + 1].toDouble()
            val mx = a * x + b * y
            val my = c * x + d * y
            fb[index] = mx.toFloat()
            fb[index + 1] = my.toFloat()
            minx = min(minx, mx)
            maxx = max(maxx, mx)
            miny = min(miny, my)
            maxy = max(maxy, my)
        }
    }

    fun vertexX(idx: Int): Double = fb[bufferStart + idx * 5].toDouble()

    fun vertexY(idx: Int): Double = fb[bufferStart + idx * 5 + 1].toDouble()

    fun addVertex(x: Double, y: Double, u: Double, v: Double, offset: Double) {
        if (x != x || y != y) {
            return// throw RuntimeException("$x $y $u $v")
        }

        minx = min(minx, x)
        maxx = max(maxx, x)
        miny = min(miny, y)
        maxy = max(maxy, y)

        if (bufferPosition + 5 > bufferEnd) {
            throw IndexOutOfBoundsException("expansion of $vertexCount vertices is full")
        }
        fb[bufferPosition++] = x.toFloat()
        fb[bufferPosition++] = y.toFloat()
        fb[bufferPosition++] = u.toFloat()
        fb[bufferPosition++] = v.toFloat()
        fb[bufferPosition++] = offset.toFloat()

        vertexCount++
    }
}


internal class Path(private val scratch: ExpansionScratch? = null) {
    var convex: Boolean = false
    var closed: Boolean = false
    private var nbevel: Int = 0
    val contours = mutableListOf<MutableList<PathPoint>>()

    /** a point of the path, from the [scratch] when there is one */
    private fun point(): PathPoint = scratch?.point() ?: PathPoint()

    /** an expansion of [floats] floats, from the [scratch] when there is one */
    private fun expansion(type: ExpansionType, floats: Int): Expansion =
        scratch?.expansion(type, floats) ?: Expansion(type, FloatArray(floats), 0)

    companion object {
        /**
         * Signed area of a closed [loop], the left side (positive cross product turns) of a loop is its outside when
         * the signed area is positive
         */
        fun signedArea(loop: List<Vector2>): Double {
            var area = 0.0
            for (i in loop.indices) {
                val a = loop[i]
                val b = loop[(i + 1) % loop.size]
                area += a.x * b.y - b.x * a.y
            }
            return area * 0.5
        }

        /**
         * A path of the line strip [segments], with the [corners] that are not smoothed. When [map] is given, the
         * points are mapped by the linear map (a, b, c, d) it holds: x' = a x + b y, y' = c x + d y. The points and
         * expansions of the path are taken from [scratch] when it is given.
         */
        fun fromLineStrip(
            segments: List<Vector2>,
            corners: List<Boolean>,
            closed: Boolean,
            scratch: ExpansionScratch? = null,
            map: DoubleArray? = null
        ): Path {
            val sp = Path(scratch)
            if (segments.isEmpty()) {
                return sp
            }
            sp.addContour(segments, corners, if (closed) DROP_CLOSING else KEEP, closed, map)
            sp.closed = closed
            return sp
        }

        /**
         * A path of the closed [contours], whose last point repeats the first, with the [corners] that are not
         * smoothed. When [map] is given, the points are mapped by it, see [fromLineStrip]. The points and expansions
         * of the path are taken from [scratch] when it is given.
         */
        fun fromLineLoops(
            contours: List<List<Vector2>>,
            corners: List<List<Boolean>>,
            scratch: ExpansionScratch? = null,
            map: DoubleArray? = null
        ): Path {
            val sp = Path(scratch)
            for (contourIndex in contours.indices) {
                sp.addContour(contours[contourIndex], corners[contourIndex], DROP_LAST, true, map, always = true)
            }
            sp.closed = true
            return sp
        }

        /**
         * A path of the line segment from [start] to [end], like [fromLineStrip] of the two points
         */
        fun fromSegment(start: Vector2, end: Vector2, scratch: ExpansionScratch? = null): Path {
            val sp = Path(scratch)
            val contour = ArrayList<PathPoint>(2)
            for (position in 0 until 2) {
                val point = sp.point()
                val p = if (position == 0) start else end
                point.x = p.x
                point.y = p.y
                point.flags = 0
                contour.add(point)
            }
            sp.contours.add(contour)
            sp.closed = false
            return sp
        }

        /** keep all points */
        private const val KEEP = 0

        /** drop the last point when it lies on the first */
        private const val DROP_CLOSING = 1

        /** drop the last point */
        private const val DROP_LAST = 2
    }

    /**
     * Adds a contour of [positions] with the [corners] that are not smoothed, mapped by the linear [map] (a, b, c, d)
     * when it is given, without the last position as [drop] tells. Empty contours are only added when [always] is
     * true. The corners of open contours ([closed] is false) are cleared at their ends.
     */
    private fun addContour(
        positions: List<Vector2>,
        corners: List<Boolean>,
        drop: Int,
        closed: Boolean,
        map: DoubleArray?,
        always: Boolean = false
    ) {
        val a = map?.get(0) ?: 1.0
        val b = map?.get(1) ?: 0.0
        val c = map?.get(2) ?: 0.0
        val d = map?.get(3) ?: 1.0
        val n = when (drop) {
            DROP_LAST -> (positions.size - 1).coerceAtLeast(0)
            DROP_CLOSING -> {
                val first = positions[0]
                val last = positions[positions.size - 1]
                val dx: Double
                val dy: Double
                if (map != null) {
                    dx = (a * last.x + b * last.y) - (a * first.x + b * first.y)
                    dy = (c * last.x + d * last.y) - (c * first.x + d * first.y)
                } else {
                    dx = last.x - first.x
                    dy = last.y - first.y
                }
                if (dx * dx + dy * dy < 10E-6) positions.size - 1 else positions.size
            }

            else -> positions.size
        }
        if (n == 0 && !always) {
            return
        }
        val contour = ArrayList<PathPoint>(n)
        for (index in 0 until n) {
            val position = positions[index]
            val point = point()
            if (map != null) {
                point.x = a * position.x + b * position.y
                point.y = c * position.x + d * position.y
            } else {
                point.x = position.x
                point.y = position.y
            }
            point.flags = if (corners[index]) CORNER else 0
            contour.add(point)
        }
        if (!closed && n > 0) {
            contour[0].flags = 0
            contour[n - 1].flags = 0
        }
        contours.add(contour)
    }

    private fun calculateJoins(points: List<PathPoint>, w: Double, lineJoin: LineJoin, miterLimit: Double) {
        if (points.isEmpty()) {
            return
        }
        nbevel = 0

        val iw = if (w > 0.0) 1.0 / w else 0.0
        var nleft = 0
        var turnSum = 0.0

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
                // scale the averaged normal into the miter vector, whose length is 1 / |dm|.
                // NanoVG clamps this scale at 600, which makes the miter of joins sharper than
                // ~4.7 degrees (miter ratio > sqrt(600)) shrink back towards the vertex instead
                // of growing; joins that would produce excessive miters are beveled below instead.
                val scale = 1.0 / dmr2
                p1.dmx *= scale
                p1.dmy *= scale
            }
            p1.flags = if (p1.flags and CORNER != 0) CORNER else 0
            val cross = p1.dx * p0.dy - p0.dx * p1.dy
            if (cross > 0.0) {
                nleft += 1
                p1.flags = p1.flags or LEFT
            }
            val dot = p0.dx * p1.dx + p0.dy * p1.dy
            turnSum += atan2(cross, dot)

            // Calculate if we should use bevel or miter for inner join.
            val limit = max(1.01, min(p0.length, p1.length) * iw)
            if (dmr2 * limit * limit < 1.0f) {
                p1.flags = p1.flags or INNER_BEVEL
            }

            // Check to see if the corner needs to be beveled.
            if (p1.flags and CORNER != 0) {
                if (lineJoin === LineJoin.MITER_CLIP && dmr2 * miterLimit * miterLimit < 1.0f) {
                    p1.flags = p1.flags or CLIP
                } else if (dmr2 * miterLimit * miterLimit < 1.0f || lineJoin === LineJoin.BEVEL || lineJoin === LineJoin.ROUND) {
                    p1.flags = p1.flags or BEVEL
                }
            } else if (dmr2 * miterLimit * miterLimit < 1.0f) {
                // A smooth (non-corner) vertex normally turns only slightly, but one that turns
                // sharply (e.g. at a cusp of a flattened curve) would otherwise get an unbounded
                // miter, now that the miter scale is no longer clamped.
                p1.flags = p1.flags or BEVEL
            }

            if (p1.flags and (BEVEL or INNER_BEVEL or CLIP) != 0) {
                nbevel++
            }
            // clipped miters have a quad beyond their bevel
            if (p1.flags and CLIP != 0) {
                nbevel += 2
            }

            p0 = p1
            p1ptr++
            if (p1ptr < points.size) {
                p1 = points[p1ptr]
            }
        }
        // A simple polygon is convex when every corner turns the same way, regardless of whether
        // that's clockwise or counter-clockwise -- `nleft` alone only recognizes one of the two
        // windings, which misclassifies e.g. the clockwise rectangles OPENRNDR's default
        // (CW_NEGATIVE_Y) polarity produces as non-convex.
        //
        // That sign check alone isn't sufficient, though: a self-intersecting star polygon (e.g.
        // a pentagram traced vertex-to-vertex) also turns the same way at every vertex, but winds
        // around its center more than once. By the turning number theorem, any *simple* closed
        // curve's signed turning sums to exactly one full turn (+-2*PI); a curve that winds twice
        // (like that pentagram) sums to +-4*PI instead. The fast convex fill path assumes a
        // non-overlapping fan triangulation, which only holds for a simple polygon, so both checks
        // are required.
        val sameSignedTurns = nleft == points.size || nleft == 0
        val windsOnce = abs(abs(turnSum) - 2.0 * PI) < 1E-2
        convex = sameSignedTurns && windsOnce
    }

    /**
     * The fill inset and the fringe are offset along the join normals, which point into the filled region only when
     * it lies on the side of left (positive cross product) turns. Reverses all contours when the outer contour, the
     * one with the largest area, winds the other way. Reversing all contours keeps holes wound opposite to the outer
     * contour.
     */
    private fun orientForFill() {
        fun signedArea(points: List<PathPoint>): Double {
            var area = 0.0
            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                area += a.x * b.y - b.x * a.y
            }
            return area * 0.5
        }

        val outerArea = contours.maxByOrNull { abs(signedArea(it)) }?.let { signedArea(it) } ?: return
        if (outerArea > 0.0) {
            for (contour in contours) {
                contour.reverse()
            }
        }
    }

    fun prepare(points: List<PathPoint>) {
        if (points.isEmpty()) {
            return
        }

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

    /**
     * @param strokeWeight half of the stroke weight
     * @param outsideFraction the fraction of the stroke that lies outside of a closed path, 0.5 centers the stroke
     * on the path, open paths are always centered
     * @param leftIsOutside is the left side (positive cross product turns) of a closed path its outside,
     * see [leftIsOutside]
     */
    fun expandStroke(
        fringeWidth: Double,
        strokeWeight: Double,
        lineCap: LineCap,
        lineJoin: LineJoin,
        miterLimit: Double,
        outsideFraction: Double = 0.5,
        leftIsOutside: Boolean = true
    ): Expansion {
        // the fringe is centered on the stroke edge, half of it lies outside of the stroke weight
        val weight = strokeWeight + fringeWidth * 0.5

        // widths of the stroke to the left and right of the path, including half of the fringe
        val lw: Double
        val rw: Double
        if (closed && outsideFraction != 0.5) {
            val outside = 2.0 * strokeWeight * outsideFraction
            val inside = 2.0 * strokeWeight - outside
            lw = (if (leftIsOutside) outside else inside) + fringeWidth * 0.5
            rw = (if (leftIsOutside) inside else outside) + fringeWidth * 0.5
        } else {
            lw = weight
            rw = weight
        }
        // the across-stroke coordinate of the path itself
        val mu = lw / (lw + rw)

        if (contours.isNotEmpty() && contours[0].renderable) {
            val points = contours[0]

            val tessTol = 0.1 * fringeWidth
            val capSteps = curveDivs(max(lw, rw), PI, tessTol)

            prepare(points)

            calculateJoins(points, max(lw, rw), lineJoin, miterLimit)

            var cverts = 0
            cverts += if (lineJoin == LineJoin.ROUND) {
                (points.size + nbevel * (capSteps + 2) + 1) * 2 // plus one for loop
            } else {
                (points.size + nbevel * 5 + 1) * 2 // plus one for loop
            }

            if (!closed) {
                cverts += if (lineCap == LineCap.ROUND) {
                    (capSteps * 2 + 2) * 2
                } else {
                    (3 + 3) * 2
                }
            }

            val expansion = expansion(ExpansionType.STROKE, cverts * 5)

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
                    LineCap.ROUND -> expansion.roundCapStart(p0, dx, dy, weight, capSteps, offset)
                }
            }


            // -- middle
            for (j in start until end) {
                offset += p0.length
                if (p1.flags and (BEVEL or INNER_BEVEL or CLIP) != 0) {
                    if (lineJoin === LineJoin.ROUND) {
                        expansion.roundJoin(p0, p1, lw, rw, 0.0, 1.0, capSteps, offset, mu)
                    } else {
                        expansion.bevelJoin(p0, p1, lw, rw, 0.0, 1.0, offset, aa, mu, miterLimit)
                    }
                } else {
                    expansion.addVertex(p1.x + p1.dmx * lw, p1.y + p1.dmy * lw, 0.0, 1.0, offset)
                    expansion.addVertex(p1.x - p1.dmx * rw, p1.y - p1.dmy * rw, 1.0, 1.0, offset)
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
            } else {
                offset += p0.length
            }

            // -- end
            if (closed) {
                // Loop it
                expansion.addVertex(expansion.vertexX(0), expansion.vertexY(0), 0.0, 1.0, offset)
                expansion.addVertex(expansion.vertexX(1), expansion.vertexY(1), 1.0, 1.0, offset)
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
                    LineCap.BUTT -> expansion.buttCapEnd(p1, dx, dy, weight, aa * 0.5, aa, offset)
                    LineCap.SQUARE -> expansion.buttCapEnd(p1, dx, dy, weight, -weight + aa, aa, offset)
                    LineCap.ROUND -> expansion.roundCapEnd(p1, dx, dy, weight, capSteps, offset)
                }
            }
            return expansion
        } else {
            return expansion(ExpansionType.SKIP, 0)
        }
    }

    /**
     * Expands the closed contours into polygons offset from the outline by [offset] (positive grows the filled
     * region, negative shrinks it), to be rendered with even-odd parity into the stencil buffer to clip strokes.
     */
    fun expandClip(offset: Double): List<Expansion> {
        if (contours.isEmpty()) {
            return emptyList()
        }
        orientForFill()
        val result = ArrayList<Expansion>(contours.size)
        for (points in contours) {
            if (points.size < 3) continue
            prepare(points)
            calculateJoins(points, abs(offset), LineJoin.MITER, Double.POSITIVE_INFINITY)
            val clip = expansion(ExpansionType.FILL, points.size * 5)
            for (point in points) {
                val scale = limitedMiterScale(point)
                val dx = point.dmx * scale
                val dy = point.dmy * scale
                clip.addVertex(point.x - dx * offset, point.y - dy * offset, 0.5, 1.0, 0.0)
            }
            result.add(clip)
        }
        return result
    }

    /**
     * Expands the closed contours into anti-aliasing fringes of width [fringeWidth] centered on the outline. The
     * fringes fade out towards the outside of the filled region when [fadeOutwards] is true, towards the inside
     * otherwise. Rendered with `strokeMult = 1.0`.
     */
    fun expandClipFringe(fringeWidth: Double, fadeOutwards: Boolean): List<Expansion> {
        if (contours.isEmpty()) {
            return emptyList()
        }
        orientForFill()
        val h = fringeWidth * 0.5
        // u = 0.5 is opaque, u = 0.0 is transparent
        val uInside = if (fadeOutwards) 0.5 else 0.0
        val uOutside = if (fadeOutwards) 0.0 else 0.5
        val result = ArrayList<Expansion>(contours.size)
        for (points in contours) {
            if (points.size < 3) continue
            prepare(points)
            calculateJoins(points, h, LineJoin.MITER, Double.POSITIVE_INFINITY)
            val fringe = expansion(ExpansionType.FRINGE, (points.size + 1) * 2 * 5)
            for (i in 0..points.size) {
                val point = points[i % points.size]
                val scale = limitedMiterScale(point)
                val dx = point.dmx * scale
                val dy = point.dmy * scale
                fringe.addVertex(point.x + dx * h, point.y + dy * h, uInside, 1.0, 0.0)
                fringe.addVertex(point.x - dx * h, point.y - dy * h, uOutside, 1.0, 0.0)
            }
            result.add(fringe)
        }
        return result
    }

    /**
     * The scale that limits the miter vector of [point], which points into the filled region after [orientForFill],
     * to length 4
     */
    private fun limitedMiterScale(point: PathPoint): Double {
        val length = sqrt(point.dmx * point.dmx + point.dmy * point.dmy)
        return if (length > 4.0) 4.0 / length else 1.0
    }

    /**
     * The fringes of a stenciled fill of the outline [points]: two strips that are [w] wide, on both sides of the
     * outline. Their vertices on the outline are those of the fill, so that a pixel exactly on the outline is drawn by
     * the strip on the side of the outline that the fill counts it to. u is 0 on the side the miters of the points
     * point to, 0.5 on the outline and 1 on the other side. v is 2 in the strip on the side of the miters, 1 in the
     * other strip.
     */
    private fun centeredFringes(points: List<PathPoint>, w: Double, offset: Double, result: MutableList<Expansion>) {
        var size = 2
        for (point in points) {
            size += if (point.flags and (BEVEL or INNER_BEVEL) != 0) 4 else 2
        }
        val inner = expansion(ExpansionType.FRINGE, size * 5)
        val outer = expansion(ExpansionType.FRINGE, size * 5)

        fun across(p: PathPoint, nx: Double, ny: Double) {
            inner.addVertex(p.x + nx * w, p.y + ny * w, 0.0, 2.0, offset)
            inner.addVertex(p.x, p.y, 0.5, 2.0, offset)
            outer.addVertex(p.x, p.y, 0.5, 1.0, offset)
            outer.addVertex(p.x - nx * w, p.y - ny * w, 1.0, 1.0, offset)
        }

        var p0 = points.last()
        for (p1 in points) {
            if (p1.flags and (BEVEL or INNER_BEVEL) != 0) {
                // around the corner, from the normal of the incoming segment to that of the outgoing segment
                across(p1, p0.dy, -p0.dx)
                across(p1, p1.dy, -p1.dx)
            } else {
                across(p1, p1.dmx, p1.dmy)
            }
            p0 = p1
        }
        // close the strips
        inner.addVertex(inner.vertexX(0), inner.vertexY(0), 0.0, 2.0, offset)
        inner.addVertex(points[0].x, points[0].y, 0.5, 2.0, offset)
        outer.addVertex(points[0].x, points[0].y, 0.5, 1.0, offset)
        outer.addVertex(outer.vertexX(1), outer.vertexY(1), 1.0, 1.0, offset)
        result.add(inner)
        result.add(outer)
    }

    fun expandFill(
        fringeWidth: Double,
        weight: Double,
        lineJoin: LineJoin,
        miterLimit: Double
    ): List<Expansion> {
        if (contours.isNotEmpty()) {
            val result = mutableListOf<Expansion>()

            orientForFill()
            contours.forEach { prepare(it) }
            contours.forEach { calculateJoins(it, weight, lineJoin, miterLimit) }

            if (contours.size > 1) {
                convex = false
            }
            val woff = 0.5 * fringeWidth
            val generateFringe = fringeWidth > 0.0
            val pathOffset = 0.0


            for (points in contours) {
                if (points.isEmpty())
                    continue

                var size = 4
                points.forEach { point ->
                    size += if ((point.flags and BEVEL) != 0) {
                        12
                    } else {
                        4
                    }
                }
                val fill = expansion(ExpansionType.FILL, size * 5)
                // convex fills are inset by half of the fringe, their fringe fades from the inset outline. Other fills
                // are stenciled on their outline, their fringes are centered on it, see ExpansionDrawer
                val fillOffset = if (convex) woff else 0.0
                if (generateFringe) {
                    var p0 = points.last()
                    var p1 = points[0]
                    var p1ptr = 0

                    for (j in points.indices) {
                        if (p1.flags and BEVEL != 0) {
                            if (p1.flags and LEFT != 0) {
                                fill.addVertex(p1.x + p1.dmx * fillOffset, p1.y + p1.dmy * fillOffset, 0.5, 1.0, pathOffset)
                            } else {
                                val dlx0 = p0.dy
                                val dly0 = -p0.dx
                                val dlx1 = p1.dy
                                val dly1 = -p1.dx

                                val lx0 = p1.x + dlx0 * fillOffset
                                val ly0 = p1.y + dly0 * fillOffset
                                val lx1 = p1.x + dlx1 * fillOffset
                                val ly1 = p1.y + dly1 * fillOffset

                                fill.addVertex(lx0, ly0, 0.5, 1.0, pathOffset)
                                fill.addVertex(lx1, ly1, 0.5, 1.0, pathOffset)
                            }
                        } else {
                            fill.addVertex(p1.x + p1.dmx * fillOffset, p1.y + p1.dmy * fillOffset, 0.5, 1.0, pathOffset)
                        }
                        p0 = p1
                        p1ptr++
                        if (p1ptr < points.size) {
                            p1 = points[p1ptr]
                        }
                    }

                } else {
                    for (j in points.indices) {
                        fill.addVertex(points[j].x, points[j].y, 0.5, 1.0, pathOffset)
                    }
                }
                result.add(fill)
            }


            // Calculate fringe
            if (generateFringe) {
                for (points in contours) {

                    if (points.isEmpty()) {
                        continue
                    }

                    if (!convex) {
                        centeredFringes(points, woff, pathOffset, result)
                        continue
                    }

                    var size = 2
                    for (point in points) {
                        size += if (point.flags and (BEVEL or INNER_BEVEL) != 0) {
                            10
                        } else {
                            4
                        }
                    }
                    val fringe = expansion(ExpansionType.FRINGE, size * 5)

                    // Create only half a fringe for convex shapes so that
                    // the shape can be rendered without stenciling.
                    val lw = woff    // This should generate the same vertex as fill inset above.
                    val rw = weight - woff
                    val lu = 0.5    // Set outline fade at middle.
                    val ru = 1.0

                    // Looping
                    var p0 = points[points.size - 1]
                    var p1 = points[0]

                    var p1ptr = 0
                    for (j in points.indices) {
                        if (p1.flags and (BEVEL or INNER_BEVEL) != 0) {
                            fringe.bevelJoin(p0, p1, lw, rw, lu, ru, offset = pathOffset)
                        } else {
                            fringe.addVertex(p1.x + (p1.dmx * lw), p1.y + (p1.dmy * lw), lu, 1.0, pathOffset)
                            fringe.addVertex(p1.x - (p1.dmx * rw), p1.y - (p1.dmy * rw), ru, 1.0, pathOffset)
                        }
                        p0 = p1
                        p1ptr++
                        if (p1ptr < points.size) {
                            p1 = points[p1ptr]
                        }
                    }
                    // Loop it
                    fringe.addVertex(fringe.vertexX(0), fringe.vertexY(0), lu, 1.0, pathOffset)
                    fringe.addVertex(fringe.vertexX(1), fringe.vertexY(1), ru, 1.0, pathOffset)
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

        /** the miter of the join is clipped, see [org.openrndr.draw.LineJoin.MITER_CLIP] */
        const val CLIP = 0x10
    }

    /** clears the point, for reuse */
    fun clear() {
        x = 0.0; y = 0.0; dx = 0.0; dy = 0.0; length = 0.0; dmx = 0.0; dmy = 0.0; flags = 0
    }

    override fun toString(): String {
        return "PathPoint(x=$x, y=$y, flags=$flags)"
    }
}

/**
 * The memory of the paths and expansions of a draw: the points of paths and the vertex data of expansions. It is
 * reused by the next draw after [reset], expansions and paths taken from it are only valid until then. Not thread
 * safe, every drawer has its own.
 */
internal class ExpansionScratch {
    private companion object {
        /** the size of the chunks of vertex data, in floats */
        const val CHUNK_SIZE = 64 * 1024
    }

    private val chunks = ArrayList<FloatArray>()
    private var chunkIndex = 0
    private var chunkPosition = 0

    private val points = ArrayList<PathPoint>()
    private var pointCount = 0

    /** makes the memory available to the next draw, invalidates the expansions and paths taken from it */
    fun reset() {
        chunkIndex = 0
        chunkPosition = 0
        pointCount = 0
    }

    /** a cleared point */
    fun point(): PathPoint {
        if (pointCount == points.size) {
            points.add(PathPoint())
        }
        return points[pointCount++].also { it.clear() }
    }

    /** an expansion with room for [floats] floats of vertex data */
    fun expansion(type: ExpansionType, floats: Int): Expansion {
        while (true) {
            if (chunkIndex == chunks.size) {
                chunks.add(FloatArray(max(CHUNK_SIZE, floats)))
                chunkPosition = 0
            }
            val chunk = chunks[chunkIndex]
            if (chunk.size - chunkPosition >= floats) {
                val expansion = Expansion(type, chunk, chunkPosition, chunkPosition + floats)
                chunkPosition += floats
                return expansion
            }
            if (chunkPosition == 0) {
                // a chunk that is too small for the expansion on its own is replaced by a larger one
                chunks[chunkIndex] = FloatArray(max(CHUNK_SIZE, floats))
            } else {
                chunkIndex++
                chunkPosition = 0
            }
        }
    }
}

internal fun curveDivs(r: Double, arc: Double, tol: Double): Int {
    val da = acos(r / (r + tol)) * 2.0
    return max(2, ceil(arc / da).toInt())
}
