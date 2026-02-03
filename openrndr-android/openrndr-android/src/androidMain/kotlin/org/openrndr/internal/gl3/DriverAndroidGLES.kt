package org.openrndr.internal.gl3

import android.opengl.GLES30
import android.opengl.GLES32
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ArrayCubemap
import org.openrndr.draw.ArrayTexture
import org.openrndr.draw.AtomicCounterBuffer
import org.openrndr.draw.BlendMode
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.BufferTexture
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ComputeShader
import org.openrndr.draw.ComputeStyleManager
import org.openrndr.draw.Cubemap
import org.openrndr.draw.DepthBuffer
import org.openrndr.draw.DepthFormat
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.DrawStyle
import org.openrndr.draw.DrawThread
import org.openrndr.draw.IndexBuffer
import org.openrndr.draw.IndexType
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.Session
import org.openrndr.draw.ShadeStructure
import org.openrndr.draw.ShadeStyleManager
import org.openrndr.draw.Shader
import org.openrndr.draw.ShaderStorageBuffer
import org.openrndr.draw.ShaderStorageFormat
import org.openrndr.draw.ShaderType
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.VertexFormat
import org.openrndr.draw.VolumeTexture
import org.openrndr.internal.Driver
import org.openrndr.internal.DriverProperties
import org.openrndr.internal.FontMapManager
import org.openrndr.internal.GLSL
import org.openrndr.internal.ResourceThread
import org.openrndr.internal.ShaderGenerators
import org.openrndr.internal.ShaderLanguage
import org.openrndr.internal.gl3.DriverGL3.ShaderVertexDescription
import org.openrndr.internal.glcommon.ComputeStyleManagerGLCommon
import org.openrndr.internal.glcommon.ShadeStyleManagerGLCommon
import org.openrndr.internal.glcommon.ShaderGeneratorsGLCommon
import java.nio.Buffer

private val logger = KotlinLogging.logger {}

class DriverAndroidGLES(val version: DriverVersionGL) : Driver {

    private var currentWidth = 0
    private var currentHeight = 0
    private var currentFill = ColorRGBa.WHITE

    private lateinit var renderTarget: RenderTargetGLES

    override val contextID: Long
        get() {
            return Thread.currentThread().id
        }

    data class Capabilities(
        val programUniform: Boolean,
        val textureStorage: Boolean,
        val textureMultisampleStorage: Boolean,
        val compute: Boolean,
    )

    val capabilities = Capabilities(
        programUniform = version.isAtLeast(
            DriverVersionGL.GL_VERSION_4_1,
            DriverVersionGL.GLES_VERSION_3_1
        ),
        textureStorage = version.isAtLeast(
            DriverVersionGL.GL_VERSION_4_1,
            DriverVersionGL.GLES_VERSION_3_0
        ),
        textureMultisampleStorage = version.isAtLeast(
            DriverVersionGL.GL_VERSION_4_3,
            DriverVersionGL.GLES_VERSION_3_1
        ),
        compute = version.isAtLeast(
            DriverVersionGL.GL_VERSION_4_3,
            DriverVersionGL.GLES_VERSION_3_1
        )
    )

    fun onSurfaceChanged(width: Int, height: Int) {
        logger.info { "onSurfaceChanged - width: $width - height: $height" }
        currentWidth = width
        currentHeight = height
        renderTarget = RenderTargetGLES.create(width, height, 1.0, BufferMultisample.Disabled, null)
        viewport(width, height) // make sure GL viewport matches display

    }

    override fun createShader(
        vsCode: String,
        tcsCode: String?,
        tesCode: String?,
        gsCode: String?,
        fsCode: String,
        name: String,
        session: Session?
    ): Shader {
        logger.trace {
            "creating shader:\n${gsCode}\n${vsCode}\n${fsCode}"
        }
        val vertexShader = VertexShaderGL3.fromString(vsCode, name)
        val geometryShader = gsCode?.let { GeometryShaderGL3.fromString(it, name) }
        val tcShader = tcsCode?.let { TessellationControlShaderGL3.fromString(it, name) }
        val teShader = tesCode?.let { TessellationEvaluationShaderGL3.fromString(it, name) }
        val fragmentShader = FragmentShaderGL3.fromString(fsCode, name)

        synchronized(this) {
            return ShaderGLES.create(
                vertexShader,
                tcShader,
                teShader,
                geometryShader,
                fragmentShader,
                name,
                session
            )
        }


//        // If explicit code is provided, compile it; otherwise use the solid fill shader.
//        val vs = vsCode.ifBlank { BASIC_SOLID_VS }
//        val fs = fsCode.ifBlank { BASIC_SOLID_FS }
//        return ShaderGLES.fromSource(vs, fs, session)
    }

