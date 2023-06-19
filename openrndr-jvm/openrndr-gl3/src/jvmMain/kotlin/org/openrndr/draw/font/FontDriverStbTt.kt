package org.openrndr.draw.font

import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTruetype
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.contours
import org.openrndr.utils.buffer.MPPBuffer
import org.openrndr.utils.url.resolveFileOrUrl
import java.lang.ref.Cleaner
import java.nio.Buffer
import java.nio.ByteBuffer

class FaceStbTt(data: ByteBuffer, fontInfo: STBTTFontinfo) : Face, AutoCloseable {
    companion object {
        val cleaner: Cleaner = Cleaner.create()
    }

    class State(val data: ByteBuffer, val fontInfo: STBTTFontinfo) : Runnable {
        override fun run() {
            MemoryUtil.memFree(data)
        }
    }

    val fontInfo: STBTTFontinfo
        get() {
            return state.fontInfo
        }
    private val state = State(data, fontInfo)
    private val cleanable = cleaner.register(this, state)

    internal fun scaleForSize(size: Double): Double {
        return (STBTruetype.stbtt_ScaleForPixelHeight(state.fontInfo, size.toFloat())).toDouble()
    }

    override fun ascent(size: Double): Double {
        stackPush().use {
            val ascent = it.mallocInt(1)
            STBTruetype.stbtt_GetFontVMetrics(state.fontInfo, ascent, null, null)
            return ascent.get(0) * scaleForSize(size)
        }
    }

    override fun descent(size: Double): Double {
        stackPush().use {
            val descent = it.mallocInt(1)
            STBTruetype.stbtt_GetFontVMetrics(state.fontInfo, null, descent, null)
            return descent.get(0) * scaleForSize(size)
        }
    }

    override fun lineGap(size: Double): Double {
        stackPush().use {
            val lineGap = it.mallocInt(1)
            STBTruetype.stbtt_GetFontVMetrics(state.fontInfo, null, null, lineGap)
            return lineGap.get(0) * scaleForSize(size)
        }
    }

    override fun kernAdvance(size: Double, left: Char, right: Char): Double {
        val leftIdx = STBTruetype.stbtt_FindGlyphIndex(state.fontInfo, left.code)
        val rightIdx = STBTruetype.stbtt_FindGlyphIndex(state.fontInfo, right.code)
        return STBTruetype.stbtt_GetGlyphKernAdvance(state.fontInfo, leftIdx, rightIdx) * scaleForSize(size)
    }

    override fun glyphForCharacter(character: Char): Glyph {
        val glyphIndex = STBTruetype.stbtt_FindGlyphIndex(state.fontInfo, character.code)
        return GlyphStbTt(this, character, glyphIndex)
    }

    override fun bounds(size: Double): Rectangle {
        val scale = scaleForSize(size)
        stackPush().use { stack ->
            val px0 = stack.mallocInt(1)
            val px1 = stack.mallocInt(1)
            val py0 = stack.mallocInt(1)
            val py1 = stack.mallocInt(1)
            STBTruetype.stbtt_GetFontBoundingBox(state.fontInfo, px0, py0, px1, py1)
            val x0 = px0.get() * scale
            val y0 = py0.get() * scale
            val x1 = px1.get() * scale
            val y1 = py1.get() * scale
            // returned values are for up=+y, convert to up=-y
            return Rectangle(x0, -y0, x1 - x0, -(y1 - y0)).normalized
        }
    }

    override fun close() {
        cleanable.clean()
    }
}

class GlyphStbTt(private val face: FaceStbTt, private val character: Char, private val glyphIndex: Int) : Glyph {
    override fun shape(size: Double): Shape {
        val shape = STBTruetype.stbtt_GetCodepointShape(face.fontInfo, character.code) ?: return Shape.EMPTY
        val scale = face.scaleForSize(size)

        // returned values are for up=+y, convert to up=-y
        val shapeContours = contours {
            for (i in 0 until shape.remaining()) {
                val v = shape.get()
                if (v.type() == STBTruetype.STBTT_vmove) {
                    moveTo(v.x() * scale, v.y() * -scale)
                } else if (v.type() == STBTruetype.STBTT_vline) {
                    lineTo(v.x() * scale, v.y() * -scale)
                } else if (v.type() == STBTruetype.STBTT_vcurve) {
                    curveTo(v.cx() * scale, v.cy() * -scale, v.x() * scale, v.y() * -scale)
                } else if (v.type() == STBTruetype.STBTT_vcubic) {
                    curveTo(
                        v.cx() * scale,
                        v.cy() * -scale,
                        v.cx1() * scale,
                        v.cy1() * -scale,
                        v.x() * scale,
                        v.y() * -scale
                    )
                }
                v.free()
            }
        }
        shape.free()
        return Shape(shapeContours.map { it.reversed.close() })
    }

