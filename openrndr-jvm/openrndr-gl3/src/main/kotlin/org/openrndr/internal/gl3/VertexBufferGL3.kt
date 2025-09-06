package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.BufferUtils

import org.lwjgl.opengl.GL33C.*
import org.lwjgl.opengl.GL44C.GL_DYNAMIC_STORAGE_BIT
import org.lwjgl.opengl.GL44C.glBufferStorage
import org.lwjgl.opengl.GL45C.glGetNamedBufferSubData
import org.lwjgl.opengl.GL45C.glNamedBufferSubData
import org.lwjgl.opengles.GLES30
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.utils.buffer.MPPBuffer
import java.nio.Buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

private val logger = KotlinLogging.logger {}

private val bufferId = AtomicInteger(0)

class VertexBufferShadowGL3(override val vertexBuffer: VertexBufferGL3) : VertexBufferShadow, AutoCloseable {
    val buffer: ByteBuffer =
        ByteBuffer.allocateDirect(vertexBuffer.vertexCount * vertexBuffer.vertexFormat.size).apply {
            order(ByteOrder.nativeOrder())
        }

    override fun upload(offsetInBytes: Int, sizeInBytes: Int) {
        logger.trace { "uploading shadow to vertex buffer" }
        (buffer as Buffer).rewind()
        (buffer as Buffer).position(offsetInBytes)
        (buffer as Buffer).limit(offsetInBytes + sizeInBytes)
        vertexBuffer.write(buffer)
        (buffer as Buffer).limit(buffer.capacity())
    }

    override fun download() {
        (buffer as Buffer).rewind()
        vertexBuffer.read(buffer)
    }

    override fun destroy() {
        close()
    }

    override fun writer(): BufferWriter {
        return BufferWriterGL3(buffer, vertexBuffer.vertexFormat.size, vertexBuffer.vertexFormat.alignment, null)
    }

    override fun reader(): BufferReader {
        return ByteBufferReader(buffer, vertexBuffer.vertexFormat.alignment, null)
    }

    override fun close() {
    }
}

