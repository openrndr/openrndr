package org.openrndr.draw

import org.khronos.webgl.Float32Array
import org.openrndr.internal.Driver

actual abstract class VertexBuffer {
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
        w.rewind()
        w.putter()
        val count = w.positionElements

        shadow.upload(0, w.position * 4)
        w.rewind()
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

    abstract fun write(data: FloatArray, offset:Int, floatCount: Int)
    abstract fun write(data: Float32Array, offset:Int, floatCount : Int)

}