    override fun createComputeShader(code: String, name: String, session: Session?): ComputeShader {
        TODO("Not yet implemented")
    }

    override fun createShadeStyleManager(
        name: String,
        vsGenerator: (ShadeStructure) -> String,
        tcsGenerator: ((ShadeStructure) -> String)?,
        tesGenerator: ((ShadeStructure) -> String)?,
        gsGenerator: ((ShadeStructure) -> String)?,
        fsGenerator: (ShadeStructure) -> String,
        session: Session?
    ): ShadeStyleManager {
        return ShadeStyleManagerGLCommon(
            name,
            vsGenerator = vsGenerator,
            tcsGenerator = tcsGenerator,
            tesGenerator = tesGenerator,
            gsGenerator = gsGenerator,
            fsGenerator = fsGenerator
        )
    }

    override fun createRenderTarget(
        width: Int,
        height: Int,
        contentScale: Double,
        multisample: BufferMultisample,
        session: Session?
    ): RenderTarget {
        logger.trace { "creating render target $width x $height @ ${contentScale}x $multisample" }
        synchronized(this) {
            val renderTarget =
                RenderTargetGLES.create(width, height, contentScale, multisample, session)
            session?.track(renderTarget)
            return renderTarget
        }
    }

    override fun createArrayCubemap(
        width: Int,
        layers: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session?
    ): ArrayCubemap {
        TODO("Not yet implemented")
    }

    override fun createArrayTexture(
        width: Int,
        height: Int,
        layers: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session?
    ): ArrayTexture {
        TODO("Not yet implemented")
    }

    override fun createAtomicCounterBuffer(
        counterCount: Int,
        session: Session?
    ): AtomicCounterBuffer {
        TODO("Not yet implemented")
    }

    override fun createColorBuffer(
        width: Int,
        height: Int,
        contentScale: Double,
        format: ColorFormat,
        type: ColorType,
        multisample: BufferMultisample,
        levels: Int,
        session: Session?
    ): ColorBuffer {
        logger.trace { "creating color buffer $width x $height @ $format:$type" }
        val colorBuffer = ColorBufferGLES.create(
            width,
            height,
            contentScale,
            format,
            type,
            multisample,
            levels,
            session
        )
        session?.track(colorBuffer)
        return colorBuffer
    }

    override fun createDepthBuffer(
        width: Int,
        height: Int,
        format: DepthFormat,
        multisample: BufferMultisample,
        session: Session?
    ): DepthBuffer {
        logger.trace { "creating depth buffer $width x $height @ $format" }
        synchronized(this) {
            val depthBuffer = DepthBufferGLES.create(width, height, format, multisample, session)
            return depthBuffer
        }
    }

    override fun createBufferTexture(
        elementCount: Int,
        format: ColorFormat,
        type: ColorType,
        session: Session?
    ): BufferTexture {
        TODO("Not yet implemented")
    }

    override fun createCubemap(
        width: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session?
    ): Cubemap {
        TODO("Not yet implemented")
    }

    override fun createVolumeTexture(
        width: Int,
        height: Int,
        depth: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session?
    ): VolumeTexture {
        TODO("Not yet implemented")
    }

    override fun createResourceThread(session: Session?, f: () -> Unit): ResourceThread {
        TODO("Not yet implemented")
    }

    override fun createDrawThread(session: Session?): DrawThread {
        TODO("Not yet implemented")
    }

    private val defaultVAOs = HashMap<Long, Int>()
    private val defaultVAO: Int
        get() = defaultVAOs.getOrPut(contextID) {
            val vaos = IntArray(1)
            synchronized(Driver.instance) {
                logger.debug { "[context=$contextID] creating default VAO" }
                glGenVertexArrays(vaos)
            }
            vaos[0]
        }

    override fun clear(color: ColorRGBa) {
        // Bind the on-screen framebuffer (default)
        glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        // Clear entire surface
        glDisable(GLES30.GL_SCISSOR_TEST)
        // Set the clear color
        glClearColor(
            color.r.toFloat(),
            color.g.toFloat(),
            color.b.toFloat(),
            color.a.toFloat()
        )
        // Clear color (and optionally depth/stencil)
        glClear(
            GLES30.GL_COLOR_BUFFER_BIT /* or add: or GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_STENCIL_BUFFER_BIT */
        )

//        val solidShader = ShaderGLES.fromSource(BASIC_SOLID_VS, BASIC_SOLID_FS, Session.active)
//        drawVertexBuffer(
//            solidShader,
//            listOf(vb),
//            DrawPrimitive.TRIANGLES,
//            0,
//            3,
//            0
//        )
    }

