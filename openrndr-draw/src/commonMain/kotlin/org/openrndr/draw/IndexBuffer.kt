package org.openrndr.draw


enum class IndexType(val sizeInBytes:Int) {
    INT16(2),
    INT32(4)
}

expect interface IndexBuffer {
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

