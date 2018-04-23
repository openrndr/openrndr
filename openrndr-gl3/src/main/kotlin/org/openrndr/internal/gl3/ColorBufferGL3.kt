package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.EXTTextureCompressionS3TC.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.*
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL14.GL_MIRRORED_REPEAT

import org.lwjgl.opengl.GL21.GL_SRGB8
import org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8
import org.lwjgl.opengl.GL30.*
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImageWrite
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.MinifyingFilter
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

import org.openrndr.draw.*
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
            ConversionEntry(ColorFormat.RG, ColorType.FLOAT16, GL_RG16F),
            ConversionEntry(ColorFormat.RG, ColorType.FLOAT32, GL_RG32F),
            ConversionEntry(ColorFormat.RGB, ColorType.UINT8, GL_RGB8),
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
        logger.debug {
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

    override fun write(x: Int, y: Int, color: ColorRGBa) {
        val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y
        val offset = (ay * colorBuffer.effectiveWidth + x) * colorBuffer.format.componentCount * colorBuffer.type.componentSize
        when (colorBuffer.type) {
            ColorType.UINT8 -> {
                val ir = (color.r * 255).coerceIn(0.0, 255.0).toByte()
                val ig = (color.g * 255).coerceIn(0.0, 255.0).toByte()
                val ib = (color.b * 255).coerceIn(0.0, 255.0).toByte()
                val ia = (color.a * 255).coerceIn(0.0, 255.0).toByte()
                buffer.put(offset, ir)
                buffer.put(offset + 1, ig)
                buffer.put(offset + 2, ib)
                if (colorBuffer.format.componentCount>3) {
                    buffer.put(offset + 3, ia)
                }
            }
            ColorType.UINT16 -> {
                val ir = (color.r * 65535).coerceIn(0.0, 65535.0).toChar()
                val ig = (color.g * 65535).coerceIn(0.0, 65535.0).toChar()
                val ib = (color.b * 65335).coerceIn(0.0, 65535.0).toChar()
                val ia = (color.a * 65535).coerceIn(0.0, 65535.0).toChar()
                buffer.putChar(offset, ir)
                buffer.putChar(offset + 2, ig)
                buffer.putChar(offset + 4, ib)
                if (colorBuffer.format.componentCount>3) {
                    buffer.putChar(offset + 6, ia)
                }
            }
            ColorType.FLOAT32 -> {
                buffer.putFloat(offset, color.r.toFloat())
                buffer.putFloat(offset + 4, color.g.toFloat())
                buffer.putFloat(offset + 8, color.b.toFloat())
                if (colorBuffer.format.componentCount>3) {
                    buffer.putFloat(offset + 12, color.a.toFloat())
                }
            }
            else -> TODO("support for ${colorBuffer.type}")
        }
    }

    override fun read(x: Int, y: Int): ColorRGBa {

        val ay = if (colorBuffer.flipV) y else colorBuffer.effectiveHeight - 1 - y

        val offset = (ay * colorBuffer.effectiveWidth + x) * colorBuffer.format.componentCount * colorBuffer.type.componentSize
        return when (colorBuffer.type) {
            ColorType.UINT8 -> {
                val ir = buffer.get(offset).toInt() and 0xff
                val ig = buffer.get(offset + 1).toInt() and 0xff
                val ib = buffer.get(offset + 2).toInt() and 0xff
                val ia = if (colorBuffer.format == ColorFormat.RGBa) (buffer.get(offset + 3).toInt() and 0xff).toInt() else 255
                ColorRGBa(ir / 255.0, ig / 255.0, ib / 255.0, ia / 255.0)
            }
            ColorType.FLOAT32 -> {
                val fr = buffer.getFloat(offset)
                val fg = buffer.getFloat(offset + 4)
                val fb = buffer.getFloat(offset + 8)
                val fa = if (colorBuffer.format == ColorFormat.RGBa) (buffer.getFloat(offset + 12)) else 1.0f
                ColorRGBa(fr.toDouble(), fg.toDouble(), fb.toDouble(), fa.toDouble())
            }

            else -> TODO("support for ${colorBuffer.type}")
        }
    }

    override fun writer(): BufferWriter {
        return BufferWriterGL3(buffer)
    }
}


class ColorBufferDataGL3(val width: Int, val height: Int, val format: ColorFormat, val type: ColorType, val data: ByteBuffer) {

