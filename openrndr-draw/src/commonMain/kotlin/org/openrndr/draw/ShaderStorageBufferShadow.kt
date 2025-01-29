package org.openrndr.draw

/**
 * Represents a shadow interface for managing a shader storage buffer.
 * The shadow allows operations such as uploading, downloading, and releasing resources,
 * as well as providing a means to obtain a writer for structured buffer data manipulation.
 */
interface ShaderStorageBufferShadow: AutoCloseable {
    val shaderStorageBuffer: ShaderStorageBuffer

    /**
     * Uploads data to the shader storage buffer.
     * This method allows specifying the offset and size of the data to be uploaded.
     *
     * @param offset The starting offset within the buffer where the upload begins. Defaults to 0.
     * @param size The size of the data to be uploaded, in bytes. Defaults to the size of the buffer's format.
     */
    fun upload(offset: Int = 0, size: Int = shaderStorageBuffer.format.size)
    fun uploadElements(elementOffset: Int = 0, elementCount: Int = shaderStorageBuffer.format.elements.size) {
        upload(elementOffset * shaderStorageBuffer.format.size, elementCount * shaderStorageBuffer.format.size)
    }

    /**
     * Downloads the data from the shader storage buffer to the host.
     * This operation retrieves the current content of the buffer, making it accessible for local processing.
     * Ensure any necessary synchronization is handled to maintain data integrity during the transfer.
     */
    fun download()
    fun destroy()


    /**
     * Provides a writer for structured manipulation of buffer data in the shader storage buffer.
     *
     * @return an instance of BufferWriter, allowing data to be written into the associated buffer
     */
    fun writer(): BufferWriter


    /**
     * Provides a reader for structured reading of buffer data from the shader storage buffer.
     *
     * @return an instance of BufferReader, enabling data to be read from the associated buffer
     */
    fun reader(): BufferReader
}