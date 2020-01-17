package org.openrndr.internal.gl3

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.opengl.ARBTextureCompressionBPTC.*
import org.lwjgl.opengl.EXTTextureCompressionS3TC.*
import org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT
import org.lwjgl.opengl.EXTTextureSRGB.*
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.stb.STBIWriteCallback
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImageWrite
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.tinyexr.EXRChannelInfo
import org.lwjgl.util.tinyexr.EXRHeader
import org.lwjgl.util.tinyexr.EXRImage
import org.lwjgl.util.tinyexr.EXRVersion
import org.lwjgl.util.tinyexr.TinyEXR.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.BufferMultisample.Disabled
import org.openrndr.draw.BufferMultisample.SampleCount
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

internal data class ConversionEntry(val format: ColorFormat, val type: ColorType, val glFormat: Int)

private val logger = KotlinLogging.logger {}

internal fun internalFormat(format: ColorFormat, type: ColorType): Int {
    val entries = arrayOf(
            ConversionEntry(ColorFormat.R, ColorType.UINT8, GL_R8),
            ConversionEntry(ColorFormat.R, ColorType.UINT8_INT, GL_R8UI),
            ConversionEntry(ColorFormat.R, ColorType.SINT8_INT, GL_R8I),
            ConversionEntry(ColorFormat.R, ColorType.UINT16, GL_R16),
            ConversionEntry(ColorFormat.R, ColorType.UINT16_INT, GL_R16UI),
            ConversionEntry(ColorFormat.R, ColorType.SINT16_INT, GL_RG16I),
            ConversionEntry(ColorFormat.R, ColorType.FLOAT16, GL_R16F),
            ConversionEntry(ColorFormat.R, ColorType.FLOAT32, GL_R32F),
            ConversionEntry(ColorFormat.RG, ColorType.UINT8, GL_RG8),
            ConversionEntry(ColorFormat.RG, ColorType.UINT8_INT, GL_RG8UI),
            ConversionEntry(ColorFormat.RG, ColorType.SINT16_INT, GL_RG16I),
            ConversionEntry(ColorFormat.RG, ColorType.UINT16, GL_RG16),
            ConversionEntry(ColorFormat.RG, ColorType.UINT16_INT, GL_RG16UI),
            ConversionEntry(ColorFormat.RG, ColorType.FLOAT16, GL_RG16F),
            ConversionEntry(ColorFormat.RG, ColorType.FLOAT32, GL_RG32F),
            ConversionEntry(ColorFormat.RGB, ColorType.UINT8, GL_RGB8),
            ConversionEntry(ColorFormat.RGB, ColorType.UINT8, GL_RGB8UI),
            ConversionEntry(ColorFormat.RGB, ColorType.UINT16, GL_RGB16),
            ConversionEntry(ColorFormat.RGB, ColorType.UINT16_INT, GL_RGB16UI),
            ConversionEntry(ColorFormat.RGB, ColorType.SINT16_INT, GL_RGB16I),
            ConversionEntry(ColorFormat.RGB, ColorType.FLOAT16, GL_RGB16F),
            ConversionEntry(ColorFormat.RGB, ColorType.FLOAT32, GL_RGB32F),
            ConversionEntry(ColorFormat.BGR, ColorType.UINT8, GL_RGB8),
            ConversionEntry(ColorFormat.RGBa, ColorType.UINT8, GL_RGBA8),
            ConversionEntry(ColorFormat.RGBa, ColorType.UINT8_INT, GL_RGBA8UI),
            ConversionEntry(ColorFormat.RGBa, ColorType.UINT16, GL_RGBA16),
            ConversionEntry(ColorFormat.RGBa, ColorType.UINT16_INT, GL_RGBA16UI),
            ConversionEntry(ColorFormat.RGBa, ColorType.SINT16_INT, GL_RGBA16I),
            ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT16, GL_RGBA16F),
            ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT32, GL_RGBA32F),
            ConversionEntry(ColorFormat.R, ColorType.UINT8, GL_R8),
            ConversionEntry(ColorFormat.R, ColorType.FLOAT16, GL_R16F),
            ConversionEntry(ColorFormat.R, ColorType.FLOAT16, GL_R16F),
            ConversionEntry(ColorFormat.R, ColorType.FLOAT32, GL_R32F),
            ConversionEntry(ColorFormat.sRGB, ColorType.UINT8, GL_SRGB8),
            ConversionEntry(ColorFormat.sRGBa, ColorType.UINT8, GL_SRGB8_ALPHA8),
            ConversionEntry(ColorFormat.RGBa, ColorType.DXT1, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT),
            ConversionEntry(ColorFormat.RGBa, ColorType.DXT3, GL_COMPRESSED_RGBA_S3TC_DXT3_EXT),
            ConversionEntry(ColorFormat.RGBa, ColorType.DXT5, GL_COMPRESSED_RGBA_S3TC_DXT5_EXT),
            ConversionEntry(ColorFormat.RGB, ColorType.DXT1, GL_COMPRESSED_RGB_S3TC_DXT1_EXT),
            ConversionEntry(ColorFormat.sRGBa, ColorType.DXT1, GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT),
            ConversionEntry(ColorFormat.sRGBa, ColorType.DXT3, GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT),
            ConversionEntry(ColorFormat.sRGBa, ColorType.DXT5, GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT),
            ConversionEntry(ColorFormat.sRGB, ColorType.DXT1, GL_COMPRESSED_SRGB_S3TC_DXT1_EXT),
            ConversionEntry(ColorFormat.RGBa, ColorType.BPTC_UNORM, GL_COMPRESSED_RGBA_BPTC_UNORM_ARB),
            ConversionEntry(ColorFormat.sRGBa, ColorType.BPTC_UNORM, GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM_ARB),
            ConversionEntry(ColorFormat.RGB, ColorType.BPTC_FLOAT, GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT_ARB),
            ConversionEntry(ColorFormat.RGB, ColorType.BPTC_UFLOAT, GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT_ARB)
    )
    for (entry in entries) {
        if (entry.format === format && entry.type === type) {
            return entry.glFormat
        }
    }
    throw Exception("no conversion entry for $format/$type")
}

