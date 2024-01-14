package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.BufferUtils
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferWriter
import org.openrndr.draw.ColorBufferShadow
import org.openrndr.draw.ColorType
import java.nio.ByteBuffer
private val logger = KotlinLogging.logger {}

@Suppress("MemberVisibilityCanBePrivate")
class ColorBufferShadowGL3(override val colorBuffer: ColorBufferGL3) : ColorBufferShadow {
    val size = colorBuffer.effectiveWidth * colorBuffer.effectiveHeight
    val elementSize = colorBuffer.format.componentCount * colorBuffer.type.componentSize
    override val buffer: ByteBuffer = BufferUtils.createByteBuffer(elementSize * size)

    override fun download() {
        logger.trace {
            "downloading colorbuffer into shadow"
        }
        colorBuffer.read(buffer)
    }

    override fun upload() {
        colorBuffer.write(buffer)
    }

    override fun destroy() {
        colorBuffer.destroyShadow()
    }

    override fun write(x: Int, y: Int, r: Double, g: Double, b: Double, a: Double) {
        val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
        val offset = (ay * colorBuffer.effectiveWidth + x) * colorBuffer.format.componentCount * colorBuffer.type.componentSize
        when (colorBuffer.type) {
            ColorType.UINT8 -> {
                val ir = (r * 255).coerceIn(0.0, 255.0).toInt().toByte()
                val ig = (g * 255).coerceIn(0.0, 255.0).toInt().toByte()
                val ib = (b * 255).coerceIn(0.0, 255.0).toInt().toByte()
                val ia = (a * 255).coerceIn(0.0, 255.0).toInt().toByte()
                buffer.put(offset, ir)
                buffer.put(offset + 1, ig)
                buffer.put(offset + 2, ib)
                if (colorBuffer.format.componentCount > 3) {
                    buffer.put(offset + 3, ia)
                }
            }
            ColorType.UINT16 -> {
                val ir = (r * 65535).coerceIn(0.0, 65535.0).toInt().toChar()
                val ig = (g * 65535).coerceIn(0.0, 65535.0).toInt().toChar()
                val ib = (b * 65335).coerceIn(0.0, 65535.0).toInt().toChar()
                val ia = (a * 65535).coerceIn(0.0, 65535.0).toInt().toChar()
                buffer.putChar(offset, ir)
                buffer.putChar(offset + 2, ig)
                buffer.putChar(offset + 4, ib)
                if (colorBuffer.format.componentCount > 3) {
                    buffer.putChar(offset + 6, ia)
                }
            }
            ColorType.FLOAT32 -> {
                buffer.putFloat(offset, r.toFloat())
                buffer.putFloat(offset + 4, g.toFloat())
                buffer.putFloat(offset + 8, b.toFloat())
                if (colorBuffer.format.componentCount > 3) {
                    buffer.putFloat(offset + 12, a.toFloat())
                }
            }
            else -> TODO("support for ${colorBuffer.type}")
        }
    }

    override fun read(x: Int, y: Int): ColorRGBa {
        val componentCount = colorBuffer.format.componentCount
        val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
        val offset = (ay * colorBuffer.effectiveWidth + x) * colorBuffer.format.componentCount * colorBuffer.type.componentSize
        require(x >= 0 && x < colorBuffer.effectiveWidth) { "x out of bounds (0 < $x < ${colorBuffer.effectiveWidth}"}
        require(y >= 0 && y < colorBuffer.effectiveHeight) { "y out of bounds (0 < $y < ${colorBuffer.effectiveHeight}"}
        require(offset >= 0) { "offset > 0 ($offset)" }
        require(offset < size*elementSize ) { "offset < $size ($offset)" }

        return when (colorBuffer.type) {
            ColorType.UINT8 -> {
                val ir = buffer.get(offset).toInt() and 0xff
                val ig = if (componentCount >= 2) buffer.get(offset + 1).toInt() and 0xff else 0
                val ib = if (componentCount >= 3) buffer.get(offset + 2).toInt() and 0xff else 0
                val ia = if (componentCount >= 4) (buffer.get(offset + 3).toInt() and 0xff) else 255
                ColorRGBa(ir / 255.0, ig / 255.0, ib / 255.0, ia / 255.0)
            }
            ColorType.UINT16 -> {
                val ir = buffer.get(offset).toInt() and 0xffff
                val ig = if (componentCount >= 2) buffer.get(offset + 1).toInt() and 0xffff else 0
                val ib = if (componentCount >= 3) buffer.get(offset + 2).toInt() and 0xffff else 0
                val ia = if (componentCount >= 4) (buffer.get(offset + 3).toInt() and 0xffff) else 255
                ColorRGBa(ir / 65535.0, ig / 65535.0, ib / 65535.0, ia / 65535.0)
            }
            ColorType.FLOAT32 -> {
                val fr = buffer.getFloat(offset)
                val fg = if (componentCount >= 2) buffer.getFloat(offset + 4) else 0.0f
                val fb = if (componentCount >= 3) buffer.getFloat(offset + 8) else 0.0f
                val fa = if (componentCount >= 4) (buffer.getFloat(offset + 12)) else 1.0f
                ColorRGBa(fr.toDouble(), fg.toDouble(), fb.toDouble(), fa.toDouble())
            }
            else -> TODO("support for ${colorBuffer.type}")
        }
    }

    override fun writer(): BufferWriter {
        return BufferWriterGL3(buffer)
    }
}