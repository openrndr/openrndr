package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C.glBufferSubData
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL33C
import org.lwjgl.opengl.GL43C
import org.lwjgl.opengl.GL43C.*
import org.openrndr.draw.*
import java.nio.ByteBuffer

class ShaderStorageBufferGL43(val buffer: Int, override val format: ShaderStorageFormat, override val session: Session? = Session.active) : ShaderStorageBuffer {

    private var destroyed = false

    override fun bind(base: Int) {
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, base, buffer)
        debugGLErrors()
    }

    override fun clear() {
        GL33C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, buffer)
        glClearBufferData(GL_SHADER_STORAGE_BUFFER, GL_R8UI, GL30C.GL_RED_INTEGER, GL11C.GL_UNSIGNED_BYTE, intArrayOf(0))


    }


    override fun write(source: ByteBuffer, writeOffset: Int) {
        val allowed = format.size - writeOffset
        require(source.remaining() <= allowed)
        GL33C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, buffer)
        debugGLErrors()
        glBufferSubData(GL_SHADER_STORAGE_BUFFER, writeOffset.toLong(), source)
        debugGLErrors()
        GL33C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0)
    }

    override fun read(target: ByteBuffer, readOffset: Int) {
        val needed = format.size - readOffset
        require(target.remaining() >= needed)

        GL33C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, buffer)
        debugGLErrors()
        GL43C.glGetBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, readOffset.toLong(), target)
        debugGLErrors()
        GL33C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0)
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
    companion object {
        fun create(format: ShaderStorageFormat, session: Session?) : ShaderStorageBufferGL43 {
            val ssbo = GL33C.glGenBuffers()
            GL33C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, ssbo)
            checkGLErrors()
            GL33C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, format.size.toLong(), GL33C.GL_STREAM_DRAW)
            checkGLErrors()
            return ShaderStorageBufferGL43(ssbo, format, session)
        }
    }

}