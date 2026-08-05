package org.openrndr.fontdriver.freetype

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.freetype.FT_Glyph
import org.lwjgl.util.freetype.FT_OutlineGlyph
import org.lwjgl.util.freetype.FT_Vector
import org.lwjgl.util.freetype.FreeType.FT_Get_Glyph
import org.lwjgl.util.freetype.FreeType.FT_LOAD_DEFAULT
import org.lwjgl.util.freetype.FreeType.FT_Load_Glyph
import org.lwjgl.util.freetype.FreeType.FT_Render_Glyph
import org.lwjgl.util.freetype.FreeType.FT_RENDER_MODE_NORMAL
import org.openrndr.draw.font.Glyph
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.Segment2D
import org.openrndr.utils.buffer.MPPBuffer

class GlyphFreetype(private val face: FaceFreetype, private val character: Char, override val index: Int, override val code: Int) : Glyph {

    //val scale = face.sizeInPoints

    override fun shape(): Shape {
        face.makeActive()
        FT_Load_Glyph(face.ftFace, index, FT_LOAD_DEFAULT)
        val glyph = PointerBuffer.allocateDirect(1)
        FT_Get_Glyph(face.ftFace.glyph() ?: error("no slot"), glyph)
        val realGlyph = FT_Glyph.create(glyph.get(0))

        val outlineGlyph = FT_OutlineGlyph.create(realGlyph.address())
        val outline = outlineGlyph.outline()

        val contours = mutableListOf<ShapeContour>()

        val nContours = outline.n_contours()
        val nPoints = outline.n_points()

        if (nContours <= 0 || nPoints <= 0) {
            return Shape.EMPTY
        }

        val points = outline.points()
        val tags = outline.tags()
        val contourEnds = outline.contours()

        var startPointIdx = 0
        for (c in 0 until nContours) {
            val endPointIdx = contourEnds.get(c).toInt()
            val contourPoints = mutableListOf<Vector2>()
            val contourTags = mutableListOf<Byte>()

            for (p in startPointIdx..endPointIdx) {
                val pt = points.get(p)
                contourPoints.add(Vector2(pt.x() / 64.0, -pt.y() / 64.0))
                contourTags.add(tags.get(p))
            }

            if (contourPoints.isNotEmpty()) {
                val segments = mutableListOf<Segment2D>()

                // Helper to get point and tag with wrapping
                fun getPoint(i: Int) = contourPoints[i % contourPoints.size]
                fun getTag(i: Int) = contourTags[i % contourTags.size].toInt()
                fun isOn(i: Int) = (getTag(i) and 1) != 0
                fun isConic(i: Int) = (getTag(i) and 1) == 0 && (getTag(i) and 2) == 0
                fun isCubic(i: Int) = (getTag(i) and 1) == 0 && (getTag(i) and 2) != 0

                val n = contourPoints.size
                var startIdx = 0
                if (!isOn(0)) {
                    // Find an on-curve point to start with
                    var found = false
                    for (i in 0 until n) {
                        if (isOn(i)) {
                            startIdx = i
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        // All points are off-curve (conics).
                        // The start point is the midpoint between the last and first points.
                        val first = getPoint(0)
                        val last = getPoint(n - 1)
                        var currentPos = (first + last) * 0.5
                        val startPos = currentPos
                        for (i in 0 until n) {
                            val nextOff = getPoint(i)
                            val nextOffNext = getPoint(i + 1)
                            val nextOn = (nextOff + nextOffNext) * 0.5
                            segments.add(Segment2D(currentPos, nextOff, nextOn))
                            currentPos = nextOn
                        }
                        contours.add(ShapeContour(segments, true))
                        startPointIdx = endPointIdx + 1
                        continue
                    }
                }

                var currentIdx = startIdx
                var currentPos = getPoint(currentIdx)
                val totalSteps = n
                var stepsTaken = 0

                while (stepsTaken < totalSteps) {
                    val nextIdx = (currentIdx + 1) % n
                    if (isOn(nextIdx)) {
                        segments.add(Segment2D(currentPos, getPoint(nextIdx)))
                        currentPos = getPoint(nextIdx)
                        currentIdx = nextIdx
                        stepsTaken++
                    } else if (isConic(nextIdx)) {
                        val offIdx = nextIdx
                        val nextNextIdx = (offIdx + 1) % n
                        if (isOn(nextNextIdx)) {
                            segments.add(Segment2D(currentPos, getPoint(offIdx), getPoint(nextNextIdx)))
                            currentPos = getPoint(nextNextIdx)
                            currentIdx = nextNextIdx
                            stepsTaken += 2
                        } else {
                            // Two consecutive conics, insert on-curve point at midpoint
                            val off1 = getPoint(offIdx)
                            val off2 = getPoint(nextNextIdx)
                            val mid = (off1 + off2) * 0.5
                            segments.add(Segment2D(currentPos, off1, mid))
                            currentPos = mid
                            currentIdx = offIdx // Move to the first off-curve point
                            stepsTaken += 1
                        }
                    } else if (isCubic(nextIdx)) {
                        val off1 = getPoint(nextIdx)
                        val off2Idx = (nextIdx + 1) % n
                        val nextNextNextIdx = (nextIdx + 2) % n
                        // Cubic segments in fonts (like Type 1) have two off-curve points
                        segments.add(Segment2D(currentPos, off1, getPoint(off2Idx), getPoint(nextNextNextIdx)))
                        currentPos = getPoint(nextNextNextIdx)
                        currentIdx = nextNextNextIdx
                        stepsTaken += 3
                    } else {
                        // Should not happen with valid FreeType tags
                        stepsTaken++
                    }
                }
                if (segments.isNotEmpty()) {
                    contours.add(ShapeContour(segments, true))
                }
            }
            startPointIdx = endPointIdx + 1
        }

        return Shape(contours)
    }

    override fun advanceWidth(): Double {
        face.makeActive()
        FT_Load_Glyph(face.ftFace, index, FT_LOAD_DEFAULT)
        return (face.ftFace.glyph()?.advance()?.x() ?: 0L) / 64.0
    }

    override fun leftSideBearing(): Double {
        face.makeActive()
        FT_Load_Glyph(face.ftFace, index, FT_LOAD_DEFAULT)
        return (face.ftFace.glyph()?.metrics()?.horiBearingX() ?: 0L) / 64.0
    }

    override fun topSideBearing(): Double {
        face.makeActive()
        FT_Load_Glyph(face.ftFace, index, FT_LOAD_DEFAULT)
        return (face.ftFace.glyph()?.metrics()?.vertBearingY() ?: 0L) / 64.0
    }

    override fun bounds(): Rectangle {
        face.makeActive()
        FT_Load_Glyph(face.ftFace, index, FT_LOAD_DEFAULT)
        val metrics = face.ftFace.glyph()?.metrics() ?: error("no metrics")
        val x = metrics.horiBearingX() / 64.0
        val y = -metrics.horiBearingY() / 64.0
        val width = metrics.width() / 64.0
        val height = metrics.height() / 64.0
        return Rectangle(x, y, width, height)
    }

    override fun bitmapBounds(subpixel: Boolean): IntRectangle {
        face.makeActive()
        FT_Load_Glyph(face.ftFace, index, FT_LOAD_DEFAULT)
        val metrics = face.ftFace.glyph()?.metrics() ?: error("no metrics")
        val contentScale = face.contentScale
        val x = ((metrics.horiBearingX() / 64.0) * contentScale).toInt()
        val y = ((-metrics.horiBearingY() / 64.0) * contentScale).toInt()
        val width = ((metrics.width() / 64.0) * contentScale).toInt()
        val height = ((metrics.height() / 64.0) * contentScale).toInt()
        return IntRectangle(x, y, width, height)
    }

    override fun rasterize(
        bitmap: MPPBuffer,
        stride: Int,
        subpixel: Boolean
    ) {
        face.makeActive()
        face.rasterizing {
            FT_Load_Glyph(face.ftFace, index, FT_LOAD_DEFAULT)
            val slot = face.ftFace.glyph() ?: error("no glyph slot")

            FT_Render_Glyph(slot, FT_RENDER_MODE_NORMAL)
            val ftBitmap = slot.bitmap()
            val width = ftBitmap.width()
            val height = ftBitmap.rows()
            val pitch = ftBitmap.pitch()
            val buffer = ftBitmap.buffer(height * pitch)

            if (buffer != null) {
                val bitmapAddress = MemoryUtil.memAddress(bitmap.byteBuffer)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val sourceIndex = y * pitch + x
                        val targetIndex = y * stride + x
                        val value = buffer.get(sourceIndex)
                        MemoryUtil.memPutByte(bitmapAddress + targetIndex, value)
                    }
                }
            }
        }
    }
}