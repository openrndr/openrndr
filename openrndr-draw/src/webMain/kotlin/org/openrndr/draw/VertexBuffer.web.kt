package org.openrndr.draw

import js.buffer.ArrayBuffer
import js.typedarrays.Float32Array
import org.openrndr.internal.Driver
import org.openrndr.utils.buffer.MPPBuffer

actual abstract class VertexBuffer : AutoCloseable {
    actual abstract val session: Session?
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
    actual fun put(elementOffset: Int, putter: BufferWriter.() -> Unit): Int {
        val w = shadow.writer()
        w.positionElements = elementOffset
        w.putter()
        // *4 is because the underlying buffer is 4-byte floats
        if (w.position * 4 % vertexFormat.size != 0) {
            throw RuntimeException("incomplete vertices written at ${w.position}. likely violating the specified vertex format $vertexFormat")
        }
        val count = w.positionElements - elementOffset
        shadow.uploadElements(elementOffset, count)
        return count
    }

    actual companion object {
        actual fun createDynamic(
            format: VertexFormat,
            vertexCount: Int,
            session: Session?
        ): VertexBuffer {
            return Driver.instance.createDynamicVertexBuffer(format, vertexCount, session)
        }

        actual fun createFromFloats(
            format: VertexFormat,
            data: FloatArray,
            session: Session?
        ): VertexBuffer {
            TODO("Not yet implemented")
        }
    }

    abstract fun write(data: FloatArray, offsetBytes: Int, floatCount: Int)
    abstract fun write(
        data: Float32Array<ArrayBuffer>,
        offsetBytes: Int,
        floatCount: Int
    )
    actual abstract fun write(
        source: MPPBuffer,
        targetByteOffset: Int,
        sourceByteOffset: Int,
        byteLength: Int
    )

}