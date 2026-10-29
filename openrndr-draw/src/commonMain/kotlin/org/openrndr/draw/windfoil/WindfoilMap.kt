package org.openrndr.draw.windfoil

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.boundary.BoundaryMap
import org.openrndr.math.Vector2
import org.openrndr.shape.FillRule
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Shape
import org.openrndr.shape.toQuadratics
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * The bands of a shape in a [WindfoilMap]
 * @property bounds the bounds of the shape
 * @property rowBase the index of the first band of the shape in [WindfoilMap.rows]
 * @property bandCount the number of horizontal bands over the height of [bounds]
 * @property bandHeight the height of a band
 * @property inverseBandHeight the reciprocal of [bandHeight], 0.0 for shapes with a single band
 * @property fillRule the fill rule of the shape
 */
class WindfoilShape(
    val bounds: Rectangle,
    val rowBase: Int,
    val bandCount: Int,
    val bandHeight: Double,
    val inverseBandHeight: Double,
    val fillRule: FillRule
)

/**
 * GPU storage for shapes drawn with the windfoil algorithm, which computes the box filtered coverage of a pixel from
 * the winding integral of the curves of a shape over the pixel. Shapes are split in pieces that are monotone in x and
 * y, which are filed in horizontal bands.
 *
 * [curves] holds three texels per piece (start, control and end point) and must be an RG FLOAT32 color buffer.
 * [rows] holds two texels per band and must be an RGBa UINT32_INT color buffer, with the first piece and the number
 * of pieces of the band, and the ink profile of the band: its winding density and the x range of its pieces.
 * Both are addressed as flat arrays in rows of the width of the color buffer.
 */
class WindfoilMap(val curves: ColorBuffer, val rows: ColorBuffer) : BoundaryMap {
    init {
        require(curves.format == ColorFormat.RG && curves.type == ColorType.FLOAT32) {
            "curves must be an RG FLOAT32 color buffer"
        }
        require(rows.format == ColorFormat.RGBa && rows.type == ColorType.UINT32_INT) {
            "rows must be an RGBa UINT32_INT color buffer"
        }
        curves.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
        rows.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
    }

    val shapes = mutableListOf<WindfoilShape>()

    private val pieceData = mutableListOf<Float>()
    private val rowData = mutableListOf<Int>()
    private var uploadedPieceTexels = 0
    private var uploadedRowTexels = 0
    private var batching = false

    /**
     * Starts a batch, in which shapes are uploaded to the GPU once [endBatch] is called instead of after each shape
     */
    override fun startBatch() {
        require(!batching)
        batching = true
    }

    override fun endBatch() {
        require(batching)
        batching = false
        upload()
    }

    /**
     * Adds [shape] and returns its index. Open contours are closed, like contours of filled shapes in SVG.
     * @param quadraticTolerance the tolerance with which cubic segments are approximated with quadratic segments
     */
    override fun addShape(shape: Shape, fillRule: FillRule, quadraticTolerance: Double): Int {
        val bounds = if (shape.contours.isEmpty()) Rectangle(0.0, 0.0, 0.0, 0.0) else shape.bounds
        val pieces = mutableListOf<Piece>()
        for (contour in shape.contours) {
            val segments = contour.segments.toMutableList()
            if (segments.isNotEmpty() && segments.last().end != segments.first().start) {
                segments.add(Segment2D(segments.last().end, segments.first().start))
            }
            for (segment in segments) {
                for (quadratic in segment.toQuadratics(quadraticTolerance)) {
                    val control = quadratic.control.firstOrNull() ?: (quadratic.start + quadratic.end) * 0.5
                    monotonePieces(Piece(quadratic.start, control, quadratic.end), pieces)
                }
            }
        }
        val windfoilShape = bandPieces(pieces, bounds, fillRule)
        shapes.add(windfoilShape)
        if (!batching) {
            upload()
        }
        return shapes.size - 1
    }

