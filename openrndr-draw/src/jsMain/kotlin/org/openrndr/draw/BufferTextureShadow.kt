package org.openrndr.draw

actual abstract class BufferTextureShadow: AutoCloseable {
    actual abstract val bufferTexture: BufferTexture
    actual abstract fun upload(offset: Int, sizeInBytes: Int)
    actual abstract fun download()
    actual abstract fun destroy()

}