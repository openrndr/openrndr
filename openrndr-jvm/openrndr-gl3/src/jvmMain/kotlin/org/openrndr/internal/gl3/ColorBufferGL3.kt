package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.ARBTextureCompressionBPTC.*
import org.lwjgl.opengl.EXTTextureCompressionS3TC.*
import org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT
import org.lwjgl.opengl.EXTTextureSRGB.*
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.opengl.GL43C.glCopyImageSubData
import org.lwjgl.opengl.GL44.glClearTexImage
import org.lwjgl.opengl.GL45C.glGenerateTextureMipmap
import org.lwjgl.opengles.GLES30
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.tinyexr.TinyEXR.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.draw.BufferMultisample.Disabled
import org.openrndr.draw.BufferMultisample.SampleCount
import org.openrndr.filter.color.copy
import org.openrndr.internal.Driver
import org.openrndr.internal.ImageData
import org.openrndr.internal.ImageDriver
import org.openrndr.internal.ImageSaveConfiguration
import org.openrndr.shape.IntRectangle
import org.openrndr.utils.buffer.MPPBuffer
import java.io.File
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

internal data class ConversionEntry(val format: ColorFormat, val type: ColorType, val glFormat: Int, val glType: Int)

private val logger = KotlinLogging.logger {}

enum class TextureStorageModeGL {
    IMAGE,
    STORAGE
}

/**
 * Exposes TINYEXR_COMPRESSIONTYPE for convention
 */
class ExrCompression {
    companion object {
        val NONE = TINYEXR_COMPRESSIONTYPE_NONE
        val RLE = TINYEXR_COMPRESSIONTYPE_RLE
        val ZIPS = TINYEXR_COMPRESSIONTYPE_ZIPS
        val ZIP = TINYEXR_COMPRESSIONTYPE_ZIP
        val PIZ = TINYEXR_COMPRESSIONTYPE_PIZ

        // experimental, see https://tinyexr.docsforge.com/master/getting-started/#zfp
        val ZFP = TINYEXR_COMPRESSIONTYPE_ZFP
    }
}

