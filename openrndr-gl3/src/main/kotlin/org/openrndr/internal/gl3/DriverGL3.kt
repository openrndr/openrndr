package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.*
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.opengl.GL31.glDrawArraysInstanced
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import org.lwjgl.opengl.GL40.glBlendFunci
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.internal.FontMapManager
import org.openrndr.internal.ResourceThread
import org.openrndr.internal.ShaderGenerators
import org.openrndr.math.Matrix44

import java.nio.Buffer

private val logger = KotlinLogging.logger {}
internal val useDebugContext = System.getProperty("org.openrndr.gl3.debug") != null

class DriverGL3 : Driver {
    override fun createResourceThread(f: () -> Unit): ResourceThread {
        return ResourceThreadGL3.create(f)
    }
    override val shaderGenerators: ShaderGenerators = ShaderGeneratorsGL3()
    private val vaos = mutableMapOf<Long, Int>()
    internal var defaultVAO = 1

    private fun hash(shader: ShaderGL3, vertexBuffers: List<VertexBuffer>, instanceAttributes: List<VertexBuffer>): Long {
        var hash = 0L
        hash += shader.program
        for (i in 0 until vertexBuffers.size) {
            hash += (vertexBuffers[i] as VertexBufferGL3).bufferHash shl (12 + (i * 12))
        }
        for (i in 0 until instanceAttributes.size) {
            hash += (instanceAttributes[i] as VertexBufferGL3).bufferHash shl (12 + ((i + vertexBuffers.size) * 12))
        }
        return hash
    }

    override fun internalShaderResource(resourceId: String): String {
        val resource = this.javaClass.getResource("shaders/$resourceId")
        if (resource != null) {
            val url = resource.toExternalForm()
            return codeFromURL(url)
        } else {
            throw RuntimeException("could not find internal shader resource $resourceId")
        }
    }

    init {
        logger.trace { "initializing DriverGL3" }
    }

    private var fontImageMapManagerInstance: FontImageMapManagerGL3? = null

    override val fontImageMapManager: FontMapManager
        get() {
            if (fontImageMapManagerInstance == null) {
                fontImageMapManagerInstance = FontImageMapManagerGL3()
            }
            return fontImageMapManagerInstance!!
        }

    override val fontVectorMapManager: FontMapManager
        get() {
            TODO("not implemented")
        }

    override fun clear(color: ColorRGBa) {
        glClearColor(color.r.toFloat(), color.g.toFloat(), color.b.toFloat(), color.a.toFloat())
        glClearDepth(1.0)
        glDisable(GL_SCISSOR_TEST)
        glDepthMask(true)

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        glDepthMask(false)
        debugGLErrors {
            when (it) {
                GL_INVALID_VALUE -> "any bit other than the three defined bits is set in mask."
                else -> null
            }
        }
    }

    override fun createStaticVertexBuffer(format: VertexFormat, buffer: Buffer): VertexBuffer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createShadeStyleManager(vertexShaderGenerator: (ShadeStructure) -> String,
                                         fragmentShaderGenerator: (ShadeStructure) -> String): ShadeStyleManager {
        return ShadeStyleManagerGL3(vertexShaderGenerator, fragmentShaderGenerator)
    }

    override fun createShader(vsCode: String, fsCode: String): Shader {
        logger.trace {
            "creating shader:\n${vsCode}\n${fsCode}"
        }
        val vertexShader = VertexShaderGL3.fromString(vsCode)
        val fragmentShader = FragmentShaderGL3.fromString(fsCode)
        return ShaderGL3.create(vertexShader, fragmentShader)
    }

    override fun createBufferTexture(elementCount: Int,
                                     format: ColorFormat,
                                     type: ColorType): BufferTexture {
        logger.trace { "creating buffer texture" }
        return BufferTextureGL3.create(elementCount, format, type)
    }

    override fun createCubemap(width: Int, format: ColorFormat, type: ColorType): Cubemap {
        logger.trace { "creating cubemap $width" }
        return CubemapGL3.create(width, format, type)
    }

