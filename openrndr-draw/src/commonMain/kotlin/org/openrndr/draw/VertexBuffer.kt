package org.openrndr.draw

import org.openrndr.utils.buffer.MPPBuffer


/**
 * Represents a vertex buffer that stores vertex data in GPU memory and allows for efficient
 * rendering operations. The vertex buffer conforms to a specific vertex format and supports
 * various operations for writing, managing, and releasing vertex data resources.
 */
expect abstract class VertexBuffer : AutoCloseable {
    abstract val session: Session?

    /**
     * Specifies the format of vertex data stored in the vertex buffer.
     *
     * This property determines the layout and attributes of the vertex data, including the organization,
     * types, and order of components such as positions, normals, colors, texture coordinates,
     * and any custom attributes. It ensures that the vertex data conforms to a consistent structure
     * required for rendering or processing.
     */
    abstract val vertexFormat: VertexFormat

    /**
     * The number of vertices stored in this vertex buffer.
     *
     * This property determines the total count of vertices available for rendering or processing
     * within the buffer.
     */
    abstract val vertexCount: Int

    /**
     * Gives a read/write shadow for the vertex buffer
     */
    abstract val shadow: VertexBufferShadow

    /**
     * Destroy the vertex buffer
     */
    abstract fun destroy()

    /**
     * Writes data to the vertex buffer using the provided [BufferWriter] starting at the specified element offset.
     *
     * @param elementOffset The element offset at which to start writing data. Defaults to 0.
     * @param putter A lambda function where data can be written using the [BufferWriter].
     * @return The number of elements written to the vertex buffer.
     */
    fun put(elementOffset: Int = 0, putter: BufferWriter.() -> Unit): Int

    abstract fun write(
        source: MPPBuffer,
        targetByteOffset: Int = 0,
        sourceByteOffset: Int = 0,
        byteLength: Int = source.capacity()
    )

    companion object {
        fun createDynamic(format: VertexFormat, vertexCount: Int, session: Session? = Session.active): VertexBuffer
        fun createFromFloats(format: VertexFormat, data: FloatArray, session: Session?): VertexBuffer
    }
}

/**
 * VertexBuffer builder function.
 * @param vertexFormat a VertexFormat object that describes the vertex layout
 * @param vertexCount the number of vertices the vertex buffer should hold
 */
fun vertexBuffer(vertexFormat: VertexFormat, vertexCount: Int, session: Session? = Session.active): VertexBuffer {
    return VertexBuffer.createDynamic(vertexFormat, vertexCount, session)
}