package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL33C
import org.lwjgl.opengl.GL45C.*
import org.lwjgl.opengles.GLES30
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import java.nio.ByteBuffer

class ShaderStorageBufferGL43(
    val buffer: Int,
    override val format: ShaderStorageFormat,
    override val session: Session? = Session.active
) : ShaderStorageBuffer {
    private var destroyed = false

    override fun clear() {
        when (Driver.glType) {
            DriverTypeGL.GL -> {
                if ((Driver.instance as DriverGL3).version >= DriverVersionGL.GL_VERSION_4_5) {
                    glClearNamedBufferData(
                        buffer,
                        GL_R8UI,
                        GL30C.GL_RED_INTEGER,
                        GL11C.GL_UNSIGNED_BYTE,
                        intArrayOf(0)
                    )
                } else {
                    GL33C.glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer)
                    glClearBufferData(
                        GL_SHADER_STORAGE_BUFFER,
                        GL_R8UI,
                        GL30C.GL_RED_INTEGER,
                        GL11C.GL_UNSIGNED_BYTE,
                        intArrayOf(0)
                    )
                }
            }

            DriverTypeGL.GLES -> {
                error("not supported")
            }
        }
    }


    override fun write(source: ByteBuffer, writeOffset: Int) {
        val allowed = format.size - writeOffset
        require(source.remaining() <= allowed)
        if (Driver.glType == DriverTypeGL.GL && Driver.glVersion <= DriverVersionGL.GL_VERSION_4_5 ||
            Driver.glType == DriverTypeGL.GLES
        ) {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer)
            debugGLErrors()
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, writeOffset.toLong(), source)
            debugGLErrors()
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)
        } else {
            glNamedBufferSubData(buffer, writeOffset.toLong(), source)
            debugGLErrors()
        }
    }

    override fun read(target: ByteBuffer, readOffset: Int) {
        val needed = format.size - readOffset
        require(target.remaining() >= needed) {
            "target buffer remaining bytes: ${target.remaining()}, need ${needed} bytes"
        }

        when (Driver.glType) {
            DriverTypeGL.GL -> {
                glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer)
                debugGLErrors()
                glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, readOffset.toLong(), target)
                debugGLErrors()
                glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)
                debugGLErrors()
            }

            DriverTypeGL.GLES -> {
                glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer)
                debugGLErrors()
                val mappedBuffer = GLES30.glMapBufferRange(
                    GL_SHADER_STORAGE_BUFFER,
                    readOffset.toLong(),
                    target.remaining().toLong(),
                    GL_MAP_READ_BIT
                )
                checkGLErrors()
                require(mappedBuffer != null)
                target.put(mappedBuffer)
                GLES30.glUnmapBuffer(GL_SHADER_STORAGE_BUFFER)
                glBindBuffer(GL_COPY_READ_BUFFER, 0)
                debugGLErrors()
            }
        }
    }

    override fun destroy() {
        if (!destroyed) {
            glDeleteBuffers(buffer)
            session?.untrack(this)
        }
    }

    override fun put(elementOffset: Int, putter: BufferWriterStd430.() -> Unit): Int {
        val w = shadow.writer()
        w.rewind()
        w.positionElements = elementOffset
        w.putter()
        if (w.position % format.size != 0) {
            throw RuntimeException("incomplete members written. likely violating the specified shaders storage format $format")
        }
        val count = w.positionElements
        shadow.uploadElements(elementOffset, count)
        w.rewind()
        return count
    }


    internal var realShadow: ShaderStorageBufferShadowGL3? = null
    override val shadow: ShaderStorageBufferShadow
        get() {
            if (destroyed) {
                throw IllegalStateException("buffer is destroyed")
            }
            if (realShadow == null) {
                realShadow = ShaderStorageBufferShadowGL3(this)
            }
            return realShadow!!
        }

    private fun elementPosition(elementName: String): Long {
        var position = 0L
        for (element in format.elements) {
            if (element.name == elementName) {
                break
            } else {
                if (element is ShaderStorageStruct) {
                    TODO("struct sizes not implemented yet")
                } else {
                    val s = when (val t = (element as ShaderStoragePrimitive).type) {
                        BufferPrimitiveType.VECTOR3_FLOAT32, BufferPrimitiveType.VECTOR3_INT32,
                        BufferPrimitiveType.VECTOR4_FLOAT32, BufferPrimitiveType.VECTOR4_INT32 -> 16

                        BufferPrimitiveType.VECTOR2_FLOAT32, BufferPrimitiveType.VECTOR2_INT32 -> 8
                        BufferPrimitiveType.FLOAT32, BufferPrimitiveType.INT32 -> 4
                        else -> TODO("size not implemented for $t")
                    }
                    position += s * element.arraySize.coerceAtLeast(1)
                }
            }
        }
        return position
    }

    override fun vertexBufferView(elementName: String?): VertexBuffer {
        val vertexFormat = shaderStorageFormatToVertexFormat(this.format, elementName)
        val vertexCount = this.format.elements.first().arraySize
        val offset = if (vertexFormat.items.first().attribute == this.format.elements.first().name) 0 else {
            elementPosition(elementName!!)
        }
        return VertexBufferGL3(this.buffer, offset, vertexFormat, vertexCount, this.session)
    }

    companion object {
        fun create(format: ShaderStorageFormat, session: Session?): ShaderStorageBufferGL43 {
            val ssbo = glGenBuffers()
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
            checkGLErrors()

            val useBufferStorage =
                (Driver.instance as DriverGL3).version >= DriverVersionGL.GL_VERSION_4_4 && Driver.glType == DriverTypeGL.GL

            if (useBufferStorage) {
                glBufferStorage(GL_SHADER_STORAGE_BUFFER, format.size.toLong(), GL_DYNAMIC_STORAGE_BIT)
            } else {
                glBufferData(GL_SHADER_STORAGE_BUFFER, format.size.toLong(), GL33C.GL_DYNAMIC_COPY)
            }
            checkGLErrors()
            return ShaderStorageBufferGL43(ssbo, format, session)
        }
    }
}