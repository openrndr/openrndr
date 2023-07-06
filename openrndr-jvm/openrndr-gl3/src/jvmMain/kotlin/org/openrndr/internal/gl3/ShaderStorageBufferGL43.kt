package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C.glBufferSubData
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL33C
import org.lwjgl.opengl.GL43C.*
import org.lwjgl.opengl.GL45C.*
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import java.nio.ByteBuffer

class ShaderStorageBufferGL43(val buffer: Int, override val format: ShaderStorageFormat, override val session: Session? = Session.active) : ShaderStorageBuffer {
    private var destroyed = false

    override fun clear() {
        if ((Driver.instance as DriverGL3).version >= DriverVersionGL.VERSION_4_5) {
            glClearNamedBufferData(buffer,
                GL_R8UI,
                GL30C.GL_RED_INTEGER,
                GL11C.GL_UNSIGNED_BYTE,
                intArrayOf(0) )
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


    override fun write(source: ByteBuffer, writeOffset: Int) {
        val allowed = format.size - writeOffset
        require(source.remaining() <= allowed)
        if ((Driver.instance as DriverGL3).version <= DriverVersionGL.VERSION_4_5) {
            GL33C.glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer)
            debugGLErrors()
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, writeOffset.toLong(), source)
            debugGLErrors()
            GL33C.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)
        } else {
            glNamedBufferSubData(buffer, writeOffset.toLong(), source)
            debugGLErrors()
        }
    }

    override fun read(target: ByteBuffer, readOffset: Int) {
        val needed = format.size - readOffset
        require(target.remaining() >= needed)

        GL33C.glBindBuffer(GL_SHADER_STORAGE_BUFFER, buffer)
        debugGLErrors()
        glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, readOffset.toLong(), target)
        debugGLErrors()
        GL33C.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0)
        debugGLErrors()
    }

    override fun destroy() {
        if (!destroyed) {
            GL33C.glDeleteBuffers(buffer)
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

    override fun vertexBufferView(): VertexBuffer {
        val vertexFormat = shaderStorageFormatToVertexFormat(this.format)
        val vertexCount = this.format.elements.first().arraySize
        return VertexBufferGL3(this.buffer, vertexFormat, vertexCount, this.session)
    }

    companion object {
        fun create(format: ShaderStorageFormat, session: Session?) : ShaderStorageBufferGL43 {
            val ssbo = GL33C.glGenBuffers()
            GL33C.glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
            checkGLErrors()

            val useBufferStorage = (Driver.instance as DriverGL3).version >= DriverVersionGL.VERSION_4_4

            if (useBufferStorage) {
                glBufferStorage(GL_SHADER_STORAGE_BUFFER, format.size.toLong(), GL_DYNAMIC_STORAGE_BIT)
            } else {
                GL33C.glBufferData(GL_SHADER_STORAGE_BUFFER, format.size.toLong(), GL33C.GL_DYNAMIC_COPY)
            }
            checkGLErrors()
            return ShaderStorageBufferGL43(ssbo, format, session)
        }
    }
}