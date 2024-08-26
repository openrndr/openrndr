package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11C.GL_INVALID_OPERATION
import org.lwjgl.opengl.GL20C.GL_CURRENT_PROGRAM
import org.lwjgl.system.MemoryStack.stackPush
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ShaderUniforms
import org.openrndr.math.*
import java.nio.Buffer

private val logger = KotlinLogging.logger {}

interface ShaderUniformsGL3 : ShaderUniforms {
    fun bound(f: () -> Unit) {
        glUseProgram(programObject)
        f()
    }

    val programObject: Int
    val name: String
    val uniforms: MutableMap<String, Int>

    val useProgramUniform: Boolean

    fun uniformIndex(uniform: String, query: Boolean = false): Int =
        uniforms.getOrPut(uniform) {
            val location = glGetUniformLocation(programObject, uniform)
            debugGLErrors()
            if (location == -1 && !query) {
                logger.warn {
                    "Shader '${name}' does not have a uniform called '$uniform'"
                }
            }
            location
        }

    @Suppress("DuplicatedCode")
    override fun uniform(name: String, value: ColorRGBa) {
        val linearValue = value.toLinear()
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform4f(
                    programObject,
                    index,
                    linearValue.r.toFloat(),
                    linearValue.g.toFloat(),
                    linearValue.b.toFloat(),
                    linearValue.alpha.toFloat()
                )
            } else {
                bound {
                    glUniform4f(
                        index,
                        linearValue.r.toFloat(),
                        linearValue.g.toFloat(),
                        linearValue.b.toFloat(),
                        linearValue.alpha.toFloat()
                    )
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Vector3) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform3f(programObject, index, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
            } else {
                bound {
                    glUniform3f(index, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    @Suppress("DuplicatedCode")
    override fun uniform(name: String, value: Vector4) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform4f(
                    programObject,
                    index,
                    value.x.toFloat(),
                    value.y.toFloat(),
                    value.z.toFloat(),
                    value.w.toFloat()
                )
            } else {
                bound {
                    glUniform4f(index, value.x.toFloat(), value.y.toFloat(), value.z.toFloat(), value.w.toFloat())
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float, w: Float) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform4f(programObject, index, x, y, z, w)
            } else {
                bound {
                    glUniform4f(index, x, y, z, w)
                }
            }
        }
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform3f(programObject, index, x, y, z)
            } else {
                bound {
                    glUniform3f(index, x, y, z)
                }
            }
        }
    }

    override fun uniform(name: String, x: Float, y: Float) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform2f(programObject, index, x, y)
            }
            bound {
                glUniform2f(index, x, y)
            }
        }
    }

    override fun uniform(name: String, value: Int) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform1i(programObject, index, value)
            } else {
                bound {
                    glUniform1i(index, value)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: BooleanVector2) {
        uniform(name, value.toIntVector2())
    }

    override fun uniform(name: String, value: BooleanVector3) {
        uniform(name, value.toIntVector3())
    }

    override fun uniform(name: String, value: BooleanVector4) {
        uniform(name, value.toIntVector4())
    }

    override fun uniform(name: String, value: IntVector2) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform2i(programObject, index, value.x, value.y)
            } else {
                bound {
                    glUniform2i(index, value.x, value.y)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: IntVector3) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform3i(programObject, index, value.x, value.y, value.z)
            } else {
                bound {
                    glUniform3i(index, value.x, value.y, value.z)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: IntVector4) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform4i(programObject, index, value.x, value.y, value.z, value.w)
            } else {
                bound {
                    glUniform4i(index, value.x, value.y, value.z, value.w)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Boolean) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform1i(programObject, index, if (value) 1 else 0)
            }
            bound {
                glUniform1i(index, if (value) 1 else 0)
                postUniformCheck(name, index, value)
            }
        }
    }

    override fun uniform(name: String, value: Vector2) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform2f(programObject, index, value.x.toFloat(), value.y.toFloat())
            } else {
                bound {
                    glUniform2f(index, value.x.toFloat(), value.y.toFloat())
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Float) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform1f(programObject, index, value)
            } else {
                bound {
                    glUniform1f(index, value)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Double) {

        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniform1f(programObject, index, value.toFloat())
            } else {
                bound {
                    glUniform1f(index, value.toFloat())
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Matrix33) {
        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniformMatrix3fv(programObject, index, false, value.toFloatArray())
            } else {
                bound {
                    logger.trace { "Setting uniform '$name' to $value" }
                    glUniformMatrix3fv(index, false, value.toFloatArray())
                    postUniformCheck(name, index, value)
                }
            }
        }
    }


    override fun uniform(name: String, value: Matrix44) {

        val index = uniformIndex(name)
        if (index != -1) {
            if (useProgramUniform) {
                glProgramUniformMatrix4fv(programObject, index, false, value.toFloatArray())
            } else {
                bound {
                    logger.trace { "Setting uniform '$name' to $value" }
                    glUniformMatrix4fv(index, false, value.toFloatArray())
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<IntVector4>) {

        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }

            val intValues = IntArray(value.size * 4)
            for (i in value.indices) {
                intValues[i * 4] = value[i].x
                intValues[i * 4 + 1] = value[i].y
                intValues[i * 4 + 2] = value[i].z
                intValues[i * 4 + 3] = value[i].w
            }
            if (useProgramUniform) {
                glProgramUniform4iv(programObject, index, intValues)
            } else {
                bound {
                    glUniform4iv(index, intValues)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<IntVector3>) {

        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }

            val intValues = IntArray(value.size * 3)
            for (i in value.indices) {
                intValues[i * 3] = value[i].x
                intValues[i * 3 + 1] = value[i].y
                intValues[i * 3 + 2] = value[i].z
            }
            if (useProgramUniform) {
                glProgramUniform3iv(programObject, index, intValues)
            } else {
                bound {
                    glUniform3iv(index, intValues)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<IntVector2>) {

        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }

            val intValues = IntArray(value.size * 3)
            for (i in value.indices) {
                intValues[i * 2] = value[i].x
                intValues[i * 2 + 1] = value[i].y
            }
            if (useProgramUniform) {
                glProgramUniform2iv(programObject, index, intValues)
            } else {
                bound {
                    glUniform2iv(index, intValues)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<Vector2>) {
        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }
            val floatValues = FloatArray(value.size * 2)
            for (i in value.indices) {
                floatValues[i * 2] = value[i].x.toFloat()
                floatValues[i * 2 + 1] = value[i].y.toFloat()
            }
            if (useProgramUniform) {
                glProgramUniform2fv(programObject, index, floatValues)
            } else {
                bound {
                    glUniform2fv(index, floatValues)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<Vector3>) {
        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }
            val floatValues = FloatArray(value.size * 3)
            for (i in value.indices) {
                floatValues[i * 3] = value[i].x.toFloat()
                floatValues[i * 3 + 1] = value[i].y.toFloat()
                floatValues[i * 3 + 2] = value[i].z.toFloat()
            }
            if (useProgramUniform) {
                glProgramUniform3fv(programObject, index, floatValues)
            } else {
                bound {
                    glUniform3fv(index, floatValues)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<Vector4>) {
        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }

            val floatValues = FloatArray(value.size * 4)
            for (i in value.indices) {
                floatValues[i * 4] = value[i].x.toFloat()
                floatValues[i * 4 + 1] = value[i].y.toFloat()
                floatValues[i * 4 + 2] = value[i].z.toFloat()
                floatValues[i * 4 + 3] = value[i].w.toFloat()
            }
            if (useProgramUniform) {
                glProgramUniform4fv(programObject, index, floatValues)
            } else {
                bound {
                    glUniform4fv(index, floatValues)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: FloatArray) {

        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }
            if (useProgramUniform) {
                glProgramUniform1fv(programObject, index, value)
            } else {
                bound {
                    glUniform1fv(index, value)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: IntArray) {
        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }
            if (useProgramUniform) {
                glProgramUniform1iv(programObject, index, value)
            } else {
                bound {
                    glUniform1iv(index, value)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<Double>) {
        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }
            @Suppress("SpellCheckingInspection") val fvalue = value.map { it.toFloat() }.toFloatArray()
            if (useProgramUniform) {
                glProgramUniform1fv(programObject, index, fvalue)
            } else {
                bound {
                    glUniform1fv(index, fvalue)
                    postUniformCheck(name, index, fvalue)
                }
            }
        }
    }


    override fun uniform(name: String, value: Array<ColorRGBa>) {
        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }


            val floatValues = FloatArray(value.size * 4)
            for (i in value.indices) {
                val lvalue = value[i].toLinear()
                floatValues[i * 4] = lvalue.r.toFloat()
                floatValues[i * 4 + 1] = lvalue.g.toFloat()
                floatValues[i * 4 + 2] = lvalue.b.toFloat()
                floatValues[i * 4 + 3] = lvalue.alpha.toFloat()
            }
            if (useProgramUniform) {
                glProgramUniform4fv(programObject, index, floatValues)
            } else {
                bound {
                    glUniform4fv(index, floatValues)
                    postUniformCheck(name, index, value)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<Matrix33>) {
        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }
            stackPush().use { stack ->
                val floatValues = stack.mallocFloat(value.size * 3 * 3)
                for (j in value.indices) {
                    value[j].put(floatValues)
                }
                floatValues.flip()
                if (useProgramUniform) {
                    glProgramUniformMatrix3fv(programObject, index, false, floatValues)
                } else {
                    bound {
                        glUniformMatrix3fv(index, false, floatValues)
                    }
                }
            }
            postUniformCheck(name, index, value)
        }
    }

    override fun uniform(name: String, value: Array<Matrix44>) {
        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }

            stackPush().use { stack ->
                val floatValues = stack.mallocFloat(value.size * 4 * 4)
                for (j in value.indices) {
                    value[j].put(floatValues)
                }
                floatValues.flip()
                if (useProgramUniform) {
                    glProgramUniformMatrix4fv(programObject, index, false, floatValues)
                } else {
                    bound {
                        glUniformMatrix4fv(index, false, floatValues)
                    }
                }

            }
            postUniformCheck(name, index, value)
        }
    }

    private fun postUniformCheck(name: String, index: Int, @Suppress("UNUSED_PARAMETER") value: Any) {
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
}