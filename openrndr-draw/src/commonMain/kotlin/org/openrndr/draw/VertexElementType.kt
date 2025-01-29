package org.openrndr.draw

/**
 * Enum class representing various types of vertex elements used in graphics programming.
 *
 * Each vertex element type is associated with specific properties:
 * - `componentCount`: Number of components in the type (e.g., a scalar has 1, a 4-component vector has 4).
 * - `sizeInBytes`: Total size in bytes required to store one instance of the type.
 * - `std430AlignmentInBytes`: Alignment requirement in bytes as per the std430 layout rules in GLSL.
 */
enum class VertexElementType(val componentCount: Int,
                             val sizeInBytes: Int,
                             val std430AlignmentInBytes: Int

    ) {
    /** signed 8-bit integer */
    INT8(1, 1, 4),
    /** unsigned 8 bit integer */
    UINT8(1, 1, 4),

    /** unsigned 16-bit integer */
    UINT16(1, 2, 4),
    /** signed 16-bit integer */
    INT16(1, 2, 4),

    /** unsigned 32-bit integer */
    UINT32(1, 4, 4),

    /** signed 32-bit integer */
    INT32(1, 4, 4),

    VECTOR2_UINT8(2, 2, 4),
    VECTOR2_INT8(2, 2, 4),
    VECTOR2_UINT16(2, 4, 4),
    VECTOR2_INT16(2, 4, 4),
    VECTOR2_UINT32(2, 8, 8),
    VECTOR2_INT32(2, 8, 8),

    VECTOR3_UINT8(3, 3, 4),
    VECTOR3_INT8(3, 3, 4),
    VECTOR3_UINT16(3, 6, 8),
    VECTOR3_INT16(3, 6, 8),
    VECTOR3_UINT32(3, 12, 16),
    VECTOR3_INT32(3, 12, 16),

    VECTOR4_UINT8(4, 4, 4),
    VECTOR4_INT8(4, 4, 4),
    VECTOR4_UINT16(4, 8, 8),
    VECTOR4_INT16(4, 8, 8),
    VECTOR4_UINT32(4, 16, 16),
    VECTOR4_INT32(4, 16, 16),

    /** 32 bit float, or single precision float scalar */
    FLOAT32(1, 4, 4),
    /** 32-bit float, or single precision float 2-component vector */
    VECTOR2_FLOAT32(2, 8, 8),
    /** 32-bit float, or single precision float 3-component vector */
    VECTOR3_FLOAT32(3, 12, 16),
    /** 32-bit float, or single precision float 4-component vector */
    VECTOR4_FLOAT32(4, 16, 16),
    /** 32 bit float, or single precision float 2x2 matrix */
    MATRIX22_FLOAT32(4, 4 * 4, 16),
    /** 32 bit float, or single precision float 3x3 matrix */
    MATRIX33_FLOAT32(9, 9 * 4, 16),
    /** 32 bit float, or single precision float 4x4 matrix */
    MATRIX44_FLOAT32(16, 16 * 4, 16),
}
