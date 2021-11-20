package org.openrndr.webgl

import WebGLRenderingFixedCompressedTexImage
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.TexImageSource
import org.khronos.webgl.WebGLFramebuffer
import org.khronos.webgl.WebGLTexture
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.shape.IntRectangle
import org.openrndr.utils.buffer.MPPBuffer
import org.w3c.dom.Image
import kotlin.js.Promise
import kotlin.math.log2
import org.khronos.webgl.WebGLRenderingContext as GL

internal fun promiseImage(url: String): Promise<Image> {
    return Promise<Image>() { resolve, _ ->
        val image = Image()
        image.addEventListener("load", {
            resolve(image)

        }, false)
        image.src = url
    }
}


class ColorBufferWebGL(
    val context: GL,
    val target: Int,
    val texture: WebGLTexture,
    override val width: Int,
    override val height: Int,
    override val contentScale: Double,
    override val format: ColorFormat,
    override val type: ColorType,
    override val levels: Int,
    override val multisample: BufferMultisample,
    override val session: Session?

) : ColorBuffer() {

    companion object {
        fun create(
            context: GL,
            width: Int,
            height: Int,
            contentScale: Double = 1.0,
            format: ColorFormat = ColorFormat.RGBa,
            type: ColorType = ColorType.UINT8,
            multisample: BufferMultisample,
            levels: Int,
            session: Session?
        ): ColorBufferWebGL {
            if (type == ColorType.FLOAT16) {
                require((Driver.instance as DriverWebGL).capabilities.halfFloatTextures) {
                    """no support for half float textures."""
                }
            }
            if (type == ColorType.FLOAT32) {
                require((Driver.instance as DriverWebGL).capabilities.floatTextures) {
                    """no support for float textures"""
                }
            }
            val texture = context.createTexture() ?: error("failed to create texture")
            context.activeTexture(GL.TEXTURE0)
            when (multisample) {
                BufferMultisample.Disabled -> context.bindTexture(GL.TEXTURE_2D, texture)
                is BufferMultisample.SampleCount -> error("multisample not supported on WebGL(1)")
            }
            val effectiveWidth = (width * contentScale).toInt()
            val effectiveHeight = (height * contentScale).toInt()
            if (levels > 1) {
                //context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAX_LEVEL, levels - 1)
            }
            val (internalFormat, _) = internalFormat(format, type)



            if (!type.compressed) {
                for (level in 0 until levels) {
                    val div = 1 shl level
                    context.texImage2D(
                        GL.TEXTURE_2D,
                        level,
                        internalFormat,
                        effectiveWidth / div,
                        effectiveHeight / div,
                        0,
                        internalFormat,
                        type.glType(),
                        null
                    )
                }
            } else {
                for (level in 0 until levels) {
                    val div = 1 shl level
                    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                    val fcontext = context as? WebGLRenderingFixedCompressedTexImage ?: error("cast failed")
                    fcontext.compressedTexImage2D(
                        GL.TEXTURE_2D,
                        level,
                        internalFormat,
                        effectiveWidth / div,
                        effectiveHeight / div,
                        0,
                        null
                    )
                }
            }

            val caps = (Driver.instance as DriverWebGL).capabilities
            if (type == ColorType.UINT8 ||
                (type == ColorType.FLOAT16 && caps.halfFloatTexturesLinear) ||
                (type == ColorType.FLOAT32 && caps.floatTexturesLinear)
            ) {
                context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR)
                context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
            } else {
                context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.NEAREST)
                context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.NEAREST)
            }
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
            return ColorBufferWebGL(
                context,
                GL.TEXTURE_2D,
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

        fun fromImage(context: GL, image: Image, session: Session? = Session.active): ColorBufferWebGL {
            val texture = context.createTexture() ?: error("failed to create texture")
            context.activeTexture(GL.TEXTURE0)
            context.bindTexture(GL.TEXTURE_2D, texture)
            context.texImage2D(GL.TEXTURE_2D, 0, GL.RGBA, GL.RGBA, GL.UNSIGNED_BYTE, image)
            if (log2(image.width.toDouble()) % 1.0 == 0.0 && log2(image.height.toDouble()) % 1.0 == 0.0) {
                context.generateMipmap(GL.TEXTURE_2D)
            }
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR)
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
            return ColorBufferWebGL(
                context, GL.TEXTURE_2D, texture, image.width, image.height, 1.0,
                ColorFormat.RGBa, ColorType.UINT8, 1, BufferMultisample.Disabled, session
            )
        }

        @Suppress("UNUSED_PARAMETER")
        fun fromUrl(context: GL, url: String, session: Session? = Session.active): ColorBufferWebGL {
            error("use fromUrlSuspend")
            //val image = promiseImage(url).await()
            //return fromImage(context, image, session)
        }

        suspend fun fromUrlSuspend(context: GL, url: String, session: Session? = Session.active): ColorBufferWebGL {
            val image = promiseImage(url).await()
            return fromImage(context, image, session)
        }

    }

    override fun destroy() {
        context.deleteTexture(texture)
    }

    override fun bind(unit: Int) {
        context.activeTexture(unit + GL.TEXTURE0)
        context.bindTexture(target, texture)
    }

    override fun generateMipmaps() {
        bind(0)
        context.generateMipmap(target)
    }

    override var anisotropy: Double
        get() = TODO("Not yet implemented")
        set(_) {}

    override var flipV: Boolean = false

    override fun copyTo(target: ColorBuffer, fromLevel: Int, toLevel: Int, filter: MagnifyingFilter) {
        val sourceRectangle: IntRectangle = IntRectangle(
            0,
            0,
            this.effectiveWidth / (1 shl fromLevel),
            this.effectiveHeight / (1 shl fromLevel)
        )
        val targetRectangle: IntRectangle = IntRectangle(
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
        TODO("Not yet implemented")
    }

    override fun copyTo(target: ArrayTexture, layer: Int, fromLevel: Int, toLevel: Int) {
        TODO("Not yet implemented")
    }

    override fun write(
        source: TexImageSource,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level:Int
    ) {
        require(!type.compressed)
        bind(0)
        context.pixelStorei(GL.UNPACK_FLIP_Y_WEBGL, 1)
        this.context.texSubImage2D(target, level, x, y, GL.RGBA, GL.UNSIGNED_BYTE, source)
    }

    override fun write(
        source: ArrayBufferView,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    ) {
        bind(0)
        context.pixelStorei(GL.UNPACK_FLIP_Y_WEBGL, 1)

        if (!sourceType.compressed) {
            this.context.texSubImage2D(
                target,
                level,
                x,
                y,
                width,
                height,
                sourceFormat.glFormat(),
                sourceType.glType(),
                source
            )
        } else {
            this.context.compressedTexSubImage2D(
                target,
                level,
                x,
                y,
                width,
                height,
                sourceType.glType(),
                source
            )
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
        write(sourceBuffer.dataView, sourceFormat, sourceType, x, y, width, height, level)
    }

    private val readFrameBuffer by lazy {
        context.createFramebuffer() ?: error("failed to create framebuffer")
    }

    override fun read(target: ArrayBufferView, x: Int, y: Int, width: Int, height: Int, level: Int) {
        bind(0)
        val current = context.getParameter(GL.FRAMEBUFFER_BINDING) as WebGLFramebuffer?
        context.bindFramebuffer(GL.FRAMEBUFFER, readFrameBuffer)
        context.framebufferTexture2D(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0, this.target, texture,0)
        context.readPixels(x, y, effectiveWidth, effectiveHeight, GL.RGBA, GL.UNSIGNED_BYTE, target)
        context.bindFramebuffer(GL.FRAMEBUFFER, current)
    }

    override fun filter(filterMin: MinifyingFilter, filterMag: MagnifyingFilter) {
        bind(0)
        context.texParameteri(target, GL.TEXTURE_MIN_FILTER, filterMin.toGLFilter())
        context.texParameteri(target, GL.TEXTURE_MAG_FILTER, filterMag.toGLFilter())
    }

    override var wrapU: WrapMode
        get() = TODO("Not yet implemented")
        set(_) {}
    override var wrapV: WrapMode
        get() = TODO("Not yet implemented")
        set(_) {}

    override fun fill(color: ColorRGBa) {
        TODO("Not yet implemented")
    }
}
