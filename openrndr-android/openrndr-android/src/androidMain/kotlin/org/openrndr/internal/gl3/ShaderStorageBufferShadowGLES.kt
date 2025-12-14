package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.BufferAlignment
import org.openrndr.draw.BufferReader
import org.openrndr.draw.BufferWriter
import org.openrndr.draw.ByteBufferReader
import org.openrndr.draw.ShaderStorageBufferShadow
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val logger = KotlinLogging.logger {}

class ShaderStorageBufferShadowGLES(
    override val shaderStorageBuffer: ShaderStorageBufferGLES,
) : ShaderStorageBufferShadow {

    val buffer: ByteBuffer =
        ByteBuffer.allocateDirect(shaderStorageBuffer.vbo.sizeInBytes).order(ByteOrder.nativeOrder())

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

    override fun writer(): BufferWriter {
        buffer.rewind()
        return BufferWriterGLES(
            buffer,
            shaderStorageBuffer.format.size,
            BufferAlignment.STD430,
            shaderStorageBuffer.format.elementSequence().iterator()
        )
    }

    override fun close() {
        destroy()
    }

    override fun reader(): BufferReader {
        buffer.rewind()
        return ByteBufferReader(
            buffer,
            BufferAlignment.STD430,
            shaderStorageBuffer.format.elementSequence().iterator()
        )
    }
}