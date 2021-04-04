package org.openrndr.webgl

import org.khronos.webgl.WebGLRenderbuffer
import org.openrndr.draw.BufferMultisample
import org.khronos.webgl.WebGLRenderingContext as GL
import org.openrndr.draw.DepthBuffer
import org.openrndr.draw.DepthFormat
import org.openrndr.draw.Session

class DepthBufferWebGL(
    val context: GL,
    val buffer: WebGLRenderbuffer,
    override val width: Int,
    override val height: Int,
    override val format: DepthFormat,
    override val multisample: BufferMultisample,
    override val session: Session?
) : DepthBuffer {
    companion object {
        fun create(
            context: GL,
            width: Int,
            height: Int,
            format: DepthFormat,
            multisample: BufferMultisample,
            session: Session?
        ): DepthBufferWebGL {
            val buffer = context.createRenderbuffer() ?: error("buffer creation failed")
            context.bindRenderbuffer(GL.RENDERBUFFER, buffer)
            context.renderbufferStorage(GL.RENDERBUFFER, GL.DEPTH_STENCIL, width, height)
            return DepthBufferWebGL(context, buffer, width, height, DepthFormat.DEPTH24_STENCIL8, multisample, session)
        }
    }

    override fun resolveTo(target: DepthBuffer) {
        TODO("Not yet implemented")
    }

    override fun copyTo(target: DepthBuffer) {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        context.deleteRenderbuffer(buffer)
    }

    override fun bind(textureUnit: Int) {
        TODO("Not yet implemented")
    }


}