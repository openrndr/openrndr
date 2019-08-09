package org.openrndr.internal.gl3


import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL43C.*
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ComputeShader

class ComputeShaderGL43(val programObject: Int) : ComputeShader {
    override fun execute(width: Int, height: Int, depth: Int) {
        glUseProgram(programObject)
        checkGLErrors()
        glDispatchCompute(width, height, depth)
        checkGLErrors()
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        checkGLErrors()
    }

    override fun image(name:String, image:Int, colorBuffer: ColorBuffer) {
        colorBuffer as ColorBufferGL3
        glUseProgram(programObject)
        checkGLErrors {
            when(it) {
                GL_INVALID_OPERATION -> " program ($programObject) is not a program object / program could not be made part of current state / transform feedback mode is active"

                else -> null
            }
        }
        glBindImageTexture(image, colorBuffer.texture, 0, false, 0, GL_WRITE_ONLY, colorBuffer.format())
        checkGLErrors()

    }

    companion object {
        fun createFromCode(code: String): ComputeShaderGL43 {
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

            return ComputeShaderGL43(program)
        }
    }
}