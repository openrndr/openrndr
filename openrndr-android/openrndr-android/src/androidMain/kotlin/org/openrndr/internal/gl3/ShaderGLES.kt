package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.Session
import org.openrndr.draw.Shader
import org.openrndr.draw.ShaderType
import org.openrndr.draw.UniformBlock
import org.openrndr.draw.UniformBlockLayout
import org.openrndr.draw.UniformDescription
import org.openrndr.draw.UniformType
import org.openrndr.internal.Driver
import java.nio.Buffer

private val logger = KotlinLogging.logger {}

class ShaderGLES(
    override val programObject: Int,
    override val name: String,
    @Suppress("UNUSED_PARAMETER") vertexShader: VertexShaderGL3,
    @Suppress("UNUSED_PARAMETER") tessellationControlShader: TessellationControlShaderGL3?,
    @Suppress("UNUSED_PARAMETER") tessellationEvaluationShader: TessellationEvaluationShaderGL3?,
    geometryShader: GeometryShaderGL3?,
    @Suppress("UNUSED_PARAMETER") fragmentShader: FragmentShaderGL3,
    override val session: Session?
) : Shader, ShaderUniformsGL3, ShaderBufferBindingsGLES, ShaderImageBindingsGL43 {

    override val ssbo: Int = createSSBO()
    override val ssboResourceIndices = mutableMapOf<String, Int>()
    override val useProgramUniform = Driver.capabilities.programUniform

    override val types: Set<ShaderType> =
        if (geometryShader != null) setOf(
            ShaderType.VERTEX,
            ShaderType.GEOMETRY,
            ShaderType.FRAGMENT
        ) else
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
        ): ShaderGLES {
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
                return ShaderGLES(
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

        val blockIndex = blockIndex(blockName)

        if (blockIndex == -1) {
            return null
        }

        val blockSize = run {
            val blockSizeBuffer = BufferUtils.createIntBuffer(1)
            glGetActiveUniformBlockiv(
                programObject,
                blockIndex,
                GL_UNIFORM_BLOCK_DATA_SIZE,
                blockSizeBuffer
            )
            checkGLErrors {
                when (it) {
                    GL_INVALID_VALUE -> "uniformBlockIndex ($blockIndex) is greater than or equal to the value of GL_ACTIVE_UNIFORM_BLOCKS (${
                        glGetInteger(
                            GL_ACTIVE_UNIFORM_BLOCKS
                        )
                    }) or is not the index of an active uniform block in program.\n"

                    else -> null
                }
            }
            blockSizeBuffer[0]
        }

        if (blockSize != 0) {
            val uniformCount = run {
                val uniformCountBuffer = BufferUtils.createIntBuffer(1)
                glGetActiveUniformBlockiv(
                    programObject,
                    blockIndex,
                    GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS,
                    uniformCountBuffer
                )
                checkGLErrors()
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
                checkGLErrors()
                (uniformIndicesBuffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                uniformIndicesBuffer.get(array)
                array
            }

            (uniformIndicesBuffer as Buffer).rewind()

            val uniformTypes = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(programObject, uniformIndicesBuffer, GL_UNIFORM_TYPE, buffer)
                checkGLErrors()
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }
            val uniformSizes = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(programObject, uniformIndicesBuffer, GL_UNIFORM_SIZE, buffer)
                checkGLErrors()
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }

            val uniformOffsets = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(
                    programObject,
                    uniformIndicesBuffer,
                    GL_UNIFORM_OFFSET,
                    buffer
                )
                checkGLErrors()
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }

            val uniformStrides = run {
                val buffer = BufferUtils.createIntBuffer(uniformCount)
                glGetActiveUniformsiv(
                    programObject,
                    uniformIndicesBuffer,
                    GL_UNIFORM_ARRAY_STRIDE,
                    buffer
                )
                checkGLErrors()
                (buffer as Buffer).rewind()
                val array = IntArray(uniformCount)
                buffer.get(array)
                array
            }

            val uniformNames = uniformIndices.map {
                glGetActiveUniformName(programObject, it, 128).also {
                    checkGLErrors()
                }
            }


            return UniformBlockLayout(blockSize, (0 until uniformCount).map {
                @Suppress("RegExpRedundantEscape")
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
        if (Thread.currentThread() != (block as UniformBlockGLES).thread) {
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

    private fun blockIndex(block: String): Int {
        return blocks.getOrPut(block) {
            glGetUniformBlockIndex(programObject, block).also {
                checkGLErrors()
            }
        }
    }

    override fun begin() {
        if (ssbo != -1) {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
        }
        logger.trace { "shader begin $name" }
        running = true

        glUseProgram(programObject)

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

        glUseProgram(0)

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
            (Driver.instance as DriverAndroidGLES).destroyVAOsForShader(this)
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