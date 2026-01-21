package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

/**
 * Abstract class for writing data into a buffer. Provides methods for writing
 * various types of data such as vectors, matrices, colors, and primitive types.
 */
expect abstract class BufferWriter {


    /**
     * Writes one or more 3D vectors to the buffer.
     *
     * @param v one or more 3D vectors of type Vector3 to be written
     */
//    abstract fun write(vararg v: Vector3)

    /**
     * Writes the provided byte value to the buffer.
     *
     * @param v the byte value to be written
     */
    abstract fun write(v: Byte)

    /**
     * Writes the provided 16-bit short integer to the buffer.
     *
     * @param v the 16-bit short integer value to be written
     */
    abstract fun write(v: Short)

    /**
     * Writes the provided 3D vector to the buffer.
     *
     * @param v the 3D vector to be written
     */
    abstract fun write(v: Vector3)

    /**
     * Writes the provided 2D vector to the buffer.
     *
     * @param v the 2D vector to be written
     */
    abstract fun write(v: Vector2)

    /**
     * Writes the provided 4D vector to the buffer.
     *
     * @param v the 4D vector to be written
     */
    abstract fun write(v: Vector4)

    /**
     * Writes the provided integer value to the buffer.
     *
     * @param v the integer value to be written
     */
    abstract fun write(v: Int)

    /**
     * Writes the provided 2D integer vector to the buffer.
     *
     * @param v the 2D integer vector to be written
     */
    abstract fun write(v: IntVector2)

    /**
     * Writes the provided 3D integer vector to the buffer.
     *
     * @param v the 3D integer vector to be written
     */
    abstract fun write(v: IntVector3)

    /**
     * Writes the provided 4D integer vector to the buffer.
     *
     * @param v the 4D integer vector to be written
     */
    abstract fun write(v: IntVector4)

    /**
     * Writes a 3x3 matrix to the buffer.
     *
     * @param v the 3x3 matrix to be written
     */
    abstract fun write(v: Matrix33)

    /**
     * Writes the provided 4x4 matrix to the buffer.
     *
     * @param v the 4x4 matrix to be written
     */
    abstract fun write(v: Matrix44)

    /**
     * Writes the provided floating-point value to the buffer.
     *
     * @param v the floating-point value to be written
     */
    abstract fun write(v: Float)

    /**
     * Writes two floating-point values to the buffer.
     *
     * @param x the first floating-point value to be written
     * @param y the second floating-point value to be written
     */
    abstract fun write(x: Float, y: Float)

    /**
     * Writes three floating-point values to the buffer.
     *
     * @param x the first floating-point value to be written
     * @param y the second floating-point value to be written
     * @param z the third floating-point value to be written
     */
    abstract fun write(x: Float, y: Float, z: Float)

    /**
     * Writes four floating-point values to the buffer.
     *
     * @param x the first floating-point value to be written
     * @param y the second floating-point value to be written
     * @param z the third floating-point value to be written
     * @param w the fourth floating-point value to be written
     */
    abstract fun write(x: Float, y: Float, z: Float, w: Float)

    /**
     * Writes the provided color value to the buffer.
     *
     * @param v the color value of type ColorRGBa to be written
     */
    abstract fun write(v: ColorRGBa)

    /**
     * Writes the specified portion of the provided float array to the buffer starting at the given offset.
     *
     * @param a the float array to be written
     * @param offset the starting index in the float array, default is 0
     * @param size the number of elements to write from the float array, default is the size of the array
     */
    abstract fun write(a: FloatArray, offset: Int = 0, size: Int = a.size)

    /**
     * rewind the underlying buffer
     */
    abstract fun rewind()
    abstract var position: Int
    abstract var positionElements: Int


}