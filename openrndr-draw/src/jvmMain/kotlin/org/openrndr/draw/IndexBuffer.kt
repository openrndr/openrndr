package org.openrndr.draw

import org.openrndr.internal.Driver
import java.nio.ByteBuffer


actual interface IndexBuffer {
    actual companion object {
        actual fun createDynamic(elementCount: Int, type: IndexType): IndexBuffer = Driver.instance.createDynamicIndexBuffer(elementCount, type, session = Session.active)
        //fun createStatic(format: VertexFormat, buffer:Buffer):VertexBuffer
    }

    fun write(data: ByteBuffer, offset: Int = 0)
    fun read(data: ByteBuffer, offset: Int = 0)
    actual val indexCount: Int
    actual val type: IndexType
    actual fun destroy()
}
