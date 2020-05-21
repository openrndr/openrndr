package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*
import java.nio.ByteBuffer

interface BufferWriter {
    fun write(vararg v: Vector3) {
        v.forEach { write(it) }
    }

    fun copyBuffer(sourceBuffer: ByteBuffer, sourceOffset: Int, sourceSizeInBytes: Int)
    fun write(v: Vector3)
    fun write(v: Vector2)
    fun write(v: Vector4)
    fun write(v: Int)
    fun write(v: IntVector2)
    fun write(v: IntVector3)
    fun write(v: IntVector4)
    fun write(v: Matrix33)
    fun write(v: Matrix44)
    fun write(v: Float)
    fun write(x: Float, y: Float)
    fun write(x: Float, y: Float, z: Float)
    fun write(x: Float, y: Float, z: Float, w: Float)
    fun write(v: ColorRGBa)
    fun write(a: FloatArray, offset: Int = 0, size: Int = a.size)

    /**
     * rewind the underlying buffer
     */
    fun rewind()
    var position: Int
    var positionElements: Int
}
