package org.openrndr.draw

actual interface BufferTextureShadow {
    actual val bufferTexture: BufferTexture
    actual fun upload(offset: Int, sizeInBytes: Int)
    actual fun download()
    actual fun destroy()

}