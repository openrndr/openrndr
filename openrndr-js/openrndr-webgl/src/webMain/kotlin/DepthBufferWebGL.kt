package org.openrndr.webgl

import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.DepthBuffer
import org.openrndr.draw.DepthFormat
import org.openrndr.draw.Session
import web.gl.*
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
            context.checkErrors("create render buffer")
            context.bindRenderbuffer(RENDERBUFFER, buffer)
            context.checkErrors("bindRenderBuffer")

            val webGlFormat = when(format) {
                DepthFormat.DEPTH16 -> DEPTH_COMPONENT16
                DepthFormat.DEPTH_STENCIL -> DEPTH_STENCIL
                DepthFormat.STENCIL8 -> STENCIL_INDEX8
                else -> error("unsupported depth buffer format $format")
            }

            context.renderbufferStorage(RENDERBUFFER, webGlFormat, width, height)
            context.checkErrors("renderBufferStorage")
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
        context.checkErrors("deleteRenderBuffer")
    }

    override fun close() {
        destroy()
    }

}