package org.openrndr.draw

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