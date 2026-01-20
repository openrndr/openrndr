package org.openrndr.webgl

import io.github.oshai.kotlinlogging.KotlinLogging
import js.buffer.ArrayBuffer
import js.typedarrays.Float32Array
import org.openrndr.draw.*
import org.openrndr.utils.buffer.MPPBuffer
import web.gl.WebGLBuffer
import web.gl.WebGL2RenderingContext as GL


private val logger = KotlinLogging.logger {  }

class VertexBufferShadowWebGL(override val vertexBuffer: VertexBuffer) : VertexBufferShadow {

    val shadow = Float32Array<ArrayBuffer>(vertexBuffer.vertexCount * (vertexBuffer.vertexFormat.size / 4))

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

    override fun close() {
        destroy()
    }

    override fun reader(): BufferReader {
        TODO("Not yet implemented")
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
            logger.debug { "Creating vertex buffer with $vertexCount vertices, format $vertexFormat" }
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

    override fun write(data: FloatArray, offsetBytes: Int, floatCount: Int) {
        logger.error { "not implemented" }

        error("not implemented")
//        // this one
//        bind()
//        val offsetFloats = offsetBytes / 4
//        context.bufferSubData(
//            GL.ARRAY_BUFFER,
//            offsetBytes,
//            Float32Array<ArrayBuffer>(data.toTypedArray()).subarray(offsetFloats, offsetFloats + floatCount)
//        )
//        unbind()
    }

    override fun write(data: Float32Array<ArrayBuffer>, offsetBytes: Int, floatCount: Int) {
        bind()
        val offsetFloats = offsetBytes / 4
        context.bufferSubData(GL.ARRAY_BUFFER, offsetBytes, data.subarray(offsetFloats, offsetFloats + floatCount))
        unbind()
    }

    override fun write(source: MPPBuffer, targetByteOffset: Int, sourceByteOffset: Int, byteLength: Int) {
        bind()
        context.bufferSubData(
            GL.ARRAY_BUFFER, targetByteOffset,
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