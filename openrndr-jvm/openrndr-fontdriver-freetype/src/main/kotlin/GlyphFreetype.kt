import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.freetype.FT_Glyph
import org.lwjgl.util.freetype.FT_Outline
import org.lwjgl.util.freetype.FT_OutlineGlyph
import org.lwjgl.util.freetype.FT_Vector
import org.lwjgl.util.freetype.FT_Outline_Funcs
import org.lwjgl.util.freetype.FreeType.FT_ADVANCE_FLAG_FAST_ONLY
import org.lwjgl.util.freetype.FreeType.FT_Get_Glyph
import org.lwjgl.util.freetype.FreeType.FT_LOAD_DEFAULT
import org.lwjgl.util.freetype.FreeType.FT_Load_Glyph
import org.lwjgl.util.freetype.FreeType.FT_Outline_Decompose
import org.lwjgl.util.freetype.FreeType.FT_Outline_New
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

class GlyphFreetype(private val face: FaceFreetype, private val character: Char, private val glyphIndex: Int) : Glyph {

    //val scale = face.sizeInPoints

    override fun shape(): Shape {
        FT_Load_Glyph(face.ftFace, glyphIndex, FT_LOAD_DEFAULT)
        val glyph = PointerBuffer.allocateDirect(1)
        FT_Get_Glyph(face.ftFace.glyph() ?: error("no slot"), glyph)
        val realGlyph = FT_Glyph.create(glyph.get(0))

        val outlineGlyph = FT_OutlineGlyph.create(realGlyph.address())
        val outline = outlineGlyph.outline()

        val contours = mutableListOf<ShapeContour>()
        var currentSegments = mutableListOf<Segment2D>()
        var currentPoint = Vector2.ZERO
        var firstPoint = Vector2.ZERO

        val funcs = FT_Outline_Funcs.malloc()

        funcs.move_to { to, _ ->
            if (currentSegments.isNotEmpty()) {
                contours.add(ShapeContour(currentSegments.toList(), true))
                currentSegments = mutableListOf()
            }
            val toVec = FT_Vector.create(to)
            currentPoint = Vector2(toVec.x() / 64.0, -toVec.y() / 64.0)
            firstPoint = currentPoint
            0
        }

        funcs.line_to { to, _ ->
            val toVec = FT_Vector.create(to)
            val endPoint = Vector2(toVec.x() / 64.0, -toVec.y() / 64.0)
            currentSegments.add(Segment2D(currentPoint, endPoint))
            currentPoint = endPoint
            0
        }

        funcs.conic_to { control, to, _ ->
            val controlVec = FT_Vector.create(control)
            val toVec = FT_Vector.create(to)
            val controlPoint = Vector2(controlVec.x()  / 64.0, -controlVec.y() / 64.0)
            val endPoint = Vector2(toVec.x() / 64.0, -toVec.y() / 64.0)
            currentSegments.add(Segment2D(currentPoint, controlPoint, endPoint))
            currentPoint = endPoint
            0
        }

        funcs.cubic_to { control1, control2, to, _ ->
            val control1Vec = FT_Vector.create(control1)
            val control2Vec = FT_Vector.create(control2)
            val toVec = FT_Vector.create(to)
            val c1 = Vector2(control1Vec.x() / 64.0, -control1Vec.y() / 64.0)
            val c2 = Vector2(control2Vec.x() / 64.0, -control2Vec.y()  / 64.0)
            val endPoint = Vector2(toVec.x() / 64.0, -toVec.y()  / 64.0)
            currentSegments.add(Segment2D(currentPoint, c1, c2, endPoint))
            currentPoint = endPoint
            0
        }

        FT_Outline_Decompose(outline, funcs, 0L)

        if (currentSegments.isNotEmpty()) {
            contours.add(ShapeContour(currentSegments.toList(), true))
        }

        funcs.free()

        return Shape(contours)
    }

    override fun advanceWidth(): Double {
        FT_Load_Glyph(face.ftFace, glyphIndex, FT_LOAD_DEFAULT)
        return (face.ftFace.glyph()?.advance()?.x() ?: 0L) / 64.0
    }

    override fun leftSideBearing(): Double {
        FT_Load_Glyph(face.ftFace, glyphIndex, FT_LOAD_DEFAULT)
        return (face.ftFace.glyph()?.metrics()?.horiBearingX() ?: 0L) / 64.0
    }

    override fun topSideBearing(): Double {
        FT_Load_Glyph(face.ftFace, glyphIndex, FT_LOAD_DEFAULT)
        return (face.ftFace.glyph()?.metrics()?.vertBearingY() ?: 0L) / 64.0
    }

    override fun bounds(): Rectangle {
        FT_Load_Glyph(face.ftFace, glyphIndex, FT_LOAD_DEFAULT)
        val metrics = face.ftFace.glyph()?.metrics() ?: error("no metrics")
        val x = metrics.horiBearingX() / 64.0
        val y = -metrics.horiBearingY() / 64.0
        val width = metrics.width() / 64.0
        val height = metrics.height() / 64.0
        return Rectangle(x, y, width, height)
    }

    override fun bitmapBounds(subpixel: Boolean): IntRectangle {
        FT_Load_Glyph(face.ftFace, glyphIndex, FT_LOAD_DEFAULT)
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

        face.rasterizing {
            FT_Load_Glyph(face.ftFace, glyphIndex, FT_LOAD_DEFAULT)
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