@Suppress("MemberVisibilityCanBePrivate")
class ColorBufferShadowGL3(override val colorBuffer: ColorBufferGL3) : ColorBufferShadow {
    val size = colorBuffer.width * colorBuffer.height
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
                val ir = (r * 255).coerceIn(0.0, 255.0).toByte()
                val ig = (g * 255).coerceIn(0.0, 255.0).toByte()
                val ib = (b * 255).coerceIn(0.0, 255.0).toByte()
                val ia = (a * 255).coerceIn(0.0, 255.0).toByte()
                buffer.put(offset, ir)
                buffer.put(offset + 1, ig)
                buffer.put(offset + 2, ib)
                if (colorBuffer.format.componentCount > 3) {
                    buffer.put(offset + 3, ia)
                }
            }
            ColorType.UINT16 -> {
                val ir = (r * 65535).coerceIn(0.0, 65535.0).toChar()
                val ig = (g * 65535).coerceIn(0.0, 65535.0).toChar()
                val ib = (b * 65335).coerceIn(0.0, 65535.0).toChar()
                val ia = (a * 65535).coerceIn(0.0, 65535.0).toChar()
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

    override inline fun mapBoolean(crossinline mapper: (r: Double, g: Double, b: Double, a: Double) -> Boolean): Array<BooleanArray> {
        val result = Array(colorBuffer.effectiveHeight) { BooleanArray(colorBuffer.effectiveWidth) }
        (buffer as Buffer).rewind()
        when (Pair(colorBuffer.type, colorBuffer.format)) {
            Pair(ColorType.UINT8, ColorFormat.RGBa) -> {
                for (y in 0 until colorBuffer.effectiveHeight) {
                    val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
                    for (x in 0 until colorBuffer.effectiveWidth) {
                        val ir = buffer.get().toInt() and 0xff
                        val ig = buffer.get().toInt() and 0xff
                        val ib = buffer.get().toInt() and 0xff
                        val ia = buffer.get().toInt() and 0xff
                        result[ay][x] = mapper(ir / 255.0, ig / 255.0, ib / 255.0, ia / 255.0)
                    }
                }
            }
            Pair(ColorType.UINT8, ColorFormat.RGB) -> {
                for (y in 0 until colorBuffer.effectiveHeight) {
                    val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
                    for (x in 0 until colorBuffer.effectiveWidth) {
                        val ir = buffer.get().toInt() and 0xff
                        val ig = buffer.get().toInt() and 0xff
                        val ib = buffer.get().toInt() and 0xff
                        result[ay][x] = mapper(ir / 255.0, ig / 255.0, ib / 255.0, 1.0)
                    }
                }
            }
            Pair(ColorType.FLOAT32, ColorFormat.RGBa) -> {
                val fr = buffer.getFloat()
                val fg = buffer.getFloat()
                val fb = buffer.getFloat()
                val fa = buffer.getFloat()
                mapper(fr.toDouble(), fg.toDouble(), fb.toDouble(), fa.toDouble())
            }
            Pair(ColorType.FLOAT32, ColorFormat.RGB) -> {
                val fr = buffer.getFloat()
                val fg = buffer.getFloat()
                val fb = buffer.getFloat()
                mapper(fr.toDouble(), fg.toDouble(), fb.toDouble(), 1.0)
            }
            else -> throw NotImplementedError()
        }
        return result
    }

    override inline fun mapDouble(crossinline mapper: (r: Double, g: Double, b: Double, a: Double) -> Double): Array<DoubleArray> {
        val result = Array(colorBuffer.effectiveHeight) { DoubleArray(colorBuffer.effectiveWidth) }
        (buffer as Buffer).rewind()
        when (Pair(colorBuffer.type, colorBuffer.format)) {
            Pair(ColorType.UINT8, ColorFormat.RGBa) -> {
                for (y in 0 until colorBuffer.effectiveHeight) {
                    val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
                    for (x in 0 until colorBuffer.effectiveWidth) {
                        val ir = buffer.get().toInt() and 0xff
                        val ig = buffer.get().toInt() and 0xff
                        val ib = buffer.get().toInt() and 0xff
                        val ia = buffer.get().toInt() and 0xff
                        result[ay][x] = mapper(ir / 255.0, ig / 255.0, ib / 255.0, ia / 255.0)
                    }
                }
            }
            Pair(ColorType.UINT8, ColorFormat.RGB) -> {
                for (y in 0 until colorBuffer.effectiveHeight) {
                    val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
                    for (x in 0 until colorBuffer.effectiveWidth) {
                        val ir = buffer.get().toInt() and 0xff
                        val ig = buffer.get().toInt() and 0xff
                        val ib = buffer.get().toInt() and 0xff
                        result[ay][x] = mapper(ir / 255.0, ig / 255.0, ib / 255.0, 1.0)
                    }
                }
            }
            Pair(ColorType.FLOAT32, ColorFormat.RGBa) -> {
                val fr = buffer.getFloat()
                val fg = buffer.getFloat()
                val fb = buffer.getFloat()
                val fa = buffer.getFloat()
                mapper(fr.toDouble(), fg.toDouble(), fb.toDouble(), fa.toDouble())
            }
            Pair(ColorType.FLOAT32, ColorFormat.RGB) -> {
                val fr = buffer.getFloat()
                val fg = buffer.getFloat()
                val fb = buffer.getFloat()
                mapper(fr.toDouble(), fg.toDouble(), fb.toDouble(), 1.0)
            }
            else -> throw NotImplementedError()
        }
        return result
    }

    override inline fun mapFloat(crossinline mapper: (r: Double, g: Double, b: Double, a: Double) -> Float): Array<FloatArray> {
        val result = Array(colorBuffer.effectiveHeight) { FloatArray(colorBuffer.effectiveWidth) }
        (buffer as Buffer).rewind()
        when (Pair(colorBuffer.type, colorBuffer.format)) {
            Pair(ColorType.UINT8, ColorFormat.RGBa) -> {
                for (y in 0 until colorBuffer.effectiveHeight) {
                    val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
                    for (x in 0 until colorBuffer.effectiveWidth) {
                        val ir = buffer.get().toInt() and 0xff
                        val ig = buffer.get().toInt() and 0xff
                        val ib = buffer.get().toInt() and 0xff
                        val ia = buffer.get().toInt() and 0xff
                        result[ay][x] = mapper(ir / 255.0, ig / 255.0, ib / 255.0, ia / 255.0)
                    }
                }
            }
            Pair(ColorType.UINT8, ColorFormat.RGB) -> {
                for (y in 0 until colorBuffer.effectiveHeight) {
                    val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
                    for (x in 0 until colorBuffer.effectiveWidth) {
                        val ir = buffer.get().toInt() and 0xff
                        val ig = buffer.get().toInt() and 0xff
                        val ib = buffer.get().toInt() and 0xff
                        result[ay][x] = mapper(ir / 255.0, ig / 255.0, ib / 255.0, 1.0)
                    }
                }
            }
            Pair(ColorType.FLOAT32, ColorFormat.RGBa) -> {
                val fr = buffer.getFloat()
                val fg = buffer.getFloat()
                val fb = buffer.getFloat()
                val fa = buffer.getFloat()
                mapper(fr.toDouble(), fg.toDouble(), fb.toDouble(), fa.toDouble())
            }

            Pair(ColorType.FLOAT32, ColorFormat.RGB) -> {
                val fr = buffer.getFloat()
                val fg = buffer.getFloat()
                val fb = buffer.getFloat()
                mapper(fr.toDouble(), fg.toDouble(), fb.toDouble(), 1.0)
            }
            else -> throw NotImplementedError()
        }
        return result
    }

    override inline fun mapInt(crossinline mapper: (r: Double, g: Double, b: Double, a: Double) -> Int): Array<IntArray> {
        val result = Array(colorBuffer.effectiveHeight) { IntArray(colorBuffer.effectiveWidth) }
        (buffer as Buffer).rewind()
        when (Pair(colorBuffer.type, colorBuffer.format)) {
            Pair(ColorType.UINT8, ColorFormat.RGBa) -> {
                for (y in 0 until colorBuffer.effectiveHeight) {
                    val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
                    for (x in 0 until colorBuffer.effectiveWidth) {
                        val ir = buffer.get().toInt() and 0xff
                        val ig = buffer.get().toInt() and 0xff
                        val ib = buffer.get().toInt() and 0xff
                        val ia = buffer.get().toInt() and 0xff
                        result[ay][x] = mapper(ir / 255.0, ig / 255.0, ib / 255.0, ia / 255.0)
                    }
                }
            }
            Pair(ColorType.UINT8, ColorFormat.RGB) -> {
                for (y in 0 until colorBuffer.effectiveHeight) {
                    val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
                    for (x in 0 until colorBuffer.effectiveWidth) {
                        val ir = buffer.get().toInt() and 0xff
                        val ig = buffer.get().toInt() and 0xff
                        val ib = buffer.get().toInt() and 0xff
                        result[ay][x] = mapper(ir / 255.0, ig / 255.0, ib / 255.0, 1.0)
                    }
                }
            }
            Pair(ColorType.FLOAT32, ColorFormat.RGBa) -> {
                val fr = buffer.getFloat()
                val fg = buffer.getFloat()
                val fb = buffer.getFloat()
                val fa = buffer.getFloat()
                mapper(fr.toDouble(), fg.toDouble(), fb.toDouble(), fa.toDouble())
            }
            Pair(ColorType.FLOAT32, ColorFormat.RGB) -> {
                val fr = buffer.getFloat()
                val fg = buffer.getFloat()
                val fb = buffer.getFloat()
                mapper(fr.toDouble(), fg.toDouble(), fb.toDouble(), 1.0)
            }
            else -> throw NotImplementedError()
        }
        return result
    }

    override fun <T> mapIndexed(
            xrange: IntProgression,
            yrange: IntProgression,
            mapper: (x: Int, y: Int, r: Double, g: Double, b: Double, a: Double) -> T): Array<List<T>> {

        val result: Array<List<T>> = Array(yrange.size) { mutableListOf<T>() }
        (buffer as Buffer).rewind()
        buffer.order(ByteOrder.nativeOrder())
        when (Pair(colorBuffer.type, colorBuffer.format)) {
            Pair(ColorType.UINT8, ColorFormat.RGBa) -> {
                for ((iy, y) in yrange.withIndex()) {
                    val ay = if (colorBuffer.flipV) iy else result.size - 1 - iy
                    var offset = y * colorBuffer.effectiveWidth * 4 + xrange.first * 4
                    result[ay] = (xrange).map { x ->
                        val ir = buffer.get(offset).toInt() and 0xff
                        val ig = buffer.get(offset + 1).toInt() and 0xff
                        val ib = buffer.get(offset + 2).toInt() and 0xff
                        val ia = buffer.get(offset + 3).toInt() and 0xff
                        offset += xrange.step * 4
                        mapper(x, y, ir / 255.0, ig / 255.0, ib / 255.0, ia / 255.0)
                    }
                }
            }
            Pair(ColorType.UINT8, ColorFormat.RGB) -> {
                for ((iy, y) in yrange.withIndex()) {
                    val ay = if (colorBuffer.flipV) iy else result.size - 1 - iy
                    var offset = y * colorBuffer.effectiveWidth * 3 + xrange.first * 3
                    result[ay] = (xrange).map { x ->
                        val ir = buffer.get(offset).toInt() and 0xff
                        val ig = buffer.get(offset + 1).toInt() and 0xff
                        val ib = buffer.get(offset + 2).toInt() and 0xff
                        offset += xrange.step * 3
                        mapper(x, y, ir / 255.0, ig / 255.0, ib / 255.0, 1.0)
                    }
                }
            }
            Pair(ColorType.UINT8, ColorFormat.RG) -> {
                for ((iy, y) in yrange.withIndex()) {
                    val ay = if (colorBuffer.flipV) iy else result.size - 1 - iy
                    var offset = y * colorBuffer.effectiveWidth * 2 + xrange.first * 3
                    result[ay] = (xrange).map { x ->
                        val ir = buffer.get(offset).toInt() and 0xff
                        val ig = buffer.get(offset + 1).toInt() and 0xff
                        offset += xrange.step * 2
                        mapper(x, y, ir / 255.0, ig / 255.0, 0.0, 1.0)
                    }
                }
            }

            Pair(ColorType.UINT8, ColorFormat.R) -> {
                for ((iy, y) in yrange.withIndex()) {
                    val ay = if (colorBuffer.flipV) iy else result.size - 1 - iy
                    var offset = y * colorBuffer.effectiveWidth * 2 + xrange.first * 3
                    result[ay] = (xrange).map { x ->
                        val ir = buffer.get(offset).toInt() and 0xff
                        offset += xrange.step * 1
                        mapper(x, y, ir / 255.0, 0.0, 0.0, 1.0)
                    }
                }
            }

            Pair(ColorType.FLOAT32, ColorFormat.R) -> {
                for ((iy, y) in yrange.withIndex()) {
                    val ay = if (colorBuffer.flipV) iy else result.size - 1 - iy
                    var offset = y * colorBuffer.effectiveWidth * 4 + xrange.first * 4
                    result[ay] = (xrange).map { x ->
                        val ir = buffer.getFloat(offset)
                        offset += xrange.step * 4
                        mapper(x, y, ir.toDouble(), 0.0, 0.0, 1.0)
                    }
                }
            }
            Pair(ColorType.FLOAT32, ColorFormat.RG) -> {
                for ((iy, y) in yrange.withIndex()) {
                    val ay = if (colorBuffer.flipV) iy else result.size - 1 - iy
                    var offset = y * colorBuffer.effectiveWidth * 8 + xrange.first * 8
                    result[ay] = (xrange).map { x ->
                        val ir = buffer.getFloat(offset)
                        val ig = buffer.getFloat(offset + 4)
                        offset += xrange.step * 8
                        mapper(x, y, ir.toDouble(), ig.toDouble(), 0.0, 1.0)
                    }
                }
            }
            Pair(ColorType.FLOAT32, ColorFormat.RGB) -> {
                for ((iy, y) in yrange.withIndex()) {
                    val ay = if (colorBuffer.flipV) iy else result.size - 1 - iy
                    var offset = y * colorBuffer.effectiveWidth * 12 + xrange.first * 12
                    result[ay] = (xrange).map { x ->
                        val ir = buffer.getFloat(offset)
                        val ig = buffer.getFloat(offset + 4)
                        val ib = buffer.getFloat(offset + 8)
                        offset += xrange.step * 12
                        mapper(x, y, ir.toDouble(), ig.toDouble(), ib.toDouble(), 1.0)
                    }
                }
            }
            Pair(ColorType.FLOAT32, ColorFormat.RGBa) -> {
                for ((iy, y) in yrange.withIndex()) {
                    val ay = if (colorBuffer.flipV) iy else result.size - 1 - iy
                    var offset = y * colorBuffer.effectiveWidth * 16 + xrange.first * 16
                    result[ay] = (xrange).map { x ->
                        val ir = buffer.getFloat(offset)
                        val ig = buffer.getFloat(offset + 4)
                        val ib = buffer.getFloat(offset + 8)
                        val ia = buffer.getFloat(offset + 12)
                        offset += xrange.step * 16
                        mapper(x, y, ir.toDouble(), ig.toDouble(), ib.toDouble(), ia.toDouble())
                    }
                }
            }
            else -> throw NotImplementedError()
        }
        return result
    }

    override fun read(x: Int, y: Int): ColorRGBa {
        val componentCount = colorBuffer.format.componentCount
        val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
        val offset = (ay * colorBuffer.effectiveWidth + x) * colorBuffer.format.componentCount * colorBuffer.type.componentSize
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

private val IntProgression.size: Int
    get() {
        return 1 + (this.last - this.first) / this.step
    }

class ColorBufferDataGL3(val width: Int, val height: Int, val format: ColorFormat, val type: ColorType, var data: ByteBuffer?, var destroyFunction: ((ByteBuffer) -> Unit)? = null) {
    fun destroy() {
        val localData = data
        if (localData != null) {
            destroyFunction?.invoke(localData)
            //STBImage.stbi_image_free(localData)
            data = null
        }
    }

    companion object {
        fun fromUrl(urlString: String): ColorBufferDataGL3 {
            if (urlString.startsWith("data:")) {
                val decoder = Base64.getDecoder()
                val commaIndex = urlString.indexOf(",")
                val base64Data = urlString.drop(commaIndex + 1)
                val decoded = decoder.decode(base64Data)
                val buffer = ByteBuffer.allocateDirect(decoded.size)
                buffer.put(decoded)
                (buffer as Buffer).rewind()
                return fromByteBuffer(buffer, "data-url")
            } else {
                val url = URL(urlString)
                url.openStream().use {
                    val byteArray = url.readBytes()
                    if (byteArray.isEmpty()) {
                        throw RuntimeException("read 0 bytes from stream $urlString")
                    }
                    val buffer = BufferUtils.createByteBuffer(byteArray.size)
                    (buffer as Buffer).rewind()
                    buffer.put(byteArray)
                    (buffer as Buffer).rewind()
                    return fromByteBuffer(buffer, urlString)
                }
            }
        }

        fun fromStream(stream: InputStream, name: String? = null, formatHint: ImageFileFormat? = null): ColorBufferDataGL3 {
            val byteArray = stream.readBytes()
            val buffer = BufferUtils.createByteBuffer(byteArray.size)
            (buffer as Buffer).rewind()
            buffer.put(byteArray)
            (buffer as Buffer).rewind()
            return fromByteBuffer(buffer, name)
        }

        fun fromArray(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size, name: String? = null, formatHint: ImageFileFormat? = null): ColorBufferDataGL3 {
            val buffer = ByteBuffer.allocateDirect(length)
            buffer.put(bytes, offset, length)
            return fromByteBuffer(buffer, name)
        }

        fun fromByteBuffer(buffer: ByteBuffer, name: String? = null, formatHint: ImageFileFormat? = null): ColorBufferDataGL3 {

            var likelyFormat = ImageFileFormat.PNG

            if (formatHint != null) {
                likelyFormat = formatHint
            }

            if (likelyFormat == ImageFileFormat.PNG || likelyFormat == ImageFileFormat.JPG) {
                val wa = IntArray(1)
                val ha = IntArray(1)
                val ca = IntArray(1)

                STBImage.stbi_set_flip_vertically_on_load(true)
                STBImage.stbi_set_unpremultiply_on_load(false)

                val data = STBImage.stbi_load_from_memory(buffer, wa, ha, ca, 0)
                if (data != null) {
                    var offset = 0
                    if (ca[0] == 4) {
                        for (y in 0 until ha[0]) {
                            for (x in 0 until wa[0]) {
                                val a = (data.get(offset + 3).toInt() and 0xff).toDouble() / 255.0
                                val r = ((data.get(offset).toInt() and 0xff) * a).toByte()
                                val g = ((data.get(offset + 1).toInt() and 0xff) * a).toByte()
                                val b = ((data.get(offset + 2).toInt() and 0xff) * a).toByte()

                                data.put(offset, r)
                                data.put(offset + 1, g)
                                data.put(offset + 2, b)
                                offset += 4
                            }
                        }
                    }
                }

                if (data != null) {
                    return ColorBufferDataGL3(wa[0], ha[0],
                            when (ca[0]) {
                                1 -> ColorFormat.R
                                2 -> ColorFormat.RG
                                3 -> ColorFormat.RGB
                                4 -> ColorFormat.RGBa
                                else -> throw Exception("invalid component count ${ca[0]}")
                            }
                            , ColorType.UINT8, data) { b -> STBImage.stbi_image_free(b) }
                } else {
                    throw RuntimeException("failed to load image ${name ?: ("unknown image")}")
                }
            } else if (likelyFormat == ImageFileFormat.EXR) {
                val exrHeader = EXRHeader.create()
                val exrVersion = EXRVersion.create()
                val versionResult = ParseEXRVersionFromMemory(exrVersion, buffer)
                (buffer as Buffer).rewind()

                if (versionResult != TINYEXR_SUCCESS) {
                    error("failed to get version")
                }

                val errors = PointerBuffer.allocateDirect(1)

                val parseResult = ParseEXRHeaderFromMemory(exrHeader, exrVersion, buffer, errors)
                if (parseResult != TINYEXR_SUCCESS) {
                    error("failed to parse file")
                }

                for (i in 0 until exrHeader.num_channels()) {
                    exrHeader.requested_pixel_types().put(i, exrHeader.pixel_types().get(i))
                }

                val exrImage = EXRImage.create()
                InitEXRImage(exrImage)


                LoadEXRImageFromMemory(exrImage, exrHeader, buffer, errors)

                val format =
                        when (val c = exrImage.num_channels()) {
                            1 -> ColorFormat.R
                            3 -> ColorFormat.RGB
                            4 -> ColorFormat.RGBa
                            else -> error("unsupported number of channels $c")
                        }

                val type = when (val t = exrHeader.requested_pixel_types().get(0)) {
                    TINYEXR_PIXELTYPE_HALF -> ColorType.FLOAT16
                    TINYEXR_PIXELTYPE_FLOAT -> ColorType.FLOAT32
                    else -> error("unsupported pixel type [type=$t]")
                }

                val height = exrImage.height()
                val width = exrImage.width()
                val channels = exrImage.num_channels()

                val data = ByteBuffer.allocateDirect(format.componentCount * type.componentSize * exrImage.width() * exrImage.height()).order(ByteOrder.nativeOrder())
                val channelNames = (0 until exrHeader.num_channels()).map { exrHeader.channels().get(it).nameString() }
                val images = exrImage.images()!!
                val channelImages = (0 until exrHeader.num_channels()).map { images.getByteBuffer(it, width * height * type.componentSize) }


                val order = when (format) {
                    ColorFormat.R -> listOf("R").map { channelNames.indexOf(it) }
                    ColorFormat.RGB -> listOf("B", "G", "R").map { channelNames.indexOf(it) }
                    ColorFormat.RGBa -> listOf("B", "G", "R", "A").map { channelNames.indexOf(it) }
                    else -> error("unsupported channel layout")
                }
                require(order.none { it == -1 }) { "some channels are not found" }

                val orderedImages = order.map { channelImages[it] }
                orderedImages.forEach { (it as Buffer).rewind() }

                for (y in 0 until exrImage.height()) {
                    val offset = (height - 1 - y) * format.componentCount * type.componentSize * width
                    (data as Buffer).position(offset)
                    for (x in 0 until exrImage.width()) {
                        for (c in 0 until channels) {
                            for (i in 0 until type.componentSize) {
                                data.put(orderedImages[c].get())
                            }
                        }
                    }
                }
                (data as Buffer).rewind()

                FreeEXRHeader(exrHeader)
                FreeEXRImage(exrImage)
                return ColorBufferDataGL3(exrImage.width(), exrImage.height(), format, type, data)
            } else {
                error("format not supported")
            }
        }

        fun fromFile(filename: String): ColorBufferDataGL3 {
            val file = File(filename)

            val byteArray = file.readBytes()
            if (byteArray.isEmpty()) {
                throw RuntimeException("read 0 bytes from stream $filename")
            }
            val buffer = BufferUtils.createByteBuffer(byteArray.size)
            (buffer as Buffer).rewind()
            buffer.put(byteArray)
            (buffer as Buffer).rewind()

            return fromByteBuffer(buffer, filename, formatHint = ImageFileFormat.guessFromExtension(file))
        }
    }
}

@Suppress("MemberVisibilityCanBePrivate")
class ColorBufferGL3(val target: Int,
                     val texture: Int,
                     override val width: Int,
                     override val height: Int,
                     override val contentScale: Double,
                     override val format: ColorFormat,
                     override val type: ColorType,
                     override val multisample: BufferMultisample,
                     override val session: Session?) : ColorBuffer {

    private var destroyed = false
    override var flipV: Boolean = false

    internal fun format(): Int {
        return internalFormat(format, type)
    }

    companion object {
        fun fromColorBufferData(data: ColorBufferDataGL3, session: Session?): ColorBuffer {
            val cb = create(data.width, data.height, 1.0, data.format, data.type, Disabled, 1, session)
            return cb.apply {
                val d = data.data
                if (d != null) {
                    cb.write(d)
                    cb.generateMipmaps()
                } else {
                    throw RuntimeException("data is null")
                }
                data.destroy()
                glFlush()
                glFinish()
            }
        }

        fun fromUrl(url: String, session: Session?): ColorBuffer {
            val data = ColorBufferDataGL3.fromUrl(url)
            return fromColorBufferData(data, session)
        }

        fun fromFile(filename: String, session: Session?): ColorBuffer {
            val data = ColorBufferDataGL3.fromFile(filename)
            return fromColorBufferData(data, session)
        }

        fun fromStream(stream: InputStream, name: String?, formatHint: ImageFileFormat?, session: Session?): ColorBuffer {
            val data = ColorBufferDataGL3.fromStream(stream, name)
            return fromColorBufferData(data, session)
        }

        fun fromArray(array: ByteArray, offset: Int = 0, length: Int = array.size, name: String?, formatHint: ImageFileFormat?, session: Session?): ColorBuffer {
            val data = ColorBufferDataGL3.fromArray(array, offset, length, name, formatHint)
            return fromColorBufferData(data, session)
        }

        fun fromBuffer(buffer: ByteBuffer, name: String?, formatHint: ImageFileFormat?, session: Session?): ColorBuffer {
            val data = ColorBufferDataGL3.fromByteBuffer(buffer, name, formatHint)
            return fromColorBufferData(data, session)
        }

        fun create(width: Int,
                   height: Int,
                   contentScale: Double = 1.0,
                   format: ColorFormat = ColorFormat.RGBa,
                   type: ColorType = ColorType.FLOAT32,
                   multisample: BufferMultisample,
                   levels: Int,
                   session: Session?): ColorBufferGL3 {
            val internalFormat = internalFormat(format, type)
            if (width <= 0 || height <= 0) {
                throw Exception("cannot create ColorBuffer with dimensions: ${width}x$height")
            }
            checkGLErrors()

            val texture = glGenTextures()
            checkGLErrors()

            glActiveTexture(GL_TEXTURE0)

            when (multisample) {
                Disabled -> glBindTexture(GL_TEXTURE_2D, texture)
                is SampleCount -> glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, texture)
            }

            checkGLErrors()

            val effectiveWidth = (width * contentScale).toInt()
            val effectiveHeight = (height * contentScale).toInt()

            val nullBB: ByteBuffer? = null

            if (levels > 1) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, levels - 1)
            }

            for (level in 0 until levels) {
                val div = 1 shl level
                when (multisample) {
                    Disabled ->
                        glTexImage2D(GL_TEXTURE_2D, level, internalFormat, effectiveWidth / div, effectiveHeight / div, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullBB)
                    is SampleCount -> glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, multisample.sampleCount.coerceAtMost(glGetInteger(GL_MAX_COLOR_TEXTURE_SAMPLES)), internalFormat, effectiveWidth / div, effectiveHeight / div, true)
                }
            }
            checkGLErrors {
                when (it) {
                    GL_INVALID_OPERATION -> """format is GL_DEPTH_COMPONENT ${format.glFormat() == GL_DEPTH_COMPONENT} and internalFormat is not GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT16, GL_DEPTH_COMPONENT24, or GL_DEPTH_COMPONENT32F"""
                    GL_INVALID_FRAMEBUFFER_OPERATION -> "buh?"
                    else -> null
                }
            }

            val target = when (multisample) {
                Disabled -> GL_TEXTURE_2D
                is SampleCount -> GL_TEXTURE_2D_MULTISAMPLE
            }

            if (multisample == Disabled) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
                checkGLErrors()
            }

            return ColorBufferGL3(target, texture, width, height, contentScale, format, type, multisample, session)
        }
    }

    fun bound(f: ColorBufferGL3.() -> Unit) {
        checkDestroyed()
        glActiveTexture(GL_TEXTURE0)
        val current = when (multisample) {
            Disabled -> glGetInteger(GL_TEXTURE_BINDING_2D)
            is SampleCount -> glGetInteger(GL_TEXTURE_BINDING_2D_MULTISAMPLE)
        }
        glBindTexture(target, texture)
        this.f()
        glBindTexture(target, current)
    }

    fun destroyShadow() {
        realShadow = null
    }

    override fun generateMipmaps() {
        checkDestroyed()
        if (multisample == Disabled) {
            bound {

                glGenerateMipmap(target)
            }
        } else {
            throw IllegalArgumentException("generating Mipmaps for multisample targets is not possible")
        }
    }

    override fun resolveTo(target: ColorBuffer) {
        checkDestroyed()
        if (target.format != format) {
            throw IllegalArgumentException("cannot resolve to target because its color format differs. got ${target.format}, expected $format.")
        }

        if (target.type != type) {
            throw IllegalArgumentException("cannot resolve to target because its color type differs. got ${target.type}, expected $type.")
        }

        val readTarget = renderTarget(width, height, contentScale, multisample = multisample) {
            colorBuffer(this@ColorBufferGL3)
        } as RenderTargetGL3

        val writeTarget = renderTarget(target.width, target.height, target.contentScale, multisample = target.multisample) {
            colorBuffer(target)
        } as RenderTargetGL3

        writeTarget.bind()
        glBindFramebuffer(GL_READ_FRAMEBUFFER, readTarget.framebuffer)
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST)
        writeTarget.unbind()

        writeTarget.detachColorBuffers()
        writeTarget.destroy()

        readTarget.detachColorBuffers()
        readTarget.destroy()
    }

    override fun copyTo(target: ColorBuffer, fromLevel: Int, toLevel: Int) {
        checkDestroyed()
        val readTarget = renderTarget(width, height, contentScale) {
            colorBuffer(this@ColorBufferGL3, fromLevel)
        } as RenderTargetGL3

        target as ColorBufferGL3
        readTarget.bind()
        glReadBuffer(GL_COLOR_ATTACHMENT0)
        debugGLErrors()
        val div = 1 shl toLevel
        target.bound {
            glCopyTexSubImage2D(target.target, toLevel, 0, 0, 0, 0, target.width / div, target.height / div)
            debugGLErrors() {
                when (it) {
                    GL_INVALID_VALUE -> "level ($toLevel) less than 0, effective target is GL_TEXTURE_RECTANGLE (${target.target == GL_TEXTURE_RECTANGLE} and level is not 0"
                    else -> null
                }
            }
        }
        readTarget.unbind()

        readTarget.detachColorBuffers()
        readTarget.destroy()
    }

    override fun copyTo(target: ArrayTexture, layer: Int, fromLevel: Int, toLevel: Int) {
        checkDestroyed()
        val readTarget = renderTarget(width, height, contentScale) {
            colorBuffer(this@ColorBufferGL3)
        } as RenderTargetGL3

        target as ArrayTextureGL3
        readTarget.bind()
        glReadBuffer(GL_COLOR_ATTACHMENT0)
        target.bound {
            glCopyTexSubImage3D(target.target, 0, 0, 0, layer, 0, 0, target.width, target.height)
            debugGLErrors()
        }
        readTarget.unbind()

        readTarget.detachColorBuffers()
        readTarget.destroy()
    }

    override fun fill(color: ColorRGBa) {
        checkDestroyed()
        val writeTarget = renderTarget(width, height, contentScale) {
            colorBuffer(this@ColorBufferGL3)
        } as RenderTargetGL3

        writeTarget.bind()
        glClearBufferfv(GL_COLOR, 0, floatArrayOf(color.r.toFloat(), color.g.toFloat(), color.b.toFloat(), color.a.toFloat()))
        debugGLErrors()
        writeTarget.unbind()

        writeTarget.detachColorBuffers()
        writeTarget.destroy()
    }

    override var wrapU: WrapMode
        get() = TODO("not implemented")
        set(value) {
            bound {
                glTexParameteri(target, GL_TEXTURE_WRAP_S, value.glWrap())
            }
        }

    override var wrapV: WrapMode
        get() = TODO("not implemented")
        set(value) {
            bound {
                glTexParameteri(target, GL_TEXTURE_WRAP_T, value.glWrap())
            }
        }

    override var filterMin: MinifyingFilter
        get() = TODO("not implemented")
        set(value) {
            bound {
                glTexParameteri(target, GL_TEXTURE_MIN_FILTER, value.toGLFilter())
            }
        }

    override var filterMag: MagnifyingFilter
        get() = TODO("not implemented")
        set(value) {
            bound {
                glTexParameteri(target, GL_TEXTURE_MAG_FILTER, value.toGLFilter())
            }
        }

    override var anisotropy: Double
        get() = TODO("not implemented")
        set(value) {
            bound {
                glTexParameterf(target, GL_TEXTURE_MAX_ANISOTROPY_EXT, value.toFloat())
            }
        }

    override val shadow: ColorBufferShadow
        get() {
            if (multisample == Disabled) {
                if (realShadow == null) {
                    realShadow = ColorBufferShadowGL3(this)
                }
                return realShadow!!
            } else {
                throw IllegalArgumentException("multisample targets cannot be shadowed")
            }
        }

    var realShadow: ColorBufferShadow? = null

    override fun write(buffer: ByteBuffer, sourceFormat: ColorFormat, sourceType: ColorType, level: Int) {
        checkDestroyed()
        if (!buffer.isDirect) {
            throw IllegalArgumentException("buffer is not a direct buffer.")
        }

        if (multisample == Disabled) {
            bound {
                debugGLErrors()
                logger.trace {
                    "Writing to color buffer in: $format ${format.glFormat()}, $type ${type.glType()}"
                }
                (buffer as Buffer).rewind()
                buffer.order(ByteOrder.nativeOrder())
                val currentPack = intArrayOf(0)
                glGetIntegerv(GL_UNPACK_ALIGNMENT, currentPack)
                glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
                val div = 1 shl level
                if (sourceType.compressed) {
                    glCompressedTexSubImage2D(target, level, 0, 0, width / div, height / div, compressedType(sourceFormat, sourceType), buffer)
                    debugGLErrors {
                        when (it) {
                            GL_INVALID_VALUE -> "data size mismatch? ${buffer.remaining()}"
                            else -> null
                        }
                    }
                } else {
                    glTexSubImage2D(target, level, 0, 0, width / div, height / div, sourceFormat.glFormat(), sourceType.glType(), buffer)
                    debugGLErrors()
                }
                glPixelStorei(GL_UNPACK_ALIGNMENT, currentPack[0])
                debugGLErrors()
                (buffer as Buffer).rewind()
            }
        } else {
            throw IllegalArgumentException("multisample targets cannot be written to")
        }
    }


    override fun read(buffer: ByteBuffer, targetFormat: ColorFormat, targetType: ColorType, level: Int) {
        checkDestroyed()
        if (!buffer.isDirect) {
            throw IllegalArgumentException("buffer is not a direct buffer.")
        }
        if (multisample == Disabled) {
            bound {
                logger.trace {
                    "Reading from color buffer in: $format ${format.glFormat()}, $type ${type.glType()} "
                }
                debugGLErrors()
                glPixelStorei(GL_PACK_ALIGNMENT, 1)
                debugGLErrors()
                val packAlignment = glGetInteger(GL_PACK_ALIGNMENT)
                buffer.order(ByteOrder.nativeOrder())
                (buffer as Buffer).rewind()
                glGetTexImage(target, 0, targetFormat.glFormat(), targetType.glType(), buffer)
                debugGLErrors()
                (buffer as Buffer).rewind()
                glPixelStorei(GL_PACK_ALIGNMENT, packAlignment)
                debugGLErrors()
            }
        } else {
            throw IllegalArgumentException("multisample targets cannot be read from")
        }
    }

    override fun saveToFile(file: File, imageFileFormat: ImageFileFormat, async: Boolean) {
        checkDestroyed()
        if (multisample == Disabled) {
            if (type == ColorType.UINT8) {
                var pixels = BufferUtils.createByteBuffer(effectiveWidth * effectiveHeight * format.componentCount)
                (pixels as Buffer).rewind()
                read(pixels)
                (pixels as Buffer).rewind()

                runBlocking {
                    val job = GlobalScope.launch {
                        if (!flipV) {
                            val flippedPixels = BufferUtils.createByteBuffer(effectiveWidth * effectiveHeight * format.componentCount)
                            (flippedPixels as Buffer).rewind()
                            val stride = effectiveWidth * format.componentCount
                            val row = ByteArray(stride)

                            for (y in 0 until effectiveHeight) {
                                (pixels as Buffer).position((effectiveHeight - y - 1) * stride)
                                pixels.get(row)
                                flippedPixels.put(row)
                            }

                            (flippedPixels as Buffer).rewind()
                            pixels = flippedPixels
                        }

                        when (imageFileFormat) {
                            ImageFileFormat.JPG -> STBImageWrite.stbi_write_jpg(
                                    file.absolutePath,
                                    effectiveWidth, effectiveHeight,
                                    format.componentCount, pixels, 90)
                            ImageFileFormat.PNG -> STBImageWrite.stbi_write_png(
                                    file.absolutePath,
                                    effectiveWidth, effectiveHeight,
                                    format.componentCount, pixels, effectiveWidth * format.componentCount)
                            else -> error("format not supported")
                        }
                    }
                    if (!async) {
                        job.join()
                    }
                }
            } else if (type == ColorType.FLOAT16 || type == ColorType.FLOAT32) {
                require(imageFileFormat == ImageFileFormat.EXR) { "can only save floating point color buffers to EXR" }
                require(format == ColorFormat.RGB || format == ColorFormat.RGBa) { "can only save RGB and RGBA formats" }

                val exrType = if (type == ColorType.FLOAT16) TINYEXR_PIXELTYPE_HALF else TINYEXR_PIXELTYPE_FLOAT

                val exrImage = EXRImage.create()
                InitEXRImage(exrImage)

                val exrHeader = EXRHeader.create()
                InitEXRHeader(exrHeader)

                exrHeader.num_channels(3)

                val exrChannels = EXRChannelInfo.calloc(3)
                exrChannels[0].name(ByteBuffer.allocateDirect(2).apply { put('B'.toByte()); put(0.toByte()); (this as Buffer).rewind() })
                exrChannels[1].name(ByteBuffer.allocateDirect(2).apply { put('G'.toByte()); put(0.toByte()); (this as Buffer).rewind() })
                exrChannels[2].name(ByteBuffer.allocateDirect(2).apply { put('R'.toByte()); put(0.toByte()); (this as Buffer).rewind() })

                exrHeader.channels(exrChannels)

                val data = ByteBuffer.allocateDirect(type.componentSize * 3 * effectiveWidth * effectiveHeight).order(ByteOrder.nativeOrder())
                (data as Buffer).rewind()
                read(data, targetFormat = ColorFormat.RGB)
                (data as Buffer).rewind()
                val bBuffer = ByteBuffer.allocateDirect(effectiveWidth * effectiveHeight * 4).order(ByteOrder.nativeOrder())
                val gBuffer = ByteBuffer.allocateDirect(effectiveWidth * effectiveHeight * 4).order(ByteOrder.nativeOrder())
                val rBuffer = ByteBuffer.allocateDirect(effectiveWidth * effectiveHeight * 4).order(ByteOrder.nativeOrder())

                // -- de-interleave and flip data
                for (y in 0 until height) {
                    val row = if (!flipV) effectiveHeight - 1 - y else y
                    var offset = row * effectiveWidth * type.componentSize * 3

                    (data as Buffer).position(offset)

                    for (x in 0 until effectiveWidth) {
                        for (i in 0 until type.componentSize) {
                            val b = data.get()
                            bBuffer.put(b)
                        }
                        for (i in 0 until type.componentSize) {
                            val g = data.get()
                            gBuffer.put(g)
                        }
                        for (i in 0 until type.componentSize) {
                            val r = data.get()
                            rBuffer.put(r)
                        }
                    }
                }

                (bBuffer as Buffer).rewind()
                (gBuffer as Buffer).rewind()
                (rBuffer as Buffer).rewind()


                val pixelTypes = BufferUtils.createIntBuffer(4 * 3).apply {
                    put(exrType); put(exrType); put(exrType); (this as Buffer).rewind()
                }
                exrHeader.pixel_types(pixelTypes)
                (pixelTypes as Buffer).rewind()
                exrHeader.requested_pixel_types(pixelTypes)

                exrImage.width(width)
                exrImage.height(height)
                exrImage.num_channels(3)

                val images = PointerBuffer.allocateDirect(3)
                images.put(0, bBuffer)
                images.put(1, gBuffer)
                images.put(2, rBuffer)
                (images as Buffer).rewind()

                exrImage.images(images)

                val errors = PointerBuffer.allocateDirect(1)
                val result = SaveEXRImageToFile(exrImage, exrHeader, file.path, errors)

                require(result == 0) {
                    "failed to save to ${file.path}, [result=$result]"
                }
                //FreeEXRHeader(exrHeader)
                FreeEXRImage(exrImage)
            }

        } else {
            throw IllegalArgumentException("multisample targets cannot be saved to file")
        }
    }

    override fun toDataUrl(imageFileFormat: ImageFileFormat): String {
        checkDestroyed()

        require(multisample == Disabled)
        require(type == ColorType.UINT8)

        val saveBuffer = ByteBuffer.allocate(1_024 * 1_024 * 2)
        val writeFunc = object : STBIWriteCallback() {
            override fun invoke(context: Long, data: Long, size: Int) {
                val sourceBuffer = MemoryUtil.memByteBuffer(data, size)
                saveBuffer?.put(sourceBuffer)
            }
        }

        var pixels = BufferUtils.createByteBuffer(effectiveWidth * effectiveHeight * format.componentCount)
        (pixels as Buffer).rewind()
        read(pixels)
        (pixels as Buffer).rewind()
        if (!flipV) {
            val flippedPixels = BufferUtils.createByteBuffer(effectiveWidth * effectiveHeight * format.componentCount)
            (flippedPixels as Buffer).rewind()
            val stride = effectiveWidth * format.componentCount
            val row = ByteArray(stride)

            for (y in 0 until effectiveHeight) {
                (pixels as Buffer).position((effectiveHeight - y - 1) * stride)
                pixels.get(row)
                flippedPixels.put(row)
            }

            (flippedPixels as Buffer).rewind()
            pixels = flippedPixels
        }

        when (imageFileFormat) {
            ImageFileFormat.JPG -> STBImageWrite.stbi_write_jpg_to_func(
                    writeFunc, 0L,
                    effectiveWidth, effectiveHeight,
                    format.componentCount, pixels, 90)
            ImageFileFormat.PNG -> STBImageWrite.stbi_write_png_to_func(
                    writeFunc, 0L,
                    effectiveWidth, effectiveHeight,
                    format.componentCount, pixels, effectiveWidth * format.componentCount)
        }

        val byteArray = ByteArray((saveBuffer as Buffer).position())
        (saveBuffer as Buffer).rewind()
        saveBuffer.get(byteArray)
        val base64Data = Base64.getEncoder().encodeToString(byteArray)

        return "data:${imageFileFormat.mimeType};base64,$base64Data"
    }

    override fun destroy() {
        session?.untrack(this)
        glDeleteTextures(texture)
        destroyed = true
        checkGLErrors()
    }

    override fun bind(unit: Int) {
        checkDestroyed()
        if (multisample == Disabled) {
            glActiveTexture(GL_TEXTURE0 + unit)
            glBindTexture(target, texture)
        } else {
            throw IllegalArgumentException("multisample targets cannot be bound as texture")
        }
    }

    private fun checkDestroyed() {
        if (destroyed) {
            throw IllegalStateException("colorbuffer is destroyed")
        }
    }
}

