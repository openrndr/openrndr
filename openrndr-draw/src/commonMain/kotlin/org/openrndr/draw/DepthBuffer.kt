package org.openrndr.draw

import org.openrndr.internal.Driver

/**
 * Represents a depth buffer used for rendering operations. A depth buffer stores depth information
 * for rendered pixels, enabling depth testing in graphics applications. It may optionally include
 * stencil information based on its format.
 *
 * The depth buffer is associated with a specific [Session], which manages its lifecycle.
 * It has a defined width, height, format, and multisample configuration.
 */
interface DepthBuffer: AutoCloseable {
    val session: Session?
    val width: Int
    val height: Int
    val format: DepthFormat
    val multisample: BufferMultisample

    val hasDepth: Boolean
        get() = format.hasDepth

    /**
     * Indicates whether the depth buffer includes a stencil component based on its format.
     * If true, the buffer supports stencil operations.
     */
    val hasStencil: Boolean
        get() = format.hasStencil

    fun resolveTo(target: DepthBuffer)
    fun copyTo(target: DepthBuffer)
    fun destroy()
    fun bind(textureUnit: Int)
}

/**
 * Creates a depth buffer with the specified dimensions, format, and multisampling
 * configuration. A depth buffer is used in rendering to store depth information
 * for pixels, with optional stencil buffer support depending on the depth format.
 *
 * @param width The width of the depth buffer in pixels.
 * @param height The height of the depth buffer in pixels.
 * @param format The depth format specifying the depth and stencil configuration. Defaults to `DepthFormat.DEPTH24_STENCIL8`.
 * @param multisample The multisample setup for the depth buffer, which determines if and how multisampling is applied.
 * @return A `DepthBuffer` instance configured with the given parameters.
 */
fun depthBuffer(
    width: Int,
    height: Int,
    format: DepthFormat = DepthFormat.DEPTH24_STENCIL8,
    multisample: BufferMultisample
): DepthBuffer = Driver.instance.createDepthBuffer(width, height, format, multisample)
