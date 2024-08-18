package org.openrndr.internal.nullgl

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

class RenderTargetNullGL(override val width: Int, override val height: Int, override val contentScale: Double, override val multisample: BufferMultisample, override val session: Session?) : RenderTarget {
    override val colorAttachments : MutableList<ColorAttachment> = mutableListOf()

    override var depthBuffer: DepthBuffer? = null
    override fun attach(colorBuffer: ColorBuffer, level: Int, name: String?, ownedByRenderTarget: Boolean) {
        colorAttachments.add(ColorBufferAttachment(colorAttachments.size, name, colorBuffer, level, ownedByRenderTarget))
    }

    override fun attach(depthBuffer: DepthBuffer, ownedByRenderTarget: Boolean) {
        this.depthBuffer = depthBuffer
    }

    override fun attach(arrayTexture: ArrayTexture, layer: Int, level: Int, name: String?) {
        TODO("Not yet implemented")
    }

    override fun attach(arrayCubemap: ArrayCubemap, side: CubemapSide, layer: Int, level: Int, name: String?) {
        TODO("Not yet implemented")
    }

    override fun attach(cubemap: Cubemap, side: CubemapSide, level: Int, name: String?) {
        TODO("Not yet implemented")
    }

    override fun attach(volumeTexture: VolumeTexture, layer: Int, level: Int, name: String?) {
        TODO("Not yet implemented")
    }

    override fun attachLayered(arrayTexture: ArrayTexture, level: Int, name: String?) {
        TODO("Not yet implemented")
    }

    override fun attachLayered(arrayCubemap: ArrayCubemap, level: Int, name: String?) {
        TODO("Not yet implemented")
    }

    override fun attachLayered(cubemap: Cubemap, level: Int, name: String?) {
        TODO("Not yet implemented")
    }

    override fun attachLayered(volumeTexture: VolumeTexture, level: Int, name: String?) {
        TODO("Not yet implemented")
    }

    override fun detachColorAttachments() {
        TODO("Not yet implemented")
    }

    @Deprecated(
        "detachColorBuffer is deprecated, use detachColorAttachments",
        replaceWith = ReplaceWith("detachColorAttachments")
    )
    override fun detachColorBuffers() {
    }

    override fun detachDepthBuffer() {
        depthBuffer = null
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

    override fun colorBuffer(index: Int): ColorBuffer {
        return (colorAttachments[index] as ColorBufferAttachment).colorBuffer
    }



    override fun clearColor(index: Int, color: ColorRGBa) {

    }

    override fun clearDepth(depth: Double, stencil: Int) {
    }

    override fun blendMode(index: Int, blendMode: BlendMode) {

    }

    override fun bind() {

    }

    override fun unbind() {

    }

    override val hasDepthBuffer: Boolean
        get() = depthBuffer != null

    override val hasStencilBuffer: Boolean
        get() = depthBuffer?.hasStencil == true

    override val hasColorAttachments: Boolean
        get() = colorAttachments.isNotEmpty()
}