internal fun MinifyingFilter.toGLFilter(): Int {
    return when (this) {
        MinifyingFilter.NEAREST -> GL_NEAREST
        MinifyingFilter.LINEAR -> GL_LINEAR
        MinifyingFilter.LINEAR_MIPMAP_LINEAR -> GL_LINEAR_MIPMAP_LINEAR
        MinifyingFilter.LINEAR_MIPMAP_NEAREST -> GL_LINEAR_MIPMAP_NEAREST
        MinifyingFilter.NEAREST_MIPMAP_LINEAR -> GL_NEAREST_MIPMAP_LINEAR
        MinifyingFilter.NEAREST_MIPMAP_NEAREST -> GL_NEAREST_MIPMAP_NEAREST
    }
}

internal fun MagnifyingFilter.toGLFilter(): Int {
    return when (this) {
        MagnifyingFilter.NEAREST -> GL_NEAREST
        MagnifyingFilter.LINEAR -> GL_LINEAR
    }
}

internal fun WrapMode.glWrap(): Int {
    return when (this) {
        WrapMode.CLAMP_TO_EDGE -> GL_CLAMP_TO_EDGE
        WrapMode.MIRRORED_REPEAT -> GL_MIRRORED_REPEAT
        WrapMode.REPEAT -> GL_REPEAT
    }
}

