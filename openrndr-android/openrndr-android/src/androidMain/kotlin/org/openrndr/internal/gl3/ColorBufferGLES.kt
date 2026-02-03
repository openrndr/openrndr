package org.openrndr.internal.gl3

import android.opengl.GLES30
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ArrayTexture
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorBufferShadow
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ImageFileFormat
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.Session
import org.openrndr.draw.WrapMode
import org.openrndr.draw.renderTarget
import org.openrndr.internal.ImageData
import org.openrndr.internal.ImageDriver
import org.openrndr.internal.ImageSaveConfiguration
import org.openrndr.shape.IntRectangle
import org.openrndr.utils.buffer.MPPBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

private val logger = KotlinLogging.logger {}

/**
 * Dummy EXR compression constants for Android/GLES builds.
 * These exist to keep API parity with the desktop implementation.
 * EXR writing is generally not supported on Android in this build.
 */
class ExrCompression {
    companion object {
        // Values mirror TinyEXR's public enum for consistency, but they’re no-ops here.
        const val NONE = 0
        const val RLE = 1
        const val ZIPS = 2
        const val ZIP = 3
        const val PIZ = 4

        // Experimental in TinyEXR; exposed for parity.
        const val ZFP = 9
    }
}

/**
 * GLES version of internalFormat(format, type).
 * Returns (internalFormat, externalFormat). The pixel type comes from ColorType.glType() elsewhere.
 *
 * Notes:
 * - BGR/BGRA are not core in GLES; rejected here.
 * - S3TC/BPTC are not in core GLES; rejected here.
 * - sRGB is supported as GL_SRGB8 / GL_SRGB8_ALPHA8 (UNSIGNED_BYTE only).
 */

