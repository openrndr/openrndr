package org.openrndr.draw
/**
 * Vertex element type enumeration
 */
enum class VertexElementType(val componentCount: Int, val sizeInBytes: Int) {
    /** signed 8 bit integer */
    INT8(1, 1),
    /** unsigned 8 bit integer */
    UINT8(1, 1),

    UINT16(1, 2),
    /** signed 16 bit integer */
    INT16(1, 2),

    UINT32(1, 4),

    /** signed 32 bit integer */
    INT32(1, 4),

    VECTOR2_UINT8(2, 2),
    VECTOR2_INT8(2, 2),
    VECTOR2_UINT16(2, 4),
    VECTOR2_INT16(2, 4),
    VECTOR2_UINT32(2, 8),
    VECTOR2_INT32(2, 8),

    VECTOR3_UINT8(3, 3),
    VECTOR3_INT8(3, 3),
    VECTOR3_UINT16(3, 6),
    VECTOR3_INT16(3, 6),
    VECTOR3_UINT32(3, 12),
    VECTOR3_INT32(3, 12),

    VECTOR4_UINT8(3, 3),
    VECTOR4_INT8(4, 4),
    VECTOR4_UINT16(4, 8),
    VECTOR4_INT16(4, 8),
    VECTOR4_UINT32(4, 16),
    VECTOR4_INT32(4, 16),

    /** 32 bit float, or single precision float scalar */
    FLOAT32(1, 4),
    /** 32 bit float, or single precision float 2-component vector */
    VECTOR2_FLOAT32(2, 8),
    /** 32 bit float, or single precision float 3-component vector */
    VECTOR3_FLOAT32(3, 12),
    /** 32 bit float, or single precision float 4-component vector */
    VECTOR4_FLOAT32(4, 16),
    /** 32 bit float, or single precision float 2x2 matrix */
    MATRIX22_FLOAT32(4, 4 * 4),
    /** 32 bit float, or single precision float 3x3 matrix */
    MATRIX33_FLOAT32(9, 9 * 4),
    /** 32 bit float, or single precision float 4x4 matrix */
    MATRIX44_FLOAT32(16, 16 * 4),
}
