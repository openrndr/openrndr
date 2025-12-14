package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger {}

/**
 * Represents an OpenGL index buffer
 *
 * This class encapsulates an index buffer object (IBO) used for storing index data
 * in GPU memory, which is essential for indexed rendering in OpenGL. It provides
 * methods to bind, unbind, write, read, and destroy the buffer.
 *
 * @property buffer The OpenGL buffer ID.
 * @property indexCount The number of indices in the buffer.
 * @property type The type of indices stored in the buffer (e.g., INT16, INT32).
 * @property session The session associated with the index buffer, if any.
 *
 * Implements the [IndexBuffer] interface.
 */
class IndexBufferGL3(
    val buffer: Int, override val indexCount: Int, override val type: IndexType, override val session: Session?
) : IndexBuffer {

    private var isDestroyed = false

    companion object {
        fun create(elementCount: Int, type: IndexType, session: Session?): IndexBufferGL3 {
            val cb = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)
            val buffer = glGenBuffers()
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer)
            checkGLErrors()
            val sizeInBytes = type.sizeInBytes * elementCount
            val useBufferStorage =
                (Driver.instance as DriverGL3).version >= DriverVersionGL.GL_VERSION_4_4 && Driver.glVersion.type == DriverTypeGL.GL
            if (useBufferStorage) {
                glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, sizeInBytes.toLong(), GL_DYNAMIC_STORAGE_BIT)
            } else {
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeInBytes.toLong(), GL_DYNAMIC_DRAW)
            }
            checkGLErrors()
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cb)
            return IndexBufferGL3(buffer, elementCount, type, session)
        }
    }

    fun bind() {
        if (isDestroyed) {
            throw IllegalStateException("buffer is destroyed")
        }
        logger.trace { "binding vertex buffer $buffer" }
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer)
        debugGLErrors()
    }

    fun unbind() {
        logger.trace { "unbinding vertex buffer" }
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        debugGLErrors()
    }

    override fun write(data: ByteBuffer, offsetInBytes: Int) {
        require(!isDestroyed)
        require(data.isDirect) { "data is not a direct ByteBuffer." }

        when (Driver.glType) {
            DriverTypeGL.GL -> bound {
                glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, offsetInBytes.toLong(), data)
                debugGLErrors()
            }

            DriverTypeGL.GLES -> bound {
                glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, offsetInBytes.toLong(), data)
                debugGLErrors()
            }
        }
    }

    override fun read(data: ByteBuffer, offsetInBytes: Int) {
        require(!isDestroyed)
        require(data.isDirect) { "data is not a direct ByteBuffer." }

        when (Driver.glType) {
            DriverTypeGL.GL -> bound {
                glGetBufferSubData(GL_ELEMENT_ARRAY_BUFFER, offsetInBytes.toLong(), data)
            }

            DriverTypeGL.GLES -> {
                bind()
                val bufferLengthInBytes = (type.sizeInBytes * indexCount) - offsetInBytes
                val buffer = glMapBufferRange(
                    GL_ELEMENT_ARRAY_BUFFER,
                    offsetInBytes.toLong(),
                    bufferLengthInBytes.toLong(),
                    GL_MAP_READ_BIT
                )
                require(buffer != null)
                data.put(buffer)
                glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER)
            }
        }
        checkGLErrors()
    }

    override fun destroy() {
        glDeleteBuffers(buffer)
        isDestroyed = true
    }


    private fun bound(f: IndexBufferGL3.() -> Unit) {
        bind()
        this.f()
        unbind()
    }

    override fun shaderStorageBufferView(): ShaderStorageBuffer {
        val sbf = when (type) {
            IndexType.INT16 -> error("16 bit indices are not supported.")
            IndexType.INT32 -> shaderStorageFormat {
                primitive("indices", BufferPrimitiveType.INT32, indexCount)
            }
        }
        return ShaderStorageBufferGL43(buffer, false, sbf, session)
    }

    override fun close() {
        destroy()
    }
}