internal fun ColorFormat.glFormat(): Int {
    return when (this) {
        ColorFormat.R -> GL_RED
        ColorFormat.RG -> GL_RG
        ColorFormat.RGB -> GL_RGB
        ColorFormat.RGBa -> GL_RGBA
        ColorFormat.sRGB -> GL_RGB
        ColorFormat.sRGBa -> GL_RGBA
        ColorFormat.BGR -> GL_BGR
        ColorFormat.BGRa -> GL_BGRA
    }
}

internal fun ColorType.glType(): Int {
    return when (this) {
        ColorType.UINT8, ColorType.UINT8_INT -> GL_UNSIGNED_BYTE
        ColorType.SINT8_INT -> GL_BYTE
        ColorType.UINT16, ColorType.UINT16_INT -> GL_UNSIGNED_SHORT
        ColorType.SINT16_INT -> GL_SHORT
        ColorType.FLOAT16 -> GL_HALF_FLOAT
        ColorType.FLOAT32 -> GL_FLOAT
        ColorType.DXT1, ColorType.DXT3, ColorType.DXT5,
        ColorType.BPTC_UNORM, ColorType.BPTC_FLOAT, ColorType.BPTC_UFLOAT -> throw RuntimeException("gl type of compressed types cannot be queried")
    }
}

