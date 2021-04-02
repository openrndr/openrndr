package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

actual abstract class BufferWriter {
    actual abstract fun write(vararg v: Vector3)
    actual abstract fun write(v: Byte)
    actual abstract fun write(v: Short)
    actual abstract fun write(v: Vector3)
    actual abstract fun write(v: Vector2)
    actual abstract fun write(v: Vector4)
    actual abstract fun write(v: Int)
    actual abstract fun write(v: IntVector2)
    actual abstract fun write(v: IntVector3)
    actual abstract fun write(v: IntVector4)
    actual abstract fun write(v: Matrix33)
    actual abstract fun write(v: Matrix44)
    actual abstract fun write(v: Float)
    actual abstract fun write(x: Float, y: Float)
    actual abstract fun write(x: Float, y: Float, z: Float)
    actual abstract fun write(x: Float, y: Float, z: Float, w: Float)
    actual abstract fun write(v: ColorRGBa)
    actual abstract fun write(a: FloatArray, offset: Int, size: Int)

    /**
     * rewind the underlying buffer
     */
    actual abstract fun rewind()
    actual abstract var position: Int
    actual abstract var positionElements: Int


}