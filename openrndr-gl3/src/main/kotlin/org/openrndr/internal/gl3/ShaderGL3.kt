package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33C.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.math.*
import java.io.File
import java.io.FileWriter
import java.nio.Buffer
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger {}

private var blockBindings = 0

class UniformBlockGL3(override val layout: UniformBlockLayout, val blockBinding: Int, val ubo: Int, val shadowBuffer: ByteBuffer) : UniformBlock {

    internal val thread = Thread.currentThread()
    private val lastValues = mutableMapOf<String, Any>()
    var realDirty: Boolean = true
    override val dirty: Boolean
        get() = realDirty

    companion object {
        fun create(layout: UniformBlockLayout): UniformBlockGL3 {
            synchronized(Driver.driver) {
                val ubo = glGenBuffers()
                glBindBuffer(GL_UNIFORM_BUFFER, ubo)
                glBufferData(GL_UNIFORM_BUFFER, layout.sizeInBytes.toLong(), GL_DYNAMIC_DRAW)
                glBindBuffer(GL_UNIFORM_BUFFER, 0)

                glBindBufferBase(GL_UNIFORM_BUFFER, blockBindings, ubo)
                blockBindings++
                val buffer = BufferUtils.createByteBuffer(layout.sizeInBytes)
                return UniformBlockGL3(layout, blockBindings - 1, ubo, buffer)
            }
        }
    }

    override fun uniform(name: String, value: Float) {
        if (lastValues[name] != value) {
            val entry = layout.entries[name]
            if (entry != null) {
                if (entry.type == UniformType.FLOAT32 && entry.size == 1) {
                    shadowBuffer.putFloat(entry.offset, value)
                } else {
                    throw RuntimeException("uniform mismatch")
                }
            } else {
                throw RuntimeException("uniform not found $name")
            }
            lastValues[name] = value
            realDirty = true
        }
    }

    override fun uniform(name: String, value: Vector2) {
        if (lastValues[name] != value) {
            val entry = layout.entries[name]
            if (entry != null) {
                if (entry.type == UniformType.VECTOR2_FLOAT32 && entry.size == 1) {
                    shadowBuffer.putFloat(entry.offset, value.x.toFloat())
                    shadowBuffer.putFloat(entry.offset + 4, value.y.toFloat())
                } else {
                    throw RuntimeException("uniform mismatch")
                }
            } else {
                throw RuntimeException("uniform not found $name")
            }
            lastValues[name] = value
            realDirty = true
        }
    }

    override fun uniform(name: String, value: Vector3) {
        if (lastValues[name] != value) {
            val entry = layout.entries[name]
            if (entry != null) {
                if (entry.type == UniformType.VECTOR3_FLOAT32 && entry.size == 1) {
                    shadowBuffer.putFloat(entry.offset, value.x.toFloat())
                    shadowBuffer.putFloat(entry.offset + 4, value.y.toFloat())
                    shadowBuffer.putFloat(entry.offset + 8, value.z.toFloat())
                } else {
                    throw RuntimeException("uniform mismatch")
                }
            } else {
                throw RuntimeException("uniform not found $name")
            }
            lastValues[name] = value
            realDirty = true
        }
    }

    override fun uniform(name: String, value: ColorRGBa) {
        if (lastValues[name] != value) {
            val entry = layout.entries[name]
            if (entry != null) {
                if (entry.type == UniformType.VECTOR4_FLOAT32 && entry.size == 1) {
                    shadowBuffer.putFloat(entry.offset, value.r.toFloat())
                    shadowBuffer.putFloat(entry.offset + 4, value.g.toFloat())
                    shadowBuffer.putFloat(entry.offset + 8, value.b.toFloat())
                    shadowBuffer.putFloat(entry.offset + 12, value.a.toFloat())
                } else {
                    throw RuntimeException("uniform mismatch")
                }
            } else {
                throw RuntimeException("uniform not found $name")
            }
            lastValues[name] = value
            realDirty = true
        }
    }

    override fun uniform(name: String, value: Vector4) {
        if (lastValues[name] != value) {
            val entry = layout.entries[name]
            if (entry != null) {
                if (entry.type == UniformType.VECTOR4_FLOAT32 && entry.size == 1) {
                    shadowBuffer.putFloat(entry.offset, value.x.toFloat())
                    shadowBuffer.putFloat(entry.offset + 4, value.y.toFloat())
                    shadowBuffer.putFloat(entry.offset + 8, value.z.toFloat())
                    shadowBuffer.putFloat(entry.offset + 12, value.w.toFloat())
                } else {
                    throw RuntimeException("uniform mismatch")
                }
            } else {
                throw RuntimeException("uniform not found $name")
            }
            lastValues[name] = value
            realDirty = true
        }
    }


