package org.openrndr.webgl

import org.khronos.webgl.Float32Array
import org.khronos.webgl.WebGLBuffer
import org.openrndr.draw.*
import org.openrndr.utils.buffer.MPPBuffer
import org.khronos.webgl.WebGLRenderingContext as GL


class VertexBufferShadowWebGL(override val vertexBuffer: VertexBuffer) : VertexBufferShadow {

    val shadow = Float32Array(vertexBuffer.vertexCount * (vertexBuffer.vertexFormat.size / 4))

    override fun upload(offsetInBytes: Int, sizeInBytes: Int) {
        vertexBuffer.write(shadow, offsetInBytes, sizeInBytes / 4)
    }

    override fun download() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        //
    }

    override fun writer(): BufferWriter {
        return BufferWriterWebGL(shadow, vertexBuffer.vertexFormat.size)
    }

}

class VertexBufferWebGL(
    val context: GL,
    val buffer: WebGLBuffer,
    override val vertexFormat: VertexFormat,
    override val vertexCount: Int,
    override val session: Session?
) : VertexBuffer() {

    override fun close() {
        destroy()
    }

    private var destroyed = false

    companion object {
        fun createDynamic(
            context: GL,
            vertexFormat: VertexFormat,
            vertexCount: Int,
            session: Session?
        ): VertexBufferWebGL {
            val buffer = context.createBuffer() ?: error("failed to create buffer")
            context.bindBuffer(GL.ARRAY_BUFFER, buffer)
            val sizeInBytes = vertexFormat.size * vertexCount
            context.bufferData(GL.ARRAY_BUFFER, sizeInBytes, GL.DYNAMIC_DRAW)
            return VertexBufferWebGL(context, buffer, vertexFormat, vertexCount, session)
        }
    }

    private var realShadow: VertexBufferShadowWebGL? = null
    override val shadow: VertexBufferShadow
    get() {
        if (realShadow == null) {
            realShadow = VertexBufferShadowWebGL(this)
        }
        return realShadow!!
    }


    override fun destroy() {
        if (!destroyed) {
            context.deleteBuffer(buffer)
            destroyed = true
        }
        Session.active.untrack(this)
    }

    override fun write(data: FloatArray, offsetBytes:Int, floatCount:Int) {
        bind()
        val offsetFloats = offsetBytes / 4
        context.bufferSubData(GL.ARRAY_BUFFER, offsetBytes, Float32Array(data.toTypedArray()).subarray(offsetFloats, offsetFloats + floatCount))
        unbind()
    }

    override fun write(data: Float32Array, offsetBytes:Int, floatCount: Int) {
        bind()
        val offsetFloats = offsetBytes / 4
        context.bufferSubData(GL.ARRAY_BUFFER, offsetBytes, data.subarray(offsetFloats, offsetFloats + floatCount))
        unbind()
    }

    override fun write(source: MPPBuffer, targetByteOffset: Int, sourceByteOffset: Int, byteLength: Int) {
        bind()
        context.bufferSubData(GL.ARRAY_BUFFER, targetByteOffset,
            source.dataView
            )
        unbind()
    }

    fun bind() {
        context.bindBuffer(GL.ARRAY_BUFFER, buffer)
    }

    fun unbind() {
        context.bindBuffer(GL.ARRAY_BUFFER, null as WebGLBuffer?)
    }
}