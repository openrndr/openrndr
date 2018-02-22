package org.openrndr.internal.gl3

import org.openrndr.draw.BufferWriter
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.VertexBufferShadow
import org.openrndr.draw.VertexFormat

import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING
import org.lwjgl.system.MemoryUtil.NULL

import java.nio.ByteBuffer
import java.nio.ByteOrder

private val logger = KotlinLogging.logger {}

class VertexBufferShadowGL3(override val vertexBuffer:VertexBufferGL3): VertexBufferShadow {
    val buffer: ByteBuffer =
            BufferUtils.createByteBuffer(vertexBuffer.vertexCount * vertexBuffer.vertexFormat.size).apply {
                order(ByteOrder.nativeOrder())
            }


    override fun upload(offset:Int, size:Int) {
        logger.trace { "uploading shadow to vertex buffer" }
        buffer.rewind()
        buffer.position(offset)
        buffer.limit(offset+size)
        vertexBuffer.write(buffer)
        buffer.limit(buffer.capacity())
    }

    override fun download() {
        buffer.rewind()
        vertexBuffer.read(buffer)
    }

    override fun destroy() {
        vertexBuffer.realShadow = null
    }

    override fun writer():BufferWriter {
        return BufferWriterGL3(buffer, vertexBuffer.vertexFormat.size)
    }

}

class VertexBufferGL3(val buffer:Int, override val vertexFormat: VertexFormat, override val vertexCount:Int) : VertexBuffer {

    internal var realShadow:VertexBufferShadowGL3? = null

    companion object {
        fun createDynamic(vertexFormat:VertexFormat, vertexCount:Int) : VertexBufferGL3 {
            val buffer = glGenBuffers()
            glBindBuffer(GL_ARRAY_BUFFER, buffer)
            val sizeInBytes = vertexFormat.size * vertexCount
            nglBufferData(GL_ARRAY_BUFFER, sizeInBytes.toLong(), NULL, GL_DYNAMIC_DRAW)
            checkGLErrors()
            return VertexBufferGL3(buffer, vertexFormat, vertexCount)
        }
    }

    override val shadow: VertexBufferShadow
        get() {
            if (realShadow == null) {
                realShadow = VertexBufferShadowGL3(this)
            }
            return realShadow!!
        }


    override fun write(data: ByteBuffer, offset:Int) {
        logger.trace { "writing to vertex buffer, ${data.remaining()} bytes" }
        data.rewind()
        debugGLErrors()
        bind()
        debugGLErrors()

//        val vaos = IntArray(1)
//        glGetIntegerv(GL_VERTEX_ARRAY_BINDING, vaos)
//        val vbos = IntArray(1)
//        glGetIntegerv(GL_ARRAY_BUFFER_BINDING, vbos)
        glBufferSubData(GL_ARRAY_BUFFER, offset.toLong(), data)

        checkGLErrors {

            val vertexArrayBinding = IntArray(1)
            glGetIntegerv(GL_VERTEX_ARRAY_BINDING, vertexArrayBinding)

            val arrayBufferBinding = IntArray(1)
            glGetIntegerv(GL_ARRAY_BUFFER_BINDING, arrayBufferBinding)

            val isBuffer = glIsBuffer(buffer)
            when(it) {
                GL_INVALID_OPERATION ->  "zero is bound to target. (is buffer: $isBuffer, GL_VERTEX_ARRAY_BINDING: ${vertexArrayBinding[0]}, GL_ARRAY_BUFFER_BINDING: ${arrayBufferBinding[0]})"
                GL_INVALID_VALUE -> "offset ($offset) or size is negative, or offset+sizeoffset+size is greater than the value of GL_BUFFER_SIZE for the specified buffer object."
                else -> null
            }
        }
    }

    override fun read(data:ByteBuffer, offset:Int) {
        bind()
    }

    override fun destroy() {
        glDeleteBuffers(buffer)
        checkGLErrors()
    }

    override fun bind() {
        logger.trace { "binding vertex buffer ${buffer}" }
        glBindBuffer(GL_ARRAY_BUFFER, buffer)
        debugGLErrors()
    }

    override fun unbind() {
        logger.trace { "unbinding vertex buffer" }
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        debugGLErrors()
    }

}