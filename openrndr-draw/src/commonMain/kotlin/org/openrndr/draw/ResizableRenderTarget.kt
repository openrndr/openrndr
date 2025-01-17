package org.openrndr.draw

/**
 * A class that represents a render target capable of being resized dynamically.
 *
 * This class manages a [RenderTarget] instance and allows its dimensions and content scale to be
 * updated after creation. Existing GPU resources associated with the current [RenderTarget] are
 * properly destroyed and the [RenderTarget] is reconstructed when resized.
 *
 * @param width The initial width of the render target.
 * @param height The initial height of the render target.
 * @param contentScale The scaling factor for the render target's pixel density.
 * @param multisample The multisample configuration for the render target.
 * @param session The session managing the GPU resources of the render target. Can be null.
 * @param builder A function used to configure the render target during its construction.
 */
class ResizableRenderTarget(
    width: Int,
    height: Int,
    contentScale: Double = 1.0,
    multisample: BufferMultisample = BufferMultisample.Disabled,
    session: Session?,
    val builder: RenderTargetBuilder.() -> Unit
) {
    var renderTarget = renderTarget(width, height, contentScale, multisample, session, builder)

    fun resize(renderTarget: RenderTarget) {
        resize(renderTarget.width, renderTarget.height, renderTarget.contentScale)
    }

    fun resize(
        newWidth: Int = renderTarget.width,
        newHeight: Int = renderTarget.height,
        newContentScale: Double = renderTarget.contentScale
    ) {
        if (renderTarget.width != newWidth || renderTarget.height != newHeight || renderTarget.contentScale != newContentScale) {

            for (attachment in renderTarget.colorAttachments) {
                when (attachment) {
                    is ColorBufferAttachment -> attachment.colorBuffer.destroy()
                    else -> error("unsupported attachment `$attachment` in ResizableRenderTarget")
                }
            }
            renderTarget.depthBuffer?.destroy()
            renderTarget.detachColorAttachments()
            renderTarget.detachDepthBuffer()
            renderTarget.destroy()

            renderTarget = renderTarget(
                newWidth,
                newHeight,
                newContentScale,
                renderTarget.multisample,
                renderTarget.session,
                builder
            )
        }
    }
}

fun resizableRenderTarget(
    width: Int, height: Int, contentScale: Double, multisample: BufferMultisample = BufferMultisample.Disabled,
    session: Session? = Session.active, builder: RenderTargetBuilder.() -> Unit
) =
    ResizableRenderTarget(width, height, contentScale, multisample, session, builder)