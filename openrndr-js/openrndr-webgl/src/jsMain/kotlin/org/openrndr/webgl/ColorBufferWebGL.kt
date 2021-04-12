package org.openrndr.webgl

import kotlinx.coroutines.await
import kotlinx.coroutines.yield
import org.khronos.webgl.WebGLBuffer
import org.khronos.webgl.WebGLTexture
import org.openrndr.draw.*
import org.openrndr.shape.IntRectangle
import org.w3c.dom.Image
import kotlin.js.Promise
import kotlin.math.log2
import org.khronos.webgl.WebGLRenderingContext as GL

internal fun promiseImage(url: String): Promise<Image> {
    return Promise<Image>() { resolve, reject ->
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
            type: ColorType = ColorType.FLOAT32,
            multisample: BufferMultisample,
            levels: Int,
            session: Session?
        ): ColorBufferWebGL {
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
            val (internalFormat, internalType) = internalFormat(format, type)
            for (level in 0 until levels) {
                val div = 1 shl level
                context.texImage2D(
                    GL.TEXTURE_2D,
                    level,
                    internalFormat,
                    effectiveWidth / div,
                    effectiveHeight / div,
                    0,
                    internalType,
                    GL.UNSIGNED_BYTE,
                    null
                )
            }

            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR)
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
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
            if (log2(image.width.toDouble())%1.0 == 0.0 && log2(image.height.toDouble())%1.0 == 0.0) {
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
        set(value) {}

    override var flipV: Boolean = false

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
}