    private fun bandPieces(pieces: List<Piece>, bounds: Rectangle, fillRule: FillRule): WindfoilShape {
        val y0 = bounds.y
        val y1 = bounds.y + bounds.height
        val bandCount = if (pieces.size <= TARGET_PER_BAND) 1 else min(ceil(pieces.size / TARGET_PER_BAND.toDouble()).toInt(), MAX_BANDS)
        val inverseBandHeight = if (bandCount > 1 && y1 > y0) bandCount / (y1 - y0) else 0.0
        // the band height and its reciprocal are uploaded as floats, band edges in the shader use the uploaded values
        val bandHeight = if (bandCount > 1 && inverseBandHeight > 0.0) (1.0f / inverseBandHeight.toFloat()).toDouble() else y1 - y0

        fun bandIndex(y: Double): Int =
            if (inverseBandHeight <= 0.0) 0 else floor((y - y0) * inverseBandHeight).toInt().coerceIn(0, bandCount - 1)

        val buckets = List(bandCount) { mutableListOf<Piece>() }
        var xLeft = Double.POSITIVE_INFINITY
        for (piece in pieces) {
            for (b in bandIndex(piece.yMin)..bandIndex(piece.yMax)) {
                buckets[b].add(piece)
            }
            xLeft = min(xLeft, piece.xMin)
        }

        val rowBase = rowData.size / 8
        for ((band, bucket) in buckets.withIndex()) {
            // sorted bands let the shader stop at the first piece left of the pixel
            if (bucket.size > SORT_MIN) {
                bucket.sortByDescending { it.xMax }
            }
            val start = pieceData.size / 6
            for (piece in bucket) {
                pieceData.add(piece.start.x.toFloat()); pieceData.add(piece.start.y.toFloat())
                pieceData.add(piece.control.x.toFloat()); pieceData.add(piece.control.y.toFloat())
                pieceData.add(piece.end.x.toFloat()); pieceData.add(piece.end.y.toFloat())
            }
            // an empty band gets an inverted hull, which holds no ink
            val xMin = bucket.minOfOrNull { it.xMin } ?: 3e38
            val xMax = bucket.maxOfOrNull { it.xMax } ?: -3e38
            val area = if (bucket.isEmpty()) 0.0 else bandWindingArea(bucket, xLeft, y0 + band * bandHeight, y0 + (band + 1) * bandHeight)
            val density = area / max(bandHeight * (xMax - xMin), 1e-30)
            rowData.add(start); rowData.add(bucket.size); rowData.add(density.toFloat().toBits()); rowData.add(xMin.toFloat().toBits())
            rowData.add(xMax.toFloat().toBits()); rowData.add(0); rowData.add(0); rowData.add(0)
        }
        return WindfoilShape(bounds, rowBase, bandCount, bandHeight, inverseBandHeight, fillRule)
    }

    private fun upload() {
        val pieceTexels = pieceData.size / 2
        if (pieceTexels > uploadedPieceTexels) {
            require(pieceTexels <= curves.width * curves.height) { "curves color buffer is full" }
            writeTexels(curves, uploadedPieceTexels, pieceTexels, 8) { buffer, texel ->
                buffer.putFloat(pieceData[texel * 2]); buffer.putFloat(pieceData[texel * 2 + 1])
            }
            uploadedPieceTexels = pieceTexels
        }
        val rowTexels = rowData.size / 4
        if (rowTexels > uploadedRowTexels) {
            require(rowTexels <= rows.width * rows.height) { "rows color buffer is full" }
            writeTexels(rows, uploadedRowTexels, rowTexels, 16) { buffer, texel ->
                for (i in 0 until 4) buffer.putInt(rowData[texel * 4 + i])
            }
            uploadedRowTexels = rowTexels
        }
    }

    /**
     * Writes texels [from] until [to] of the flat array in [colorBuffer], one texture row at a time
     */
    private fun writeTexels(colorBuffer: ColorBuffer, from: Int, to: Int, bytesPerTexel: Int, put: (MPPBuffer, Int) -> Unit) {
        var texel = from
        while (texel < to) {
            val x = texel % colorBuffer.width
            val y = texel / colorBuffer.width
            val width = min(colorBuffer.width - x, to - texel)
            val buffer = MPPBuffer.allocate(width * bytesPerTexel)
            for (i in 0 until width) put(buffer, texel + i)
            buffer.rewind()
            colorBuffer.write(buffer, x = x, y = y, width = width, height = 1)
            texel += width
        }
    }

    companion object {
        // pieces per band before duplication, bands with more pieces than SORT_MIN are sorted by their maximum x,
        // SORT_MIN must match the shader
        internal const val TARGET_PER_BAND = 10
        internal const val MAX_BANDS = 64
        internal const val SORT_MIN = 4
    }
}

/**
 * A quadratic Bézier piece
 */