internal fun internalFormat(format: ColorFormat, type: ColorType): Pair<Int, Int> {
    val entries = arrayOf(
        ConversionEntry(ColorFormat.R, ColorType.UINT8, GL_R8, GL_RED),
        ConversionEntry(ColorFormat.R, ColorType.UINT8_INT, GL_R8UI, GL_RED_INTEGER),
        ConversionEntry(ColorFormat.R, ColorType.SINT8_INT, GL_R8I, GL_RED_INTEGER),
        ConversionEntry(ColorFormat.R, ColorType.UINT16, GL_R16, GL_RED),
        ConversionEntry(ColorFormat.R, ColorType.UINT16_INT, GL_R16UI, GL_RED_INTEGER),
        ConversionEntry(ColorFormat.R, ColorType.SINT16_INT, GL_R16I, GL_RED_INTEGER),
        ConversionEntry(ColorFormat.R, ColorType.UINT32_INT, GL_R32UI, GL_RED_INTEGER),
        ConversionEntry(ColorFormat.R, ColorType.SINT32_INT, GL_R32I, GL_RED_INTEGER),

        ConversionEntry(ColorFormat.R, ColorType.FLOAT16, GL_R16F, GL_RED),
        ConversionEntry(ColorFormat.R, ColorType.FLOAT32, GL_R32F, GL_RED),

        ConversionEntry(ColorFormat.RG, ColorType.UINT8, GL_RG8, GL_RG),
        ConversionEntry(ColorFormat.RG, ColorType.UINT8_INT, GL_RG8UI, GL_RG_INTEGER),
        ConversionEntry(ColorFormat.RG, ColorType.SINT16_INT, GL_RG16I, GL_RG_INTEGER),
        ConversionEntry(ColorFormat.RG, ColorType.SINT32_INT, GL_RG32I, GL_RG_INTEGER),
        ConversionEntry(ColorFormat.RG, ColorType.UINT16, GL_RG16, GL_RG),
        ConversionEntry(ColorFormat.RG, ColorType.UINT16_INT, GL_RG16UI, GL_RG_INTEGER),
        ConversionEntry(ColorFormat.RG, ColorType.UINT32_INT, GL_RG32UI, GL_RG_INTEGER),

        ConversionEntry(ColorFormat.RG, ColorType.FLOAT16, GL_RG16F, GL_RG),
        ConversionEntry(ColorFormat.RG, ColorType.FLOAT32, GL_RG32F, GL_RG),

        ConversionEntry(ColorFormat.RGB, ColorType.UINT8, GL_RGB8, GL_RGB),
        ConversionEntry(ColorFormat.RGB, ColorType.UINT8_INT, GL_RGB8UI, GL_RGB_INTEGER),
        ConversionEntry(ColorFormat.RGB, ColorType.UINT16, GL_RGB16, GL_RGB),
        ConversionEntry(ColorFormat.RGB, ColorType.UINT16_INT, GL_RGB16UI, GL_RGB_INTEGER),
        ConversionEntry(ColorFormat.RGB, ColorType.UINT32_INT, GL_RGB32UI, GL_RGB_INTEGER),
        ConversionEntry(ColorFormat.RGB, ColorType.SINT16_INT, GL_RGB16I, GL_RGB_INTEGER),
        ConversionEntry(ColorFormat.RGB, ColorType.SINT32_INT, GL_RGB32I, GL_RGB_INTEGER),
        ConversionEntry(ColorFormat.RGB, ColorType.FLOAT16, GL_RGB16F, GL_RGB),
        ConversionEntry(ColorFormat.RGB, ColorType.FLOAT32, GL_RGB32F, GL_RGB),
        ConversionEntry(ColorFormat.BGR, ColorType.UINT8, GL_RGB8, GL_BGR),

        ConversionEntry(ColorFormat.RGBa, ColorType.UINT8, GL_RGBA8, GL_RGBA),
        ConversionEntry(ColorFormat.RGBa, ColorType.UINT8_INT, GL_RGBA8UI, GL_RGBA_INTEGER),
        ConversionEntry(ColorFormat.RGBa, ColorType.UINT16, GL_RGBA16, GL_RGBA),
        ConversionEntry(ColorFormat.RGBa, ColorType.UINT16_INT, GL_RGBA16UI, GL_RGBA_INTEGER),
        ConversionEntry(ColorFormat.RGBa, ColorType.SINT16_INT, GL_RGBA16I, GL_RGBA_INTEGER),
        ConversionEntry(ColorFormat.RGBa, ColorType.UINT32_INT, GL_RGBA32UI, GL_RGBA_INTEGER),
        ConversionEntry(ColorFormat.RGBa, ColorType.SINT32_INT, GL_RGBA32I, GL_RGBA_INTEGER),
        ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT16, GL_RGBA16F, GL_RGBA),
        ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT32, GL_RGBA32F, GL_RGBA),

        ConversionEntry(ColorFormat.RGB, ColorType.UINT8_SRGB, GL_SRGB8, GL_RGB),
        ConversionEntry(ColorFormat.RGBa, ColorType.UINT8_SRGB, GL_SRGB8_ALPHA8, GL_RGBA),
        ConversionEntry(ColorFormat.RGBa, ColorType.DXT1, GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, GL_RGBA),
        ConversionEntry(ColorFormat.RGBa, ColorType.DXT3, GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, GL_RGBA),
        ConversionEntry(ColorFormat.RGBa, ColorType.DXT5, GL_COMPRESSED_RGBA_S3TC_DXT5_EXT, GL_RGBA),
        ConversionEntry(ColorFormat.RGB, ColorType.DXT1, GL_COMPRESSED_RGB_S3TC_DXT1_EXT, GL_RGBA),
        ConversionEntry(ColorFormat.RGBa, ColorType.DXT1_SRGB, GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT, GL_RGBA),
        ConversionEntry(ColorFormat.RGBa, ColorType.DXT3_SRGB, GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT, GL_RGBA),
        ConversionEntry(ColorFormat.RGBa, ColorType.DXT5_SRGB, GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT, GL_RGBA),
        ConversionEntry(ColorFormat.RGB, ColorType.DXT1_SRGB, GL_COMPRESSED_SRGB_S3TC_DXT1_EXT, GL_RGBA),
        ConversionEntry(ColorFormat.RGBa, ColorType.BPTC_UNORM, GL_COMPRESSED_RGBA_BPTC_UNORM_ARB, GL_RGBA),
        ConversionEntry(ColorFormat.RGBa, ColorType.BPTC_UNORM_SRGB, GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM_ARB, GL_RGBA),
        ConversionEntry(ColorFormat.RGB, ColorType.BPTC_FLOAT, GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT_ARB, GL_RGBA),
        ConversionEntry(ColorFormat.RGB, ColorType.BPTC_UFLOAT, GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT_ARB, GL_RGBA)
    )
    for (entry in entries) {
        if (entry.format === format && entry.type === type) {
            return Pair(entry.glFormat, entry.glType)
        }
    }
    throw Exception("no conversion entry for $format/$type")
}