    override fun createDynamicVertexBuffer(
        format: VertexFormat,
        vertexCount: Int,
        session: Session?
    ): VertexBuffer {
        synchronized(this) {
            val vertexBuffer = VertexBufferGLES.createDynamic(format, vertexCount, session)
            session?.track(vertexBuffer)
            return vertexBuffer
        }
    }

    override fun createStaticVertexBuffer(
        format: VertexFormat,
        buffer: Buffer,
        session: Session?
    ): VertexBuffer {
        TODO("not implemented")
    }

    override fun createDynamicIndexBuffer(
        elementCount: Int,
        type: IndexType,
        session: Session?
    ): IndexBuffer {
        synchronized(this) {
            val indexBuffer = IndexBufferGL3.create(elementCount, type, session)
            session?.track(indexBuffer)
            return indexBuffer
        }
    }

    override fun createShaderStorageBuffer(
        format: ShaderStorageFormat,
        session: Session?
    ): ShaderStorageBuffer {
        TODO("Not yet implemented")
    }

    override fun drawVertexBuffer(
        shader: Shader,
        vertexBuffers: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        vertexOffset: Int,
        vertexCount: Int,
        verticesPerPatch: Int
    ) {
        debugGLErrors {
            "a pre-existing GL error occurred before Driver.drawVertexBuffer "
        }

        if (drawPrimitive == DrawPrimitive.PATCHES) {
            if (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_1) {
                glPatchParameteri(GL_PATCH_VERTICES, verticesPerPatch)
            }
        }

        shader as ShaderGLES
        // -- find or create a VAO for our shader + vertex buffers combination
        val shaderVertexDescription = ShaderVertexDescription(
            Driver.instance.contextID,
            shader.programObject,
            IntArray(vertexBuffers.size) { (vertexBuffers[it] as VertexBufferGLES).buffer },
            IntArray(0)
        )

        val vao = vaos.getOrPut(shaderVertexDescription) {
            logger.debug {
                "[context=$contextID] creating new VAO for hash $shaderVertexDescription"
            }

            val arrays = IntArray(1)
            synchronized(Driver.instance) {
                glGenVertexArrays(arrays)
                glBindVertexArray(arrays[0])
                setupFormat(vertexBuffers, emptyList(), shader)
                glBindVertexArray(defaultVAO)
            }
            arrays[0]
        }
        glBindVertexArray(vao)
        debugGLErrors {
            when (it) {
                GL_INVALID_OPERATION -> "array ($vao) is not zero or the name of a vertex array object previously returned from a call to glGenVertexArrays"
                else -> "unknown error $it"
            }
        }

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

    override fun drawIndexedVertexBuffer(
        shader: Shader,
        indexBuffer: IndexBuffer,
        vertexBuffers: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        indexOffset: Int,
        indexCount: Int,
        verticesPerPatch: Int
    ) {
        println("drawIndexedVertexBuffer")
    }

    override fun drawInstances(
        shader: Shader,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        vertexOffset: Int,
        vertexCount: Int,
        instanceOffset: Int,
        instanceCount: Int,
        verticesPerPatch: Int
    ) {
        // ES note: baseInstance is not available (until GLES 3.2 with extensions).
        require(instanceOffset == 0) { "non-zero instanceOffset is not supported on OpenGL ES" }

        // ES note: tessellation/patches are not supported (before GLES 3.2)
        require(drawPrimitive != DrawPrimitive.PATCHES) {
            "DrawPrimitive.PATCHES is not supported on OpenGL ES"
        }

        shader as ShaderGLES

        // -- find or create a VAO for our shader + vertex buffers + instance buffers combination
        val hash = ShaderVertexDescription(
            contextID,
            shader.programObject,
            IntArray(vertexBuffers.size) { (vertexBuffers[it] as VertexBufferGLES).buffer },
            IntArray(instanceAttributes.size) { (instanceAttributes[it] as VertexBufferGLES).buffer }
        )

        val vao = vaos.getOrPut(hash) {
            logger.debug {
                "creating new instances VAO for hash $hash"
            }
            val arrays = IntArray(1)
            synchronized(Driver.instance) {
                glGenVertexArrays(arrays)
                glBindVertexArray(arrays[0])
                setupFormat(vertexBuffers, instanceAttributes, shader)
                debugGLErrors()
                glBindVertexArray(defaultVAO)
                debugGLErrors()
            }
            arrays[0]
        }

        debugGLErrors()
        glBindVertexArray(vao)
        debugGLErrors()

        logger.trace { "drawing $instanceCount instances with $drawPrimitive(${drawPrimitive.glType()}) and $vertexCount vertices with vertexOffset $vertexOffset " }
        if (instanceOffset == 0) {
            glDrawArraysInstanced(drawPrimitive.glType(), vertexOffset, vertexCount, instanceCount)
        } else {
            glDrawArraysInstancedBaseInstance(
                drawPrimitive.glType(),
                vertexOffset,
                vertexCount,
                instanceCount,
                instanceOffset
            )
        }

        debugGLErrors()

        // Restore default VAO
        glBindVertexArray(defaultVAO)
    }

    override fun drawIndexedInstances(
        shader: Shader,
        indexBuffer: IndexBuffer,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        indexOffset: Int,
        indexCount: Int,
        instanceOffset: Int,
        instanceCount: Int,
        verticesPerPatch: Int
    ) {
        println("drawIndexedInstances")
    }

    private fun setupFormat(
        vertexBuffer: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        shader: ShaderGLES
    ) {
        run {
            debugGLErrors()

            val scalarVectorTypes = setOf(
                VertexElementType.UINT8,
                VertexElementType.VECTOR2_UINT8,
                VertexElementType.VECTOR3_UINT8,
                VertexElementType.VECTOR4_UINT8,
                VertexElementType.INT8,
                VertexElementType.VECTOR2_INT8,
                VertexElementType.VECTOR3_INT8,
                VertexElementType.VECTOR4_INT8,
                VertexElementType.UINT16,
                VertexElementType.VECTOR2_UINT16,
                VertexElementType.VECTOR3_UINT16,
                VertexElementType.VECTOR4_UINT16,
                VertexElementType.INT16,
                VertexElementType.VECTOR2_INT16,
                VertexElementType.VECTOR3_INT16,
                VertexElementType.VECTOR4_INT16,
                VertexElementType.UINT32,
                VertexElementType.VECTOR2_UINT32,
                VertexElementType.VECTOR3_UINT32,
                VertexElementType.VECTOR4_UINT32,
                VertexElementType.INT32,
                VertexElementType.VECTOR2_INT32,
                VertexElementType.VECTOR3_INT32,
                VertexElementType.VECTOR4_INT32,
                VertexElementType.FLOAT32,
                VertexElementType.VECTOR2_FLOAT32,
                VertexElementType.VECTOR3_FLOAT32,
                VertexElementType.VECTOR4_FLOAT32
            )

            fun setupBuffer(buffer: VertexBufferGLES, divisor: Int = 0) {
                val prefix = if (divisor == 0) "a" else "i"
                var attributeBindings = 0

                glBindBuffer(GL_ARRAY_BUFFER, buffer.buffer)
                val format = buffer.vertexFormat
                for (item in format.items) {
                    // skip over padding attributes
                    if (item.attribute == "_") {
                        continue
                    }

                    val attributeIndex = shader.attributeIndex("${prefix}_${item.attribute}")
                    if (attributeIndex != -1) {
                        when (item.type) {
                            in scalarVectorTypes -> {
                                for (i in 0 until item.arraySize) {
                                    glEnableVertexAttribArray(attributeIndex + i)
                                    debugGLErrors {
                                        when (it) {
                                            GL_INVALID_OPERATION -> "no vertex array object is bound"
                                            GL_INVALID_VALUE -> "index ($attributeIndex) is greater than or equal to GL_MAX_VERTEX_ATTRIBS"
                                            else -> null
                                        }
                                    }
                                    val glType = item.type.glType()

                                    if (glType == GL_FLOAT) {
                                        glVertexAttribPointer(
                                            attributeIndex + i,
                                            item.type.componentCount,
                                            glType,
                                            false,
                                            format.size,
                                            buffer.offset + item.offset.toLong() + i * item.type.sizeInBytes
                                        )
                                    } else {
                                        glVertexAttribIPointer(
                                            attributeIndex + i,
                                            item.type.componentCount,
                                            glType,
                                            format.size,
                                            buffer.offset + item.offset.toLong() + i * item.type.sizeInBytes
                                        )

                                    }
                                    debugGLErrors {
                                        when (it) {
                                            GL_INVALID_VALUE -> "index ($attributeIndex) is greater than or equal to GL_MAX_VERTEX_ATTRIBS"
                                            else -> null
                                        }
                                    }
                                    glVertexAttribDivisor(attributeIndex, divisor)
                                    attributeBindings++
                                }
                            }

                            VertexElementType.MATRIX44_FLOAT32 -> {
                                for (i in 0 until item.arraySize) {
                                    for (column in 0 until 4) {
                                        glEnableVertexAttribArray(attributeIndex + column + i * 4)
                                        debugGLErrors()

                                        glVertexAttribPointer(
                                            attributeIndex + column + i * 4,
                                            4,
                                            item.type.glType(),
                                            false,
                                            format.size,
                                            buffer.offset + item.offset.toLong() + column * 16 + i * 64
                                        )
                                        debugGLErrors()

                                        glVertexAttribDivisor(
                                            attributeIndex + column + i * 4,
                                            divisor
                                        )
                                        debugGLErrors()
                                        attributeBindings++
                                    }
                                }
                            }

                            VertexElementType.MATRIX33_FLOAT32 -> {
                                for (i in 0 until item.arraySize) {
                                    for (column in 0 until 3) {
                                        glEnableVertexAttribArray(attributeIndex + column + i * 3)
                                        debugGLErrors()

                                        glVertexAttribPointer(
                                            attributeIndex + column + i * 3,
                                            3,
                                            item.type.glType(),
                                            false,
                                            format.size,
                                            buffer.offset + item.offset.toLong() + column * 12 + i * 48
                                        )
                                        debugGLErrors()

                                        glVertexAttribDivisor(
                                            attributeIndex + column + i * 3,
                                            divisor
                                        )
                                        debugGLErrors()
                                        attributeBindings++
                                    }
                                }
                            }

                            else -> {
                                TODO("implement support for ${item.type}")
                            }
                        }
                    }
                }

                if (attributeBindings > 16) {
                    throw RuntimeException("Maximum vertex attributes exceeded $attributeBindings (limit is 16)")
                }
            }
            vertexBuffer.forEach {
                require(!(it as VertexBufferGLES).isDestroyed)
                setupBuffer(it, 0)
            }

            instanceAttributes.forEach {
                setupBuffer(it as VertexBufferGLES, 1)
            }
        }
    }

    override fun setState(drawStyle: DrawStyle) {
        // keep your current fill color cache if you use it later
        val fill = drawStyle.fill ?: ColorRGBa.WHITE
        currentFill = fill

        fun enableBlend() {
            GLES32.glEnable(GLES32.GL_BLEND)
        }

        fun disableBlend() {
            GLES32.glDisable(GLES32.GL_BLEND)
        }

        fun func(srcRGB: Int, dstRGB: Int, srcA: Int = srcRGB, dstA: Int = dstRGB) =
            GLES32.glBlendFuncSeparate(srcRGB, dstRGB, srcA, dstA)

        fun eq(rgb: Int, a: Int = rgb) = GLES32.glBlendEquationSeparate(rgb, a)

        when (drawStyle.blendMode) {
            BlendMode.REPLACE -> {
                // no blending — overwrite dst with src
                disableBlend()
            }

            BlendMode.OVER -> {
                // standard alpha compositing
                enableBlend()
                func(
                    GLES32.GL_SRC_ALPHA,
                    GLES32.GL_ONE_MINUS_SRC_ALPHA,
                    GLES32.GL_ONE,
                    GLES32.GL_ONE_MINUS_SRC_ALPHA
                )
                eq(GLES32.GL_FUNC_ADD)
            }

            BlendMode.ADD -> {
                enableBlend()
                func(GLES32.GL_ONE, GLES32.GL_ONE)            // src + dst
                eq(GLES32.GL_FUNC_ADD)
            }

            BlendMode.SUBTRACT -> {
                enableBlend()
                func(GLES32.GL_ONE, GLES32.GL_ONE)            // src - dst
                eq(GLES32.GL_FUNC_REVERSE_SUBTRACT)
            }

            BlendMode.MULTIPLY -> {
                enableBlend()
                // common multiply approximation
                func(
                    GLES32.GL_DST_COLOR,
                    GLES32.GL_ONE_MINUS_SRC_ALPHA,
                    GLES32.GL_ONE,
                    GLES32.GL_ONE_MINUS_SRC_ALPHA
                )
                eq(GLES32.GL_FUNC_ADD)
            }

            BlendMode.SCREEN -> {
                enableBlend()
                // screen ≈ 1 - (1-src)*(1-dst)
                func(
                    GLES32.GL_ONE,
                    GLES32.GL_ONE_MINUS_SRC_COLOR,
                    GLES32.GL_ONE,
                    GLES32.GL_ONE_MINUS_SRC_ALPHA
                )
                eq(GLES32.GL_FUNC_ADD)
            }

            BlendMode.LIGHTEN -> {
                enableBlend()
                func(
                    GLES32.GL_ONE,
                    GLES32.GL_ONE
                )            // factors irrelevant; equation picks max
                eq(GLES32.GL_MAX, GLES32.GL_MAX)              // ES 3.0 supports MIN/MAX
            }

            BlendMode.DARKEN -> {
                enableBlend()
                func(GLES32.GL_ONE, GLES32.GL_ONE)
                eq(GLES32.GL_MIN, GLES32.GL_MIN)
            }

            else -> {
                // Fallback to OVER if new/unknown mode appears
                enableBlend()
                func(
                    GLES32.GL_SRC_ALPHA,
                    GLES32.GL_ONE_MINUS_SRC_ALPHA,
                    GLES32.GL_ONE,
                    GLES32.GL_ONE_MINUS_SRC_ALPHA
                )
                eq(GLES32.GL_FUNC_ADD)
            }
        }
    }

    fun viewport(width: Int, height: Int) {
        glViewport(0, 0, width, height)
    }

    override fun destroyContext(context: Long) {

    }

    fun destroyVAOsForVertexBuffer(vertexBuffer: VertexBufferGLES) {
        val candidates = vaos.keys.filter {
            it.vertexBuffers.contains(vertexBuffer.buffer) || it.instanceAttributeBuffers.contains(
                vertexBuffer.buffer
            )
        }
        for (candidate in candidates) {
            val value = vaos[candidate] ?: error("no vao found")
            logger.debug { "removing VAO $value for $candidate" }
            glDeleteVertexArrays(value)
            debugGLErrors()
            vaos.remove(candidate)
        }
    }

    override val fontImageMapManager: FontMapManager
        get() = TODO("Not yet implemented")
    override val fontVectorMapManager: FontMapManager
        get() = TODO("Not yet implemented")

    override val shaderLanguage: ShaderLanguage
        get() = GLSL("300 es")

    override val shaderGenerators: ShaderGenerators = ShaderGeneratorsGLCommon()

    private data class VaoKey(
        val contextID: Long,
        val programId: Int,
        val vertexBuffers: List<Int>,
        val instanceAttributeBuffers: List<Int>,
        val indexBuffer: Int?
    )

    private val vaos = mutableMapOf<ShaderVertexDescription, Int>()

    inline fun checkGLErrors(crossinline errorFunction: ((Int) -> String?) = { null }) {
        val error = glGetError()
        if (error != GL_NO_ERROR) {
            val message = when (error) {
                GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
                GL_INVALID_VALUE -> "GL_INVALID_VALUE"
                GL_INVALID_ENUM -> "GL_INVALID_ENUM"
                GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
                GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
                GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW"
                GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW"
                else -> "<untranslated: $error>"
            }
            throw GL3Exception(
                "[context=${Driver.instance.contextID}] GL ERROR: $message ${
                    errorFunction.invoke(
                        error
                    )
                }"
            )
        }
    }

