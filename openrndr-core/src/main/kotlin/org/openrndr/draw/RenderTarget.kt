package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.draw.colorBuffer as _colorBuffer
import org.openrndr.draw.depthBuffer as _depthBuffer

sealed class ColorAttachment(val index: Int, val name: String?)
class ColorBufferAttachment(
        index: Int,
        name: String?,
        val colorBuffer: ColorBuffer,
        val level: Int
) : ColorAttachment(index, name)

class ArrayTextureAttachment(
        index: Int,
        name: String?,
        val arrayTexture: ArrayTexture,
        val layer: Int,
        val level: Int
) : ColorAttachment(index, name)

class ArrayCubemapAttachment(
        index: Int,
        name: String?,
        val arrayCubemap: ArrayCubemap,
        val side: CubemapSide,
        val layer: Int,
        val level: Int
) : ColorAttachment(index, name)

class CubemapAttachment(
        index: Int,
        name: String?,
        val cubemap: Cubemap,
        val side: CubemapSide,
        val level: Int
) : ColorAttachment(index, name)

interface RenderTarget {
    val session: Session?

    val width: Int
    val height: Int
    val contentScale: Double
    val multisample: BufferMultisample

    val effectiveWidth: Int get() = (width * contentScale).toInt()
    val effectiveHeight: Int get() = (height * contentScale).toInt()

    val colorAttachments: List<ColorAttachment>

    fun colorAttachmentIndexByName(name: String): Int? {
        return colorAttachments.find { it.name == name }?.index
    }

    fun colorAttachmentByName(name: String): ColorAttachment? {
        return colorAttachments.find { it.name == name }
    }

    val depthBuffer: DepthBuffer?

    companion object {
        val active: RenderTarget
            get() = Driver.instance.activeRenderTarget
    }

    fun attach(colorBuffer: ColorBuffer, level: Int = 0, name: String? = null)
    fun attach(depthBuffer: DepthBuffer)

    fun attach(arrayTexture: ArrayTexture, layer: Int, level: Int = 0, name: String? = null)
    fun attach(arrayCubemap: ArrayCubemap, side: CubemapSide, layer: Int, level: Int = 0, name: String? = null)

    fun detachColorAttachments()

    @Deprecated("detachColorBuffer is deprecated, use detachColorAttachments", replaceWith = ReplaceWith("detachColorAttachments"))
    fun detachColorBuffers()
    fun detachDepthBuffer()
    fun destroy()

    fun colorBuffer(index: Int): ColorBuffer

    fun clearColor(index: Int, color: ColorRGBa)
    fun clearDepth(depth: Double = 1.0, stencil: Int = 0)

    fun blendMode(index: Int, blendMode: BlendMode)

    fun bind()
    fun unbind()

    val hasDepthBuffer: Boolean
    val hasColorAttachments: Boolean

    fun resolveTo(to: RenderTarget) {
        require(this.width == to.width && this.height == to.height)
        require(colorAttachments.size == to.colorAttachments.size)
        require((to.depthBuffer == null) == (depthBuffer == null))
        for (i in colorAttachments.indices) {
            when (val a = colorAttachments[i]) {
                is ColorBufferAttachment -> a.colorBuffer.resolveTo((to.colorAttachments[i] as ColorBufferAttachment).colorBuffer)
            }
        }
        depthBuffer?.resolveTo((to.depthBuffer!!))
    }
}

@Suppress("unused")
class RenderTargetBuilder(private val renderTarget: RenderTarget) {

    @Deprecated("you should not use this", replaceWith = ReplaceWith("colorBuffer()"), level = DeprecationLevel.ERROR)
    @Suppress("UNUSED_PARAMETER")
    fun colorBuffer(width: Int, height: Int, contentScale: Double = 1.0, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8, multisample: BufferMultisample): Nothing {
        throw IllegalStateException("use colorBuffer without width and height arguments")
    }

    fun colorBuffer(colorBuffer: ColorBuffer, level: Int = 0) {
        renderTarget.attach(colorBuffer, level)
    }

    fun colorBuffer(name: String, colorBuffer: ColorBuffer, level: Int = 0) {
        if (colorBuffer.multisample == renderTarget.multisample) {
            renderTarget.attach(colorBuffer, level, name)
        } else {
            throw IllegalArgumentException("${colorBuffer.multisample} != ${renderTarget.multisample}")
        }
    }

    fun colorBuffer(name: String, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8) {
        val cb = _colorBuffer(renderTarget.width, renderTarget.height, renderTarget.contentScale, format, type, renderTarget.multisample)
        renderTarget.attach(cb, 0, name)
    }

    fun colorBuffer(format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8) {
        val cb = _colorBuffer(renderTarget.width, renderTarget.height, renderTarget.contentScale, format, type, renderTarget.multisample)
        renderTarget.attach(cb)
    }

    fun colorBuffer(name: String, arrayTexture: ArrayTexture, layer: Int, level: Int = 0) {
        renderTarget.attach(arrayTexture, layer, level, name)
    }

    fun arrayTexture(arrayTexture: ArrayTexture, layer: Int, level: Int = 0) {
        renderTarget.attach(arrayTexture, layer, level)
    }

    fun arrayCubemap(name: String, arrayCubemap: ArrayCubemap, side: CubemapSide, layer: Int, level: Int = 0) {
        renderTarget.attach(arrayCubemap, side, layer, level, name)
    }

    fun arrayCubemap(arrayCubemap: ArrayCubemap, side: CubemapSide, layer: Int, level: Int = 0) {
        renderTarget.attach(arrayCubemap, side, layer, level)
    }

    fun depthBuffer(format: DepthFormat = DepthFormat.DEPTH24_STENCIL8) {
        renderTarget.attach(_depthBuffer(renderTarget.effectiveWidth, renderTarget.effectiveHeight, format, renderTarget.multisample))
    }

    fun depthBuffer(depthBuffer: DepthBuffer) {
        if (depthBuffer.multisample == renderTarget.multisample) {
            renderTarget.attach(depthBuffer)
        } else {
            throw IllegalArgumentException("${depthBuffer.multisample} != ${renderTarget.multisample}")
        }
    }
}

fun renderTarget(width: Int, height: Int,
                 contentScale: Double = 1.0,
                 multisample: BufferMultisample = BufferMultisample.Disabled,
                 session: Session? = Session.active,
                 builder: RenderTargetBuilder.() -> Unit): RenderTarget {
    if (width <= 0 || height <= 0) {
        throw IllegalArgumentException("unsupported resolution ($width×$height)")
    }

    val renderTarget = Driver.instance.createRenderTarget(width, height, contentScale, multisample, session)
    RenderTargetBuilder(renderTarget).builder()
    return renderTarget
}