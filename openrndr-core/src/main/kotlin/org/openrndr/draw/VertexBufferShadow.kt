package org.openrndr.draw

interface VertexBufferShadow {
    val vertexBuffer: VertexBuffer

    fun upload(offset: Int = 0, size: Int = vertexBuffer.vertexCount * vertexBuffer.vertexFormat.size)
    fun uploadElements(elementOffset: Int = 0, elementCount: Int = vertexBuffer.vertexCount) {
        upload(elementOffset * vertexBuffer.vertexFormat.size, elementCount * vertexBuffer.vertexFormat.size)
    }

    fun download()
    fun destroy()
    fun writer(): BufferWriter
}