    override fun uniform(name: String, value: Matrix44) {
        if (lastValues[name] !== value) {

            val entry = layout.entries[name]
            if (entry != null) {
                if (entry.type == UniformType.MATRIX44_FLOAT32 && entry.size == 1) {
                    (shadowBuffer as Buffer).position(entry.offset)
                    shadowBuffer.putFloat(value.c0r0.toFloat())
                    shadowBuffer.putFloat(value.c0r1.toFloat())
                    shadowBuffer.putFloat(value.c0r2.toFloat())
                    shadowBuffer.putFloat(value.c0r3.toFloat())

                    shadowBuffer.putFloat(value.c1r0.toFloat())
                    shadowBuffer.putFloat(value.c1r1.toFloat())
                    shadowBuffer.putFloat(value.c1r2.toFloat())
                    shadowBuffer.putFloat(value.c1r3.toFloat())

                    shadowBuffer.putFloat(value.c2r0.toFloat())
                    shadowBuffer.putFloat(value.c2r1.toFloat())
                    shadowBuffer.putFloat(value.c2r2.toFloat())
                    shadowBuffer.putFloat(value.c2r3.toFloat())

                    shadowBuffer.putFloat(value.c3r0.toFloat())
                    shadowBuffer.putFloat(value.c3r1.toFloat())
                    shadowBuffer.putFloat(value.c3r2.toFloat())
                    shadowBuffer.putFloat(value.c3r3.toFloat())

                } else {
                    throw RuntimeException("uniform mismatch")
                }
            } else {
                throw RuntimeException("uniform not found $name")
            }
            realDirty = true
            lastValues[name] = value
        }
    }

    override fun uniform(name: String, value: Matrix55) {
        if (lastValues[name] !== value) {
            val entry = layout.entries[name]
            if (entry != null) {
                val values = value.floatArray
                if (entry.type == UniformType.FLOAT32 && entry.size == 25) {
                    for (i in 0 until 25) {
                        shadowBuffer.putFloat(entry.offset + i * entry.stride, values[i])
                    }
                } else {
                    throw RuntimeException("uniform mismatch")
                }


            } else {
                throw RuntimeException("uniform not found $name")
            }
            realDirty = true
            lastValues[name] = value
        }
    }

    override fun uniform(name: String, value: Array<Float>) {
        val entry = layout.entries[name]
        if (entry != null) {
            if (entry.type == UniformType.FLOAT32 && entry.size == value.size) {
                for (i in 0 until value.size) {
                    shadowBuffer.putFloat(entry.offset + i * entry.stride, value[i])
                }
            } else {
                throw RuntimeException("uniform mismatch")
            }
        } else {
            throw RuntimeException("uniform not found $name")
        }
        realDirty = true
    }

    override fun uniform(name: String, value: Array<Vector2>) {
        val entry = layout.entries[name]
        if (entry != null) {
            if (entry.type == UniformType.VECTOR4_FLOAT32 && entry.size == value.size) {
                shadowBuffer.safePosition(entry.offset)
                for (i in 0 until value.size) {
                    shadowBuffer.putFloat(value[i].x.toFloat())
                    shadowBuffer.putFloat(value[i].y.toFloat())
                }
            } else {
                throw RuntimeException("uniform mismatch")
            }
        } else {
            throw RuntimeException("uniform not found $name")
        }
        realDirty = true
    }

    override fun uniform(name: String, value: Array<Vector3>) {
        val entry = layout.entries[name]
        if (entry != null) {
            if (entry.type == UniformType.VECTOR4_FLOAT32 && entry.size == value.size) {
                shadowBuffer.safePosition(entry.offset)
                for (i in 0 until value.size) {
                    shadowBuffer.putFloat(value[i].x.toFloat())
                    shadowBuffer.putFloat(value[i].y.toFloat())
                    shadowBuffer.putFloat(value[i].z.toFloat())
                }
            } else {
                throw RuntimeException("uniform mismatch")
            }
        } else {
            throw RuntimeException("uniform not found $name")
        }
        realDirty = true
    }

