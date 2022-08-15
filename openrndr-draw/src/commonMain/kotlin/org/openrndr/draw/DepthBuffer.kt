package org.openrndr.draw

import org.openrndr.internal.Driver

interface DepthBuffer {
    val session: Session?
    val width: Int
    val height: Int
    val format: DepthFormat
    val multisample: BufferMultisample

    val hasStencil: Boolean
        get() = format == DepthFormat.DEPTH32F_STENCIL8 || format == DepthFormat.DEPTH24_STENCIL8 || format == DepthFormat.DEPTH_STENCIL

    companion object {
        @Deprecated("use the depthBuffer() builder function instead")
        fun create(width: Int, height: Int, format: DepthFormat = DepthFormat.DEPTH24_STENCIL8, multisample: BufferMultisample): DepthBuffer = Driver.instance.createDepthBuffer(width, height, format, multisample)
    }

    fun resolveTo(target: DepthBuffer)
    fun copyTo(target: DepthBuffer)
    fun destroy()
    fun bind(textureUnit: Int)
}

fun depthBuffer(width: Int, height: Int, format: DepthFormat = DepthFormat.DEPTH24_STENCIL8, multisample: BufferMultisample): DepthBuffer = Driver.instance.createDepthBuffer(width, height, format, multisample)