    override fun createCubemapFromUrls(urls: List<String>): Cubemap {

        logger.trace { "creating cubemap from urls $urls" }
        return when {
            urls.size == 1 -> CubemapGL3.fromUrl(urls[0])
            urls.size == 6 -> CubemapGL3.fromUrls(urls)
            else -> throw RuntimeException("expected 1 or 6 urls")
        }
    }

    override fun createRenderTarget(width: Int, height: Int, contentScale: Double): RenderTarget {
        logger.trace { "creating render target $width x $height @ ${contentScale}x" }
        return RenderTargetGL3.create(width, height, contentScale)
    }

    override fun createColorBuffer(width: Int, height: Int, contentScale: Double, format: ColorFormat, type: ColorType): ColorBuffer {
        logger.trace { "creating color buffer $width x $height @ $format:$type" }
        return ColorBufferGL3.create(width, height, contentScale, format, type)
    }

    override fun createColorBufferFromUrl(url: String): ColorBuffer {
        return ColorBufferGL3.fromUrl(url)
    }

    override fun createColorBufferFromFile(url: String): ColorBuffer {
        return ColorBufferGL3.fromFile(url)
    }

    override fun createDepthBuffer(width: Int, height: Int, format: DepthFormat): DepthBuffer {
        logger.trace { "creating depth buffer $width x $height @ $format" }
        return DepthBufferGL3.create(width, height, format)
    }

    override fun createDynamicVertexBuffer(format: VertexFormat, vertexCount: Int): VertexBuffer = VertexBufferGL3.createDynamic(format, vertexCount)

    override fun drawVertexBuffer(shader: Shader, vertexBuffers: List<VertexBuffer>, drawPrimitive: DrawPrimitive, vertexOffset: Int, vertexCount: Int) {
        shader as ShaderGL3
        // -- find or create a VAO for our shader + vertex buffers combination
        val hash = hash(shader, vertexBuffers, emptyList())
        val vao = vaos.getOrPut(hash) {
            logger.debug {
                "creating new VAO for hash $hash"
            }
            val arrays = IntArray(1)
            glGenVertexArrays(arrays)
            glBindVertexArray(arrays[0])
            setupFormat(vertexBuffers, emptyList(), shader)
            glBindVertexArray(defaultVAO)
            arrays[0]
        }
        glBindVertexArray(vao)

        logger.trace { "drawing vertex buffer with $drawPrimitive(${drawPrimitive.glType()}) and $vertexCount vertices with vertexOffset $vertexOffset " }
        glDrawArrays(drawPrimitive.glType(), vertexOffset, vertexCount)
        debugGLErrors {
            when (it) {
                GL_INVALID_ENUM -> "mode ($drawPrimitive) is not an accepted value."
                GL_INVALID_VALUE -> "count ($vertexCount) is negative."
                GL_INVALID_OPERATION -> "a non-zero buffer object name is bound to an enabled array and the buffer object's data store is currently mapped."
                else -> null
            }
        }
        // -- restore defaultVAO binding
        glBindVertexArray(defaultVAO)
    }

    override fun drawInstances(shader: Shader, vertexBuffers: List<VertexBuffer>, instanceAttributes: List<VertexBuffer>, drawPrimitive: DrawPrimitive, vertexOffset: Int, vertexCount: Int, instanceCount: Int) {

        // -- find or create a VAO for our shader + vertex buffers + instance buffers combination
        val hash = hash(shader as ShaderGL3, vertexBuffers, instanceAttributes)

        val vao = vaos.getOrPut(hash) {
            logger.debug {
                "creating new instances VAO for hash $hash"
            }
            val arrays = IntArray(1)
            glGenVertexArrays(arrays)
            glBindVertexArray(arrays[0])
            setupFormat(vertexBuffers, instanceAttributes, shader)
            glBindVertexArray(defaultVAO)
            arrays[0]
        }

        glBindVertexArray(vao)

        logger.trace { "drawing $instanceCount instances with $drawPrimitive(${drawPrimitive.glType()}) and $vertexCount vertices with vertexOffset $vertexOffset " }
        glDrawArraysInstanced(drawPrimitive.glType(), vertexOffset, vertexCount, instanceCount)
        debugGLErrors {
            when (it) {
                GL_INVALID_ENUM -> "mode is not one of the accepted values."
                GL_INVALID_VALUE -> "count ($instanceCount) or primcount ($vertexCount) are negative."
                GL_INVALID_OPERATION -> "a non-zero buffer object name is bound to an enabled array and the buffer object's data store is currently mapped."
                else -> null
            }
        }
        // -- restore default VAO binding
        glBindVertexArray(defaultVAO)
    }

