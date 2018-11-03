package org.openrndr.draw

import org.openrndr.internal.Driver

interface RenderTarget {
    val width: Int
    val height: Int
    val contentScale: Double

    val effectiveWidth: Int get() = (width * contentScale).toInt()
    val effectiveHeight: Int get() = (height * contentScale).toInt()

    val colorBuffers: List<ColorBuffer>
    val depthBuffer: DepthBuffer?

    companion object {
        fun create(width: Int, height: Int, contentScale: Double): RenderTarget = Driver.instance.createRenderTarget(width, height, contentScale)
        val active: RenderTarget
            get() = Driver.instance.activeRenderTarget
    }

    fun attach(name: String, colorBuffer: ColorBuffer)

    fun attach(colorBuffer: ColorBuffer)
    fun attach(depthBuffer: DepthBuffer)
    fun detachColorBuffers()
    fun detachDepthBuffer()
    fun destroy()

    fun colorBuffer(index: Int): ColorBuffer
    fun colorBuffer(name: String): ColorBuffer
    fun colorBufferIndex(name: String): Int

    fun bind()
    fun unbind()

    val hasDepthBuffer: Boolean
    val hasColorBuffer: Boolean
}

@Suppress("unused")
class RenderTargetBuilder(private val renderTarget: RenderTarget) {


    fun colorBuffer(colorBuffer: ColorBuffer) {
        renderTarget.attach(colorBuffer)
    }

    fun colorBuffer(name: String, colorBuffer: ColorBuffer) {
        renderTarget.attach(name, colorBuffer)
    }

    fun colorBuffer(name: String, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8) {
        val cb = ColorBuffer.create(renderTarget.width, renderTarget.height, renderTarget.contentScale, format, type)
        renderTarget.attach(name, cb)
    }

    fun colorBuffer(format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8) {
        val cb = ColorBuffer.create(renderTarget.width, renderTarget.height, renderTarget.contentScale, format, type)
        renderTarget.attach(cb)
    }

    fun depthBuffer(format: DepthFormat = DepthFormat.DEPTH24_STENCIL8) {
        renderTarget.attach(DepthBuffer.create(renderTarget.effectiveWidth, renderTarget.effectiveHeight, format))
    }

    fun depthBuffer(depthBuffer: DepthBuffer) {
        renderTarget.attach(depthBuffer)
    }
}


fun renderTarget(width: Int, height: Int, contentScale: Double = 1.0, builder: RenderTargetBuilder.() -> Unit): RenderTarget {
    val renderTarget = RenderTarget.create(width, height, contentScale)
    RenderTargetBuilder(renderTarget).builder()
    return renderTarget
}

