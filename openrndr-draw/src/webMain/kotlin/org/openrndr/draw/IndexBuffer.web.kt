package org.openrndr.draw

actual interface IndexBuffer: AutoCloseable {
    actual companion object {
        actual fun createDynamic(
            elementCount: Int,
            type: IndexType
        ): IndexBuffer {
            TODO("Not yet implemented")
        }
    }

    actual val indexCount: Int
    actual val type: IndexType
    actual fun destroy()
    actual val session: Session?
}