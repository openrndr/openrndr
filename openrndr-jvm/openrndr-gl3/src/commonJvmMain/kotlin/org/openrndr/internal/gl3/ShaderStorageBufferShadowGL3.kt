package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
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

    override fun writer(): BufferWriter {
        buffer.rewind()
        return BufferWriterGL3(buffer,  shaderStorageBuffer.format.size, BufferAlignment.STD430, shaderStorageBuffer.format.elementSequence().iterator())
    }

    override fun close() {
        destroy()
    }

    override fun reader(): BufferReader {
        buffer.rewind()
        return ByteBufferReader(buffer, BufferAlignment.STD430, shaderStorageBuffer.format.elementSequence().iterator())
    }
}