class VertexBufferGL3(
    val buffer: Int,
    val offset: Long,
    override val vertexFormat: VertexFormat,
    override val vertexCount: Int,
    override val session: Session?
) : VertexBuffer() {

    override fun close() {
        destroy()
    }

    internal val bufferHash = bufferId.getAndAdd(1)
    internal var realShadow: VertexBufferShadowGL3? = null

    internal var isDestroyed = false

    override fun toString(): String {
        return "VertexBufferGL3(vertexFormat: $vertexFormat, vertexCount: $vertexCount, buffer: $buffer, session: $session)"
    }

    companion object {
        fun createDynamic(vertexFormat: VertexFormat, vertexCount: Int, session: Session?): VertexBufferGL3 {
            debugGLErrors {
                "pre-existing errors before creating vertex buffer"
            }
            val buffer = glGenBuffers()
            debugGLErrors()
            logger.debug {
                "created new vertex buffer[buffer=${buffer}, vertexCount=${vertexCount}, vertexFormat=${vertexFormat}]"
            }
            glBindBuffer(GL_ARRAY_BUFFER, buffer)
            debugGLErrors()
            val sizeInBytes = vertexFormat.size * vertexCount
            val useBufferStorage =
                (Driver.instance as DriverGL3).version >= DriverVersionGL.GL_VERSION_4_4 && (Driver.instance as DriverGL3).version.type == DriverTypeGL.GL

            if (useBufferStorage) {
                glBufferStorage(GL_ARRAY_BUFFER, max(1L, sizeInBytes.toLong()), GL_DYNAMIC_STORAGE_BIT)
            } else {
                glBufferData(GL_ARRAY_BUFFER, max(1L, sizeInBytes.toLong()), GL_DYNAMIC_DRAW)
            }
            debugGLErrors()
            return VertexBufferGL3(buffer, 0L, vertexFormat, vertexCount, session)
        }
    }

    override val shadow: VertexBufferShadow
        get() {
            if (isDestroyed) {
                error("buffer is destroyed")
            }
            if (realShadow == null) {
                realShadow = VertexBufferShadowGL3(this)
            }
            return realShadow ?: error("no shadow")
        }

    private val useNamedBuffer =
        (Driver.instance as DriverGL3).version >= DriverVersionGL.GL_VERSION_4_5 && (Driver.instance as DriverGL3).version.type == DriverTypeGL.GL

    override fun write(data: ByteBuffer, offsetInBytes: Int) {
        if (isDestroyed) {
            error("buffer is destroyed")
        }

        if (data.isDirect) {
            logger.trace { "writing to vertex buffer, ${data.remaining()} bytes" }

            if (useNamedBuffer) {
                glNamedBufferSubData(buffer, offsetInBytes.toLong(), data)
            } else {
                debugGLErrors()
                bind()
                debugGLErrors()
                glBufferSubData(GL_ARRAY_BUFFER, offsetInBytes.toLong(), data)
            }

            debugGLErrors {
                val vertexArrayBinding = IntArray(1)
                glGetIntegerv(GL_VERTEX_ARRAY_BINDING, vertexArrayBinding)

                val arrayBufferBinding = IntArray(1)
                glGetIntegerv(GL_ARRAY_BUFFER_BINDING, arrayBufferBinding)

                val isBuffer = glIsBuffer(buffer)
                when (it) {
                    GL_INVALID_OPERATION -> "zero is bound to target. (is buffer: $isBuffer, GL_VERTEX_ARRAY_BINDING: ${vertexArrayBinding[0]}, GL_ARRAY_BUFFER_BINDING: ${arrayBufferBinding[0]})"
                    GL_INVALID_VALUE -> "offset ($offsetInBytes) or size is negative, or offset+sizeoffset+size is greater than the value of GL_BUFFER_SIZE for the specified buffer object."
                    else -> null
                }
            }
        } else {
            val temp = BufferUtils.createByteBuffer(data.capacity())
            temp.put(data)
            temp.flip()
            write(temp, offsetInBytes)
        }
    }

    override fun write(source: MPPBuffer, targetByteOffset: Int, sourceByteOffset: Int, byteLength: Int) {
        source.byteBuffer.position(sourceByteOffset)
        source.byteBuffer.limit(sourceByteOffset + byteLength)
        write(source.byteBuffer, targetByteOffset)
    }

    override fun read(data: ByteBuffer, offsetInBytes: Int) {
        if (isDestroyed) {
            error("buffer is destroyed")
        }
        when (Driver.glType) {
            DriverTypeGL.GL -> {
                if (data.isDirect) {
                    if (useNamedBuffer) {
                        glGetNamedBufferSubData(buffer, offsetInBytes.toLong(), data)
                    } else {
                        bind()
                        glGetBufferSubData(GL_ARRAY_BUFFER, offsetInBytes.toLong(), data)
                        debugGLErrors()
                    }
                } else {
                    val temp = BufferUtils.createByteBuffer(data.capacity())
                    read(temp, offsetInBytes)
                    data.put(temp)
                }
            }

            DriverTypeGL.GLES -> {
                bind()
                val bufferLengthInBytes = (vertexFormat.size * vertexCount) - offsetInBytes
                val buffer = GLES30.glMapBufferRange(
                    GL_ARRAY_BUFFER,
                    offsetInBytes.toLong(),
                    bufferLengthInBytes.toLong(),
                    GL_MAP_READ_BIT
                )
                data.put(buffer)
                GLES30.glUnmapBuffer(GL_ARRAY_BUFFER)
            }
        }
    }

    override fun destroy() {
        if (!isDestroyed) {
            logger.debug {
                "destroying vertex buffer with id $buffer"
            }
            session?.untrack(this)
            isDestroyed = true
            realShadow = null
            glDeleteBuffers(buffer)
            (Driver.instance as DriverGL3).destroyVAOsForVertexBuffer(this)
            checkGLErrors()
            Session.active.untrack(this)
        }
    }

    override fun shaderStorageBufferView(): ShaderStorageBuffer {
        require(
            (Driver.instance as DriverGL3).version.isAtLeast(
                DriverVersionGL.GL_VERSION_4_3,
                DriverVersionGL.GLES_VERSION_3_1
            )
        ) {

        }
        require(vertexFormat.isInStd430Layout) { "Vertex buffer is not according to Std 430 layout rules."}
        val ssf = shaderStorageFormat {
            struct("Vertex_${vertexFormat.hashCode().toUInt()}", "vertex", vertexCount) {
                for (item in vertexFormat.items) {
                    if (item.attribute == "_") {
                        continue
                    }
                    val bpt = when (item.type) {
                        VertexElementType.INT32 -> BufferPrimitiveType.INT32
                        VertexElementType.UINT32 -> BufferPrimitiveType.UINT32
                        VertexElementType.VECTOR2_INT32 -> BufferPrimitiveType.VECTOR2_INT32
                        VertexElementType.VECTOR3_INT32 -> BufferPrimitiveType.VECTOR3_INT32
                        VertexElementType.VECTOR4_INT32 -> BufferPrimitiveType.VECTOR4_INT32
                        VertexElementType.VECTOR2_UINT32 -> BufferPrimitiveType.VECTOR2_UINT32
                        VertexElementType.VECTOR3_UINT32 -> BufferPrimitiveType.VECTOR3_UINT32
                        VertexElementType.VECTOR4_UINT32 -> BufferPrimitiveType.VECTOR4_UINT32
                        VertexElementType.FLOAT32 -> BufferPrimitiveType.FLOAT32
                        VertexElementType.VECTOR2_FLOAT32 -> BufferPrimitiveType.VECTOR2_FLOAT32
                        VertexElementType.VECTOR3_FLOAT32 -> BufferPrimitiveType.VECTOR3_FLOAT32
                        VertexElementType.VECTOR4_FLOAT32 -> BufferPrimitiveType.VECTOR4_FLOAT32
                        VertexElementType.MATRIX22_FLOAT32 -> BufferPrimitiveType.MATRIX22_FLOAT32
                        VertexElementType.MATRIX33_FLOAT32 -> BufferPrimitiveType.MATRIX33_FLOAT32
                        VertexElementType.MATRIX44_FLOAT32 -> BufferPrimitiveType.MATRIX44_FLOAT32
                        else -> error("Unsupported vertex element type: ${item.type}, it is not compatible with the STD430 layout rules.")
                    }
                    primitive(item.attribute, bpt, item.arraySize)
                }
            }
        }
        return ShaderStorageBufferGL43(buffer, false, ssf, session)
    }

    fun bind() {
        if (isDestroyed) {
            error("buffer is destroyed")
        }
        logger.trace { "binding vertex buffer $buffer" }
        glBindBuffer(GL_ARRAY_BUFFER, buffer)
        debugGLErrors()
    }

    fun unbind() {
        logger.trace { "unbinding vertex buffer" }
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        debugGLErrors()
    }
}