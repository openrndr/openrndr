package org.openrndr.draw


/**
 * Represents the type of index based on its size in bytes.
 *
 * This enum class is used to define index types with their respective size in bytes.
 *
 * @property sizeInBytes The size of the index type in bytes.
 */
enum class IndexType(val sizeInBytes:Int) {
    INT16(2),
    INT32(4)
}

/**
 * Represents an abstraction of an index buffer used in rendering operations.
 *
 * Index buffers are used to store indices that define the order in which vertices
 * are processed by the rendering pipeline. This interface provides functionality to
 * manage and destroy an index buffer, as well as query its properties.
 */
expect interface IndexBuffer: AutoCloseable {
    companion object {
        fun createDynamic(elementCount: Int, type: IndexType): IndexBuffer
    }

    val indexCount: Int
    val type: IndexType
    fun destroy()
}


fun indexBuffer(elementCount: Int, type: IndexType): IndexBuffer {
    return IndexBuffer.createDynamic(elementCount, type)
}