    private fun setupFormat(vertexBuffer: List<VertexBuffer>, instanceAttributes: List<VertexBuffer>, shader: ShaderGL3) {
        debugGLErrors()
        fun setupBuffer(buffer: VertexBuffer, divisor: Int = 0) {
            val prefix = if (divisor == 0) "a" else "i"

            glBindBuffer(GL_ARRAY_BUFFER, (buffer as VertexBufferGL3).buffer)
            val format = buffer.vertexFormat
            for (item in format.items) {
                val attributeIndex = shader.attributeIndex("${prefix}_${item.attribute}")
                if (attributeIndex != -1) {
                    if (item.type == VertexElementType.FLOAT32 || item.type == VertexElementType.VECTOR2_FLOAT32 || item.type == VertexElementType.VECTOR3_FLOAT32 || item.type == VertexElementType.VECTOR4_FLOAT32) {
                        for (i in 0 until item.arraySize) {
                            glEnableVertexAttribArray(attributeIndex)
                            debugGLErrors {
                                when (it) {
                                    GL_INVALID_OPERATION -> "no vertex array object is bound"
                                    GL_INVALID_VALUE -> "index ($attributeIndex) is greater than or equal to GL_MAX_VERTEX_ATTRIBS"
                                    else -> null
                                }
                            }
                            glVertexAttribPointer(attributeIndex,
                                    item.type.componentCount,
                                    item.type.glType(), false, format.size, item.offset.toLong() + i * item.type.sizeInBytes)
                            debugGLErrors {
                                when (it) {
                                    GL_INVALID_VALUE -> "index ($attributeIndex) is greater than or equal to GL_MAX_VERTEX_ATTRIBS"
                                    else -> null
                                }
                            }
                            glVertexAttribDivisor(attributeIndex, divisor)
                        }
                    } else if (item.type == VertexElementType.MATRIX44_FLOAT32) {
                        for (i in 0 until item.arraySize) {
                            for (column in 0 until 4) {
                                glEnableVertexAttribArray(attributeIndex + column + i * 4)
                                debugGLErrors()

                                glVertexAttribPointer(attributeIndex + column + i * 4,
                                        4,
                                        item.type.glType(), false, format.size, item.offset.toLong() + column * 16 + i * 64)
                                debugGLErrors()

                                glVertexAttribDivisor(attributeIndex + column + i * 4, 1)
                                debugGLErrors()
                            }
                        }
                    } else {
                        TODO("implement support for ${item.type}")
                    }
                }
            }
        }
        vertexBuffer.forEach {
            setupBuffer(it, 0)
        }

        instanceAttributes.forEach {
            setupBuffer(it, 1)
        }
    }

    private fun teardownFormat(format: VertexFormat, shader: ShaderGL3) {
        for (item in format.items) {
            // custom attribute
            val attributeIndex = shader.attributeIndex(item.attribute)
            if (attributeIndex != -1) {
                glDisableVertexAttribArray(attributeIndex)
                debugGLErrors()
            }
        }
    }

    private fun teardownFormat(vertexBuffers: List<VertexBuffer>, instanceAttributes: List<VertexBuffer>, shader: ShaderGL3) {
        vertexBuffers.forEach { teardownFormat(it.vertexFormat, shader) }
        instanceAttributes.forEach { teardownFormat(it.vertexFormat, shader) }
    }

