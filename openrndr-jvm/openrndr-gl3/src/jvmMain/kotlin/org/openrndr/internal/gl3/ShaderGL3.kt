package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.opengl.GL43C
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.measure
import java.io.File
import java.io.FileWriter
import java.nio.Buffer
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

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

        val infoBytes = ByteArray(logLength[0])

        // NVIDIA driver: `0(72) : error`
        // Intel driver:  `0:72(8): error`
        val errorLinePattern = "\\d+[:(](\\d+)[ 0-9()]*?:".toRegex()

        infoLog.get(infoBytes)
        val infoString = String(infoBytes)
        println("GLSL compilation problems in\n $infoString")

        val errorLineMatches = errorLinePattern.find(infoString)
        val firstErrorLine = if (errorLineMatches != null) {
            errorLineMatches.groupValues[1].toIntOrNull()
        } else {
            1
        } ?: 1

        val errorLogFile = File("ShaderError.glsl")

        if (errorLogFile.exists()) {
            if (!errorLogFile.canWrite()) {
                errorLogFile.setWritable(true)
            }
            errorLogFile.delete()
        }

        FileWriter(errorLogFile).use {
            it.write(code)
            it.write("// -------------\n")
            it.write("// $sourceFile\n")
            it.write("// created ${LocalDateTime.now()}\n")
            it.write("/*\n")
            it.write(infoString)
            it.write("*/\n")
        }
        errorLogFile.setReadOnly()

        // This is to trick IntelliJ into displaying a clickable link.
        System.err.println("click.to.see.shader.code(ShaderError.glsl:$firstErrorLine)")
        logger.error { "GLSL shader compilation failed for $sourceFile" }
        throw Exception("Shader error: $sourceFile")
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
        throw Exception("Shader error: $sourceFile")
    }
}