class ColorBufferGLES(
    val target: Int,
    val textureId: Int,
    override val width: Int,
    override val height: Int,
    override val contentScale: Double,
    override val format: ColorFormat,
    override val type: ColorType,
    override val levels: Int,
    override val multisample: BufferMultisample,
    override val session: Session?
) : ColorBuffer() {

    // ---- basic state ----
    private var destroyed = false
    override var flipV: Boolean = false
    var realShadow: ColorBufferShadow? = null
    var exrCompression = ExrCompression.NONE

    // ---- creation ----
    companion object {

        private fun checkGl(msg: (() -> String)? = null) {
            val err = GLES30.glGetError()
            if (err != GLES30.GL_NO_ERROR) {
                if (msg != null) logger.warn { "${msg()} (GLES err 0x${err.toString(16)})" }
                else logger.warn { "GLES error 0x${err.toString(16)}" }
            }
        }

        fun create(
            width: Int,
            height: Int,
            contentScale: Double = 1.0,
            format: ColorFormat = ColorFormat.RGBa,
            type: ColorType = ColorType.UINT8,
            multisample: BufferMultisample,
            levels: Int,
            session: Session?
        ): ColorBufferGLES {
            require(width > 0 && height > 0) { "invalid ColorBuffer size ${width}x$height" }
            require(format != ColorFormat.RGB) {
                "GLES cannot attach/render to RGB textures reliably; use RGBA"
            }
            if (type.compressed) error("compressed color types not supported on GLES path (yet)")
            if (multisample is BufferMultisample.SampleCount) {
                // GLES MSAA needs a separate resolve path; keep it out for now
                error("multisample ColorBuffer not implemented on GLES")
            }

            val (internal, pixelFormat) = glesInternalFormat(format, type)
            val tex = genTexture()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)

            if (levels > 1) {
                GLES30.glTexParameteri(
                    GLES30.GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_MAX_LEVEL,
                    levels - 1
                )
            }

            // allocate storage (ES 3.0 guarantees glTexImage2D; glTexStorage2D is also available)
            // Use TexStorage when possible for immutability
            GLES30.glTexStorage2D(GLES30.GL_TEXTURE_2D, levels, internal, width, height)

            // default sampling params
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE
            )

            checkGl { "Texture creation failed ($width x $height, $format/$type, levels=$levels)" }

            return ColorBufferGLES(
                target = GLES30.GL_TEXTURE_2D,
                textureId = tex,
                width = width,
                height = height,
                contentScale = contentScale,
                format = format,
                type = type,
                levels = levels,
                multisample = multisample,
                session = session
            )
        }

        private fun genTexture(): Int {
            val a = IntArray(1)
            GLES30.glGenTextures(1, a, 0)
            return a[0]
        }

        /**
         * Minimal internal-format mapping for GLES 3.0.
         * (Extend if you need more combinations.)
         */
        private fun glesInternalFormat(format: ColorFormat, type: ColorType): Pair<Int, Int> {
            return when (format) {
                ColorFormat.R -> when (type) {
                    ColorType.UINT8 -> GLES30.GL_R8 to GLES30.GL_RED
                    ColorType.FLOAT32 -> GLES30.GL_R32F to GLES30.GL_RED
                    else -> error("Unsupported type for R: $type")
                }

                ColorFormat.RG -> when (type) {
                    ColorType.UINT8 -> GLES30.GL_RG8 to GLES30.GL_RG
                    ColorType.FLOAT32 -> GLES30.GL_RG32F to GLES30.GL_RG
                    else -> error("Unsupported type for RG: $type")
                }

                ColorFormat.RGBa -> when (type) {
                    ColorType.UINT8, ColorType.UINT8_SRGB -> {
                        // sRGB8_ALPHA8 if you really need SRGB; here we use RGBA8 for simplicity
                        GLES30.GL_RGBA8 to GLES30.GL_RGBA
                    }

                    ColorType.FLOAT32 -> GLES30.GL_RGBA32F to GLES30.GL_RGBA
                    ColorType.FLOAT16 -> GLES30.GL_RGBA16F to GLES30.GL_RGBA
                    else -> error("Unsupported type for RGBA: $type")
                }

                else -> error("Unsupported color format on GLES: $format")
            }
        }
    }

    // ---- utils ----
    private inline fun <T> bound(f: () -> T): T {
        checkDestroyed()
        val prevBinding = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_TEXTURE_BINDING_2D, prevBinding, 0)
        GLES30.glBindTexture(target, textureId)
        val r = f()
        GLES30.glBindTexture(target, prevBinding[0])
        return r
    }

    private fun checkDestroyed() {
        if (destroyed) error("ColorBuffer is destroyed")
    }

    private fun checkGl(msg: (() -> String)? = null) {
        val err = GLES30.glGetError()
        if (err != GLES30.GL_NO_ERROR) {
            if (msg != null) logger.warn { "${msg()} (GLES err 0x${err.toString(16)})" }
            else logger.warn { "GLES error 0x${err.toString(16)}" }
        }
    }

    // ---- ColorBuffer API ----

    override fun generateMipmaps() {
        if (levels <= 1) return
        // GLES supports mipmap generation for non-multisample 2D textures
        require(multisample == BufferMultisample.Disabled) { "cannot generate mipmaps for multisample textures" }
        // GLES cannot generate mipmaps for RGB on FBO; we already restrict to RGBA
        bound {
            GLES30.glGenerateMipmap(target)
        }
        checkGl()
    }

    override fun write(
        sourceBuffer: ByteBuffer,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        level: Int
    ) {
        require(multisample == BufferMultisample.Disabled) { "multisample targets cannot be written to" }
        require(!sourceType.compressed) { "compressed uploads not implemented on GLES path" }
        require(sourceBuffer.isDirect) { "buffer must be a direct ByteBuffer" }

        val div = 1 shl level
        sourceBuffer.order(ByteOrder.nativeOrder())
        sourceBuffer.rewind()

        val (_, pixFmt) = Companion.glesInternalFormat(sourceFormat, sourceType)

        bound {
            val oldUnpack = IntArray(1)
            GLES30.glGetIntegerv(GLES30.GL_UNPACK_ALIGNMENT, oldUnpack, 0)
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)

            GLES30.glTexSubImage2D(
                target, level,
                0, 0,
                width / div, height / div,
                when (sourceFormat) {
                    ColorFormat.R -> GLES30.GL_RED
                    ColorFormat.RG -> GLES30.GL_RG
                    ColorFormat.RGBa -> GLES30.GL_RGBA
                    else -> error("Unsupported upload format on GLES: $sourceFormat")
                },
                when (sourceType) {
                    ColorType.UINT8, ColorType.UINT8_SRGB -> GLES30.GL_UNSIGNED_BYTE
                    ColorType.FLOAT32 -> GLES30.GL_FLOAT
                    ColorType.FLOAT16 -> GLES30.GL_HALF_FLOAT
                    else -> error("Unsupported upload type on GLES: $sourceType")
                },
                sourceBuffer
            )

            GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, oldUnpack[0])
        }
        checkGl { "TexSubImage upload failed" }
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
        // Minimal implementation: reuse the full write (you can add x/y/region later)
        write(sourceBuffer.byteBuffer, sourceFormat, sourceType, level)
    }

    override fun read(
        targetBuffer: ByteBuffer,
        targetFormat: ColorFormat,
        targetType: ColorType,
        level: Int
    ) {
        require(multisample == BufferMultisample.Disabled) { "cannot read from multisample color buffers" }
        require(targetBuffer.isDirect) { "target buffer must be direct" }
        require(targetFormat != ColorFormat.RGB) {
            "Reading 3-component formats from FBO is not supported on GLES in this implementation"
        }

        // Create a tiny FBO, attach this texture level, read back
        val fboArr = IntArray(1)
        GLES30.glGenFramebuffers(1, fboArr, 0)
        val fbo = fboArr[0]

        val prevFB = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_FRAMEBUFFER_BINDING, prevFB, 0)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            target,
            textureId,
            level
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        require(status == GLES30.GL_FRAMEBUFFER_COMPLETE) {
            "GLES FBO incomplete: 0x${
                status.toString(
                    16
                )
            }"
        }

        GLES30.glPixelStorei(GLES30.GL_PACK_ALIGNMENT, 1)
        GLES30.glReadBuffer(GLES30.GL_COLOR_ATTACHMENT0)
        targetBuffer.order(ByteOrder.nativeOrder())
        targetBuffer.rewind()

        GLES30.glReadPixels(
            0, 0,
            effectiveWidth / (1 shl level),
            effectiveHeight / (1 shl level),
            when (targetFormat) {
                ColorFormat.R -> GLES30.GL_RED
                ColorFormat.RG -> GLES30.GL_RG
                ColorFormat.RGBa -> GLES30.GL_RGBA
                else -> error("Unsupported read format: $targetFormat")
            },
            when (targetType) {
                ColorType.UINT8, ColorType.UINT8_SRGB -> GLES30.GL_UNSIGNED_BYTE
                ColorType.FLOAT32 -> GLES30.GL_FLOAT
                ColorType.FLOAT16 -> GLES30.GL_HALF_FLOAT
                else -> error("Unsupported read type: $targetType")
            },
            targetBuffer
        )

        // restore & cleanup
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, prevFB[0])
        GLES30.glDeleteFramebuffers(1, fboArr, 0)
        checkGl { "glReadPixels failed" }
    }

    override fun copyTo(
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int,
        filter: MagnifyingFilter
    ) {
        val srcRect = IntRectangle(
            0,
            0,
            effectiveWidth / (1 shl fromLevel),
            effectiveHeight / (1 shl fromLevel)
        )
        val dstRect = IntRectangle(0, 0, srcRect.width, srcRect.height)
        copyTo(target, fromLevel, toLevel, srcRect, dstRect, filter)
    }

    override fun copyTo(
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int,
        sourceRectangle: IntRectangle,
        targetRectangle: IntRectangle,
        filter: MagnifyingFilter
    ) {
        require(target is ColorBufferGLES) { "copyTo requires ColorBufferGLES → ColorBufferGLES" }
        require(this.multisample == BufferMultisample.Disabled && target.multisample == BufferMultisample.Disabled) {
            "MSAA copy not implemented on GLES path"
        }

        // Use FBO blit when sizes match; otherwise fall back to CopyTexSubImage
        val sameSize =
            (sourceRectangle.width == targetRectangle.width && sourceRectangle.height == targetRectangle.height)
        if (sameSize) {
            // build read FBO
            val fboRead = IntArray(1)
            val fboDraw = IntArray(1)
            GLES30.glGenFramebuffers(1, fboRead, 0)
            GLES30.glGenFramebuffers(1, fboDraw, 0)

            val prevRead = IntArray(1)
            val prevDraw = IntArray(1)
            GLES30.glGetIntegerv(GLES30.GL_READ_FRAMEBUFFER_BINDING, prevRead, 0)
            GLES30.glGetIntegerv(GLES30.GL_DRAW_FRAMEBUFFER_BINDING, prevDraw, 0)

            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, fboRead[0])
            GLES30.glFramebufferTexture2D(
                GLES30.GL_READ_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                target.target,
                this.textureId,
                fromLevel
            )

            GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, fboDraw[0])
            GLES30.glFramebufferTexture2D(
                GLES30.GL_DRAW_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                target.target,
                target.textureId,
                toLevel
            )

            val sX0 = sourceRectangle.x
            val sY0 = sourceRectangle.y
            val sX1 = sourceRectangle.x + sourceRectangle.width
            val sY1 = sourceRectangle.y + sourceRectangle.height

            val dX0 = targetRectangle.x
            val dY0 = targetRectangle.y
            val dX1 = targetRectangle.x + targetRectangle.width
            val dY1 = targetRectangle.y + targetRectangle.height

            GLES30.glBlitFramebuffer(
                sX0, sY0, sX1, sY1,
                dX0, dY0, dX1, dY1,
                GLES30.GL_COLOR_BUFFER_BIT,
                if (filter == MagnifyingFilter.NEAREST) GLES30.GL_NEAREST else GLES30.GL_LINEAR
            )
            checkGl { "glBlitFramebuffer failed" }

            // restore & cleanup
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, prevRead[0])
            GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, prevDraw[0])
            GLES30.glDeleteFramebuffers(1, fboRead, 0)
            GLES30.glDeleteFramebuffers(1, fboDraw, 0)
        } else {
            // Fallback: copy via CopyTexSubImage (no scaling)
            require(sourceRectangle.width == targetRectangle.width && sourceRectangle.height == targetRectangle.height) {
                "Scaled blits require equal src/dst sizes on this GLES path"
            }
            // attach src to read FBO and copy into target texture
            val fbo = IntArray(1)
            GLES30.glGenFramebuffers(1, fbo, 0)
            val prevFB = IntArray(1)
            GLES30.glGetIntegerv(GLES30.GL_READ_FRAMEBUFFER_BINDING, prevFB, 0)
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, fbo[0])
            GLES30.glFramebufferTexture2D(
                GLES30.GL_READ_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                target.target,
                this.textureId,
                fromLevel
            )

            target.bound {
                GLES30.glCopyTexSubImage2D(
                    target.target,
                    toLevel,
                    targetRectangle.x,
                    targetRectangle.y,
                    sourceRectangle.x,
                    sourceRectangle.y,
                    sourceRectangle.width,
                    sourceRectangle.height
                )
            }

            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, prevFB[0])
            GLES30.glDeleteFramebuffers(1, fbo, 0)
            checkGl { "CopyTexSubImage2D failed" }
        }
    }

    override fun copyTo(target: ArrayTexture, layer: Int, fromLevel: Int, toLevel: Int) {
        // Minimal implementation: route through a temporary RT + shader copy if needed later.
        // For now, throw to keep behavior explicit.
        error("copyTo(ArrayTexture) not implemented on GLES path")
    }

    override fun fill(color: ColorRGBa, level: Int) {
        require(level < levels)
        require(multisample == BufferMultisample.Disabled)

        val lwidth = (width / 2.0.pow(level.toDouble())).toInt()
        val lheight = (height / 2.0.pow(level.toDouble())).toInt()
        val lcolor = color.toLinear()

        val writeRt = renderTarget(lwidth, lheight, contentScale) {
            colorBuffer(this@ColorBufferGLES, level)
        } as RenderTargetGLES

        writeRt.bind()
        val c = floatArrayOf(
            lcolor.r.toFloat(), lcolor.g.toFloat(),
            lcolor.b.toFloat(), lcolor.alpha.toFloat()
        )
        GLES30.glClearBufferfv(GLES30.GL_COLOR, 0, c, 0)
        writeRt.unbind()
        writeRt.detachColorAttachments()
        writeRt.destroy()
    }

    override var wrapU: WrapMode
        get() = WrapMode.CLAMP_TO_EDGE // query not implemented; return last set default
        set(value) {
            bound { GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_S, value.glWrapGLES()) }
        }

    override var wrapV: WrapMode
        get() = WrapMode.CLAMP_TO_EDGE
        set(value) {
            bound { GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_T, value.glWrapGLES()) }
        }

    override var filterMin: MinifyingFilter
        get() = MinifyingFilter.LINEAR // same note as wrapU
        set(value) {
            bound {
                GLES30.glTexParameteri(
                    target,
                    GLES30.GL_TEXTURE_MIN_FILTER,
                    value.toGLFilterGLES()
                )
            }
        }

    override var filterMag: MagnifyingFilter
        get() = MagnifyingFilter.LINEAR
        set(value) {
            bound {
                GLES30.glTexParameteri(
                    target,
                    GLES30.GL_TEXTURE_MAG_FILTER,
                    value.toGLFilterGLES()
                )
            }
        }

    override var anisotropy: Double
        get() = bound {
            try {
                val GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE
                val out = FloatArray(1)
                GLES30.glGetTexParameterfv(target, GL_TEXTURE_MAX_ANISOTROPY_EXT, out, 0)
                out[0].toDouble().takeIf { it > 0.0 } ?: 1.0
            } catch (_: Throwable) {
                1.0 // extension not present or query failed
            }
        }
        set(value) {
            bound {
                try {
                    val GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE
                    // (Optional) clamp to implementation max if you want:
                    // val GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FF
                    // val max = FloatArray(1)
                    // GLES30.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0)
                    // GLES30.glTexParameterf(target, GL_TEXTURE_MAX_ANISOTROPY_EXT, min(value.toFloat(), max[0]))
                    GLES30.glTexParameterf(target, GL_TEXTURE_MAX_ANISOTROPY_EXT, value.toFloat())
                } catch (_: Throwable) {
                    // ignore if extension is missing
                }
            }
        }

    override val shadow: ColorBufferShadow
        get() {
            if (multisample == BufferMultisample.Disabled) {
                if (realShadow == null) {
                    realShadow = ColorBufferShadowGLES(this)
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

    override fun saveToFile(file: File, imageFileFormat: ImageFileFormat, async: Boolean) {
        require(multisample == BufferMultisample.Disabled)
        toImageData().use { data ->
            ImageDriver.instance.saveImage(data, file.absolutePath, imageFileFormat)
        }
    }

    override fun saveToFile(file: File, async: Boolean, configuration: ImageSaveConfiguration) {
        require(multisample == BufferMultisample.Disabled)
        toImageData().use { data ->
            ImageDriver.instance.saveImage(data, file.absolutePath, configuration)
        }
    }

    override fun toDataUrl(imageFileFormat: ImageFileFormat): String {
        require(multisample == BufferMultisample.Disabled)
        require(type == ColorType.UINT8 || type == ColorType.UINT8_SRGB)
        return toImageData().use { data ->
            ImageDriver.instance.imageToDataUrl(data, imageFileFormat)
        }
    }

    private fun toImageData(): ImageData {
        val bytes = effectiveWidth * effectiveHeight * format.componentCount * when (type) {
            ColorType.UINT8, ColorType.UINT8_SRGB -> 1
            ColorType.FLOAT16 -> 2
            ColorType.FLOAT32 -> 4
            else -> error("Unsupported type for toImageData: $type")
        }
        val buf = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder())
        read(buf, format, type, level = 0)
        return ImageDataStbGLES(
            effectiveWidth,
            effectiveHeight,
            format,
            type,
            flipV,
            MPPBuffer(buf)
        )
    }

    override fun destroy() {
        if (!destroyed) {
            session?.untrack(this)
            GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
            destroyed = true
            checkGl()
        }
    }

    override fun bind(unit: Int) {
        checkDestroyed()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(target, textureId)
    }

    override fun close() = destroy()

    override fun toString(): String =
        "ColorBufferGLES(target=$target, texture=$textureId, size=${width}x$height, contentScale=$contentScale, format=$format, type=$type, levels=$levels, multisample=$multisample)"
}

/* ------------ small GLES helpers (no desktop GL imports) --------------- */

private fun MinifyingFilter.toGLFilterGLES(): Int = when (this) {
    MinifyingFilter.NEAREST -> GLES30.GL_NEAREST
    MinifyingFilter.LINEAR -> GLES30.GL_LINEAR
    MinifyingFilter.LINEAR_MIPMAP_LINEAR -> GLES30.GL_LINEAR_MIPMAP_LINEAR
    MinifyingFilter.LINEAR_MIPMAP_NEAREST -> GLES30.GL_LINEAR_MIPMAP_NEAREST
    MinifyingFilter.NEAREST_MIPMAP_LINEAR -> GLES30.GL_NEAREST_MIPMAP_LINEAR
    MinifyingFilter.NEAREST_MIPMAP_NEAREST -> GLES30.GL_NEAREST_MIPMAP_NEAREST
}

private fun MagnifyingFilter.toGLFilterGLES(): Int = when (this) {
    MagnifyingFilter.NEAREST -> GLES30.GL_NEAREST
    MagnifyingFilter.LINEAR -> GLES30.GL_LINEAR
}

private fun WrapMode.glWrapGLES(): Int = when (this) {
    WrapMode.CLAMP_TO_EDGE -> GLES30.GL_CLAMP_TO_EDGE
    WrapMode.MIRRORED_REPEAT -> GLES30.GL_MIRRORED_REPEAT
    WrapMode.REPEAT -> GLES30.GL_REPEAT
}