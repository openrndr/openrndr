package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

actual interface BufferWriterStd430 {
    actual fun write(vararg v: Vector3)
    actual fun write(v: Boolean)
    actual fun write(v: Byte)
    actual fun write(v: Short)
    actual fun write(v: Vector3)
    actual fun write(v: Vector2)
    actual fun write(v: Vector4)
    actual fun write(v: Int)
    actual fun write(v: IntVector2)
    actual fun write(v: IntVector3)
    actual fun write(v: IntVector4)
    actual fun write(v: Matrix33)
    actual fun write(v: Matrix44)
    actual fun write(v: Double)
    actual fun write(v: Float)
    actual fun write(x: Float, y: Float)
    actual fun write(x: Float, y: Float, z: Float)
    actual fun write(x: Float, y: Float, z: Float, w: Float)
    actual fun write(v: ColorRGBa)

    /**
     * rewind the underlying buffer
     */
    actual fun rewind()
    actual var position: Int
    actual var positionElements: Int

}