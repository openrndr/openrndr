package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C.glBufferSubData
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL33C
import org.lwjgl.opengl.GL43C
import org.lwjgl.opengl.GL43C.*
import org.openrndr.draw.*
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val logger = KotlinLogging.logger {}

class ShaderStorageBufferShadowGL3(override val shaderStorageBuffer: ShaderStorageBufferGL43) : ShaderStorageBufferShadow {
    val buffer: ByteBuffer =
        BufferUtils.createByteBuffer(shaderStorageBuffer.format.size).apply {
            order(ByteOrder.nativeOrder())
        }

    override fun upload(offset: Int, size: Int) {
        logger.trace { "uploading shadow to shader storage buffer" }
        (buffer as Buffer).rewind()
        (buffer as Buffer).position(offset)
        (buffer as Buffer).limit(offset + size)
        shaderStorageBuffer.write(buffer)
        (buffer as Buffer).limit(buffer.capacity())
    }

    override fun download() {
        (buffer as Buffer).rewind()
        shaderStorageBuffer.read(buffer)
    }

    override fun destroy() {
        shaderStorageBuffer.realShadow = null
    }

    override fun writer(): BufferWriterStd430 {
        return BufferWriterStd430GL3(buffer, shaderStorageBuffer.format.members, shaderStorageBuffer.format.size)
    }
}

class ShaderStorageBufferGL43(val buffer: Int, override val format: ShaderStorageFormat, override val session: Session? = Session.active) : ShaderStorageBuffer {

    internal var realShadow: ShaderStorageBufferShadowGL3? = null
    private var destroyed = false

    override fun bind(base: Int) {
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, base, buffer)
        debugGLErrors()
    }

    override fun clear() {
        GL33C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, buffer)
        glClearBufferData(GL_SHADER_STORAGE_BUFFER, GL_R8UI, GL30C.GL_RED_INTEGER, GL11C.GL_UNSIGNED_BYTE, intArrayOf(0))
    }

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
