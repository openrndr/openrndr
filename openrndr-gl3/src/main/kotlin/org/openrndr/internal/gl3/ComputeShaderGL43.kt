package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL42C
import org.lwjgl.opengl.GL43C.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ComputeShader
import org.openrndr.draw.VertexBuffer
import org.openrndr.math.*
import java.nio.Buffer

private val logger = KotlinLogging.logger {}

class ComputeShaderGL43(val programObject: Int, val name: String = "compute_shader") : ComputeShader {

    private var destroyed = false
    private val uniforms: MutableMap<String, Int> = hashMapOf()
    private fun uniformIndex(uniform: String, query: Boolean = false): Int =
            uniforms.getOrPut(uniform) {
                val location = glGetUniformLocation(programObject, uniform)
                debugGLErrors()
                if (location == -1 && !query) {
                    logger.warn {
                        "shader ${name} does not have uniform $uniform"
                    }
                }
                location
            }

    override fun execute(width: Int, height: Int, depth: Int) {
        glUseProgram(programObject)
        checkGLErrors()
        glDispatchCompute(width, height, depth)
        checkGLErrors()
        //glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        glMemoryBarrier(GL_ALL_BARRIER_BITS)
        checkGLErrors()
    }

    private fun bound(f: () -> Unit) {
        glUseProgram(programObject)
        f()

    }

    override fun inputImage(name: String, image: Int, colorBuffer: ColorBuffer) {

        if (colorBuffer.format.componentCount != 3) {
            colorBuffer as ColorBufferGL3
            glUseProgram(programObject)
            checkGLErrors {
                when (it) {
                    GL_INVALID_OPERATION -> " program ($programObject) is not a program object / program could not be made part of current state / transform feedback mode is active"
                    else -> null
                }
            }
            glBindImageTexture(image, colorBuffer.texture, 0, false, 0, GL_READ_ONLY, colorBuffer.format())
            checkGLErrors() {
                val maxImageUnits = glGetInteger(GL_MAX_IMAGE_UNITS)
                colorBuffer as ColorBufferGL3
                when (it) {
                    GL_INVALID_VALUE ->
                        """unit(=$image) greater than or equal to the value of GL_MAX_IMAGE_UNITS(=$maxImageUnits)
texture(=${colorBuffer.texture}) is not the name of an existing texture object
level(=0) or layer(=0) is less than zero                                        
                """.trimIndent()
                    else -> null
                }
            }
            val index = uniformIndex(name)
            glUniform1i(index, image)
            checkGLErrors()
        } else {
            throw IllegalArgumentException("color buffer has unsupported format (${colorBuffer.format}), only formats with 1, 2 or 4 components are supported")
        }
    }

    override fun outputImage(name: String, image: Int, colorBuffer: ColorBuffer) {
        if (colorBuffer.format.componentCount != 3) {

            colorBuffer as ColorBufferGL3
            glUseProgram(programObject)
            checkGLErrors {
                when (it) {
                    GL_INVALID_OPERATION -> " program ($programObject) is not a program object / program could not be made part of current state / transform feedback mode is active"
                    else -> null
                }
            }
            glBindImageTexture(image, colorBuffer.texture, 0, false, 0, GL_WRITE_ONLY, colorBuffer.format())
            checkGLErrors()
            val index = uniformIndex(name)
            glUniform1i(index, image)
            checkGLErrors()
        } else {
            throw IllegalArgumentException("color buffer has unsupported format (${colorBuffer.format}), only formats with 1, 2 or 4 components are supported")
        }
    }

    override fun inputOutputImage(name: String, image: Int, colorBuffer: ColorBuffer) {
        if (colorBuffer.format.componentCount != 3) {
            colorBuffer as ColorBufferGL3
            glUseProgram(programObject)
            checkGLErrors {
                when (it) {
                    GL_INVALID_OPERATION -> " program ($programObject) is not a program object / program could not be made part of current state / transform feedback mode is active"
                    else -> null
                }
            }
            glBindImageTexture(image, colorBuffer.texture, 0, false, 0, GL_READ_WRITE, colorBuffer.format())
            checkGLErrors()
            val index = uniformIndex(name)
            glUniform1i(index, image)
            checkGLErrors()
        } else {
            throw IllegalArgumentException("color buffer has unsupported format (${colorBuffer.format}), only formats with 1, 2 or 4 components are supported")
        }
    }

