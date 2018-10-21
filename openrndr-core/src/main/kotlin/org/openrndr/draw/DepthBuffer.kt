package org.openrndr.draw

import org.openrndr.internal.Driver


interface DepthBuffer {

    val width: Int
    val height: Int
    val format: DepthFormat

    val hasStencil: Boolean
        get() = format == DepthFormat.DEPTH32F_STENCIL8 || format == DepthFormat.DEPTH24_STENCIL8

    companion object {
        fun create(width: Int, height: Int, format: DepthFormat = DepthFormat.DEPTH24_STENCIL8): DepthBuffer = Driver.instance.createDepthBuffer(width, height, format)
    }

    fun destroy()
    fun bind(textureUnit: Int)
}
