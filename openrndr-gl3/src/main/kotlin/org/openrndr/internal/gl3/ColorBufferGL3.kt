package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.EXTTextureCompressionS3TC.*
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImageWrite
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.MinifyingFilter
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

import org.openrndr.draw.*
import org.openrndr.draw.BufferMultisample.DISABLED
import org.openrndr.draw.BufferMultisample.SampleCount
import java.io.File
import java.nio.Buffer

data class ConversionEntry(val format: ColorFormat, val type: ColorType, val glFormat: Int)

private val logger = KotlinLogging.logger {}

fun internalFormat(format: ColorFormat, type: ColorType): Int {
    val entries = arrayOf(
            ConversionEntry(ColorFormat.R, ColorType.UINT8, GL_R8),
            ConversionEntry(ColorFormat.R, ColorType.UINT16, GL_R16),
            ConversionEntry(ColorFormat.R, ColorType.FLOAT16, GL_R16F),
            ConversionEntry(ColorFormat.R, ColorType.FLOAT32, GL_R32F),
            ConversionEntry(ColorFormat.RG, ColorType.UINT8, GL_RG8),
            ConversionEntry(ColorFormat.RG, ColorType.UINT16, GL_RG16),
            ConversionEntry(ColorFormat.RG, ColorType.FLOAT16, GL_RG16F),
            ConversionEntry(ColorFormat.RG, ColorType.FLOAT32, GL_RG32F),
            ConversionEntry(ColorFormat.RGB, ColorType.UINT8, GL_RGB8),
            ConversionEntry(ColorFormat.RGB, ColorType.UINT16, GL_RGB16),
            ConversionEntry(ColorFormat.RGB, ColorType.FLOAT16, GL_RGB16F),
            ConversionEntry(ColorFormat.RGB, ColorType.FLOAT32, GL_RGB32F),
            ConversionEntry(ColorFormat.BGR, ColorType.UINT8, GL_RGB8),
            ConversionEntry(ColorFormat.RGBa, ColorType.UINT8, GL_RGBA8),
            ConversionEntry(ColorFormat.RGBa, ColorType.UINT16, GL_RGBA16),
            ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT16, GL_RGBA16F),
            ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT32, GL_RGBA32F),
            ConversionEntry(ColorFormat.R, ColorType.UINT8, GL_R8),
            ConversionEntry(ColorFormat.R, ColorType.FLOAT16, GL_R16F),
            ConversionEntry(ColorFormat.R, ColorType.FLOAT32, GL_R32F),
            ConversionEntry(ColorFormat.sRGB, ColorType.UINT8, GL_SRGB8),
            ConversionEntry(ColorFormat.sRGBa, ColorType.UINT8, GL_SRGB8_ALPHA8),
            ConversionEntry(ColorFormat.RGBa, ColorType.DXT1, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT),
            ConversionEntry(ColorFormat.RGBa, ColorType.DXT3, GL_COMPRESSED_RGBA_S3TC_DXT3_EXT),
            ConversionEntry(ColorFormat.RGBa, ColorType.DXT5, GL_COMPRESSED_RGBA_S3TC_DXT5_EXT),
            ConversionEntry(ColorFormat.RGB, ColorType.DXT1, GL_COMPRESSED_RGB_S3TC_DXT1_EXT))

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
        (colorBuffer).read(buffer)
    }

    override fun upload() {
        (colorBuffer).write(buffer)
    }

    override fun destroy() {
        (colorBuffer).destroyShadow()
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
        buffer.rewind()
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
        buffer.rewind()
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
        buffer.rewind()
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
        buffer.rewind()
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


class ColorBufferDataGL3(val width: Int, val height: Int, val format: ColorFormat, val type: ColorType, var data: ByteBuffer?) {

    fun destroy() {
        val localData = data
        if (localData != null) {
            STBImage.stbi_image_free(localData)
            data = null
        }
    }

    companion object {
        fun fromUrl(urlString: String): ColorBufferDataGL3 {
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
                            , ColorType.UINT8, data)
                } else {
                    throw RuntimeException("failed to load image $urlString")
                }
            }
        }

        fun fromFile(filename: String): ColorBufferDataGL3 {
            val byteArray = File(filename).readBytes()
            if (byteArray.isEmpty()) {
                throw RuntimeException("read 0 bytes from stream $filename")
            }
            val buffer = BufferUtils.createByteBuffer(byteArray.size)

            (buffer as Buffer).rewind()
            buffer.put(byteArray)
            (buffer as Buffer).rewind()

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
                        , ColorType.UINT8, data)
            } else {
                throw RuntimeException("failed to load image $filename")
            }
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
                     override val multisample: BufferMultisample) : ColorBuffer {

    internal var realFlipV: Boolean = false
    override var flipV: Boolean
        get() = realFlipV
        set(value) {
            realFlipV = value
        }

    companion object {
        fun fromUrl(url: String): ColorBuffer {
            val data = ColorBufferDataGL3.fromUrl(url)
            val cb = create(data.width, data.height, 1.0, data.format, data.type, DISABLED)
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

        fun fromFile(filename: String): ColorBuffer {
            val data = ColorBufferDataGL3.fromFile(filename)
            val cb = create(data.width, data.height, 1.0, data.format, data.type, DISABLED)
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

        fun create(width: Int,
                   height: Int,
                   contentScale: Double = 1.0,
                   format: ColorFormat = ColorFormat.RGBa,
                   type: ColorType = ColorType.FLOAT32,
                   multisample: BufferMultisample): ColorBufferGL3 {
            val internalFormat = internalFormat(format, type)
            if (width <= 0 || height <= 0) {
                throw Exception("cannot create ColorBuffer with dimensions: ${width}x$height")
            }
            checkGLErrors()

            val texture = glGenTextures()
            checkGLErrors()

            glActiveTexture(GL_TEXTURE0)

            when (multisample) {
                DISABLED -> glBindTexture(GL_TEXTURE_2D, texture)
                is SampleCount -> glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, texture)
            }

            checkGLErrors()

            val effectiveWidth = (width * contentScale).toInt()
            val effectiveHeight = (height * contentScale).toInt()

            val nullBB: ByteBuffer? = null

            when (multisample) {
                DISABLED -> glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, effectiveWidth, effectiveHeight, 0, format.glFormat(), type.glType(), nullBB)
                is SampleCount -> glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, multisample.sampleCount.coerceAtMost(glGetInteger(GL_MAX_COLOR_TEXTURE_SAMPLES)), internalFormat, effectiveWidth, effectiveHeight, true)
            }

            checkGLErrors {
                when (it) {
                    GL_INVALID_OPERATION -> """format is GL_DEPTH_COMPONENT ${format.glFormat() == GL_DEPTH_COMPONENT} and internalFormat is not GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT16, GL_DEPTH_COMPONENT24, or GL_DEPTH_COMPONENT32F"""
                    GL_INVALID_FRAMEBUFFER_OPERATION -> "buh?"
                    else -> null
                }
            }

            val target = when (multisample) {
                DISABLED -> GL_TEXTURE_2D
                is SampleCount -> GL_TEXTURE_2D_MULTISAMPLE
            }

            if (multisample == DISABLED) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
                checkGLErrors()
            }

            return ColorBufferGL3(target, texture, width, height, contentScale, format, type, multisample)
        }
    }

    fun bound(f: ColorBufferGL3.() -> Unit) {
        glActiveTexture(GL_TEXTURE0)
        val current = when (multisample) {
            DISABLED -> glGetInteger(GL_TEXTURE_BINDING_2D)
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
        if (multisample == DISABLED) {
            bound {
                glGenerateMipmap(target)
            }
        } else {
            throw IllegalArgumentException("generating Mipmaps for multisample targets is not possible")
        }
    }

    override fun resolveTo(target: ColorBuffer) {
        if (target.multisample == DISABLED) {
            val readTarget = renderTarget(width, height, contentScale) {
                colorBuffer(this@ColorBufferGL3)
            } as RenderTargetGL3

            val writeTarget = renderTarget(target.width, target.height, target.contentScale) {
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
        } else {
            throw IllegalArgumentException("cannot resolve to multisample target")
        }
    }

    override var wrapU: WrapMode
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {
            bound {
                glTexParameteri(target, GL_TEXTURE_WRAP_S, value.glWrap())
            }
        }

    override var wrapV: WrapMode
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
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

    override val shadow: ColorBufferShadow
        get() {
            if (multisample == DISABLED) {
                if (realShadow == null) {
                    realShadow = ColorBufferShadowGL3(this)
                }
                return realShadow!!
            } else {
                throw IllegalArgumentException("multisample targets cannot be shadowed")
            }
        }

    var realShadow: ColorBufferShadow? = null

    override fun write(buffer: ByteBuffer) {
        if (multisample == DISABLED) {
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
                glTexSubImage2D(target, 0, 0, 0, width, height, format.glFormat(), type.glType(), buffer)
                glPixelStorei(GL_UNPACK_ALIGNMENT, currentPack[0])
                debugGLErrors()
                (buffer as Buffer).rewind()
            }
        } else {
            throw IllegalArgumentException("multisample targets cannot be written to")
        }
    }

    override fun read(buffer: ByteBuffer) {
        if (multisample == DISABLED) {
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
                glGetTexImage(target, 0, format.glFormat(), type.glType(), buffer)
                debugGLErrors()
                (buffer as Buffer).rewind()
                glPixelStorei(GL_PACK_ALIGNMENT, packAlignment)
                debugGLErrors()
            }
        } else {
            throw IllegalArgumentException("multisample targets cannot be read from")
        }
    }

    override fun saveToFile(file: File, fileFormat: FileFormat) {
        if (multisample == DISABLED) {
            if (type == ColorType.UINT8) {
                var pixels = BufferUtils.createByteBuffer(effectiveWidth * effectiveHeight * format.componentCount)
                (pixels as Buffer).rewind()
                read(pixels)
                (pixels as Buffer).rewind()

                if (!flipV) {
                    val flippedPixels = BufferUtils.createByteBuffer(effectiveWidth * effectiveHeight * format.componentCount)
                    (flippedPixels as Buffer).rewind()
                    val stride = width * format.componentCount
                    val row = ByteArray(stride)
                    for (y in 0 until height) {
                        (pixels as Buffer).position((height - y - 1) * stride)
                        pixels.get(row)
                        flippedPixels.put(row)
                    }
                    (flippedPixels as Buffer).rewind()
                    pixels = flippedPixels
                }

                when (fileFormat) {
                    FileFormat.JPG -> STBImageWrite.stbi_write_jpg(
                            file.absolutePath,
                            effectiveWidth, effectiveHeight,
                            format.componentCount, pixels, 90)
                    FileFormat.PNG -> STBImageWrite.stbi_write_png(
                            file.absolutePath,
                            effectiveWidth, effectiveHeight,
                            format.componentCount, pixels, effectiveWidth * format.componentCount)
                }
            } else {
                TODO("support non-UINT8 types")
            }
        } else {
            throw IllegalArgumentException("multisample targets cannot be saved to file")
        }
    }

    override fun destroy() {
        glDeleteTextures(texture)
        checkGLErrors()
    }

    override fun bind(unit: Int) {
        if (multisample == DISABLED) {
            glActiveTexture(GL_TEXTURE0 + unit)
            glBindTexture(target, texture)
        } else {
            throw IllegalArgumentException("multisample targets cannot be bound as texture")
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

private fun WrapMode.glWrap(): Int {
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
        ColorType.UINT8 -> GL_UNSIGNED_BYTE
        ColorType.UINT16 -> GL_UNSIGNED_SHORT
        ColorType.FLOAT16 -> GL_HALF_FLOAT
        ColorType.FLOAT32 -> GL_FLOAT
        ColorType.DXT1, ColorType.DXT3, ColorType.DXT5 -> throw RuntimeException("gl type of compressed types cannot be queried")
    }
}