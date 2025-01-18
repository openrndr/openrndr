package org.openrndr.webgl

import org.khronos.webgl.WebGLBuffer
import org.khronos.webgl.WebGLRenderingContext as GL
import org.openrndr.draw.IndexBuffer
import org.openrndr.draw.IndexType

class IndexBufferWebGL(val context: GL,
    val buffer: WebGLBuffer,
    override val type: IndexType,
    override val indexCount: Int
) : IndexBuffer {

    companion object {
        fun create(context: GL, type: IndexType, indexCount: Int) : IndexBufferWebGL {
            val buffer = context.createBuffer() ?: error("failed to create buffer")
            context.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, buffer)
            val sizeInBytes = type.sizeInBytes * indexCount
            context.bufferData(GL.ELEMENT_ARRAY_BUFFER, sizeInBytes, GL.DYNAMIC_DRAW)
            return IndexBufferWebGL(context, buffer, type, indexCount)
        }
    }

    fun bind() {
        context.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, buffer)
    }

    fun unbind() {
        context.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, null)
    }

    override fun destroy() {
        context.deleteBuffer(buffer)
    }

    override fun close() {
        destroy()
    }
}