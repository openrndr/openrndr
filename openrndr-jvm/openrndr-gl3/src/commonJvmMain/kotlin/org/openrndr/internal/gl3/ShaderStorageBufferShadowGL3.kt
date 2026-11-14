package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.*
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val logger = KotlinLogging.logger {}

/** the largest number of elements of a format that a shadow keeps in a list */
private const val MAX_LISTED_ELEMENTS = 1 shl 16

class ShaderStorageBufferShadowGL3(override val shaderStorageBuffer: ShaderStorageBufferGL43) : ShaderStorageBufferShadow {
    val buffer: ByteBuffer =
        BufferUtils.createByteBuffer(shaderStorageBuffer.format.size).apply {
            order(ByteOrder.nativeOrder())
        }

    override fun upload(offset: Int, size: Int) {
        logger.traceIfEnabled { "uploading shadow to shader storage buffer" }
        (buffer as Buffer).rewind()
        (buffer as Buffer).position(offset)
        (buffer as Buffer).limit(offset + size)
        shaderStorageBuffer.write(buffer, offset)
        (buffer as Buffer).limit(buffer.capacity())
    }

    override fun download() {
        (buffer as Buffer).rewind()
        shaderStorageBuffer.read(buffer)
    }

    override fun destroy() {
        shaderStorageBuffer.realShadow = null
    }

    /**
     * The elements of the format in the order they are written, kept as a list for formats of up to
     * [MAX_LISTED_ELEMENTS] elements, the sequence that makes them suspends for every element
     */
    private val listedElements: List<ShaderStorageElement>? by lazy(LazyThreadSafetyMode.NONE) {
        val elements = shaderStorageBuffer.format.elementSequence().take(MAX_LISTED_ELEMENTS + 1).toList()
        if (elements.size <= MAX_LISTED_ELEMENTS) elements else null
    }

    private fun elementIterator(): Iterator<ShaderStorageElement> =
        listedElements?.iterator() ?: shaderStorageBuffer.format.elementSequence().iterator()

    override fun writer(): BufferWriter {
        buffer.rewind()
        return BufferWriterGL3(buffer, shaderStorageBuffer.format.size, BufferAlignment.STD430, elementIterator())
    }

    override fun close() {
        destroy()
    }

    override fun reader(): BufferReader {
        buffer.rewind()
        return ByteBufferReader(buffer, BufferAlignment.STD430, elementIterator())
    }
}

