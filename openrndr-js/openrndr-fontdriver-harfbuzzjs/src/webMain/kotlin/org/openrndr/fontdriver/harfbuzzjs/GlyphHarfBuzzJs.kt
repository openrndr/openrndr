@file:OptIn(ExperimentalWasmJsInterop::class)

package org.openrndr.fontdriver.harfbuzzjs

import org.openrndr.draw.font.Glyph
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A [Glyph] of a [FaceHarfBuzzJs]
 */
class GlyphHarfBuzzJs internal constructor(
    private val face: FaceHarfBuzzJs,
    override val index: Int,
    override val code: Int
) : Glyph {

    /** x bearing, y bearing, width and (negative) height in font units */
    private val extents by lazy { hbGlyphExtents(face.hbFont, index) }

    /**
     * Calls [command] for each command of the outline in font units with y pointing up: 'M' (x, y), 'L' (x, y),
     * 'Q' (cx, cy, x, y), 'C' (c1x, c1y, c2x, c2y, x, y) and 'Z'
     */
    private fun outline(command: (type: Char, values: DoubleArray) -> Unit) {
        val path = hbGlyphPath(face.hbFont, index)
        val types = hbPathTypes(path)
        val values = hbPathValues(path)
        var offset = 0
        for (type in types) {
            val count = when (type) {
                'M', 'L' -> 2
                'Q' -> 4
                'C' -> 6
                else -> 0
            }
            command(type, DoubleArray(count) { float64At(values, offset + it) })
            offset += count
        }
    }

    override fun shape(): Shape {
        val scale = face.scale
        // pixels with y pointing down
        fun point(values: DoubleArray, i: Int) = Vector2(values[i] * scale, -values[i + 1] * scale)

        val contours = mutableListOf<ShapeContour>()
        var segments = mutableListOf<Segment2D>()
        var start = Vector2.ZERO
        var cursor = Vector2.ZERO

        fun finish() {
            if (segments.isNotEmpty()) {
                if (cursor != start) {
                    segments.add(Segment2D(cursor, start))
                }
                contours.add(ShapeContour(segments, true))
            }
            segments = mutableListOf()
        }

        outline { type, values ->
            when (type) {
                'M' -> {
                    finish()
                    start = point(values, 0)
                    cursor = start
                }
                'L' -> {
                    val end = point(values, 0)
                    segments.add(Segment2D(cursor, end))
                    cursor = end
                }
                'Q' -> {
                    val end = point(values, 2)
                    segments.add(Segment2D(cursor, point(values, 0), end))
                    cursor = end
                }
                'C' -> {
                    val end = point(values, 4)
                    segments.add(Segment2D(cursor, point(values, 0), point(values, 2), end))
                    cursor = end
                }
                'Z' -> finish()
            }
        }
        finish()
        return Shape(contours)
    }

    override fun advanceWidth(): Double = hbGlyphHAdvance(face.hbFont, index) * face.scale

    override fun leftSideBearing(): Double = int32At(extents, 0) * face.scale

    /**
     * The distance from the ascender to the top of the glyph, the fonts' vertical metrics are not used
     */
    override fun topSideBearing(): Double = (face.ascentMetrics() - int32At(extents, 1)) * face.scale

    override fun bounds(): Rectangle {
        val scale = face.scale
        return Rectangle(
            int32At(extents, 0) * scale,
            -int32At(extents, 1) * scale,
            int32At(extents, 2) * scale,
            -int32At(extents, 3) * scale
        )
    }

    override fun bitmapBounds(contentScale: Double, subpixel: Boolean): IntRectangle {
        val scale = face.scale * contentScale
        val xMin = int32At(extents, 0) * scale
        val xMax = (int32At(extents, 0) + int32At(extents, 2)) * scale
        val yTop = -int32At(extents, 1) * scale
        val yBottom = -(int32At(extents, 1) + int32At(extents, 3)) * scale
        val left = floor(xMin).toInt()
        val top = floor(yTop).toInt()
        return IntRectangle(left, top, ceil(xMax).toInt() - left, ceil(yBottom).toInt() - top)
    }

    override fun rasterize(contentScale: Double, bitmap: MPPBuffer, stride: Int, subpixel: Boolean) {
        val bounds = bitmapBounds(contentScale, subpixel)
        if (bounds.width <= 0 || bounds.height <= 0) {
            return
        }
        val scale = face.scale * contentScale
        val rasterizer = CoverageRasterizer(bounds.width, bounds.height)
        // pixels of the bitmap with y pointing down
        fun x(values: DoubleArray, i: Int) = values[i] * scale - bounds.x
        fun y(values: DoubleArray, i: Int) = -values[i + 1] * scale - bounds.y

        var open = false
        outline { type, values ->
            when (type) {
                'M' -> {
                    if (open) rasterizer.close()
                    rasterizer.moveTo(x(values, 0), y(values, 0))
                    open = true
                }
                'L' -> rasterizer.lineTo(x(values, 0), y(values, 0))
                'Q' -> rasterizer.quadTo(x(values, 0), y(values, 0), x(values, 2), y(values, 2))
                'C' -> rasterizer.cubicTo(x(values, 0), y(values, 0), x(values, 2), y(values, 2), x(values, 4), y(values, 4))
                'Z' -> {
                    rasterizer.close()
                    open = false
                }
            }
        }
        if (open) rasterizer.close()

        // the bitmap is positioned at the top left corner of the glyph
        val origin = bitmap.position()
        rasterizer.coverage { i, value ->
            bitmap.position(origin + (i / bounds.width) * stride + i % bounds.width)
            bitmap.put(value.toByte())
        }
        bitmap.position(origin)
    }
}
