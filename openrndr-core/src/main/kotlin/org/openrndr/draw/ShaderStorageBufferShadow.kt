package org.openrndr.draw

interface ShaderStorageBufferShadow {
    val shaderStorageBuffer: ShaderStorageBuffer

    fun upload(offset: Int = 0, size: Int = shaderStorageBuffer.format.size)
    fun uploadElements(elementOffset: Int = 0, elementCount: Int = shaderStorageBuffer.format.members.size) {
        upload(elementOffset * shaderStorageBuffer.format.size, elementCount * shaderStorageBuffer.format.size)
    }

    fun download()
    fun destroy()
    fun writer(): BufferWriterStd430
}