    private val ssbo = glGenBuffers()
    override fun buffer(name:String, vertexBuffer: VertexBuffer) {
        vertexBuffer as VertexBufferGL3
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        checkGLErrors()
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0 , vertexBuffer.buffer)
        checkGLErrors()

    }

    override fun uniform(name: String, value: ColorRGBa) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                glUniform4f(index, value.r.toFloat(), value.g.toFloat(), value.b.toFloat(), value.a.toFloat())
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: Vector3) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                glUniform3f(index, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: Vector4) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                glUniform4f(index, value.x.toFloat(), value.y.toFloat(), value.z.toFloat(), value.w.toFloat())
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float, w: Float) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                glUniform4f(index, x, y, z, w)
            }
        }
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                glUniform3f(index, x, y, z)
            }
        }
    }

    override fun uniform(name: String, x: Float, y: Float) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                glUniform2f(index, x, y)
            }
        }
    }

    override fun uniform(name: String, value: Int) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                glUniform1i(index, value)
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: Boolean) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                glUniform1i(index, if (value) 1 else 0)
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: Vector2) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                glUniform2f(index, value.x.toFloat(), value.y.toFloat())
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: Float) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                glUniform1f(index, value)
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: Double) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                glUniform1f(index, value.toFloat())
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: Matrix33) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                logger.trace { "Setting uniform '$name' to $value" }
                glUniformMatrix3fv(index, false, value.toFloatArray())
                postUniformCheck(name, index, value)
            }
        }
    }


    override fun uniform(name: String, value: Matrix44) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                logger.trace { "Setting uniform '$name' to $value" }
                glUniformMatrix4fv(index, false, value.toFloatArray())
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: Array<Vector2>) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                logger.trace { "Setting uniform '$name' to $value" }

                val floatValues = FloatArray(value.size * 2)
                for (i in 0 until value.size) {
                    floatValues[i * 2] = value[i].x.toFloat()
                    floatValues[i * 2 + 1] = value[i].y.toFloat()
                }

                glUniform2fv(index, floatValues)
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: Array<Vector3>) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                logger.trace { "Setting uniform '$name' to $value" }

                val floatValues = FloatArray(value.size * 3)
                for (i in 0 until value.size) {
                    floatValues[i * 3] = value[i].x.toFloat()
                    floatValues[i * 3 + 1] = value[i].y.toFloat()
                    floatValues[i * 3 + 2] = value[i].z.toFloat()
                }
                glUniform3fv(index, floatValues)
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: Array<Vector4>) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                logger.trace { "Setting uniform '$name' to $value" }

                val floatValues = FloatArray(value.size * 4)
                for (i in 0 until value.size) {
                    floatValues[i * 4] = value[i].x.toFloat()
                    floatValues[i * 4 + 1] = value[i].y.toFloat()
                    floatValues[i * 4 + 2] = value[i].z.toFloat()
                    floatValues[i * 4 + 3] = value[i].w.toFloat()
                }

                glUniform4fv(index, floatValues)
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: FloatArray) {
        bound {
            val index = uniformIndex(name)
            if (index != -1) {
                logger.trace { "Setting uniform '$name' to $value" }
                glUniform1fv(index, value)
                postUniformCheck(name, index, value)
            }
        }
    }

    private fun postUniformCheck(name: String, index: Int, value: Any) {
        debugGLErrors {
            val currentProgram = glGetInteger(GL_CURRENT_PROGRAM)

            fun checkUniform(): String {
                if (currentProgram > 0) {
                    val lengthBuffer = BufferUtils.createIntBuffer(1)
                    val sizeBuffer = BufferUtils.createIntBuffer(1)
                    val typeBuffer = BufferUtils.createIntBuffer(1)
                    val nameBuffer = BufferUtils.createByteBuffer(256)

                    glGetActiveUniform(currentProgram, index, lengthBuffer, sizeBuffer, typeBuffer, nameBuffer)
                    val nameBytes = ByteArray(lengthBuffer[0])
                    (nameBuffer as Buffer).rewind()
                    nameBuffer.get(nameBytes)
                    val retrievedName = String(nameBytes)
                    return "($name/$retrievedName): ${sizeBuffer[0]} / ${typeBuffer[0]}}"
                }
                return "no program"
            }

            when (it) {
                GL_INVALID_OPERATION -> "no current program object ($currentProgram), or uniform type mismatch (${checkUniform()}"
                else -> null
            }
        }
    }


    override fun destroy() {
        if (!destroyed) {
            glDeleteProgram(programObject)
            checkGLErrors()
        }
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