package org.openrndr.draw

import org.openrndr.internal.Driver
import java.nio.ByteBuffer

interface VertexBuffer {
    companion object {
        fun createDynamic(format: VertexFormat, vertexCount: Int): VertexBuffer = Driver.instance.createDynamicVertexBuffer(format, vertexCount)
        //fun createStatic(format: VertexFormat, buffer:Buffer):VertexBuffer
    }

    val vertexFormat: VertexFormat
    val vertexCount: Int

    val shadow: VertexBufferShadow

    fun write(data: ByteBuffer, offset: Int = 0)
    fun read(data: ByteBuffer, offset: Int = 0)

    fun destroy()

    fun put(putter: BufferWriter.() -> Unit): Int {
        val w = shadow.writer()
        w.rewind()
        w.putter()
        if (w.position % vertexFormat.size != 0) {
            throw RuntimeException("incomplete vertices written. likely violating the specified vertex format $vertexFormat")
        }
        val count = w.positionElements
        shadow.uploadElements(0, count)
        w.rewind()
        return count
    }
}

fun vertexBuffer(vertexFormat: VertexFormat, vertexCount: Int): VertexBuffer {
    return VertexBuffer.createDynamic(vertexFormat, vertexCount)
}
