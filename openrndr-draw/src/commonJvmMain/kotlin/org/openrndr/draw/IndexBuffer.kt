package org.openrndr.draw

import org.openrndr.internal.Driver
import java.nio.ByteBuffer


actual interface IndexBuffer: AutoCloseable {
    actual companion object {
        actual fun createDynamic(elementCount: Int, type: IndexType): IndexBuffer = Driver.instance.createDynamicIndexBuffer(elementCount, type, session = Session.active)
        //fun createStatic(format: VertexFormat, buffer:Buffer):VertexBuffer
    }

    /**
     * Writes the provided data to the buffer at the specified offset.
     *
     * @param data the ByteBuffer containing the data to be written.
     * @param offsetInBytes the offset in bytes at which the data will be written. Defaults to 0.
     */
    fun write(data: ByteBuffer, offsetInBytes: Int = 0)
    /**
     * Reads data from the buffer into the provided ByteBuffer starting at the specified offset.
     *
     * @param data the ByteBuffer into which data will be read.
     * @param offsetInBytes the offset in bytes from which to start reading. Defaults to 0.
     */
    fun read(data: ByteBuffer, offsetInBytes: Int = 0)
    actual val indexCount: Int
    actual val type: IndexType

    fun shaderStorageBufferView() : ShaderStorageBuffer
    actual fun destroy()
    actual val session: Session?
}
