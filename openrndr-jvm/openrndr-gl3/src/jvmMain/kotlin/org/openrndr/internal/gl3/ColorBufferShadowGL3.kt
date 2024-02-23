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
        val offset =
            (ay * colorBuffer.effectiveWidth + x) * colorBuffer.format.componentCount * colorBuffer.type.componentSize
        val cc = colorBuffer.format.componentCount
        when (colorBuffer.type) {
            ColorType.UINT8 -> {
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
                val ir = (r * 65535).coerceIn(0.0, 65535.0).toInt().toShort()
                val ig = (g * 65535).coerceIn(0.0, 65535.0).toInt().toShort()
                val ib = (b * 65335).coerceIn(0.0, 65535.0).toInt().toShort()
                val ia = (a * 65535).coerceIn(0.0, 65535.0).toInt().toShort()
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

            else -> TODO("support for ${colorBuffer.type}")
        }
    }

    override fun read(x: Int, y: Int): ColorRGBa {
        val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
        val offset =
            (ay * colorBuffer.effectiveWidth + x) * colorBuffer.format.componentCount * colorBuffer.type.componentSize
        require(x >= 0 && x < colorBuffer.effectiveWidth) { "x out of bounds (0 < $x < ${colorBuffer.effectiveWidth}" }
        require(y >= 0 && y < colorBuffer.effectiveHeight) { "y out of bounds (0 < $y < ${colorBuffer.effectiveHeight}" }
        require(offset >= 0) { "offset > 0 ($offset)" }
        require(offset < size * elementSize) { "offset < $size ($offset)" }

        val cc = colorBuffer.format.componentCount

        return when (colorBuffer.type) {
            ColorType.UINT8 -> {
                val ir = buffer.get(offset).toUByte()
                val ig = if (cc >= 2) buffer.get(offset + 1).toUByte() else 0U
                val ib = if (cc >= 3) buffer.get(offset + 2).toUByte() else 0U
                val ia = if (cc >= 4) buffer.get(offset + 3).toUByte() else 255U
                ColorRGBa(ir.toDouble() / 255.0, ig.toDouble() / 255.0, ib.toDouble() / 255.0, ia.toDouble() / 255.0)
            }

            ColorType.UINT16 -> {
                val ir = buffer.getShort(offset).toUShort()
                val ig = if (cc >= 2) buffer.getShort(offset + 2).toUShort() else 0U
                val ib = if (cc >= 3) buffer.getShort(offset + 4).toUShort() else 0U
                val ia = if (cc >= 4) (buffer.getShort(offset + 6).toUShort()) else 65535U
                ColorRGBa(
                    ir.toDouble() / 65535.0,
                    ig.toDouble() / 65535.0,
                    ib.toDouble() / 65535.0,
                    ia.toDouble() / 65535.0
                )
            }

            ColorType.FLOAT32 -> {
                val fr = buffer.getFloat(offset)
                val fg = if (cc >= 2) buffer.getFloat(offset + 4) else 0.0f
                val fb = if (cc >= 3) buffer.getFloat(offset + 8) else 0.0f
                val fa = if (cc >= 4) (buffer.getFloat(offset + 12)) else 1.0f
                ColorRGBa(fr.toDouble(), fg.toDouble(), fb.toDouble(), fa.toDouble())
            }

            else -> TODO("support for ${colorBuffer.type}")
        }
    }

    override fun writer(): BufferWriter {
        return BufferWriterGL3(buffer)
    }
}