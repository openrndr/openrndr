package org.openrndr.draw

expect abstract class BufferTextureShadow: AutoCloseable {
    abstract val bufferTexture: BufferTexture

    //abstract fun upload(offset: Int = 0, sizeInBytes: Int = bufferTexture.elementCount * bufferTexture.format.componentCount * bufferTexture.type.componentSize )
    // TODO restore default arguments when https://youtrack.jetbrains.com/issue/KT-45542 is fixed
    abstract fun upload(offset: Int = 0, sizeInBytes : Int)

    abstract fun download()
    abstract fun destroy()

}