    inline fun debugGLErrors(crossinline errorFunction: ((Int) -> String?) = { null }) {
        if (DriverGL3Configuration.useDebugContext) {
            checkGLErrors(errorFunction)
        }
    }

    override val activeRenderTarget: RenderTarget
        get() = renderTarget

    override fun finish() {

    }

    private fun readAll(input: java.io.InputStream): String =
        input.bufferedReader(Charsets.UTF_8).use { it.readText() }

    override fun internalShaderResource(resourceId: String): String {
        // 1) Try classpath (works if shaders are in /shaders inside the classpath)
        val classpathPath = "shaders/$resourceId"
        val loader = this::class.java.classLoader
        loader?.getResourceAsStream(classpathPath)?.use { stream ->
            return readAll(stream)
        }
        this::class.java.getResourceAsStream(classpathPath)?.use { stream ->
            return readAll(stream)
        }

        // 2) Try Android assets/shaders/<resourceId>
        // TODO: Need Android Context (without causing memory leak)
//        AndroidContextHolder.context?.assets?.let { am ->
//            try {
//                am.open(classpathPath).use { stream ->
//                    return readAll(stream)
//                }
//            } catch (_: Exception) { /* ignore and try next */ }
//        }
//
//        // 3) Try Android res/raw: convert "my_shader.frag" -> raw resource by name "my_shader_frag"
//        AndroidContextHolder.context?.let { ctx ->
//            val baseName = resourceId
//                .replace('.', '_')
//                .replace('-', '_')
//                .replace('/', '_')
//            val resId = ctx.resources.getIdentifier(baseName, "raw", ctx.packageName)
//            if (resId != 0) {
//                ctx.resources.openRawResource(resId).use { stream ->
//                    return readAll(stream)
//                }
//            }
//        }

        throw RuntimeException("Could not find internal shader resource '$resourceId' in classpath, assets/shaders, or res/raw")
    }

