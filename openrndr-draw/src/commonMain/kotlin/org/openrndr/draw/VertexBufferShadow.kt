package org.openrndr.draw

/**
 * Represents a shadow of a [VertexBuffer] that allows manipulation and synchronization of vertex data
 * with a buffer residing in GPU memory. This interface provides methods to upload, download, and manage
 * vertex data in a buffer.
 */
interface VertexBufferShadow : AutoCloseable {
    /**
     * The underlying GPU-resident vertex buffer that serves as the data source for this shadow buffer.
     */
    val vertexBuffer: VertexBuffer

    /**
     * Uploads vertex data to the GPU memory from the shadow buffer.
     *
     * @param offsetInBytes The byte offset in the buffer at which the upload will begin. Defaults to 0.
     * @param sizeInBytes The size of the data, in bytes, to upload. Defaults to the total size based on the vertex count
     *                    and vertex format of the associated [VertexBuffer].
     */
    fun upload(offsetInBytes: Int = 0, sizeInBytes: Int = vertexBuffer.vertexCount * vertexBuffer.vertexFormat.size)

    /**
     * Uploads a specified range of vertices from the shadow buffer to the GPU memory.
     *
     * @param elementOffset The offset, in number of elements, from the beginning of the buffer at which the upload starts. Defaults to 0.
     * @param elementCount The number of elements to upload from the buffer. Defaults to the total vertex count of the associated [VertexBuffer].
     */
    fun uploadElements(elementOffset: Int = 0, elementCount: Int = vertexBuffer.vertexCount) {
        upload(elementOffset * vertexBuffer.vertexFormat.size, elementCount * vertexBuffer.vertexFormat.size)
    }

    /**
     * Downloads vertex data from the GPU memory into the shadow buffer. This method synchronizes
     * the shadow buffer with the current state of the associated GPU-resident [VertexBuffer].
     * Typically used to retrieve the vertex data for inspection or modification.
     */
    fun download()
    fun destroy()

    /**
     * Creates and returns a [BufferWriter] to facilitate writing operations to the shadow buffer.
     * The returned [BufferWriter] allows structured data, such as vectors or matrices, to be written
     * into the buffer for subsequent GPU upload or processing.
     *
     * @return a [BufferWriter] instance for writing data to the shadow buffer
     */
    fun writer(): BufferWriter
}