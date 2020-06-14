package org.openrndr.internal.nullgl

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*

class ShaderNullGL(override val session: Session?) : Shader {

    override val types: Set<ShaderType> = emptySet()


    override fun begin() {

    }

    override fun end() {

    }

    override fun hasUniform(name: String): Boolean {
        return true
    }

    override fun createBlock(blockName: String): UniformBlock {
        val ubl = UniformBlockLayout(0, emptyMap())
        return UniformBlockNullGL(ubl)
    }

    override fun blockLayout(blockName: String): UniformBlockLayout? {
        return null
    }

    override fun block(blockName: String, block: UniformBlock) {

    }

    override fun uniform(name: String, value: Matrix33) {

    }

    override fun uniform(name: String, value: Matrix44) {

    }

    override fun uniform(name: String, value: Array<Matrix44>) {

    }


    override fun uniform(name: String, value: ColorRGBa) {

    }

    override fun uniform(name: String, value: Vector4) {

    }

    override fun uniform(name: String, value: Vector3) {

    }

    override fun uniform(name: String, value: Vector2) {

    }

    override fun uniform(name: String, x: Float, y: Float, z: Float, w: Float) {

    }

    override fun uniform(name: String, x: Float, y: Float, z: Float) {

    }

    override fun uniform(name: String, x: Float, y: Float) {
    }

    override fun uniform(name: String, value: Double) {

    }

    override fun uniform(name: String, value: Float) {

    }

    override fun uniform(name: String, value: Int) {

    }

    override fun uniform(name: String, value: Boolean) {

    }

    override fun uniform(name: String, value: Array<Vector4>) {

    }

    override fun uniform(name: String, value: Array<Vector3>) {

    }

    override fun uniform(name: String, value: Array<Vector2>) {

    }

    override fun uniform(name: String, value: FloatArray) {

    }

    override fun destroy() {

    }

}