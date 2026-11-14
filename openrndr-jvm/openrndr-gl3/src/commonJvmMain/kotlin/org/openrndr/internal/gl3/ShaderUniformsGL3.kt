package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ShaderUniforms
import org.openrndr.math.*

private val logger = KotlinLogging.logger {}

/**
 * Runs [f] with the program of this shader in use, for drivers without program uniforms
 */
internal inline fun ShaderUniformsGL3.bound(f: () -> Unit) {
    glUseProgram(programObject)
    f()
}

interface ShaderUniformsGL3 : ShaderUniforms {
    val programObject: Int
    val name: String
    val uniforms: MutableMap<String, Int>

    /**
     * The values last set at the uniform locations, values that are set again are not uploaded
     */
    val uniformCache: UniformValueCache

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
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            val linearValue = value.toLinear()
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
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Vector3) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            if (useProgramUniform) {
                glProgramUniform3f(programObject, index, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
            } else {
                bound {
                    glUniform3f(index, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
                    postUniformCheck(name, index)
                }
            }
        }
    }

    @Suppress("DuplicatedCode")
    override fun uniform(name: String, value: Vector4) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
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
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float, w: Float) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
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
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
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
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            if (useProgramUniform) {
                glProgramUniform2f(programObject, index, x, y)
            } else {
                bound {
                    glUniform2f(index, x, y)
                }
            }
        }
    }

    override fun uniform(name: String, value: Int) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            if (useProgramUniform) {
                glProgramUniform1i(programObject, index, value)
            } else {
                bound {
                    glUniform1i(index, value)
                    postUniformCheck(name, index)
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
        if (index != -1 && uniformCache.changed(index, value)) {
            if (useProgramUniform) {
                glProgramUniform2i(programObject, index, value.x, value.y)
            } else {
                bound {
                    glUniform2i(index, value.x, value.y)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: IntVector3) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            if (useProgramUniform) {
                glProgramUniform3i(programObject, index, value.x, value.y, value.z)
            } else {
                bound {
                    glUniform3i(index, value.x, value.y, value.z)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: IntVector4) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            if (useProgramUniform) {
                glProgramUniform4i(programObject, index, value.x, value.y, value.z, value.w)
            } else {
                bound {
                    glUniform4i(index, value.x, value.y, value.z, value.w)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Boolean) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            if (useProgramUniform) {
                glProgramUniform1i(programObject, index, if (value) 1 else 0)
            } else {
                bound {
                    glUniform1i(index, if (value) 1 else 0)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Vector2) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            if (useProgramUniform) {
                glProgramUniform2f(programObject, index, value.x.toFloat(), value.y.toFloat())
            } else {
                bound {
                    glUniform2f(index, value.x.toFloat(), value.y.toFloat())
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Float) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            if (useProgramUniform) {
                glProgramUniform1f(programObject, index, value)
            } else {
                bound {
                    glUniform1f(index, value)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Double) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            if (useProgramUniform) {
                glProgramUniform1f(programObject, index, value.toFloat())
            } else {
                bound {
                    glUniform1f(index, value.toFloat())
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Matrix33) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            val floats = UniformScratch.current.floats(9)
            value.copyInto(floats)
            if (useProgramUniform) {
                glProgramUniformMatrix3fv(programObject, index, false, floats)
            } else {
                bound {
                    logger.traceIfEnabled { "Setting uniform '$name' to $value" }
                    glUniformMatrix3fv(index, false, floats)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Matrix44) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            val floats = UniformScratch.current.floats(16)
            value.copyInto(floats)
            if (useProgramUniform) {
                glProgramUniformMatrix4fv(programObject, index, false, floats)
            } else {
                bound {
                    logger.traceIfEnabled { "Setting uniform '$name' to $value" }
                    glUniformMatrix4fv(index, false, floats)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    /**
     * Sets a `float[25]` uniform to [value], uploaded when it changed
     */
    override fun uniform(name: String, value: Matrix55) {
        val index = uniformIndex(name)
        if (index != -1 && uniformCache.changed(index, value)) {
            val floats = UniformScratch.current.floats(25)
            value.copyInto(floats)
            if (useProgramUniform) {
                glProgramUniform1fv(programObject, index, floats)
            } else {
                bound {
                    glUniform1fv(index, floats)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<IntVector4>) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }

            val intValues = UniformScratch.current.ints(value.size * 4)
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
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<IntVector3>) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }

            val intValues = UniformScratch.current.ints(value.size * 3)
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
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<IntVector2>) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }

            val intValues = UniformScratch.current.ints(value.size * 2)
            for (i in value.indices) {
                intValues[i * 2] = value[i].x
                intValues[i * 2 + 1] = value[i].y
            }
            if (useProgramUniform) {
                glProgramUniform2iv(programObject, index, intValues)
            } else {
                bound {
                    glUniform2iv(index, intValues)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<Vector2>) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }
            val floatValues = UniformScratch.current.floats(value.size * 2)
            for (i in value.indices) {
                floatValues[i * 2] = value[i].x.toFloat()
                floatValues[i * 2 + 1] = value[i].y.toFloat()
            }
            if (useProgramUniform) {
                glProgramUniform2fv(programObject, index, floatValues)
            } else {
                bound {
                    glUniform2fv(index, floatValues)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<Vector3>) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }
            val floatValues = UniformScratch.current.floats(value.size * 3)
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
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<Vector4>) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }

            val floatValues = UniformScratch.current.floats(value.size * 4)
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
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: FloatArray) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }
            if (useProgramUniform) {
                glProgramUniform1fv(programObject, index, value)
            } else {
                bound {
                    glUniform1fv(index, value)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: IntArray) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }
            if (useProgramUniform) {
                glProgramUniform1iv(programObject, index, value)
            } else {
                bound {
                    glUniform1iv(index, value)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<Double>) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }
            val floatValues = UniformScratch.current.floats(value.size)
            for (i in value.indices) {
                floatValues[i] = value[i].toFloat()
            }
            if (useProgramUniform) {
                glProgramUniform1fv(programObject, index, floatValues)
            } else {
                bound {
                    glUniform1fv(index, floatValues)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<ColorRGBa>) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }

            val floatValues = UniformScratch.current.floats(value.size * 4)
            for (i in value.indices) {
                val linearValue = value[i].toLinear()
                floatValues[i * 4] = linearValue.r.toFloat()
                floatValues[i * 4 + 1] = linearValue.g.toFloat()
                floatValues[i * 4 + 2] = linearValue.b.toFloat()
                floatValues[i * 4 + 3] = linearValue.alpha.toFloat()
            }
            if (useProgramUniform) {
                glProgramUniform4fv(programObject, index, floatValues)
            } else {
                bound {
                    glUniform4fv(index, floatValues)
                    postUniformCheck(name, index)
                }
            }
        }
    }

    override fun uniform(name: String, value: Array<Matrix33>) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }
            val floatValues = UniformScratch.current.floats(value.size * 9)
            for (j in value.indices) {
                value[j].copyInto(floatValues, j * 9)
            }
            if (useProgramUniform) {
                glProgramUniformMatrix3fv(programObject, index, false, floatValues)
            } else {
                bound {
                    glUniformMatrix3fv(index, false, floatValues)
                }
            }
            postUniformCheck(name, index)
        }
    }

    override fun uniform(name: String, value: Array<Matrix44>) {
        val index = uniformIndex(name)
        if (index != -1) {
            // values of this type are not compared, the next value of another type is uploaded
            uniformCache.clear(index)
            logger.traceIfEnabled { "Setting uniform '$name' to $value" }
            val floatValues = UniformScratch.current.floats(value.size * 16)
            for (j in value.indices) {
                value[j].copyInto(floatValues, j * 16)
            }
            if (useProgramUniform) {
                glProgramUniformMatrix4fv(programObject, index, false, floatValues)
            } else {
                bound {
                    glUniformMatrix4fv(index, false, floatValues)
                }
            }
            postUniformCheck(name, index)
        }
    }

    private fun postUniformCheck(name: String, index: Int) {
        debugGLErrors {
            val currentProgram = glGetInteger(GL_CURRENT_PROGRAM)

            fun checkUniform(): String {
                if (currentProgram > 0) {
                    val lengthBuffer = IntArray(1)
                    val sizeBuffer = IntArray(1)
                    val typeBuffer = IntArray(1)
                    val retrievedName = glGetActiveUniform(currentProgram, index, lengthBuffer, sizeBuffer, typeBuffer)
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