    override fun shaderConfiguration(type: ShaderType): String = buildString {
        // GLSL ES 3.0 is widely available on Android; bump to 310/320 es only if you actually need those features.
        appendLine("#version 300 es")
        appendLine("#define OR_GLES")

        // Precision qualifiers must come before any non-preprocessor tokens in ES
        appendLine("precision mediump float;")
        appendLine("precision mediump int;")
        appendLine("precision mediump sampler2D;")
        appendLine("precision mediump sampler2DArray;")
        appendLine("precision mediump samplerCube;")

        // If you truly need images and target >= 3.1 ES, you can add these conditionally:
        // appendLine("precision highp image2D;")
        // appendLine("precision highp image3D;")
        // appendLine("precision highp imageCube;")
        // appendLine("precision highp image2DArray;")

        // Do NOT emit KHR advanced blend layout here for GLES unless you:
        //  1) detect GL_KHR_blend_equation_advanced support, and
        //  2) place the #extension line BEFORE any non-preprocessor tokens, AND
        //  3) your driver actually accepts the 'layout(blend_support_all_equations) out;' on ES.
        // Most Android drivers either don’t need this or reject it, so we keep it out by default.
    }

    override fun createComputeStyleManager(session: Session?): ComputeStyleManager {
        return ComputeStyleManagerGLCommon()
    }

