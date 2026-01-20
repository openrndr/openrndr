package org.openrndr.webgl

import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.DepthBuffer
import org.openrndr.draw.DepthFormat
import org.openrndr.draw.Session
import web.gl.WebGLRenderbuffer
import web.gl.WebGL2RenderingContext as GL

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

            val webGlFormat = when(format) {
                DepthFormat.DEPTH16 -> GL.DEPTH_COMPONENT16
                DepthFormat.DEPTH_STENCIL -> GL.DEPTH_STENCIL
                DepthFormat.STENCIL8 -> GL.STENCIL_INDEX8
                else -> error("unsupported depth buffer format $format")
            }

            context.renderbufferStorage(GL.RENDERBUFFER, webGlFormat, width, height)
            return DepthBufferWebGL(context, buffer, width, height, format, multisample, session)
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

    override fun close() {
        destroy()
    }

}