package org.openrndr.draw

expect interface BufferTextureShadow {
    val bufferTexture: BufferTexture

    fun upload(offset: Int = 0, sizeInBytes: Int = bufferTexture.elementCount * bufferTexture.format.componentCount * bufferTexture.type.componentSize )
    fun download()
    fun destroy()

}