    override fun uniform(name: String, value: Array<Vector4>) {
        val entry = layout.entries[name]
        if (entry != null) {
            if (entry.type == UniformType.VECTOR4_FLOAT32 && entry.size == value.size) {
                shadowBuffer.safePosition(entry.offset)
                for (i in 0 until value.size) {
                    shadowBuffer.putFloat(value[i].x.toFloat())
                    shadowBuffer.putFloat(value[i].y.toFloat())
                    shadowBuffer.putFloat(value[i].z.toFloat())
                }
            } else {
                throw RuntimeException("uniform mismatch")
            }
        } else {
            throw RuntimeException("uniform not found $name")
        }
        realDirty = true
    }

    override fun upload() {
        if (Thread.currentThread() != thread) {
            throw IllegalStateException("current thread ${Thread.currentThread()} is not equal to creation thread $thread")
        }

        realDirty = false
        glBindBuffer(GL_UNIFORM_BUFFER, ubo)
        shadowBuffer.safeRewind()
        glBufferSubData(GL_UNIFORM_BUFFER, 0L, shadowBuffer)
        checkGLErrors()
        glBindBuffer(GL_UNIFORM_BUFFER, 0)
    }



}

private fun ByteBuffer.safePosition(offset: Int) {
    (this as Buffer).position(offset)
}

private fun ByteBuffer.safeRewind() {
    (this as Buffer).rewind()
}

internal fun checkShaderInfoLog(`object`: Int, code: String, sourceFile: String) {
    logger.debug { "getting shader info log" }
    val logLength = IntArray(1)
    glGetShaderiv(`object`, GL_INFO_LOG_LENGTH, logLength)
    logger.debug { "log length: ${logLength[0]}" }

    if (logLength[0] > 0) {
        logger.debug { "getting log" }
        val infoLog = BufferUtils.createByteBuffer(logLength[0])
        (infoLog as Buffer).rewind()
        glGetShaderInfoLog(`object`, logLength, infoLog)

        // logger.warn {
        val infoBytes = ByteArray(logLength[0])
        infoLog.get(infoBytes)
        println("GLSL compilation problems in\n ${String(infoBytes)}")
        //   "GLSL compilation problems in\n ${String(infoBytes)}"
        // }

        val temp = File("ShaderError.txt")
        FileWriter(temp).use {
            it.write(code)
        }
        System.err.println("click.to.see.shader.code(ShaderError.txt:1)")
        logger.error { "GLSL shader compilation failed for $sourceFile" }
        throw Exception("Shader error: " + sourceFile)
    }
}

fun checkProgramInfoLog(`object`: Int, sourceFile: String) {
    val logLength = IntArray(1)
    glGetProgramiv(`object`, GL_INFO_LOG_LENGTH, logLength)

    if (logLength[0] > 1) {
        val infoLog = BufferUtils.createByteBuffer(logLength[0])
        glGetProgramInfoLog(`object`, logLength, infoLog)
        val linkInfoBytes = ByteArray(logLength[0])
        infoLog.get(linkInfoBytes)
        println("GLSL link problems in\n ${String(linkInfoBytes)}")

        logger.warn {
            val infoBytes = ByteArray(logLength[0])
            infoLog.get(infoBytes)
            "GLSL link problems in\n ${String(infoBytes)}"
        }
        throw Exception("Shader error: " + sourceFile)
    }
}