    override fun advanceWidth(size: Double): Double {
        stackPush().use { stack ->
            val pAdvanceWidth = stack.mallocInt(1)
            STBTruetype.stbtt_GetGlyphHMetrics(face.fontInfo, glyphIndex, pAdvanceWidth, null)
            return pAdvanceWidth.get(0) * face.scaleForSize(size)
        }
    }

    override fun leftSideBearing(size: Double): Double {
        stackPush().use { stack ->
            val pLeftSideBearing = stack.mallocInt(1)
            STBTruetype.stbtt_GetGlyphHMetrics(face.fontInfo, glyphIndex, null, pLeftSideBearing)
            return pLeftSideBearing.get(0) * face.scaleForSize(size)
        }
    }

    override fun topSideBearing(size: Double): Double {
        TODO("Not yet implemented")
    }

    override fun bounds(size: Double): Rectangle {
        val scale = face.scaleForSize(size)
        stackPush().use { stack ->
            val px0 = stack.mallocInt(1)
            val px1 = stack.mallocInt(1)
            val py0 = stack.mallocInt(1)
            val py1 = stack.mallocInt(1)
            STBTruetype.stbtt_GetGlyphBox(face.fontInfo, glyphIndex, px0, py0, px1, py1)
            val x0 = px0.get() * scale
            val y0 = py0.get() * scale
            val x1 = px1.get() * scale
            val y1 = py1.get() * scale

            // returned values are for up=+y, convert to up=-y
            return Rectangle(x0, -y0, x1 - x0, -(y1 - y0)).normalized
        }
    }

    override fun bitmapBounds(size: Double, subpixel: Boolean): IntRectangle {
        val scale = face.scaleForSize(size).toFloat()
        stackPush().use { stack ->
            val px0 = stack.mallocInt(1)
            val px1 = stack.mallocInt(1)
            val py0 = stack.mallocInt(1)
            val py1 = stack.mallocInt(1)

            if (subpixel) {
                STBTruetype.stbtt_GetGlyphBitmapBoxSubpixel(
                    face.fontInfo,
                    glyphIndex,
                    scale, scale,
                    0.0f, 0.0f,
                    px0, py0,
                    px1, py1
                )
            } else {
                STBTruetype.stbtt_GetGlyphBitmapBox(
                    face.fontInfo,
                    glyphIndex,
                    scale, scale,
                    px0, py0,
                    px1, py1
                )
            }
            val x0 = px0.get()
            val y0 = py0.get()
            val x1 = px1.get()
            val y1 = py1.get()

            // returned values are for up=-y
            return IntRectangle(x0, y0, x1 - x0, y1 - y0)
        }
    }

    override fun rasterize(size: Double, bitmap: MPPBuffer, stride: Int, subpixel: Boolean) {
        val bitmapBuffer = bitmap.byteBuffer

        val scale = face.scaleForSize(size).toFloat()
        val bounds = bitmapBounds(size, subpixel)
        STBTruetype.stbtt_MakeGlyphBitmapSubpixel(
            face.fontInfo,
            bitmapBuffer,
            bounds.width,
            bounds.height,
            stride,
            scale,
            scale,
            0.0f,
            0.0f,
            glyphIndex
        )
    }
}


/**
 * FontDriver implementation based on stb_truetype
 * @since 0.4.3
 */
class FontDriverStbTt : FontDriver {
    override fun loadFace(fileOrUrl: String): Face {
        val (file, url) = resolveFileOrUrl(fileOrUrl)
        val byteArray = file?.readBytes() ?: url?.readBytes() ?: error("no file or url")
        val fileSize = byteArray.size

        val bb = MemoryUtil.memAlloc(fileSize)
        bb.put(byteArray, 0, fileSize)
        (bb as Buffer).rewind()
        val fontInfo = STBTTFontinfo.create()

        val status = STBTruetype.stbtt_InitFont(fontInfo, bb)
        check(status) { "failed to load font $fileOrUrl" }
        return FaceStbTt(bb, fontInfo)
    }
}