package org.openrndr.draw

actual interface ShaderStorageBuffer: AutoCloseable {
    actual val session: Session?
    actual val format: ShaderStorageFormat
    actual fun clear()
    actual fun destroy()
    actual val shadow: ShaderStorageBufferShadow
    actual fun put(
        elementOffset: Int,
        putter: BufferWriterStd430.() -> Unit
    ): Int

    actual fun vertexBufferView(elementName: String?): VertexBuffer
}