@Suppress("MemberVisibilityCanBePrivate")
class ColorBufferGL3(
    val target: Int,
    val texture: Int,
    override val width: Int,
    override val height: Int,
    override val contentScale: Double,
    override val format: ColorFormat,
    override val type: ColorType,
    override val levels: Int,
    override val multisample: BufferMultisample,
    override val session: Session?
) : ColorBuffer() {

    override fun close() {
        destroy()
    }

    var exrCompression = ExrCompression.NONE

    private var destroyed = false
    override var flipV: Boolean = false

    internal fun glFormat(): Int {
        return internalFormat(format, type).first
    }

    companion object {
        fun create(
            width: Int,
            height: Int,
            contentScale: Double = 1.0,
            format: ColorFormat = ColorFormat.RGBa,
            type: ColorType = ColorType.FLOAT32,
            multisample: BufferMultisample,
            levels: Int,
            session: Session?
        ): ColorBufferGL3 {
            if (type.compressed && multisample is SampleCount) {
                error("cannot create ColorBuffer that is both compressed and multi-sampled")
            }

            val (internalFormat, internalType) = internalFormat(format, type)
            if (width <= 0 || height <= 0) {
                throw Exception("cannot create ColorBuffer with dimensions: ${width}x$height")
            }
            checkGLErrors()

            val storageMode = when {
                Driver.capabilities.textureStorage -> TextureStorageModeGL.STORAGE
                else -> TextureStorageModeGL.IMAGE
            }

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

            when (storageMode) {
                TextureStorageModeGL.IMAGE -> {
                    for (level in 0 until levels) {
                        val div = 1 shl level
                        when (multisample) {
                            Disabled -> if (!type.compressed) {
                                glTexImage2D(
                                    GL_TEXTURE_2D,
                                    level,
                                    internalFormat,
                                    effectiveWidth / div,
                                    effectiveHeight / div,
                                    0,
                                    internalType,
                                    type.glType(),
                                    nullBB
                                )
                            } else {
                                glTexImage2D(
                                    GL_TEXTURE_2D,
                                    level,
                                    internalFormat,
                                    effectiveWidth,
                                    effectiveHeight,
                                    0,
                                    internalType,
                                    GL_UNSIGNED_BYTE,
                                    nullBB
                                )
                            }

                            is SampleCount -> glTexImage2DMultisample(
                                GL_TEXTURE_2D_MULTISAMPLE,
                                multisample.sampleCount,
                                internalFormat,
                                effectiveWidth / div,
                                effectiveHeight / div,
                                true
                            )
                        }
                    }
                }

                TextureStorageModeGL.STORAGE -> {
                    when (multisample) {
                        Disabled ->
                            glTexStorage2D(GL_TEXTURE_2D, levels, internalFormat, effectiveWidth, effectiveHeight)

                        is SampleCount -> glTexStorage2DMultisample(
                            GL_TEXTURE_2D_MULTISAMPLE,
                            multisample.sampleCount.coerceAtMost(glGetInteger(GL_MAX_COLOR_TEXTURE_SAMPLES)),
                            internalFormat,
                            effectiveWidth,
                            effectiveHeight,
                            true
                        )
                    }
                }
            }
            checkGLErrors {
                logger.error { "Texture creation failed. width: $width, height: $height, contentScale: $contentScale, format: $format, type: $type, multisample: $multisample" }
                when (it) {
                    GL_INVALID_OPERATION -> """format is GL_DEPTH_COMPONENT (${format.glFormat() == GL_DEPTH_COMPONENT}) and internalFormat is not GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT16, GL_DEPTH_COMPONENT24, or GL_DEPTH_COMPONENT32F"""
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

            return ColorBufferGL3(
                target,
                texture,
                width,
                height,
                contentScale,
                format,
                type,
                levels,
                multisample,
                session
            )
        }
    }

    fun <T> bound(f: ColorBufferGL3.() -> T): T {
        checkDestroyed()
        glActiveTexture(GL_TEXTURE0)
        debugGLErrors()
        val current = when (multisample) {
            Disabled -> glGetInteger(GL_TEXTURE_BINDING_2D)
            is SampleCount -> glGetInteger(GL_TEXTURE_BINDING_2D_MULTISAMPLE)
        }
        glBindTexture(target, texture)
        debugGLErrors()
        val t = this.f()
        glBindTexture(target, current)
        debugGLErrors()
        return t
    }

    fun destroyShadow() {
        realShadow = null
    }

    override fun generateMipmaps() {
        if (levels == 1) {
            return
        }

        if (Driver.glType == DriverTypeGL.GLES) {
            if (format.componentCount == 3) {
                return
            }
        }
        val useGenerateTextureMipmap =
            Driver.glType == DriverTypeGL.GL && Driver.glVersion >= DriverVersionGL.GL_VERSION_4_5

        checkDestroyed()
        if (multisample == Disabled) {
            if (useGenerateTextureMipmap) {
                glGenerateTextureMipmap(texture)
            } else {
                bound {
                    glGenerateMipmap(target)
                    debugGLErrors {
                        "failed to generate mipmap for $this"
                    }
                }
            }
        } else {
            throw IllegalArgumentException("generating Mipmaps for multisample targets is not possible")
        }
    }

    override fun copyTo(target: ColorBuffer, fromLevel: Int, toLevel: Int, filter: MagnifyingFilter) {
        val sourceRectangle = IntRectangle(
            0,
            0,
            this.effectiveWidth / (1 shl fromLevel),
            this.effectiveHeight / (1 shl fromLevel)
        )
        val targetRectangle = IntRectangle(
            0,
            0,
            sourceRectangle.width,
            sourceRectangle.height
        )
        copyTo(target, fromLevel, toLevel, sourceRectangle, targetRectangle, filter)
    }

    override fun copyTo(
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int,
        sourceRectangle: IntRectangle,
        targetRectangle: IntRectangle,
        filter: MagnifyingFilter
    ) {
        checkDestroyed()


        val fromDiv = 1 shl fromLevel
        val toDiv = 1 shl toLevel
        val refRectangle = IntRectangle(0, 0, effectiveWidth / fromDiv, effectiveHeight / fromDiv)

        val useTexSubImage =
            target.type.compressed || (refRectangle == sourceRectangle && refRectangle == targetRectangle && multisample == target.multisample)

        val useCopyFilter = Driver.glType == DriverTypeGL.GLES && (
                this.multisample is Disabled && target.multisample is SampleCount ||
                        type.isFloat != target.type.isFloat
                )

        if (useCopyFilter) {
            require(
                this.effectiveWidth == target.effectiveWidth && this.effectiveHeight == target.effectiveHeight &&
                        sourceRectangle.x == 0 && sourceRectangle.y == 0 && sourceRectangle == targetRectangle && sourceRectangle.width == effectiveWidth && sourceRectangle.height == effectiveHeight
            )

            copy.apply(this, target)
            return
        }


        if (!useTexSubImage) {
            val readTarget = renderTarget(
                width / fromDiv,
                height / fromDiv,
                contentScale,
                multisample = multisample
            ) {
                colorBuffer(this@ColorBufferGL3, fromLevel)
            } as RenderTargetGL3

            val writeTarget = renderTarget(
                target.width / toDiv,
                target.height / toDiv,
                target.contentScale,
                multisample = target.multisample
            ) {
                colorBuffer(target, toLevel)
            } as RenderTargetGL3

            writeTarget.bind()
            glBindFramebuffer(GL_READ_FRAMEBUFFER, readTarget.framebuffer)
            debugGLErrors()

            val ssx = sourceRectangle.x
            val ssy = sourceRectangle.y
            val sex = sourceRectangle.width + ssx
            val sey = sourceRectangle.height + ssy

            val tsx = targetRectangle.x
            val tsy = targetRectangle.y
            val tex = targetRectangle.width + tsx
            val tey = targetRectangle.height + tsy

            @Suppress("SpellCheckingInspection")
            fun sflip(y: Int): Int {
                return this.effectiveHeight / fromDiv - y
            }

            @Suppress("SpellCheckingInspection")
            fun tflip(y: Int): Int {
                return target.effectiveHeight / toDiv - y
            }

            glBlitFramebuffer(
                ssx,
                sflip(ssy),
                sex,
                sflip(sey),
                tsx,
                tflip(tsy),
                tex,
                tflip(tey),
                GL_COLOR_BUFFER_BIT,
                if (Driver.glType == DriverTypeGL.GLES) GL_NEAREST else filter.toGLFilter()
            )
            debugGLErrors {
                logger.error {
                    """failed to copy ${this@ColorBufferGL3} to $target, source rect: $sourceRectangle, target rect: $targetRectangle"""
                }
                null
            }
            writeTarget.unbind()

            writeTarget.detachColorAttachments()
            writeTarget.destroy()

            readTarget.detachColorAttachments()
            readTarget.destroy()
        } else {
            require(sourceRectangle == refRectangle && targetRectangle == refRectangle) {
                "cropped or scaled copyTo is not allowed with the selected color buffers: $this -> $target"
            }

            val useFrameBufferCopy =
                true // Driver.glVersion < DriverVersionGL.VERSION_4_3 || (type != target.type || format != target.format)

            if (useFrameBufferCopy) {
                checkDestroyed()
                val readTarget = renderTarget(width / fromDiv, height / fromDiv, contentScale) {
                    colorBuffer(this@ColorBufferGL3, fromLevel)
                } as RenderTargetGL3

                target as ColorBufferGL3
                readTarget.bind()
                glReadBuffer(GL_COLOR_ATTACHMENT0)
                debugGLErrors()
                target.bound {
                    glCopyTexSubImage2D(
                        target.target,
                        toLevel,
                        0,
                        0,
                        0,
                        0,
                        target.effectiveWidth / toDiv,
                        target.effectiveHeight / toDiv
                    )
                    debugGLErrors {
                        when (it) {
                            GL_INVALID_VALUE -> "level ($toLevel) less than 0, effective target is GL_TEXTURE_RECTANGLE (${target.target == GL_TEXTURE_RECTANGLE} and level is not 0"

                            else -> null
                        }
                    }
                }
                readTarget.unbind()

                readTarget.detachColorAttachments()
                readTarget.destroy()
            } else {
                target as ColorBufferGL3
                glCopyImageSubData(
                    texture,
                    this.target,
                    fromLevel,
                    0,
                    0,
                    0,
                    target.texture,
                    target.target,
                    toLevel,
                    0,
                    0,
                    0,
                    effectiveWidth,
                    effectiveHeight,
                    1
                )
                debugGLErrors()
            }
        }
    }

    override fun copyTo(target: ArrayTexture, layer: Int, fromLevel: Int, toLevel: Int) {
        debugGLErrors {
            "leaking error"
        }
        require(fromLevel < this.levels) { """requested to copy from mipmap level $fromLevel, but source color buffer has $levels mipmap levels.""" }
        require(toLevel < target.levels) { """requested to copy to mipmap level $toLevel, but target array texture only has $levels mipmap levels.""" }

        val useCopyFilter = Driver.glType == DriverTypeGL.GLES && (

                type.isFloat != target.type.isFloat
                )

        if (useCopyFilter) {
            require(
                this.effectiveWidth == target.width && this.effectiveHeight == target.height
            )

            val rt = renderTarget(target.width, target.height) {
                arrayTexture(target, layer)
            }
            copy.apply(arrayOf(this), rt)
            rt.destroy()
            return
        }


        if (!type.compressed) {
            checkDestroyed()
            val fromDiv = 1 shl fromLevel
            val toDiv = 1 shl toLevel
            val readTarget = renderTarget(width / fromDiv, height / fromDiv, contentScale) {
                colorBuffer(this@ColorBufferGL3, fromLevel)
            } as RenderTargetGL3
            debugGLErrors()

            target as ArrayTextureGL3
            readTarget.bind()
            glReadBuffer(GL_COLOR_ATTACHMENT0)
            debugGLErrors()

            target.bound {
                glCopyTexSubImage3D(
                    target.target,
                    toLevel,
                    0,
                    0,
                    layer,
                    0,
                    0,
                    target.width / toDiv,
                    target.height / toDiv
                )
                debugGLErrors {
                    when (it) {
                        GL_INVALID_FRAMEBUFFER_OPERATION -> "the object bound to GL_READ_FRAMEBUFFER_BINDING is not framebuffer complete."
                        else -> null
                    }
                }
            }
            readTarget.unbind()

            readTarget.detachColorAttachments()
            readTarget.destroy()
        } else {
            if (type == target.type && format == target.format) {
                if (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_3) {
                    val tgl = target as ArrayTextureGL3
                    val fromDiv = 1 shl fromLevel
                    glCopyImageSubData(
                        texture,
                        this.target,
                        fromLevel,
                        0,
                        0,
                        0,
                        tgl.texture,
                        tgl.target,
                        toLevel,
                        0,
                        0,
                        layer,
                        effectiveWidth / fromDiv,
                        effectiveHeight / fromDiv,
                        1
                    )
                    debugGLErrors()
                } else {
                    val copyBuffer = MemoryUtil.memAlloc(bufferSize(fromLevel).toInt())
                    try {
                        read(copyBuffer, level = fromLevel)
                        copyBuffer.rewind()
                        target.write(layer = layer, copyBuffer, level = toLevel)
                        copyBuffer.rewind()
                    } finally {
                        MemoryUtil.memFree(copyBuffer)
                    }
                }
            } else {
                error("can't copy from compressed source ${format}/${type} to compressed target ${target.format}/${target.type}")
            }
        }
    }

    override fun fill(color: ColorRGBa, level: Int) {
        checkDestroyed()
        require(level < levels)
        @Suppress("SpellCheckingInspection") val lwidth = (width / 2.0.pow(level.toDouble())).toInt()
        @Suppress("SpellCheckingInspection") val lheight = (height / 2.0.pow(level.toDouble())).toInt()
        @Suppress("SpellCheckingInspection") val lcolor = color.toLinear()

        val floatColorData = floatArrayOf(
            lcolor.r.toFloat(),
            lcolor.g.toFloat(),
            lcolor.b.toFloat(),
            lcolor.alpha.toFloat()
        )
        when {
            (Driver.glVersion < DriverVersionGL.GL_VERSION_4_4 || Driver.glType == DriverTypeGL.GLES) -> {

                val writeTarget = renderTarget(lwidth, lheight, contentScale) {
                    colorBuffer(this@ColorBufferGL3, level)
                } as RenderTargetGL3

                writeTarget.bind()
                glClearBufferfv(GL_COLOR, 0, floatColorData)
                debugGLErrors()
                writeTarget.unbind()

                writeTarget.detachColorAttachments()
                writeTarget.destroy()
            }

            else -> {
                glClearTexImage(
                    texture,
                    level,
                    ColorFormat.RGBa.glFormat(),
                    ColorType.FLOAT32.glType(),
                    floatColorData
                )
                debugGLErrors()
            }
        }
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
        get() {
            return bound {
                glGetTexParameterf(target, GL_TEXTURE_MAX_ANISOTROPY_EXT).toDouble()
            }
        }
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

    override fun filter(filterMin: MinifyingFilter, filterMag: MagnifyingFilter) {
        this.filterMin = filterMin
        this.filterMag = filterMag
    }

    var realShadow: ColorBufferShadow? = null

    override fun write(sourceBuffer: ByteBuffer, sourceFormat: ColorFormat, sourceType: ColorType, level: Int) {

        require(sourceBuffer.remaining() > 0) {
            "sourceBuffer $sourceBuffer has no remaining data"
        }

        val div = 1 shl level
        checkDestroyed()
        if (!sourceBuffer.isDirect) {
            throw IllegalArgumentException("buffer is not a direct buffer.")
        }
        if (!sourceType.compressed) {
            val bytesNeeded =
                sourceFormat.componentCount * sourceType.componentSize * (effectiveWidth / div) * (effectiveHeight / div)
            require(bytesNeeded <= sourceBuffer.remaining()) {
                "write requires $bytesNeeded bytes, buffer only has ${sourceBuffer.remaining()} bytes left, buffer capacity is ${sourceBuffer.capacity()}"
            }
        }

        if (multisample == Disabled) {
            bound {
                debugGLErrors()
                logger.trace {
                    "Writing to color buffer in: $format ${format.glFormat()}, $type ${type.glType()}"
                }
                sourceBuffer.order(ByteOrder.nativeOrder())
                val currentPack = intArrayOf(0)
                glGetIntegerv(GL_UNPACK_ALIGNMENT, currentPack)
                glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

                if (sourceType.compressed) {
                    glCompressedTexSubImage2D(
                        target,
                        level,
                        0,
                        0,
                        width / div,
                        height / div,
                        compressedType(sourceFormat, sourceType),
                        sourceBuffer
                    )
                    debugGLErrors {
                        when (it) {
                            GL_INVALID_VALUE -> "data size mismatch? ${sourceBuffer.remaining()}"
                            else -> null
                        }
                    }
                } else {
                    val internalType = internalFormat(sourceFormat, sourceType).second
                    glTexSubImage2D(
                        target,
                        level,
                        0,
                        0,
                        width / div,
                        height / div,
                        internalType,
                        sourceType.glType(),
                        sourceBuffer
                    )
                    debugGLErrors()
                }
                glPixelStorei(GL_UNPACK_ALIGNMENT, currentPack[0])
                debugGLErrors()
            }
        } else {
            throw IllegalArgumentException("multisample targets cannot be written to")
        }
    }

    override fun write(
        sourceBuffer: MPPBuffer,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    ) {
        write(sourceBuffer.byteBuffer, sourceFormat, sourceType, level = level)
    }


    override fun read(targetBuffer: ByteBuffer, targetFormat: ColorFormat, targetType: ColorType, level: Int) {
        debugGLErrors()
        checkDestroyed()
        if (!targetBuffer.isDirect) {
            throw IllegalArgumentException("buffer is not a direct buffer.")
        }
        require(multisample == Disabled) { "can not read from multi-sampled color buffers" }
        when (Driver.glType) {
            DriverTypeGL.GL -> {

                bound {
                    logger.trace {
                        "Reading from color buffer in: $format ${format.glFormat()}, $type ${type.glType()} "
                    }
                    debugGLErrors()
                    glPixelStorei(GL_PACK_ALIGNMENT, 1)
                    debugGLErrors()
                    val packAlignment = glGetInteger(GL_PACK_ALIGNMENT)
                    targetBuffer.order(ByteOrder.nativeOrder())
                    (targetBuffer as Buffer).rewind()
                    if (!targetType.compressed) {
                        val internalType = internalFormat(targetFormat, targetType).second
                        glGetTexImage(target, level, internalType, targetType.glType(), targetBuffer)
                    } else {
                        require(targetType == type && targetFormat == format) {
                            "source format/type (${format}/${type}) and target format/type ${targetFormat}/${targetType}) must match"
                        }
                        glGetCompressedTexImage(target, level, targetBuffer)
                    }
                    debugGLErrors()
                    (targetBuffer as Buffer).rewind()
                    glPixelStorei(GL_PACK_ALIGNMENT, packAlignment)
                    debugGLErrors()
                }
            }

            DriverTypeGL.GLES -> {
                require(targetFormat.componentCount != 3) {
                    "Reading from ColorBuffers with 3-component format (=$format/$type) is not supported in GLES. "
                }
                val fb = glGenFramebuffers()
                checkGLErrors()
                val currentFB = glGetInteger(GL_FRAMEBUFFER_BINDING)
                debugGLErrors()
                glBindFramebuffer(GL_FRAMEBUFFER, fb)
                debugGLErrors()
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, target, texture, level)
                debugGLErrors()
                val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
                require(status == GL_FRAMEBUFFER_COMPLETE) {
                    when (status) {
                        GL_FRAMEBUFFER_UNDEFINED -> "target is the default framebuffer, but the default framebuffer does not exist."
                        GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "any of the framebuffer attachment points are framebuffer incomplete."
                        GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "the framebuffer does not have at least one image attached to it."
                        GL_FRAMEBUFFER_UNSUPPORTED -> "depth and stencil attachments, if present, are not the same renderbuffer, or if the combination of internal formats of the attached images violates an implementation-dependent set of restrictions."
                        GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "he value of GL_RENDERBUFFER_SAMPLES is not the same for all attached renderbuffers or, if the attached images are a mix of renderbuffers and textures, the value of GL_RENDERBUFFER_SAMPLES is not zero."
                        else -> "unknown error $status"
                    }
                }
                glPixelStorei(GL_PACK_ALIGNMENT, 1)
                debugGLErrors()
                GLES30.glReadBuffer(GL_COLOR_ATTACHMENT0)
                debugGLErrors()
                GLES30.glReadPixels(
                    0,
                    0,
                    effectiveWidth,
                    effectiveHeight,
                    format.glFormat(),
                    type.glType(),
                    targetBuffer
                )
                debugGLErrors()
                glDeleteFramebuffers(fb)
                debugGLErrors()
                glBindFramebuffer(GL_FRAMEBUFFER, currentFB)
                debugGLErrors()
            }
        }
    }

    fun toImageData(): ImageData {
        val buffer = MemoryUtil.memAlloc(effectiveWidth * effectiveHeight * format.componentCount * type.componentSize)
        read(buffer)
        val data = ImageDataStb(effectiveWidth, effectiveHeight, format, type, flipV, MPPBuffer(buffer))
        return data
    }

    override fun saveToFile(file: File, imageFileFormat: ImageFileFormat, async: Boolean) {
        checkDestroyed()
        require(multisample == Disabled)
        toImageData().use { data ->
            ImageDriver.instance.saveImage(data, file.absolutePath, imageFileFormat)
        }
    }

    override fun saveToFile(file: File, async: Boolean, configuration: ImageSaveConfiguration) {
        checkDestroyed()
        require(multisample == Disabled)
        toImageData().use { data ->
            ImageDriver.instance.saveImage(data, file.absolutePath, configuration)
        }
    }

    override fun toDataUrl(imageFileFormat: ImageFileFormat): String {
        checkDestroyed()
        require(multisample == Disabled)
        require(type == ColorType.UINT8 || type == ColorType.UINT8_SRGB)

        return toImageData().use { data ->
            ImageDriver.instance.imageToDataUrl(data, imageFileFormat)
        }
    }

    override fun destroy() {
        if (!destroyed) {
            debugGLErrors { "pre-existing errors before destroying colorbuffer" }
            session?.untrack(this)
            glDeleteTextures(texture)
            destroyed = true
            checkGLErrors()
        }
    }

    override fun bind(unit: Int) {
        checkDestroyed()
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(target, texture)
    }

    private fun checkDestroyed() {
        if (destroyed) {
            throw IllegalStateException("color buffer is destroyed")
        }
    }

    override fun toString(): String {
        return "ColorBufferGL3(target=$target, texture=$texture, width=$width, height=$height, contentScale=$contentScale, format=$format, type=$type, levels=$levels, multisample=$multisample)"
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
        ColorFormat.BGR -> GL_BGR
        ColorFormat.BGRa -> GL_BGRA
    }
}

internal fun ColorType.glType(): Int {
    return when (this) {
        ColorType.UINT8, ColorType.UINT8_SRGB, ColorType.UINT8_INT -> GL_UNSIGNED_BYTE
        ColorType.SINT8_INT -> GL_BYTE
        ColorType.UINT16, ColorType.UINT16_INT -> GL_UNSIGNED_SHORT
        ColorType.SINT16_INT -> GL_SHORT
        ColorType.UINT32_INT -> GL_UNSIGNED_INT
        ColorType.SINT32_INT -> GL_INT
        ColorType.FLOAT16 -> GL_HALF_FLOAT
        ColorType.FLOAT32 -> GL_FLOAT
        ColorType.DXT1, ColorType.DXT3, ColorType.DXT5,
        ColorType.DXT1_SRGB, ColorType.DXT3_SRGB, ColorType.DXT5_SRGB,
        ColorType.BPTC_UNORM_SRGB,
        ColorType.BPTC_UNORM, ColorType.BPTC_FLOAT, ColorType.BPTC_UFLOAT -> throw RuntimeException("gl type of compressed types cannot be queried")
    }
}


internal fun compressedType(format: ColorFormat, type: ColorType): Int {
    when (format) {
        ColorFormat.RGBa -> return when (type) {
            ColorType.DXT1 -> GL_COMPRESSED_RGBA_S3TC_DXT1_EXT
            ColorType.DXT3 -> GL_COMPRESSED_RGBA_S3TC_DXT3_EXT
            ColorType.DXT5 -> GL_COMPRESSED_RGBA_S3TC_DXT5_EXT
            ColorType.DXT1_SRGB -> GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT
            ColorType.DXT3_SRGB -> GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT
            ColorType.DXT5_SRGB -> GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT
            ColorType.BPTC_UNORM -> GL_COMPRESSED_RGBA_BPTC_UNORM_ARB
            ColorType.BPTC_UNORM_SRGB -> GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM_ARB
            else -> throw IllegalArgumentException()
        }

        ColorFormat.RGB -> return when (type) {
            ColorType.DXT1 -> GL_COMPRESSED_RGB_S3TC_DXT1_EXT
            ColorType.DXT1_SRGB -> GL_COMPRESSED_SRGB_S3TC_DXT1_EXT
            else -> throw IllegalArgumentException()
        }

        else -> throw IllegalArgumentException()
    }
}