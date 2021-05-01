package org.openrndr.webgl

import org.khronos.webgl.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*
import org.khronos.webgl.WebGLRenderingContext as GL

class ShaderWebGL(
    val context: GL,
    val program: WebGLProgram,
    override val session: Session?
) : Shader {
    companion object {
        fun create(
            context: GL,
            vertexShader: VertexShaderWebGL,
            fragmentShader: FragmentShaderWebGL,
            name: String,
            session: Session?
        ): ShaderWebGL {
            val program = context.createProgram() ?: error("failed to create program")
            context.attachShader(program, vertexShader.shaderObject)
            context.attachShader(program, fragmentShader.shaderObject)
            context.linkProgram(program)
            return ShaderWebGL(context, program, session)
        }
    }
    var userShader = false


    fun attributeIndex(name:String) : Int {
        val index =  context.getAttribLocation(program, name)
        if (index == -1) {
            //console.warn("missing attribute $name")
        }
        return index
    }

    override val types: Set<ShaderType> = setOf(ShaderType.FRAGMENT, ShaderType.VERTEX)

    override fun begin() {
        context.useProgram(program)
    }

    override fun end() {
        context.useProgram(null as WebGLProgram?)
    }

    fun uniformIndex(uniform:String, query: Boolean = false) : WebGLUniformLocation? {
        val index = context.getUniformLocation(program, uniform)
        if (index == null) {
            //console.warn("missing uniform $uniform")
        }
        return index
    }

    override fun hasUniform(name: String): Boolean {
        return uniformIndex(name, query = true) != null
    }

    override fun createBlock(blockName: String): UniformBlock? {
        error("uniform blocks are not supported by WebGL")
    }

    override fun blockLayout(blockName: String): UniformBlockLayout? {
        error("uniform blocks are not supported by WebGL")
    }

    override fun block(blockName: String, block: UniformBlock) {
        error("uniform blocks are not supported by WebGL")
    }

    override fun uniform(name: String, value: Matrix33) {
        context.uniformMatrix3fv(uniformIndex(name), false, value.toFloat32Array())
    }

    override fun uniform(name: String, value: Matrix44) {
        context.uniformMatrix4fv(uniformIndex(name), false, value.toFloat32Array())
    }

    override fun uniform(name: String, value: ColorRGBa) {
        context.uniform4fv(uniformIndex(name), value.toFloat32Array())
    }

    override fun uniform(name: String, value: Vector4) {
        context.uniform4fv(uniformIndex(name), value.toFloat32Array())
    }

    override fun uniform(name: String, value: Vector3) {
        context.uniform3fv(uniformIndex(name), value.toFloat32Array())
    }

    override fun uniform(name: String, value: Vector2) {
        context.uniform2fv(uniformIndex(name), value.toFloat32Array())
    }

    override fun uniform(name: String, value: IntVector4) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: IntVector3) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: IntVector2) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float, w: Float) {
        context.uniform4f(uniformIndex(name), x, y, z, w)
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float) {
        context.uniform3f(uniformIndex(name), x, y, z)
    }

    override fun uniform(name: String, x: Float, y: Float) {
        context.uniform2f(uniformIndex(name), x, y)
    }

    override fun uniform(name: String, value: Double) {
        context.uniform1f(uniformIndex(name), value.toFloat())
    }

    override fun uniform(name: String, value: Float) {
        context.uniform1f(uniformIndex(name), value)
    }

    override fun uniform(name: String, value: Int) {
        context.uniform1i(uniformIndex(name), value)
    }

    override fun uniform(name: String, value: Boolean) {
        context.uniform1i(uniformIndex(name), if (value) 1 else 0)
    }

    override fun uniform(name: String, value: Array<Matrix44>) {
        val floatValues = Float32Array(value.size * 4 * 4)
        var offset = 0
        for (j in value.indices) {
            val mf = value[j].toFloat32Array()
            for (i in 0 until 16) {
                floatValues[offset] = mf[i]
                offset++
            }
        }
        context.uniformMatrix4fv(uniformIndex(name), false, floatValues)
    }

    override fun uniform(name: String, value: Array<Vector4>) {
        val floatValues = Float32Array(value.size * 4)
        for (i in value.indices) {
            floatValues[i * 4] = value[i].x.toFloat()
            floatValues[i * 4 + 1] = value[i].y.toFloat()
            floatValues[i * 4 + 2] = value[i].z.toFloat()
            floatValues[i * 4 + 3] = value[i].w.toFloat()
        }
        context.uniform4fv(uniformIndex(name), floatValues)
    }

    override fun uniform(name: String, value: Array<Vector3>) {
        val floatValues = Float32Array(value.size * 3)
        for (i in value.indices) {
            floatValues[i * 3] = value[i].x.toFloat()
            floatValues[i * 3 + 1] = value[i].y.toFloat()
            floatValues[i * 3 + 2] = value[i].z.toFloat()
        }
        context.uniform3fv(uniformIndex(name), floatValues)
    }

    override fun uniform(name: String, value: Array<Vector2>) {
        val floatValues = Float32Array(value.size * 2)
        for (i in value.indices) {
            floatValues[i * 3] = value[i].x.toFloat()
            floatValues[i * 3 + 1] = value[i].y.toFloat()
        }
        context.uniform3fv(uniformIndex(name), floatValues)
    }

    override fun uniform(name: String, value: Array<IntVector4>) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: Array<IntVector3>) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: Array<IntVector2>) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: Array<ColorRGBa>) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: Array<Double>) {
        val floatValues = Float32Array(value.size)
        for (i in value.indices) {
            floatValues[i * 3] = value[i].toFloat()

        }
        context.uniform1fv(uniformIndex(name), floatValues)
    }

    override fun uniform(name: String, value: FloatArray) {
        val floatValues = Float32Array(value.size)
        for (i in value.indices) {
            floatValues[i * 3] = value[i].toFloat()

        }
        context.uniform1fv(uniformIndex(name), floatValues)
    }

    override fun uniform(name: String, value: IntArray) {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

    override fun image(name: String, image: Int, imageBinding: ImageBinding) {
        error("image bindings are not supported by WebGL")
    }
}