    override fun setState(drawStyle: DrawStyle) {
        if (drawStyle.clip != null) {
            drawStyle.clip?.let {
                val target = RenderTarget.active
                glScissor((it.x * target.contentScale).toInt(), (target.height * target.contentScale - it.y * target.contentScale - it.height * target.contentScale).toInt(), (it.width * target.contentScale).toInt(), (it.height * target.contentScale).toInt())
                glEnable(GL_SCISSOR_TEST)
            }
        } else {
            glDisable(GL_SCISSOR_TEST)
        }

        glColorMask(drawStyle.channelWriteMask.red, drawStyle.channelWriteMask.green, drawStyle.channelWriteMask.blue, drawStyle.channelWriteMask.alpha)

        when (drawStyle.depthWrite) {
            true -> glDepthMask(true)
            false -> glDepthMask(false)
        }
        glEnable(GL_DEPTH_TEST)
        debugGLErrors()

        if (drawStyle.frontStencil === drawStyle.backStencil) {
            if (drawStyle.stencil.stencilTest === StencilTest.DISABLED) {
                glDisable(GL_STENCIL_TEST)
            } else {
                glEnable(GL_STENCIL_TEST)
                glStencilFunc(glStencilTest(drawStyle.stencil.stencilTest), drawStyle.stencil.stencilTestReference, drawStyle.stencil.stencilTestMask)
                debugGLErrors()
                glStencilOp(glStencilOp(drawStyle.stencil.stencilFailOperation), glStencilOp(drawStyle.stencil.depthFailOperation), glStencilOp(drawStyle.stencil.depthPassOperation))
                debugGLErrors()
                glStencilMask(drawStyle.stencil.stencilWriteMask)
                debugGLErrors()
            }
        } else {
            glEnable(GL_STENCIL_TEST)
            glStencilFuncSeparate(GL_FRONT, glStencilTest(drawStyle.frontStencil.stencilTest), drawStyle.frontStencil.stencilTestReference, drawStyle.frontStencil.stencilTestMask)
            glStencilFuncSeparate(GL_BACK, glStencilTest(drawStyle.backStencil.stencilTest), drawStyle.backStencil.stencilTestReference, drawStyle.backStencil.stencilTestMask)
            glStencilOpSeparate(GL_FRONT, glStencilOp(drawStyle.frontStencil.stencilFailOperation), glStencilOp(drawStyle.frontStencil.depthFailOperation), glStencilOp(drawStyle.frontStencil.depthPassOperation))
            glStencilOpSeparate(GL_BACK, glStencilOp(drawStyle.backStencil.stencilFailOperation), glStencilOp(drawStyle.backStencil.depthFailOperation), glStencilOp(drawStyle.backStencil.depthPassOperation))
            glStencilMaskSeparate(GL_FRONT, drawStyle.frontStencil.stencilWriteMask)
            glStencilMaskSeparate(GL_BACK, drawStyle.backStencil.stencilWriteMask)
        }

        when (drawStyle.blendMode) {
            BlendMode.OVER -> {
                glEnable(GL_BLEND)
                glBlendFunci(0, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
            }
            BlendMode.ADD -> {
                glEnable(GL_BLEND)
                glBlendFunci(0, GL_ONE, GL_ONE)
            }

            BlendMode.REPLACE -> {
                glDisable(GL_BLEND)
            }
            BlendMode.SUBTRACT -> {
                TODO("not implemented: subtract blend")
            }
            BlendMode.MULTIPLY -> {
                TODO("not implemented: multiply blend")
            }
        }

        when (drawStyle.depthTestPass) {
            DepthTestPass.ALWAYS -> {
                glDepthFunc(GL_ALWAYS)
            }
            DepthTestPass.GREATER -> {
                glDepthFunc(GL_GREATER)
            }
            DepthTestPass.GREATER_OR_EQUAL -> {
                glDepthFunc(GL_GEQUAL)
            }
            DepthTestPass.LESS -> {
                glDepthFunc(GL_LESS)
            }
            DepthTestPass.LESS_OR_EQUAL -> {
                glDepthFunc(GL_LEQUAL)
            }
            DepthTestPass.EQUAL -> {
                glDepthFunc(GL_EQUAL)
            }
            DepthTestPass.NEVER -> {
                glDepthFunc(GL_NEVER)
            }
        }
        debugGLErrors()

        when (drawStyle.cullTestPass) {
            CullTestPass.ALWAYS -> {
                glDisable(GL_CULL_FACE)
            }
            CullTestPass.FRONT -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_BACK)
            }
            CullTestPass.BACK -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_FRONT)
            }
            CullTestPass.NEVER -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_FRONT_AND_BACK)
            }
        }
        debugGLErrors()
    }

    override val activeRenderTarget: RenderTarget
        get() = RenderTargetGL3.activeRenderTarget
}

