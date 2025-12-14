package org.openrndr.internal.gl3

import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.draw.ColorBufferShadow
import org.openrndr.draw.ColorType
import org.openrndr.draw.BufferAlignment
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * GLES shadow buffer for ColorBufferGLES.
 *
 * Notes:
 * - Uses direct native-order ByteBuffer (no LWJGL BufferUtils).
 * - Pixel addressing mirrors the GL3 implementation, including flipV handling.
 * - writer() returns BufferWriterGLES.
 */
class ColorBufferShadowGLES(override val colorBuffer: ColorBufferGLES) : ColorBufferShadow {
    private val effectiveWidth  = (colorBuffer.width  * colorBuffer.contentScale).toInt()
    private val effectiveHeight = (colorBuffer.height * colorBuffer.contentScale).toInt()

    private val size = effectiveWidth * effectiveHeight
    private val elementSize = colorBuffer.format.componentCount * colorBuffer.type.componentSize

    override val buffer: ByteBuffer =
        ByteBuffer.allocateDirect(elementSize * size).order(ByteOrder.nativeOrder())

    private val linearity =
        if (colorBuffer.type == ColorType.UINT8_SRGB) Linearity.SRGB else Linearity.LINEAR

    override fun download() {
        // mirrors GL3: read whole texture (level 0) into buffer
        buffer.clear()
        colorBuffer.read(buffer)
        buffer.rewind()
    }

    override fun upload() {
        buffer.rewind()
        colorBuffer.write(buffer)
        buffer.rewind()
    }

    override fun destroy() {
        // mirror GL3 behavior: ask the colorbuffer to drop its shadow reference
        colorBuffer.realShadow = null
    }

    override fun write(x: Int, y: Int, r: Double, g: Double, b: Double, a: Double) {
        // same addressing convention as GL3 impl
        val ay = if (colorBuffer.flipV) y else effectiveHeight - 1 - y

        require(x in 0 until effectiveWidth) { "x out of bounds (0 <= $x < $effectiveWidth)" }
        require(y in 0 until effectiveHeight) { "y out of bounds (0 <= $y < $effectiveHeight)" }

        val cc = colorBuffer.format.componentCount
        val offset = (ay * effectiveWidth + x) * elementSize

        when (colorBuffer.type) {
            ColorType.UINT8, ColorType.UINT8_SRGB -> {
                val ir = (r * 255).coerceIn(0.0, 255.0).toInt().toByte()
                val ig = (g * 255).coerceIn(0.0, 255.0).toInt().toByte()
                val ib = (b * 255).coerceIn(0.0, 255.0).toInt().toByte()
                val ia = (a * 255).coerceIn(0.0, 255.0).toInt().toByte()
                buffer.put(offset, ir)
                if (cc > 1) buffer.put(offset + 1, ig)
                if (cc > 2) buffer.put(offset + 2, ib)
                if (cc > 3) buffer.put(offset + 3, ia)
            }

            ColorType.UINT16 -> {
                val ir = (r * 65535.0).coerceIn(0.0, 65535.0).toInt().toShort()
                val ig = (g * 65535.0).coerceIn(0.0, 65535.0).toInt().toShort()
                val ib = (b * 65535.0).coerceIn(0.0, 65535.0).toInt().toShort()
                val ia = (a * 65535.0).coerceIn(0.0, 65535.0).toInt().toShort()
                buffer.putShort(offset, ir)
                if (cc > 1) buffer.putShort(offset + 2, ig)
                if (cc > 2) buffer.putShort(offset + 4, ib)
                if (cc > 3) buffer.putShort(offset + 6, ia)
            }

            ColorType.FLOAT32 -> {
                buffer.putFloat(offset, r.toFloat())
                if (cc > 1) buffer.putFloat(offset + 4, g.toFloat())
                if (cc > 2) buffer.putFloat(offset + 8, b.toFloat())
                if (cc > 3) buffer.putFloat(offset + 12, a.toFloat())
            }

            else -> TODO("Shadow write not implemented for color type ${colorBuffer.type}")
        }
    }

    override fun read(x: Int, y: Int): ColorRGBa {
        val ay = if (colorBuffer.flipV) y else effectiveHeight - 1 - y

        require(x in 0 until effectiveWidth) { "x out of bounds (0 <= $x < $effectiveWidth)" }
        require(y in 0 until effectiveHeight) { "y out of bounds (0 <= $y < $effectiveHeight)" }

        val cc = colorBuffer.format.componentCount
        val offset = (ay * effectiveWidth + x) * elementSize
        require(offset >= 0) { "offset must be >= 0 (got $offset)" }
        require(offset < size * elementSize) { "offset $offset out of range [0, ${size * elementSize})" }

        return when (colorBuffer.type) {
            ColorType.UINT8, ColorType.UINT8_SRGB -> {
                val ir = buffer.get(offset).toUByte()
                val ig = if (cc >= 2) buffer.get(offset + 1).toUByte() else 0U
                val ib = if (cc >= 3) buffer.get(offset + 2).toUByte() else 0U
                val ia = if (cc >= 4) buffer.get(offset + 3).toUByte() else 255U
                ColorRGBa(
                    ir.toDouble() / 255.0,
                    ig.toDouble() / 255.0,
                    ib.toDouble() / 255.0,
                    ia.toDouble() / 255.0,
                    linearity
                )
            }

            ColorType.UINT16 -> {
                val ir = buffer.getShort(offset).toUShort()
                val ig = if (cc >= 2) buffer.getShort(offset + 2).toUShort() else 0U
                val ib = if (cc >= 3) buffer.getShort(offset + 4).toUShort() else 0U
                val ia = if (cc >= 4) buffer.getShort(offset + 6).toUShort() else 65535U
                ColorRGBa(
                    ir.toDouble() / 65535.0,
                    ig.toDouble() / 65535.0,
                    ib.toDouble() / 65535.0,
                    ia.toDouble() / 65535.0,
                    linearity
                )
            }

            ColorType.FLOAT32 -> {
                val fr = buffer.getFloat(offset)
                val fg = if (cc >= 2) buffer.getFloat(offset + 4) else 0.0f
                val fb = if (cc >= 3) buffer.getFloat(offset + 8) else 0.0f
                val fa = if (cc >= 4) buffer.getFloat(offset + 12) else 1.0f
                ColorRGBa(fr.toDouble(), fg.toDouble(), fb.toDouble(), fa.toDouble(), linearity)
            }

            else -> TODO("Shadow read not implemented for color type ${colorBuffer.type}")
        }
    }

    override fun writer() =
        BufferWriterGLES(
            buffer = buffer,
            elementSize = 1,
            alignment = BufferAlignment.NONE,
            elementIterator = null
        )
}