    override val properties: DriverProperties by lazy {
        DriverProperties(maxRenderTargetSamples = 4, maxTextureSamples = 4, maxTextureSize = 16384)
    }

    private fun bindFormatAttributesForProgram(
        programId: Int,
        format: VertexFormat,
        baseShiftBytes: Int,
        divisor: Int
    ) {
        val stride = format.size

        format.items.forEachIndexed { rawIndex, elem ->
            if (elem.attribute == "_") return@forEachIndexed // padding

            // Try the declared name; if not found, fall back to common alias
            var loc = glGetAttribLocation(programId, elem.attribute)
            if (loc < 0) {
                val alias = when (elem.attribute) {
                    "position" -> "a_position"
                    "a_position" -> "position"
                    "color" -> "a_color"
                    "a_color" -> "color"
                    "normal" -> "a_normal"
                    "a_normal" -> "normal"
                    else -> null
                }
                if (alias != null) loc = glGetAttribLocation(programId, alias)
            }
            if (loc < 0) {
                // Attribute not used by the current shader – skip it
                return@forEachIndexed
            }

            val (size, glType, normalized, isInteger) = glAttribOf(elem.type)
            val pointer = elem.offset + baseShiftBytes

            glEnableVertexAttribArray(loc)
            val err0 = glGetError()
            if (err0 != GLES30.GL_NO_ERROR) {
                throw RuntimeException("glEnableVertexAttribArray($loc) error=$err0 (attr='${elem.attribute}')")
            }

            if (isInteger) {
                glVertexAttribIPointer(loc, size, glType, stride, pointer.toLong())
            } else {
                glVertexAttribPointer(loc, size, glType, normalized, stride, pointer.toLong())
            }
            val err1 = glGetError()
            if (err1 != GLES30.GL_NO_ERROR) {
                throw RuntimeException(
                    "glVertexAttribPointer/IPointer(loc=$loc,size=$size,stride=$stride,ptr=$pointer) error=$err1 " +
                            "(attr='${elem.attribute}', type=${elem.type})"
                )
            }

            glVertexAttribDivisor(loc, divisor)
            val err2 = glGetError()
            if (err2 != GLES30.GL_NO_ERROR) {
                throw RuntimeException("glVertexAttribDivisor($loc,$divisor) error=$err2")
            }
        }
    }

