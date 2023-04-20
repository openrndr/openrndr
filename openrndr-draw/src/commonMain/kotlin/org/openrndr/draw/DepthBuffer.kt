package org.openrndr.draw

import org.openrndr.internal.Driver

interface DepthBuffer {
    val session: Session?
    val width: Int
    val height: Int
    val format: DepthFormat
    val multisample: BufferMultisample

    val hasDepth: Boolean
        get() = format.hasDepth

    val hasStencil: Boolean
        get() = format.hasStencil

    fun resolveTo(target: DepthBuffer)
    fun copyTo(target: DepthBuffer)
    fun destroy()
    fun bind(textureUnit: Int)
}

fun depthBuffer(
    width: Int,
    height: Int,
    format: DepthFormat = DepthFormat.DEPTH24_STENCIL8,
    multisample: BufferMultisample
): DepthBuffer = Driver.instance.createDepthBuffer(width, height, format, multisample)
