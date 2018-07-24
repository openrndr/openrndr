package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

data class UniformBlockLayout(val sizeInBytes: Int, val entries: Map<String, UniformDescription>)

enum class UniformType(val sizeInBytes: Int) {
    INT32(4),
    VECTOR2_INT32(8),
    VECTOR3_INT32(12),
    VECTOR4_INT32(16),
    FLOAT32(4),
    VECTOR2_FLOAT32(8),
    VECTOR3_FLOAT32(12),
    VECTOR4_FLOAT32(16),
    MATRIX22_FLOAT32(4 * 4),
    MATRIX33_FLOAT32(9 * 4),
    MATRIX44_FLOAT32(16 * 4),
    COLOR_BUFFER_SAMPLER(4),
}

data class UniformDescription(val name: String, val type: UniformType, val size: Int, val offset: Int)

interface UniformBlock {
    fun uniform(name: String, value: Float)
    fun uniform(name: String, value: Vector2)
    fun uniform(name: String, value: Vector3)
    fun uniform(name: String, value: Vector4)
    fun uniform(name: String, value: ColorRGBa)
    fun uniform(name: String, value: Matrix44)
    fun uniform(name: String, value: Array<Float>)
    fun uniform(name: String, value: Array<Vector2>)
    fun uniform(name: String, value: Array<Vector3>)
    fun uniform(name: String, value: Array<Vector4>)
    fun upload()
}