internal class Piece(val start: Vector2, val control: Vector2, val end: Vector2) {
    val xMin get() = min(start.x, min(control.x, end.x))
    val xMax get() = max(start.x, max(control.x, end.x))
    val yMin get() = min(start.y, min(control.y, end.y))
    val yMax get() = max(start.y, max(control.y, end.y))
}

/**
 * The parameter of the extremum of the quadratic component [p0], [p1], [p2] when it lies strictly inside (0, 1)
 */
private fun extremumT(p0: Double, p1: Double, p2: Double): Double? {
    val a = p0 - 2.0 * p1 + p2
    if (a == 0.0) return null
    val t = (p0 - p1) / a
    return if (t > 0.0 && t < 1.0) t else null
}

private fun subdivide(piece: Piece, t: Double): Pair<Piece, Piece> {
    val a = piece.start + (piece.control - piece.start) * t
    val b = piece.control + (piece.end - piece.control) * t
    val m = a + (b - a) * t
    return Piece(piece.start, a, m) to Piece(m, b, piece.end)
}

/**
 * Splits [piece] at its extrema in x and y into one to three pieces that are monotone in x and y and adds them to [out]
 */
internal fun monotonePieces(piece: Piece, out: MutableList<Piece>) {
    val tx = extremumT(piece.start.x, piece.control.x, piece.end.x)
    val ty = extremumT(piece.start.y, piece.control.y, piece.end.y)
    val splits = listOfNotNull(tx, ty).sorted()
    var rest = piece
    var consumed = 0.0
    for (t in splits) {
        val denominator = 1.0 - consumed
        val local = if (denominator > 0.0) (t - consumed) / denominator else 1.0
        if (!(local > 0.0 && local < 1.0)) continue
        val (left, right) = subdivide(rest, local)
        out.add(left)
        rest = right
        consumed = t
    }
    out.add(rest)
}

/**
 * Solves y(t) = [v] for a monotone quadratic y(t) = [a] t² + [b] t + [e0] with y(1) = [e1], saturating at the
 * endpoints
 */
private fun monotoneRoot(a: Double, b: Double, e0: Double, e1: Double, v: Double, rising: Boolean): Double {
    if (if (rising) e0 >= v else e0 <= v) return 0.0
    if (if (rising) e1 <= v else e1 >= v) return 1.0
    val c = e0 - v
    if (abs(a) < 1e-12 * max(abs(b), 1.0)) return (-c / b).coerceIn(0.0, 1.0)
    val discriminant = max(b * b - 4.0 * a * c, 0.0)
    val q = -0.5 * (b + (if (b == 0.0) 1.0 else sign(b)) * sqrt(discriminant))
    val r1 = q / a
    val r2 = if (q != 0.0) c / q else 0.0
    val want = if (rising) 1.0 else -1.0
    val t = if ((2.0 * a * r1 + b) * want >= 0.0) r1 else r2
    return t.coerceIn(0.0, 1.0)
}

/**
 * The winding integral of [pieces] over the band [b0, b1], with x relative to [x0]
 */
internal fun bandWindingArea(pieces: List<Piece>, x0: Double, b0: Double, b1: Double): Double {
    var area = 0.0
    for (piece in pieces) {
        val (sx, sy) = piece.start
        val (cx, cy) = piece.control
        val (ex, ey) = piece.end
        val lo = max(b0, min(sy, ey))
        val hi = min(b1, max(sy, ey))
        if (hi <= lo) continue
        val rising = ey >= sy
        val ay = sy - 2.0 * cy + ey
        val by = 2.0 * (cy - sy)
        val ta = monotoneRoot(ay, by, sy, ey, if (rising) lo else hi, rising)
        val tb = monotoneRoot(ay, by, sy, ey, if (rising) hi else lo, rising)
        if (tb <= ta) continue
        val ax = sx - 2.0 * cx + ex
        val bx = 2.0 * (cx - sx)
        val c = sx - x0
        // (ax t² + bx t + c)(2 ay t + by), integrated term by term
        val c3 = 2.0 * ax * ay
        val c2 = ax * by + 2.0 * bx * ay
        val c1 = bx * by + 2.0 * c * ay
        val c0 = c * by
        fun f(t: Double) = ((c3 / 4.0 * t + c2 / 3.0) * t + c1 / 2.0) * t * t + c0 * t
        area += f(tb) - f(ta)
    }
    return area
}
