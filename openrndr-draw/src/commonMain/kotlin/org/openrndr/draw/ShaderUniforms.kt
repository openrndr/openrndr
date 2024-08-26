package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

interface ShaderUniforms {
    fun uniform(name: String, value: Matrix33)
    fun uniform(name: String, value: Matrix44)
    fun uniform(name: String, value: ColorRGBa)
    fun uniform(name: String, value: Vector4)
    fun uniform(name: String, value: Vector3)
    fun uniform(name: String, value: Vector2)
    fun uniform(name: String, value: IntVector2)
    fun uniform(name: String, value: IntVector3)
    fun uniform(name: String, value: IntVector4)
    fun uniform(name: String, value: BooleanVector2)
    fun uniform(name: String, value: BooleanVector3)
    fun uniform(name: String, value: BooleanVector4)
    fun uniform(name: String, x: Float, y: Float, z: Float, w: Float)
    fun uniform(name: String, x: Float, y: Float, z: Float)
    fun uniform(name: String, x: Float, y: Float)
    fun uniform(name: String, value: Double)
    fun uniform(name: String, value: Float)
    fun uniform(name: String, value: Int)
    fun uniform(name: String, value: Boolean)
    fun uniform(name: String, value: Array<IntVector4>)
    fun uniform(name: String, value: Array<IntVector3>)
    fun uniform(name: String, value: Array<IntVector2>)
    fun uniform(name: String, value: Array<Vector4>)
    fun uniform(name: String, value: Array<Vector3>)
    fun uniform(name: String, value: Array<Vector2>)
    fun uniform(name: String, value: FloatArray)
    fun uniform(name: String, value: IntArray)
    fun uniform(name: String, value: Array<ColorRGBa>)
    fun uniform(name: String, value: Array<Double>)
    fun uniform(name: String, value: Array<Matrix33>)
    fun uniform(name: String, value: Array<Matrix44>)
}