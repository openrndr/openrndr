package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.opengl.GL43C.*
import org.openrndr.draw.*
import org.openrndr.internal.Driver

private val logger = KotlinLogging.logger {}

fun ImageAccess.gl(): Int {
    return when (this) {
        ImageAccess.READ -> GL_READ_ONLY
        ImageAccess.WRITE -> GL_WRITE_ONLY
        ImageAccess.READ_WRITE -> GL_READ_WRITE
    }
}

class ComputeShaderGL43(
    override val programObject: Int,
    override val name: String = "compute_shader"
) : ComputeShader,
    ShaderBufferBindingsGL3, ShaderUniformsGL3, ShaderImageBindingsGL43 {

    override val uniforms = mutableMapOf<String, Int>()
    override val useProgramUniform = (Driver.instance as DriverGL3).version >= DriverVersionGL.VERSION_4_1
    private var destroyed = false

    override fun execute(width: Int, height: Int, depth: Int) {

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
        checkGLErrors()
        glUseProgram(this.programObject)
        checkGLErrors()
        glDispatchCompute(width, height, depth)
        checkGLErrors()
        //glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        glMemoryBarrier(GL_ALL_BARRIER_BITS)
        checkGLErrors()
    }

    override val ssbo: Int = createSSBO()

    override fun destroy() {
        if (!destroyed) {
            glDeleteProgram(this.programObject)
            checkGLErrors()
        }
    }

    companion object {
        fun createFromCode(code: String, name: String): ComputeShaderGL43 {
            val shaderObject = glCreateShader(GL_COMPUTE_SHADER)
            glShaderSource(shaderObject, code)
            checkGLErrors()
            glCompileShader(shaderObject)
            checkGLErrors()

            val compileStatus = IntArray(1)
            glGetShaderiv(shaderObject, GL_COMPILE_STATUS, compileStatus)
            checkGLErrors()
            if (compileStatus[0] != GL_TRUE) {
                checkShaderInfoLog(shaderObject, code, "compute shader")
                throw Exception("could not compile vertex shader")
            }

            val program = glCreateProgram()
            glAttachShader(program, shaderObject)
            checkGLErrors()
            glLinkProgram(program)
            checkGLErrors()
            checkProgramInfoLog(program, "compute shader")

            return ComputeShaderGL43(program, name)
        }
    }



}