internal fun compressedType(format: ColorFormat, type: ColorType): Int {
    when (format) {
        ColorFormat.RGBa -> return when (type) {
            ColorType.DXT1 -> GL_COMPRESSED_RGBA_S3TC_DXT1_EXT
            ColorType.DXT3 -> GL_COMPRESSED_RGBA_S3TC_DXT3_EXT
            ColorType.DXT5 -> GL_COMPRESSED_RGBA_S3TC_DXT5_EXT
            ColorType.BPTC_UNORM -> GL_COMPRESSED_RGBA_BPTC_UNORM_ARB
            else -> throw IllegalArgumentException()
        }
        ColorFormat.sRGBa -> return when (type) {
            ColorType.DXT1 -> GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT
            ColorType.DXT3 -> GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT
            ColorType.DXT5 -> GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT
            else -> throw IllegalArgumentException()
        }
        ColorFormat.RGB -> return when (type) {
            ColorType.DXT1 -> GL_COMPRESSED_RGB_S3TC_DXT1_EXT
            else -> throw IllegalArgumentException()
        }
        ColorFormat.sRGB -> return when (type) {
            ColorType.DXT1 -> GL_COMPRESSED_SRGB_S3TC_DXT1_EXT
            else -> throw IllegalArgumentException()
        }
        else -> throw IllegalArgumentException()
    }
}