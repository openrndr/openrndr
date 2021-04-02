package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

expect abstract class BufferWriter {


    abstract fun write(vararg v: Vector3)

    abstract fun write(v: Byte)
    abstract fun write(v: Short)
    abstract fun write(v: Vector3)
    abstract fun write(v: Vector2)
    abstract fun write(v: Vector4)
    abstract fun write(v: Int)
    abstract fun write(v: IntVector2)
    abstract fun write(v: IntVector3)
    abstract fun write(v: IntVector4)
    abstract fun write(v: Matrix33)
    abstract fun write(v: Matrix44)
    abstract fun write(v: Float)
    abstract fun write(x: Float, y: Float)
    abstract fun write(x: Float, y: Float, z: Float)
    abstract fun write(x: Float, y: Float, z: Float, w: Float)
    abstract fun write(v: ColorRGBa)
    abstract fun write(a: FloatArray, offset: Int = 0, size: Int = a.size)

    /**
     * rewind the underlying buffer
     */
    abstract fun rewind()
    abstract var position: Int
    abstract var positionElements: Int


}