class ShaderGL3(
    override val programObject: Int,
    override val name: String,
    vertexShader: VertexShaderGL3,
    tessellationControlShader: TessellationControlShaderGL3?,
    tessellationEvaluationShader: TessellationEvaluationShaderGL3?,
    geometryShader: GeometryShaderGL3?,
    fragmentShader: FragmentShaderGL3,
    override val session: Session?
) : Shader, ShaderUniformsGL3, ShaderBufferBindingsGL3, ShaderImageBindingsGL43 {

    override val ssbo: Int = createSSBO()
    override val useProgramUniform = (Driver.instance as DriverGL3).version >= DriverVersionGL.VERSION_4_2

    override val types: Set<ShaderType> =
        if (geometryShader != null) setOf(ShaderType.VERTEX, ShaderType.GEOMETRY, ShaderType.FRAGMENT) else
            setOf(ShaderType.VERTEX, ShaderType.FRAGMENT)

    private var destroyed = false
    private var running = false
    override var uniforms: MutableMap<String, Int> = hashMapOf()
    private var attributes: MutableMap<String, Int> = hashMapOf()
    private var blockBindings = hashMapOf<String, Int>()
    private val blocks: MutableMap<String, Int> = hashMapOf()

    companion object {
        fun create(
            vertexShader: VertexShaderGL3,
            tessellationControlShader: TessellationControlShaderGL3?,
            tessellationEvaluationShader: TessellationEvaluationShaderGL3?,
            geometryShader: GeometryShaderGL3?,
            fragmentShader: FragmentShaderGL3,
            name: String,
            session: Session?
        ): ShaderGL3 {
            synchronized(Driver.instance) {
                debugGLErrors()

                val program = glCreateProgram()
                debugGLErrors()

                glAttachShader(program, vertexShader.shaderObject)
                debugGLErrors()

                tessellationControlShader?.let {
                    glAttachShader(program, it.shaderObject)
                    debugGLErrors()
                }

                tessellationEvaluationShader?.let {
                    glAttachShader(program, it.shaderObject)
                    debugGLErrors()
                }

                geometryShader?.let {
                    glAttachShader(program, it.shaderObject)
                    debugGLErrors()
                }


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
                return ShaderGL3(
                    program,
                    name,
                    vertexShader,
                    tessellationControlShader,
                    tessellationEvaluationShader,
                    geometryShader,
                    fragmentShader,
                    session
                )
            }
        }
    }


    override fun hasUniform(name: String): Boolean {
        return uniformIndex(name, true) != -1
    }


    override fun createBlock(blockName: String): UniformBlock? {
        val layout = blockLayout(blockName)
        return if (layout != null) {
            UniformBlockGL3.create(layout)
        } else {
            null
        }
    }

    override fun blockLayout(blockName: String): UniformBlockLayout? {
        val blockIndex = glGetUniformBlockIndex(programObject, blockName)

        if (blockIndex == -1) {
            return null
        }

        val blockSize = run {
            val blockSizeBuffer = BufferUtils.createIntBuffer(1)
            glGetActiveUniformBlockiv(programObject, blockIndex, GL_UNIFORM_BLOCK_DATA_SIZE, blockSizeBuffer)
            blockSizeBuffer[0]
        }

        if (blockSize != 0) {
            val uniformCount = run {
                val uniformCountBuffer = BufferUtils.createIntBuffer(1)
                glGetActiveUniformBlockiv(programObject, blockIndex, GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS, uniformCountBuffer)
                uniformCountBuffer[0]
            }

            val uniformIndicesBuffer = BufferUtils.createIntBuffer(uniformCount)
            val uniformIndices = run {
                glGetActiveUniformBlockiv(
                    programObject,
                    blockIndex,
                    GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES,
                    uniformIndicesBuffer
                )
                (uniformIndicesBuffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                uniformIndicesBuffer.get(array)
                array
            }

            (uniformIndicesBuffer as Buffer).rewind()

            val uniformTypes = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(programObject, uniformIndicesBuffer, GL_UNIFORM_TYPE, buffer)
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }
            val uniformSizes = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(programObject, uniformIndicesBuffer, GL_UNIFORM_SIZE, buffer)
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }

            val uniformOffsets = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(programObject, uniformIndicesBuffer, GL_UNIFORM_OFFSET, buffer)
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }

            val uniformStrides = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(programObject, uniformIndicesBuffer, GL_UNIFORM_ARRAY_STRIDE, buffer)
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }

            val uniformNames = uniformIndices.map {
                glGetActiveUniformName(programObject, it, 128)
            }

            checkGLErrors()

            return UniformBlockLayout(blockSize, (0 until uniformCount).map {
                UniformDescription(
                    uniformNames[it].replace(Regex("\\[.*\\]"), ""),
                    uniformTypes[it].toUniformType(),
                    uniformSizes[it],
                    uniformOffsets[it],
                    uniformStrides[it]
                )
            }.associateBy { it.name })


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
        if (blockIndex != -1) {
            if (blockBindings[blockName] != block.blockBinding) {
                //checkGLErrors()
                glUniformBlockBinding(programObject, blockIndex, block.blockBinding)
                debugGLErrors()
                blockBindings[blockName] = block.blockBinding
            }
        }
    }

    fun blockIndex(block: String): Int {
        return blocks.getOrPut(block) {
            glGetUniformBlockIndex(programObject, block)
        }
    }


    override fun begin() {
        glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, ssbo)
        logger.trace { "shader begin $name" }
        running = true
        measure("glUseProgram $programObject") {
            glUseProgram(programObject)
        }
        debugGLErrors {
            when (it) {
                GL_INVALID_VALUE -> "program is neither 0 nor a value generated by OpenGL"
                GL_INVALID_OPERATION -> "program ($programObject) is not a program object / program could not be made part of current state / transform feedback mode is active."
                else -> null
            }
        }
    }

    override fun end() {
        logger.trace { "shader end $name" }
        measure("glUseProgram 0") {
            glUseProgram(0)
        }
        debugGLErrors()
        running = false
    }

    fun attributeIndex(name: String): Int =
        attributes.getOrPut(name) {
            val location = glGetAttribLocation(programObject, name)
            debugGLErrors()
            location
        }

    override fun destroy() {
        if (!destroyed) {
            session?.untrack(this)
            glDeleteProgram(programObject)
            destroyed = true
            (Driver.instance as DriverGL3).destroyVAOsForShader(this)
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
        GL_INT -> UniformType.INT32
        GL_INT_VEC2 -> UniformType.VECTOR2_INT32
        GL_INT_VEC3 -> UniformType.VECTOR3_INT32
        GL_INT_VEC4 -> UniformType.VECTOR4_INT32
        GL_FLOAT_MAT4 -> UniformType.MATRIX44_FLOAT32
        GL_FLOAT_MAT3 -> UniformType.MATRIX33_FLOAT32
        GL_FLOAT_MAT2 -> UniformType.MATRIX22_FLOAT32
        else -> throw RuntimeException("unsupported uniform type $this")
    }
}

