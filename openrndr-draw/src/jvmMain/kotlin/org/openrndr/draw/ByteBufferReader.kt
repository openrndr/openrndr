package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*
import java.nio.ByteBuffer


private fun ByteBuffer.alignTo(alignment: Int) {
    if (position() % alignment != 0) {
        position(position() + alignment - (position() % alignment))
    }
}

class ByteBufferReader(val buffer: ByteBuffer,
                       val alignment: BufferAlignment = BufferAlignment.NONE,
                       val elementIterator: Iterator<ShaderStorageElement>?

    ) : BufferReader {

    /**
     * Processes the next element in the `elementIterator` based on the specified `BufferPrimitiveType`.
     * Aligns buffer position if necessary and validates type consistency. Recursively processes
     * nested structures when encountered.
     *
     * @param type The expected type of the next buffer primitive, represented as a `BufferPrimitiveType`.
     */
    private fun next(type: BufferPrimitiveType) {
        if (elementIterator != null) {
            val element = elementIterator.next()
            when (element) {
                is ShaderStoragePrimitive -> {
                    require(element.type == type) {
                        "Type mismatch in ShaderStoragePrimitive: expected '${element.type}', but received '${type}'."
                    }
                }

                is ShaderStorageStruct -> {
                    if (buffer.position().mod(element.alignmentInBytes()) != 0) {
                        buffer.position(
                            buffer.position() + element.alignmentInBytes() - (buffer.position().mod(element.alignmentInBytes()))
                        )
                    }
                    next(type)
                }
            }
        }
    }


    override fun rewind() {
        buffer.rewind()
    }

    override fun readVector2(): Vector2 {
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(8)
        }

        next(BufferPrimitiveType.VECTOR2_FLOAT32)
        val x = buffer.float.toDouble()
        val y = buffer.float.toDouble()
        return Vector2(x, y)
    }

    override fun readVector3(): Vector3 {
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(16)
        }

        next(BufferPrimitiveType.VECTOR3_FLOAT32)
        val x = buffer.float.toDouble()
        val y = buffer.float.toDouble()
        val z = buffer.float.toDouble()
        return Vector3(x, y, z)
    }

    override fun readVector4(): Vector4 {
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(16)
        }
        next(BufferPrimitiveType.VECTOR4_FLOAT32)
        val x = buffer.float.toDouble()
        val y = buffer.float.toDouble()
        val z = buffer.float.toDouble()
        val w = buffer.float.toDouble()
        return Vector4(x, y, z, w)
    }

    override fun readIntVector2(): IntVector2 {
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(8)
        }
        next(BufferPrimitiveType.VECTOR2_INT32)
        val x = buffer.int
        val y = buffer.int
        return IntVector2(x, y)
    }

    override fun readIntVector3(): IntVector3 {
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(16)
        }
        next(BufferPrimitiveType.VECTOR3_INT32)
        val x = buffer.int
        val y = buffer.int
        val z = buffer.int
        return IntVector3(x, y, z)
    }

    override fun readIntVector4(): IntVector4 {
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(16)
        }
        next(BufferPrimitiveType.VECTOR4_INT32)
        val x = buffer.int
        val y = buffer.int
        val z = buffer.int
        val w = buffer.int
        return IntVector4(x, y, z, w)
    }

    override fun readColorRGBa(): ColorRGBa {
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(16)
        }
        next(BufferPrimitiveType.VECTOR4_FLOAT32)
        val r = buffer.float.toDouble()
        val g = buffer.float.toDouble()
        val b = buffer.float.toDouble()
        val a = buffer.float.toDouble()
        return ColorRGBa(r, g, b, a)
    }

    override fun readFloat(): Float {
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(4)
        }
        next(BufferPrimitiveType.FLOAT32)
        return buffer.float
    }

    override fun readMatrix33(): Matrix33 {
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(16)
        }
        next(BufferPrimitiveType.MATRIX33_FLOAT32)
        val m00 = buffer.float.toDouble()
        val m01 = buffer.float.toDouble()
        val m02 = buffer.float.toDouble()
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(16)
        }

        val m10 = buffer.float.toDouble()
        val m11 = buffer.float.toDouble()
        val m12 = buffer.float.toDouble()
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(16)
        }

        val m20 = buffer.float.toDouble()
        val m21 = buffer.float.toDouble()
        val m22 = buffer.float.toDouble()
        return Matrix33(
            m00, m01, m02,
            m10, m11, m12,
            m20, m21, m22
        )
    }

    override fun readMatrix44(): Matrix44 {
        if (alignment == BufferAlignment.STD430) {
            buffer.alignTo(16)
        }
        next(BufferPrimitiveType.MATRIX44_FLOAT32)
        val m00 = buffer.float.toDouble()
        val m01 = buffer.float.toDouble()
        val m02 = buffer.float.toDouble()
        val m03 = buffer.float.toDouble()
        val m10 = buffer.float.toDouble()
        val m11 = buffer.float.toDouble()
        val m12 = buffer.float.toDouble()
        val m13 = buffer.float.toDouble()
        val m20 = buffer.float.toDouble()
        val m21 = buffer.float.toDouble()
        val m22 = buffer.float.toDouble()
        val m23 = buffer.float.toDouble()
        val m30 = buffer.float.toDouble()
        val m31 = buffer.float.toDouble()
        val m32 = buffer.float.toDouble()
        val m33 = buffer.float.toDouble()
        return Matrix44(
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33
        )
    }
}