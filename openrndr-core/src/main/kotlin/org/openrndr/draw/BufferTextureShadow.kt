package org.openrndr.draw

interface BufferTextureShadow {
    val bufferTexture: BufferTexture

    fun upload(offset: Int, size: Int)
    fun download()
    fun destroy()

    fun writer(): BufferWriter
}
