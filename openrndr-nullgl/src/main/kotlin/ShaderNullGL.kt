package org.openrndr.internal.nullgl

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*

class ShaderNullGL(override val session: Session?) : Shader {

    override var textureBindings: TextureBindings
        get() = TODO("Not yet implemented")
        set(value) {}
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

    override fun uniform(name: String, value: Array<Matrix33>) {
    }


    override fun uniform(name: String, value: Array<Matrix44>) {
    }

    override fun buffer(name: String, vertexBuffer: VertexBuffer) {
    }

    override fun buffer(name: String, shaderStorageBuffer: ShaderStorageBuffer) {
    }

    override fun buffer(name: String, counterBuffer: AtomicCounterBuffer) {
    }

    override fun uniform(name: String, value: ColorRGBa) {
    }

    override fun uniform(name: String, value: Vector4) {
    }

    override fun uniform(name: String, value: Vector3) {
    }

    override fun uniform(name: String, value: Vector2) {
    }

    override fun uniform(name: String, value: IntVector4) {
    }

    override fun uniform(name: String, value: BooleanVector2) {
    }

    override fun uniform(name: String, value: BooleanVector3) {
    }

    override fun uniform(name: String, value: BooleanVector4) {
    }

    override fun uniform(name: String, value: IntVector3) {
    }

    override fun uniform(name: String, value: IntVector2) {
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

    override fun uniform(name: String, value: Array<Double>) {
    }

    override fun uniform(name: String, value: Array<ColorRGBa>) {
    }

    override fun uniform(name: String, value: Array<Vector4>) {
    }

    override fun uniform(name: String, value: Array<Vector3>) {
    }

    override fun uniform(name: String, value: Array<Vector2>) {
    }

    override fun uniform(name: String, value: Array<IntVector4>) {
    }

    override fun uniform(name: String, value: Array<IntVector3>) {
    }

    override fun uniform(name: String, value: Array<IntVector2>) {
    }

    override fun uniform(name: String, value: FloatArray) {
    }

    override fun uniform(name: String, value: IntArray) {
    }

    override fun destroy() {
    }

    override fun image(name: String, image: Int, imageBinding: ImageBinding) {
    }

    override fun image(name: String, image: Int, imageBinding: Array<out ImageBinding>) {
    }
}