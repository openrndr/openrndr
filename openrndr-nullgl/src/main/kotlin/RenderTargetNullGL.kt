package org.openrndr.internal.nullgl

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

class RenderTargetNullGL(override val width: Int, override val height: Int, override val contentScale: Double, override val multisample: BufferMultisample, override val session: Session?) : RenderTarget {
    override val colorBuffers: MutableList<ColorBuffer> = mutableListOf()
    override var depthBuffer: DepthBuffer? = null

    override fun attach(name: String, colorBuffer: ColorBuffer, level: Int) {
        TODO("Not yet implemented")
    }

    override fun attach(colorBuffer: ColorBuffer, level: Int) {
        colorBuffers.add(colorBuffer)
    }

    override fun attach(depthBuffer: DepthBuffer) {
        this.depthBuffer = depthBuffer
    }

    override fun attach(name: String, arrayTexture: ArrayTexture, layer: Int, level: Int) {
        TODO("Not yet implemented")
    }

    override fun attach(arrayTexture: ArrayTexture, layer: Int, level: Int) {
        TODO("Not yet implemented")
    }

    override fun detachColorBuffers() {
        colorBuffers.clear()
    }

    override fun detachDepthBuffer() {
        depthBuffer = null
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

    override fun colorBuffer(index: Int): ColorBuffer {
        return colorBuffers[index]
    }

    override fun colorBuffer(name: String): ColorBuffer {
        TODO("Not yet implemented")
    }

    override fun colorBufferIndex(name: String): Int {
        TODO("Not yet implemented")
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
    override val hasColorBuffer: Boolean
        get() = colorBuffers.isNotEmpty()
}