package org.openrndr.draw

/**
 * Represents a shadow interface for managing a shader storage buffer.
 * The shadow allows operations such as uploading, downloading, and releasing resources,
 * as well as providing a means to obtain a writer for structured buffer data manipulation.
 */
interface ShaderStorageBufferShadow: AutoCloseable {
    val shaderStorageBuffer: ShaderStorageBuffer

    fun upload(offset: Int = 0, size: Int = shaderStorageBuffer.format.size)
    fun uploadElements(elementOffset: Int = 0, elementCount: Int = shaderStorageBuffer.format.elements.size) {
        upload(elementOffset * shaderStorageBuffer.format.size, elementCount * shaderStorageBuffer.format.size)
    }

    fun download()
    fun destroy()
    fun writer(): BufferWriter
    fun reader(): BufferReader
}