internal fun glStencilTest(test: StencilTest): Int {
    return when (test) {
        StencilTest.NEVER -> GL_NEVER
        StencilTest.ALWAYS -> GL_ALWAYS
        StencilTest.LESS -> GL_LESS
        StencilTest.LESS_OR_EQUAL -> GL_LEQUAL
        StencilTest.GREATER -> GL_GREATER
        StencilTest.GREATER_OR_EQUAL -> GL_GEQUAL
        StencilTest.EQUAL -> GL_EQUAL
        StencilTest.NOT_EQUAL -> GL_NOTEQUAL
        else -> throw RuntimeException("unsupported test: $test")
    }
}

internal fun glStencilOp(op: StencilOperation): Int {
    return when (op) {
        StencilOperation.KEEP -> GL_KEEP
        StencilOperation.DECREASE -> GL_DECR
        StencilOperation.DECREASE_WRAP -> GL_DECR_WRAP
        StencilOperation.INCREASE -> GL_INCR
        StencilOperation.INCREASE_WRAP -> GL_INCR_WRAP
        StencilOperation.ZERO -> GL_ZERO
        StencilOperation.INVERT -> GL_INVERT
        StencilOperation.REPLACE -> GL_REPLACE
        else -> throw RuntimeException("unsupported op")
    }
}

private fun DrawPrimitive.glType(): Int {
    return when (this) {
        DrawPrimitive.TRIANGLES -> GL_TRIANGLES
        DrawPrimitive.TRIANGLE_FAN -> GL_TRIANGLE_FAN
        DrawPrimitive.POINTS -> GL_POINTS
        DrawPrimitive.LINES -> GL_LINES
        DrawPrimitive.TRIANGLE_STRIP -> GL_TRIANGLE_STRIP
    }
}

private fun VertexElementType.glType(): Int = when (this) {
    VertexElementType.FLOAT32 -> GL_FLOAT
    VertexElementType.MATRIX22_FLOAT32 -> GL_FLOAT
    VertexElementType.MATRIX33_FLOAT32 -> GL_FLOAT
    VertexElementType.MATRIX44_FLOAT32 -> GL_FLOAT
    VertexElementType.VECTOR2_FLOAT32 -> GL_FLOAT
    VertexElementType.VECTOR3_FLOAT32 -> GL_FLOAT
    VertexElementType.VECTOR4_FLOAT32 -> GL_FLOAT
}

internal fun Matrix44.toFloatArray(): FloatArray = floatArrayOf(
        c0r0.toFloat(), c0r1.toFloat(), c0r2.toFloat(), c0r3.toFloat(),
        c1r0.toFloat(), c1r1.toFloat(), c1r2.toFloat(), c1r3.toFloat(),
        c2r0.toFloat(), c2r1.toFloat(), c2r2.toFloat(), c2r3.toFloat(),
        c3r0.toFloat(), c3r1.toFloat(), c3r2.toFloat(), c3r3.toFloat())
