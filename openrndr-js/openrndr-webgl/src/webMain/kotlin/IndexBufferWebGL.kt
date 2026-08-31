package org.openrndr.webgl

import org.openrndr.draw.IndexBuffer
import org.openrndr.draw.IndexType
import org.openrndr.draw.Session
import web.gl.DYNAMIC_DRAW
import web.gl.ELEMENT_ARRAY_BUFFER
import web.gl.WebGLBuffer
import web.gl.WebGL2RenderingContext as GL

class IndexBufferWebGL(val context: GL,
                       val buffer: WebGLBuffer,
                       override val type: IndexType,
                       override val indexCount: Int,
                       override val session: Session?
) : IndexBuffer {

    companion object {
        fun create(context: GL, type: IndexType, indexCount: Int, session: Session?) : IndexBufferWebGL {
            val buffer = context.createBuffer() ?: error("failed to create buffer")
            context.bindBuffer(ELEMENT_ARRAY_BUFFER, buffer)
            context.checkErrors()
            val sizeInBytes = type.sizeInBytes * indexCount
            context.bufferData(ELEMENT_ARRAY_BUFFER, sizeInBytes, DYNAMIC_DRAW)
            context.checkErrors()
            return IndexBufferWebGL(context, buffer, type, indexCount, session)
        }
    }

    fun bind() {
        context.bindBuffer(ELEMENT_ARRAY_BUFFER, buffer)
        context.checkErrors()
    }

    fun unbind() {
        context.bindBuffer(ELEMENT_ARRAY_BUFFER, null)
        context.checkErrors()
    }

    override fun destroy() {
        context.deleteBuffer(buffer)
        context.checkErrors()
    }

    override fun close() {
        destroy()
    }
}