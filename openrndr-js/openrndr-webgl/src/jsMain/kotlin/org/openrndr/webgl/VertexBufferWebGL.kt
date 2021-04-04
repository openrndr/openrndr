package org.openrndr.webgl

import org.khronos.webgl.Float32Array
import org.khronos.webgl.WebGLBuffer
import org.openrndr.draw.Session
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.VertexBufferShadow
import org.openrndr.draw.VertexFormat
import org.khronos.webgl.WebGLRenderingContext as GL


class VertexBufferWebGL(val context: GL,
                        val buffer: WebGLBuffer,
                        override val vertexFormat: VertexFormat,
                        override val vertexCount: Int,
                        override val session: Session?
                        ) : VertexBuffer() {

    private var destroyed = false
    companion object {
        fun createDynamic(context: GL, vertexFormat: VertexFormat, vertexCount:Int, session: Session?) : VertexBufferWebGL {
            val buffer = context.createBuffer() ?: error("failed to create buffer")
            context.bindBuffer(GL.ARRAY_BUFFER, buffer)
            val sizeInBytes = vertexFormat.size * vertexCount
            context.bufferData(GL.ARRAY_BUFFER, sizeInBytes, GL.DYNAMIC_DRAW)
            return VertexBufferWebGL(context, buffer, vertexFormat, vertexCount, session)
        }
    }

    override val shadow: VertexBufferShadow
        get() = TODO("Not yet implemented")

    override fun destroy() {
        if (!destroyed) {
            context.deleteBuffer(buffer)
            destroyed = true
        }
        Session.active.untrack(this)
    }

    override fun write(data: FloatArray) {
        bind()
        context.bufferSubData(GL.ARRAY_BUFFER, 0, Float32Array(data.toTypedArray()))
        unbind()
    }

    fun bind() {
        context.bindBuffer(GL.ARRAY_BUFFER, buffer)
    }

    fun unbind() {
        context.bindBuffer(GL.ARRAY_BUFFER, null as WebGLBuffer?)
    }
}