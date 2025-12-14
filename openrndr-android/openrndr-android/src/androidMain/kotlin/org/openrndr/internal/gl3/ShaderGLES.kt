package org.openrndr.internal.gl3

import android.opengl.GLES30
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.AtomicCounterBuffer
import org.openrndr.draw.ImageBinding
import org.openrndr.draw.Session
import org.openrndr.draw.Shader
import org.openrndr.draw.ShaderStorageBuffer
import org.openrndr.draw.ShaderType
import org.openrndr.draw.UniformBlock
import org.openrndr.draw.UniformBlockLayout
import org.openrndr.draw.VertexBuffer
import org.openrndr.internal.gl.compileShader
import org.openrndr.internal.gl.linkProgram
import org.openrndr.math.BooleanVector2
import org.openrndr.math.BooleanVector3
import org.openrndr.math.BooleanVector4
import org.openrndr.math.IntVector2
import org.openrndr.math.IntVector3
import org.openrndr.math.IntVector4
import org.openrndr.math.Matrix33
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

class ShaderGLES(
    val programId: Int,
    private val owned: Boolean = true,
    override val session: Session?,
) : Shader {

    companion object {
        fun fromSource(vsSrc: String, fsSrc: String, session: Session?): ShaderGLES {
            val vs = compileShader(GLES30.GL_VERTEX_SHADER, vsSrc)
            val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fsSrc)
            val pid = linkProgram(vs, fs)
            return ShaderGLES(pid, owned = true, session = session)
        }

        private var nextBindingPoint: Int = 0
    }

    override val types: Set<ShaderType> = emptySet()

    override fun begin() {
        GLES30.glUseProgram(programId)
    }

    override fun end() {
        GLES30.glUseProgram(0)
    }

    override fun hasUniform(name: String): Boolean {
        return true
    }

    override fun createBlock(blockName: String): UniformBlock {
        // TODO: derive the real layout by querying GL (glGetActiveUniformBlockiv)
        val layout = UniformBlockLayout(0, emptyMap())

        // 1. Generate a new UBO
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        val uboId = ids[0]
        require(uboId != 0) { "Failed to create UBO for block '$blockName'" }

        // 2. Pick a binding point (you may want to manage a counter or a map per blockName)
        val bindingPoint = nextBindingPoint++

        // 3. Create the GLES implementation
        return UniformBlockGLES(layout, uboId, bindingPoint)
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
        val loc = GLES30.glGetUniformLocation(programId, name)
        if (loc >= 0) GLES30.glUniform1f(loc, value.toFloat())
    }

    override fun uniform(name: String, value: Float) {
        val loc = GLES30.glGetUniformLocation(programId, name)
        if (loc >= 0) GLES30.glUniform1f(loc, value)
    }

    override fun uniform(name: String, value: Int) {
        val loc = GLES30.glGetUniformLocation(programId, name)
        if (loc >= 0) GLES30.glUniform1i(loc, value)
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

    // helpers
    fun uniform2f(name: String, x: Float, y: Float) {
        val loc = GLES30.glGetUniformLocation(programId, name)
        if (loc >= 0) GLES30.glUniform2f(loc, x, y)
    }
    fun uniform4f(name: String, x: Float, y: Float, z: Float, w: Float) {
        val loc = GLES30.glGetUniformLocation(programId, name)
        if (loc >= 0) GLES30.glUniform4f(loc, x, y, z, w)
    }

    override fun destroy() {
        if (owned) GLES30.glDeleteProgram(programId)
    }

    override fun image(name: String, image: Int, imageBinding: ImageBinding) {
    }

    override fun image(name: String, image: Int, imageBinding: Array<out ImageBinding>) {
    }
}