    companion object {

        fun fromUrl(urlString: String): ColorBufferDataGL3 {
            val url = URL(urlString)
            url.openStream().use {


                val byteArray = url.readBytes()
                if (byteArray.size == 0) {
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
                STBImage.nstbi_set_unpremultiply_on_load(0)
                val data = STBImage.stbi_load_from_memory(buffer, wa, ha, ca, 0)

                //println("channel count ${ca[0]}")

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
            if (byteArray.size == 0) {
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
            STBImage.nstbi_set_unpremultiply_on_load(0)
            val data = STBImage.stbi_load_from_memory(buffer, wa, ha, ca, 0)

            //println("channel count ${ca[0]}")

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
                     override val type: ColorType) : ColorBuffer {


    internal var realFlipV: Boolean = false

    override var flipV: Boolean
        get() = realFlipV
        set(value) {
            realFlipV = value
        }

    companion object {

        fun fromUrl(url: String): ColorBuffer {
            val data = ColorBufferDataGL3.fromUrl(url)
            val cb = create(data.width, data.height, 1.0, data.format, data.type)
            return cb.apply {
                cb.write(data.data)
                cb.generateMipmaps()
            }
        }

        fun fromFile(filename: String): ColorBuffer {
            val data = ColorBufferDataGL3.fromFile(filename)
            val cb = create(data.width, data.height, 1.0, data.format, data.type)
            return cb.apply {
                cb.write(data.data)
                cb.generateMipmaps()
            }
        }

        fun create(width: Int, height: Int, contentScale: Double = 1.0, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.FLOAT32): ColorBufferGL3 {

            val internalFormat = internalFormat(format, type)

            if (width <= 0 || height <= 0) {
                throw Exception("cannot create ColorBuffer with dimensions: ${width}x$height")
            }

            checkGLErrors()


            val texture = glGenTextures()
            checkGLErrors()

            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, texture)
            checkGLErrors()

            val effectiveWidth = (width * contentScale).toInt()
            val effectiveHeight = (height * contentScale).toInt()

            val nullBB: ByteBuffer? = null
            glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, effectiveWidth, effectiveHeight, 0, format.glFormat(), type.glType(), nullBB)
            checkGLErrors {
                when (it) {
                    GL_INVALID_OPERATION -> """format is GL_DEPTH_COMPONENT ${format.glFormat() == GL_DEPTH_COMPONENT} and internalFormat is not GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT16, GL_DEPTH_COMPONENT24, or GL_DEPTH_COMPONENT32F"""
                    GL_INVALID_FRAMEBUFFER_OPERATION -> "buh?"
                    else -> null
                }
            }

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

            checkGLErrors()
            return ColorBufferGL3(GL_TEXTURE_2D, texture, width, height, contentScale, format, type)
        }
    }

    fun bound(f: ColorBufferGL3.() -> Unit) {
        glActiveTexture(GL_TEXTURE0)
        val current = glGetInteger(GL_TEXTURE_BINDING_2D)

        glBindTexture(target, texture)
        this.f()
        glBindTexture(target, current)
    }

    fun destroyShadow() {
        realShadow = null
    }

    override fun generateMipmaps() {
        bound {
            glGenerateMipmap(target)
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
            if (realShadow == null) {
                realShadow = ColorBufferShadowGL3(this)
            }
            return realShadow!!
        }

    var realShadow: ColorBufferShadow? = null

    override fun write(buffer: ByteBuffer) {
        bound {
            debugGLErrors()
            (buffer as Buffer).rewind()
            buffer.order(ByteOrder.nativeOrder())
            glTexSubImage2D(target, 0, 0, 0, width, height, format.glFormat(), type.glType(), buffer)
            debugGLErrors()
            (buffer as Buffer).rewind()
        }
    }

    override fun read(buffer: ByteBuffer) {
        bound {
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
    }

    override fun saveToFile(file: File) {
        if (type == ColorType.UINT8) {
            val pixels = BufferUtils.createByteBuffer(effectiveWidth * effectiveHeight * format.componentCount * 2)
            (pixels as Buffer).rewind()
            read(pixels)
            (pixels as Buffer).rewind()
//            STBImageWrite.stbi_flip_vertically_on_write(!flipV)
            STBImageWrite.stbi_write_png(file.absolutePath, effectiveWidth, effectiveHeight, format.componentCount, pixels, effectiveWidth * format.componentCount)
        } else {
            TODO("support non-UINT8 types")
        }
    }

    override fun destroy() {
        glDeleteTextures(texture)
        checkGLErrors()
    }

    override fun bind(unit: Int) {
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(target, texture)
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