class ShaderGL3(val program: Int,
                val name: String,
                val vertexShader: VertexShaderGL3,
                val fragmentShader: FragmentShaderGL3) : Shader {

    private var destroyed = false
    private var running = false
    private var uniforms: MutableMap<String, Int> = hashMapOf()
    private var attributes: MutableMap<String, Int> = hashMapOf()
    private var blockBindings = hashMapOf<String, Int>()
    private val blocks: MutableMap<String, Int> = hashMapOf()

    companion object {
        fun create(vertexShader: VertexShaderGL3, fragmentShader: FragmentShaderGL3): ShaderGL3 {

            synchronized(Driver.driver) {
                debugGLErrors()
                val name = "${vertexShader.name} / ${fragmentShader.name}"

                val program = glCreateProgram()
                debugGLErrors()

                glAttachShader(program, vertexShader.shaderObject)
                debugGLErrors()

                glAttachShader(program, fragmentShader.shaderObject)
                debugGLErrors()

                glLinkProgram(program)
                debugGLErrors()

                val linkStatus = IntArray(1)
                glGetProgramiv(program, GL_LINK_STATUS, linkStatus)
                debugGLErrors()

                if (linkStatus[0] != GL_TRUE) {
                    checkProgramInfoLog(program, "noname")
                }

                glFinish()

                return ShaderGL3(program, name, vertexShader, fragmentShader)
            }
        }
    }

    override fun createBlock(blockName: String): UniformBlock {
        val layout = blockLayout(blockName)
        if (layout != null) {
            return UniformBlockGL3.create(layout)

        } else {
            throw RuntimeException("block does not exists $blockName")
        }
    }

    override fun blockLayout(blockName: String): UniformBlockLayout? {
        val blockIndex = glGetUniformBlockIndex(program, blockName)

        if (blockIndex == -1) {
            return null
        }

        val blockSize = run {
            val blockSizeBuffer = BufferUtils.createIntBuffer(1)
            glGetActiveUniformBlockiv(program, blockIndex, GL_UNIFORM_BLOCK_DATA_SIZE, blockSizeBuffer)
            blockSizeBuffer[0]
        }

        if (blockSize != 0) {
            val uniformCount = run {
                val uniformCountBuffer = BufferUtils.createIntBuffer(1)
                glGetActiveUniformBlockiv(program, blockIndex, GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS, uniformCountBuffer)
                uniformCountBuffer[0]
            }

            val uniformIndicesBuffer = BufferUtils.createIntBuffer(uniformCount)
            val uniformIndices = run {
                glGetActiveUniformBlockiv(program, blockIndex, GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES, uniformIndicesBuffer)
                (uniformIndicesBuffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                uniformIndicesBuffer.get(array)
                array
            }

            (uniformIndicesBuffer as Buffer).rewind()

            val uniformTypes = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(program, uniformIndicesBuffer, GL_UNIFORM_TYPE, buffer)
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }
            val uniformSizes = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(program, uniformIndicesBuffer, GL_UNIFORM_SIZE, buffer)
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }

            val uniformOffsets = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(program, uniformIndicesBuffer, GL_UNIFORM_OFFSET, buffer)
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }

            val uniformStrides = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(program, uniformIndicesBuffer, GL_UNIFORM_ARRAY_STRIDE, buffer)
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }

            val uniformNames = uniformIndices.map {
                glGetActiveUniformName(program, it, 128)
            }

            checkGLErrors()

            return UniformBlockLayout(blockSize, (0 until uniformCount).map {
                UniformDescription(uniformNames[it].replace(Regex("\\[.*\\]"),""), uniformTypes[it].toUniformType(), uniformSizes[it], uniformOffsets[it], uniformStrides[it])
            }.associate { Pair(it.name, it) })


        } else {
            return null
        }

    }

    override fun block(blockName: String, block: UniformBlock) {
        if (Thread.currentThread() != (block as UniformBlockGL3).thread) {
            throw IllegalStateException("block is created on ${block.thread} and is now used on ${Thread.currentThread()}")
        }

        if (!running) {
            throw IllegalStateException("use begin() before setting blocks")
        }
        val blockIndex = blockIndex(blockName)
        if (blockIndex == -1) {
            throw IllegalArgumentException("block not found $blockName")
        }

        if (blockBindings[blockName] != block.blockBinding) {
            //checkGLErrors()
            glUniformBlockBinding(program, blockIndex, block.blockBinding)
            debugGLErrors()
            blockBindings[blockName] = block.blockBinding
        }
    }

    fun blockIndex(block: String): Int {
        return blocks.getOrPut(block) {
            glGetUniformBlockIndex(program, block)
        }
    }

    fun uniformIndex(uniform: String, query: Boolean = false): Int =
            uniforms.getOrPut(uniform) {
                val location = glGetUniformLocation(program, uniform)
                debugGLErrors()
                if (location == -1 && !query) {
                    logger.warn {
                        "shader ${name} does not have uniform $uniform"
                    }
                }
                location
            }

    override fun begin() {
        logger.trace { "shader begin $name" }
        running = true
        glUseProgram(program)
        debugGLErrors {
            when (it) {
                GL_INVALID_VALUE -> "program is neither 0 nor a value generated by OpenGL"
                GL_INVALID_OPERATION -> "program ($program) is not a program object / program could not be made part of current state / transform feedback mode is active."
                else -> null
            }
        }
    }

    override fun end() {
        logger.trace { "shader end $name" }
        glUseProgram(0)
        debugGLErrors()
        running = false
    }

    override fun hasUniform(name: String): Boolean {
        return uniformIndex(name, true) != -1
    }

    override fun uniform(name: String, value: ColorRGBa) {
        val index = uniformIndex(name)
        if (index != -1) {
            glUniform4f(index, value.r.toFloat(), value.g.toFloat(), value.b.toFloat(), value.a.toFloat())
            postUniformCheck(name, index, value)
        }
    }

    override fun uniform(name: String, value: Vector3) {
        val index = uniformIndex(name)
        if (index != -1) {
            glUniform3f(index, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
            postUniformCheck(name, index, value)
        }
    }

    override fun uniform(name: String, value: Vector4) {
        val index = uniformIndex(name)
        if (index != -1) {
            glUniform4f(index, value.x.toFloat(), value.y.toFloat(), value.z.toFloat(), value.w.toFloat())
            postUniformCheck(name, index, value)
        }
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float, w: Float) {
        val index = uniformIndex(name)
        if (index != -1) {
            glUniform4f(index, x, y, z, w)
        }
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float) {
        val index = uniformIndex(name)
        if (index != -1) {
            glUniform3f(index, x, y, z)
        }
    }

    override fun uniform(name: String, x: Float, y: Float) {
        val index = uniformIndex(name)
        if (index != -1) {
            glUniform2f(index, x, y)
        }
    }

    override fun uniform(name: String, value: Int) {
        val index = uniformIndex(name)
        if (index != -1) {
            glUniform1i(index, value)
            postUniformCheck(name, index, value)
        }
    }

    override fun uniform(name: String, value: Boolean) {
        val index = uniformIndex(name)
        if (index != -1) {
            glUniform1i(index, if (value) 1 else 0)
            postUniformCheck(name, index, value)
        }
    }

    override fun uniform(name: String, value: Vector2) {
        val index = uniformIndex(name)
        if (index != -1) {
            glUniform2f(index, value.x.toFloat(), value.y.toFloat())
            postUniformCheck(name, index, value)
        }
    }

    override fun uniform(name: String, value: Float) {
        val index = uniformIndex(name)
        if (index != -1) {
            glUniform1f(index, value)
            postUniformCheck(name, index, value)
        }
    }

    override fun uniform(name: String, value: Double) {
        val index = uniformIndex(name)
        if (index != -1) {
            glUniform1f(index, value.toFloat())
            postUniformCheck(name, index, value)
        }
    }

    override fun uniform(name: String, value: Matrix33) {
        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }
            glUniformMatrix3fv(index, false, value.toFloatArray())
            postUniformCheck(name, index, value)
        }
    }


    override fun uniform(name: String, value: Matrix44) {
        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }
            glUniformMatrix4fv(index, false, value.toFloatArray())
            postUniformCheck(name, index, value)
        }
    }

    override fun uniform(name: String, value: Array<Vector2>) {
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

    override fun uniform(name: String, value: Array<Vector3>) {
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

    override fun uniform(name: String, value: Array<Vector4>) {
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

    override fun uniform(name: String, value: FloatArray) {
        val index = uniformIndex(name)
        if (index != -1) {
            logger.trace { "Setting uniform '$name' to $value" }
            glUniform1fv(index, value)
            postUniformCheck(name, index, value)
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
                    nameBuffer.safeRewind()
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

    fun attributeIndex(name: String): Int =
            attributes.getOrPut(name) {
                val location = glGetAttribLocation(program, name)
                debugGLErrors()
                location
            }

    override fun destroy() {
        if (!destroyed) {
            glDeleteProgram(program)
            destroyed = true
            Session.active.untrack(this)
        }
    }

}

private fun Int.toUniformType(): UniformType {
    return when (this) {
        GL_FLOAT -> UniformType.FLOAT32
        GL_FLOAT_VEC2 -> UniformType.VECTOR2_FLOAT32
        GL_FLOAT_VEC3 -> UniformType.VECTOR3_FLOAT32
        GL_FLOAT_VEC4 -> UniformType.VECTOR4_FLOAT32
        GL_INT -> UniformType.VECTOR2_INT32
        GL_INT_VEC2 -> UniformType.VECTOR2_INT32
        GL_INT_VEC3 -> UniformType.VECTOR3_INT32
        GL_INT_VEC4 -> UniformType.VECTOR4_INT32
        GL_FLOAT_MAT4 -> UniformType.MATRIX44_FLOAT32
        GL_FLOAT_MAT3 -> UniformType.MATRIX33_FLOAT32
        GL_FLOAT_MAT2 -> UniformType.MATRIX22_FLOAT32
        else -> throw RuntimeException("unsupported uniform type $this")
    }
}