    fun destroyVAOsForShader(shader: ShaderGLES) {
        val candidates = vaos.keys.filter {
            it.vertexBuffers.contains(shader.programObject) || it.instanceAttributeBuffers.contains(
                shader.programObject
            )
        }
        for (candidate in candidates) {
            val value = vaos[candidate] ?: error("no vao found")
            logger.debug { "removing VAO $value for $candidate" }
            glDeleteVertexArrays(value)
            debugGLErrors()
            vaos.remove(candidate)
        }
    }

    private fun destroyAllVAOs() {
        defaultVAOs.keys.forEach {
            val value = defaultVAOs[it]!!
            glDeleteVertexArrays(value)
            debugGLErrors()
        }
        defaultVAOs.clear()

        vaos.keys.forEach {
            val value = vaos[it]!!
            glDeleteVertexArrays(value)
            debugGLErrors()
        }
        vaos.clear()
    }

    private fun DrawPrimitive.glType(): Int {
        return when (this) {
            DrawPrimitive.TRIANGLES -> GL_TRIANGLES
            DrawPrimitive.TRIANGLE_FAN -> GL_TRIANGLE_FAN
            DrawPrimitive.POINTS -> GL_POINTS
            DrawPrimitive.LINES -> GL_LINES
            DrawPrimitive.LINE_STRIP -> GL_LINE_STRIP
            DrawPrimitive.LINE_LOOP -> GL_LINE_LOOP
            DrawPrimitive.TRIANGLE_STRIP -> GL_TRIANGLE_STRIP
            DrawPrimitive.PATCHES -> GL_PATCHES
        }
    }

