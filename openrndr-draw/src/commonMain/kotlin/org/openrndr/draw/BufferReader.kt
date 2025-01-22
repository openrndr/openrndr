package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

/**
 * Abstract class for writing data into a buffer. Provides methods for writing
 * various types of data such as vectors, matrices, colors, and primitive types.
 */
interface BufferReader {

    fun rewind()

    fun readVector2(): Vector2
    fun readVector3(): Vector3
    fun readVector4(): Vector4
    fun readIntVector2(): IntVector2
    fun readIntVector3(): IntVector3
    fun readIntVector4(): IntVector4
    fun readColorRGBa(): ColorRGBa
    fun readFloat(): Float
    fun readMatrix33(): Matrix33
    fun readMatrix44(): Matrix44
}