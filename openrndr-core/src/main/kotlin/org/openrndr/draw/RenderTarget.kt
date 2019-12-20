package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer as _colorBuffer
import org.openrndr.draw.depthBuffer as _depthBuffer
import org.openrndr.internal.Driver
import java.lang.IllegalStateException

interface RenderTarget {
    val width: Int
    val height: Int
    val contentScale: Double
    val multisample: BufferMultisample

    val effectiveWidth: Int get() = (width * contentScale).toInt()
    val effectiveHeight: Int get() = (height * contentScale).toInt()

    val colorBuffers: List<ColorBuffer>
    val depthBuffer: DepthBuffer?

    companion object {
        @Deprecated("use the renderTarget builder function instead")
        fun create(width: Int, height: Int, contentScale: Double, multisample: BufferMultisample): RenderTarget = Driver.instance.createRenderTarget(width, height, contentScale, multisample)

        val active: RenderTarget
            get() = Driver.instance.activeRenderTarget
    }

    fun attach(name: String, colorBuffer: ColorBuffer)

    fun attach(colorBuffer: ColorBuffer)
    fun attach(depthBuffer: DepthBuffer)
    fun attach(name: String, arrayTexture: ArrayTexture, layer: Int)
    fun attach(arrayTexture: ArrayTexture, layer: Int)

    fun detachColorBuffers()
    fun detachDepthBuffer()
    fun destroy()

    fun colorBuffer(index: Int): ColorBuffer
    fun colorBuffer(name: String): ColorBuffer
    fun colorBufferIndex(name: String): Int

    fun clearColor(index: Int, color: ColorRGBa)
    fun clearDepth(depth: Double = 1.0, stencil: Int = 0)

    fun bind()
    fun unbind()

    val hasDepthBuffer: Boolean
    val hasColorBuffer: Boolean

    fun resolveTo(to: RenderTarget) {
        require(this.width == to.width && this.height == to.height)
        require(colorBuffers.size == to.colorBuffers.size)
        require((to.depthBuffer == null) == (depthBuffer == null))

        for (i in colorBuffers.indices) {
            colorBuffers[i].resolveTo(to.colorBuffers[i])
        }

        depthBuffer?.resolveTo((to.depthBuffer!!))
    }
}

@Suppress("unused")
class RenderTargetBuilder(private val renderTarget: RenderTarget) {

    @Deprecated("you should not use this", replaceWith = ReplaceWith("colorBuffer()"), level = DeprecationLevel.ERROR)
    fun colorBuffer(width: Int, height: Int, contentScale: Double = 1.0, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8, multisample: BufferMultisample): Nothing {
        throw IllegalStateException("use colorBuffer without width and height arguments")
    }

    fun colorBuffer(colorBuffer: ColorBuffer) {
        renderTarget.attach(colorBuffer)
    }

    fun colorBuffer(name: String, colorBuffer: ColorBuffer) {
        if (colorBuffer.multisample == renderTarget.multisample) {
            renderTarget.attach(name, colorBuffer)
        } else {
            throw IllegalArgumentException("${colorBuffer.multisample} != ${renderTarget.multisample}")
        }
    }

    fun colorBuffer(name: String, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8) {
        val cb = _colorBuffer(renderTarget.width, renderTarget.height, renderTarget.contentScale, format, type, renderTarget.multisample)
        renderTarget.attach(name, cb)
    }

    fun colorBuffer(format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8) {
        val cb = _colorBuffer(renderTarget.width, renderTarget.height, renderTarget.contentScale, format, type, renderTarget.multisample)
        renderTarget.attach(cb)
    }

    fun colorBuffer(name: String, arrayTexture: ArrayTexture, layer: Int) {
        renderTarget.attach(name, arrayTexture, layer)
    }

    fun arrayTexture(arrayTexture: ArrayTexture, layer: Int) {
        renderTarget.attach(arrayTexture, layer)
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


fun renderTarget(width: Int, height: Int, contentScale: Double = 1.0, multisample: BufferMultisample = BufferMultisample.Disabled, builder: RenderTargetBuilder.() -> Unit): RenderTarget {
    if (width <= 0 || height <= 0) {
        throw IllegalArgumentException("unsupported resolution ($width×$height)")
    }

    val renderTarget = Driver.driver.createRenderTarget(width, height, contentScale, multisample)
    RenderTargetBuilder(renderTarget).builder()
    return renderTarget
}