    private fun VertexElementType.glType(): Int = when (this) {
        VertexElementType.UINT8, VertexElementType.VECTOR2_UINT8, VertexElementType.VECTOR3_UINT8, VertexElementType.VECTOR4_UINT8 -> GL_UNSIGNED_BYTE
        VertexElementType.UINT16, VertexElementType.VECTOR2_UINT16, VertexElementType.VECTOR3_UINT16, VertexElementType.VECTOR4_UINT16 -> GL_UNSIGNED_SHORT
        VertexElementType.UINT32, VertexElementType.VECTOR2_UINT32, VertexElementType.VECTOR3_UINT32, VertexElementType.VECTOR4_UINT32 -> GL_UNSIGNED_INT

        VertexElementType.INT8, VertexElementType.VECTOR2_INT8, VertexElementType.VECTOR3_INT8, VertexElementType.VECTOR4_INT8 -> GL_BYTE
        VertexElementType.INT16, VertexElementType.VECTOR2_INT16, VertexElementType.VECTOR3_INT16, VertexElementType.VECTOR4_INT16 -> GL_SHORT
        VertexElementType.INT32, VertexElementType.VECTOR2_INT32, VertexElementType.VECTOR3_INT32, VertexElementType.VECTOR4_INT32 -> GL_INT

        VertexElementType.FLOAT32 -> GL_FLOAT
        VertexElementType.MATRIX22_FLOAT32 -> GL_FLOAT
        VertexElementType.MATRIX33_FLOAT32 -> GL_FLOAT
        VertexElementType.MATRIX44_FLOAT32 -> GL_FLOAT
        VertexElementType.VECTOR2_FLOAT32 -> GL_FLOAT
        VertexElementType.VECTOR3_FLOAT32 -> GL_FLOAT
        VertexElementType.VECTOR4_FLOAT32 -> GL_FLOAT
    }

    private data class GlAttrib(
        val size: Int,
        val glType: Int,
        val normalized: Boolean,
        val isInteger: Boolean
    )

    private fun glAttribOf(type: VertexElementType): GlAttrib = when (type) {
        VertexElementType.FLOAT32 -> GlAttrib(1, GLES30.GL_FLOAT, false, false)
        VertexElementType.VECTOR2_FLOAT32 -> GlAttrib(
            2,
            GLES30.GL_FLOAT,
            false,
            false
        )

        VertexElementType.VECTOR3_FLOAT32 -> GlAttrib(
            3,
            GLES30.GL_FLOAT,
            false,
            false
        )

        VertexElementType.VECTOR4_FLOAT32 -> GlAttrib(
            4,
            GLES30.GL_FLOAT,
            false,
            false
        )

        VertexElementType.INT32 -> GlAttrib(1, GLES30.GL_INT, false, true)
        VertexElementType.VECTOR2_INT32 -> GlAttrib(2, GLES30.GL_INT, false, true)
        VertexElementType.VECTOR3_INT32 -> GlAttrib(3, GLES30.GL_INT, false, true)
        VertexElementType.VECTOR4_INT32 -> GlAttrib(4, GLES30.GL_INT, false, true)

        VertexElementType.UINT32 -> GlAttrib(
            1,
            GLES30.GL_UNSIGNED_INT,
            false,
            true
        )

        VertexElementType.VECTOR2_UINT32 -> GlAttrib(
            2,
            GLES30.GL_UNSIGNED_INT,
            false,
            true
        )

        VertexElementType.VECTOR3_UINT32 -> GlAttrib(
            3,
            GLES30.GL_UNSIGNED_INT,
            false,
            true
        )

        VertexElementType.VECTOR4_UINT32 -> GlAttrib(
            4,
            GLES30.GL_UNSIGNED_INT,
            false,
            true
        )

        VertexElementType.UINT8 -> GlAttrib(
            1,
            GLES30.GL_UNSIGNED_BYTE,
            true,
            false
        )

        else -> throw IllegalArgumentException("Unsupported vertex element type in attribute arrays: $type")
    }

    private fun attributeLocation(name: String, fallbackIndex: Int): Int = when {
        name == "position" || name == "a_position" -> 0
        name == "normal" || name == "a_normal" -> 1
        name == "color" || name == "a_color" -> 2
        name.startsWith("texCoord") || name.startsWith("a_tex") -> {
            val idx = name.removePrefix("texCoord").removePrefix("a_tex").toIntOrNull() ?: 0
            3 + idx
        }

        else -> 8 + fallbackIndex
    }
}

val Driver.Companion.glVersion
    get() = (instance as DriverAndroidGLES).version

val Driver.Companion.glType
    get() = (instance as DriverAndroidGLES).version.type

/**
 * Quick access to capabilities
 */
val Driver.Companion.capabilities
    get() = (instance as DriverAndroidGLES).capabilities