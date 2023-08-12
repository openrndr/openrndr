package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.utils.buffer.MPPBuffer
import java.nio.ByteBuffer

actual abstract class VertexBuffer {
    actual abstract val session: Session?

    actual companion object {
        actual fun createDynamic(format: VertexFormat, vertexCount: Int, session: Session?): VertexBuffer {
            return Driver.instance.createDynamicVertexBuffer(format, vertexCount, session)
        }

        actual fun createFromFloats(format: VertexFormat, data: FloatArray, session: Session?): VertexBuffer {
            require((data.size * 4) % format.size == 0) {
                "supplied data size doesn't match format size"
            }
            val vertexBuffer = createDynamic(format, (data.size * 4) / format.size, session)
            vertexBuffer.put(0) {
                write(data, 0, data.size)
            }
            return vertexBuffer
        }
    }


    abstract fun write(data: ByteBuffer, offsetInBytes: Int = 0)
    abstract fun read(data: ByteBuffer, offsetInBytes: Int = 0)

    actual fun put(elementOffset: Int, putter: BufferWriter.() -> Unit): Int {
        val w = shadow.writer()
        w.rewind()
        w.positionElements = elementOffset
        w.putter()
        if (w.position % vertexFormat.size != 0) {
            throw RuntimeException("incomplete vertices written at ${w.position}. likely violating the specified vertex format $vertexFormat")
        }
        val count = w.positionElements - elementOffset
        shadow.uploadElements(elementOffset, count)
        w.rewind()
        return count
    }

    actual abstract val vertexFormat: VertexFormat
    actual abstract val vertexCount: Int

    /**
     * Gives a read/write shadow for the vertex buffer
     */
    actual abstract val shadow: VertexBufferShadow

    /**
     * Destroy the vertex buffer
     */
    actual abstract fun destroy()
    actual abstract fun write(
        source: MPPBuffer,
        targetByteOffset: Int,
        sourceByteOffset: Int,
        byteLength: Int
    )
}
