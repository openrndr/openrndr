package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33C.*
import org.openrndr.draw.*
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val logger = KotlinLogging.logger {}

class BufferTextureShadowGL3(override val bufferTexture: BufferTextureGL3) : BufferTextureShadow {
    val buffer: ByteBuffer =
            BufferUtils.createByteBuffer(bufferTexture.elementCount * bufferTexture.type.componentSize * bufferTexture.format.componentCount).apply {
                order(ByteOrder.nativeOrder())
            }

    override fun upload(offset: Int, sizeInBytes: Int) {
        logger.trace { "uploading shadow to buffer texture" }
        (buffer as Buffer).rewind()
        (buffer as Buffer).position(offset)
        (buffer as Buffer).limit(sizeInBytes)
        bufferTexture.write(buffer)
        (buffer as Buffer).limit(buffer.capacity())
    }

    override fun download() {
        TODO("not implemented")
    }

    override fun destroy() {
        bufferTexture.realShadow = null
    }

    override fun writer(): BufferWriter {
        return BufferWriterGL3(buffer, bufferTexture.format.componentCount * bufferTexture.type.componentSize)
    }
}

class BufferTextureGL3(val texture: Int, val buffer: Int, override val elementCount: Int, override val format: ColorFormat, override val type: ColorType, override val session: Session?) : BufferTexture {
    companion object {
        fun create(elementCount: Int, format: ColorFormat, type: ColorType, session: Session?): BufferTextureGL3 {
            val sizeInBytes = format.componentCount * type.componentSize * elementCount
            val buffers = IntArray(1)
            glGenBuffers(buffers)
            glBindBuffer(GL_TEXTURE_BUFFER, buffers[0])
            glBufferData(GL_TEXTURE_BUFFER, sizeInBytes.toLong(), GL_STREAM_DRAW)

            val textures = IntArray(1)
            glGenTextures(textures)
            glBindTexture(GL_TEXTURE_BUFFER, textures[0])
            glTexBuffer(GL_TEXTURE_BUFFER, internalFormat(format, type).first, buffers[0])

            glBindBuffer(GL_TEXTURE_BUFFER, 0)

            return BufferTextureGL3(textures[0], buffers[0], elementCount, format, type, session)
        }
    }

    internal fun glFormat(): Int {
        return internalFormat(format, type).first
    }

    internal var realShadow: BufferTextureShadowGL3? = null
    override val shadow: BufferTextureShadow
        get() {
            if (realShadow == null) {
                realShadow = BufferTextureShadowGL3(this)
            }
            return realShadow!!
        }


    override fun read(target: ByteBuffer, offset: Int, elementReadCount: Int) {
        val oldLimit = target.limit()
        target.limit(target.position() + elementReadCount * format.componentCount * type.componentSize)
        glBindBuffer(GL_TEXTURE_BUFFER, this.buffer)
        glGetBufferSubData(GL_TEXTURE_BUFFER, 0, target)
        target.limit(oldLimit)
    }

    override fun write(source: ByteBuffer, offset: Int, elementWriteCount: Int) {
        require(source.isDirect)
        val oldLimit = source.limit()
        source.limit(source.position() + elementWriteCount * format.componentCount * type.componentSize)
        glBindBuffer(GL_TEXTURE_BUFFER, this.buffer)
        debugGLErrors()
        glBufferSubData(GL_TEXTURE_BUFFER, 0L, source)
        debugGLErrors()
        source.limit(oldLimit)
    }

    override fun bind(unit: Int) {
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(GL_TEXTURE_BUFFER, texture)
    }

    internal var destroyed = false
    override fun destroy() {
        if (!destroyed) {
            session?.untrack(this)
            glDeleteTextures(texture)
            destroyed = true
        }
    }
}