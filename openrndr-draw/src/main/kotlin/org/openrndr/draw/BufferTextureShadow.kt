package org.openrndr.draw

interface BufferTextureShadow {
    val bufferTexture: BufferTexture

    fun upload(offset: Int = 0, sizeInBytes: Int = bufferTexture.elementCount * bufferTexture.format.componentCount * bufferTexture.type.componentSize )
    fun download()
    fun destroy()

    fun writer(): BufferWriter
}
