package org.openrndr.webgl

import org.khronos.webgl.WebGLRenderingContext as GL
import org.openrndr.draw.*
import org.openrndr.internal.*


class DriverWebGL(val context: GL) : Driver {
    init {
        Driver.driver = this
    }

    inner class Extensions {
        val instancedArrays by lazy {
            context.getExtension("ANGLE_instanced_arrays") as? ANGLEinstancedArrays
        }
        val standardDerivatives by lazy {
            context.getExtension("OES_standard_derivatives") as? OESStandardDerivatives
        }

        val halfFloatTextures by lazy {
            context.getExtension("OES_texture_half_float") as? OESTextureHalfFloat
        }

        val floatTextures by lazy {
            context.getExtension("OES_texture_float") as? OESTextureFloat
        }

        val colorBufferHalfFloat by lazy {
            context.getExtension("EXT_color_buffer_half_float") as? EXTColorBufferHalfFloat
        }

        val colorBufferFloat by lazy {
            context.getExtension("WEBGL_color_buffer_float") as? WEBGLColorBufferFloat
        }

        val halfFloatTexturesLinear by lazy {
            context.getExtension("OES_texture_half_float_linear") as? OESTextureHalfFloatLinear
        }

        val floatTexturesLinear by lazy {
            context.getExtension("OES_texture_float_linear") as? OESTextureFloatLinear
        }

        val drawBuffers by lazy {
            context.getExtension("WEBGL_draw_buffers") as? WEBGLDrawBuffers
        }

        val depthTexture by lazy {
            context.getExtension("WEBGL_depth_texture") as? WEBGLDepthTexture
        }
    }

    data class Capabilities(
        val instancedArrays : Boolean,
        val standardDerivatives: Boolean,
        val halfFloatTextures: Boolean,
        val floatTextures: Boolean,
        val colorBufferHalfFloat: Boolean,
        val colorBufferFloat: Boolean,
        val halfFloatTexturesLinear: Boolean,
        val floatTexturesLinear: Boolean,
        val drawBuffers: Boolean,
        val depthTexture: Boolean
    )

    val extensions = Extensions()

    val capabilities = Capabilities(
        instancedArrays = extensions.instancedArrays != null,
        standardDerivatives = extensions.standardDerivatives != null,
        halfFloatTextures = extensions.halfFloatTextures != null,
        floatTextures = extensions.floatTextures != null,
        colorBufferHalfFloat = extensions.colorBufferHalfFloat != null,
        colorBufferFloat = extensions.colorBufferFloat != null,
        halfFloatTexturesLinear = extensions.halfFloatTexturesLinear != null,
        floatTexturesLinear = extensions.floatTexturesLinear != null,
        drawBuffers = extensions.drawBuffers != null,
        depthTexture = extensions.depthTexture != null,
    )

    override val contextID: Long
        get() = context.hashCode().toLong()

    override fun createShader(
        vsCode: String,
        tcsCode: String?,
        tesCode: String?,
        gsCode: String?,
        fsCode: String,
        name: String,
        session: Session?
    ): Shader {
        require(tcsCode == null && tesCode == null && gsCode == null) {
            """only vertex and fragment shaders are supported in WebGL"""
        }
        val vertexShader = VertexShaderWebGL.fromString(context, vsCode, name)
        val fragmentShader = FragmentShaderWebGL.fromString(context, fsCode, name)

        return ShaderWebGL.create(context, vertexShader, fragmentShader, name, session)

    }

    override fun createComputeShader(code: String, session: Session?): ComputeShader {
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
        return ShadeStyleManagerWebGL(name, vsGenerator, fsGenerator, session)
    }

    override fun createRenderTarget(
        width: Int,
        height: Int,
        contentScale: Double,
        multisample: BufferMultisample,
        session: Session?
    ): RenderTargetWebGL {
        return RenderTargetWebGL.create(context, width, height, contentScale, multisample, session)
    }

    override fun createArrayCubemap(
        width: Int,
        layers: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session?
    ): ArrayCubemap {
        error("array cubemaps are not supported by WebGL(1)")
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
        error("not supported")
    }

    override fun createAtomicCounterBuffer(counterCount: Int, session: Session?): AtomicCounterBuffer {
        error("not supported")
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
        return ColorBufferWebGL.create(context, width, height, contentScale, format, type, multisample, levels, session)
    }

    override fun createColorBufferFromUrl(url: String, formatHint: ImageFileFormat?, session: Session?): ColorBuffer {
        return ColorBufferWebGL.fromUrl(context, url, session)
    }


    override suspend fun createColorBufferFromUrlSuspend(url: String, formatHint: ImageFileFormat?, session: Session?): ColorBuffer {
        return ColorBufferWebGL.fromUrlSuspend(context, url, session)
    }

    override fun createColorBufferFromFile(
        filename: String,
        formatHint: ImageFileFormat?,
        session: Session?
    ): ColorBuffer {
        error("loading from file is not supported")
    }

    override fun createColorBufferFromArray(
        array: ByteArray,
        offset: Int,
        length: Int,
        name: String?,
        formatHint: ImageFileFormat?,
        session: Session?
    ): ColorBuffer {
        TODO("Not yet implemented")
    }

    override fun createDepthBuffer(
        width: Int,
        height: Int,
        format: DepthFormat,
        multisample: BufferMultisample,
        session: Session?
    ): DepthBuffer {
        return DepthBufferWebGL.create(context, width, height, format, multisample, session)
    }

    override fun createBufferTexture(
        elementCount: Int,
        format: ColorFormat,
        type: ColorType,
        session: Session?
    ): BufferTexture {
        error("not supported")
    }

    override fun createCubemap(
        width: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session?
    ): Cubemap {
        return CubemapWebGL.create(context, width, format, type, levels, session)
    }

    override fun createCubemapFromUrls(urls: List<String>, formatHint: ImageFileFormat?, session: Session?): Cubemap {
        TODO("Not yet implemented")
    }

    override fun createCubemapFromFiles(
        filenames: List<String>,
        formatHint: ImageFileFormat?,
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
        error("not supported")
    }

    override fun clear(r: Double, g: Double, b: Double, a: Double) {
        context.clearColor(r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat())
        context.clearDepth(1.0f)
        context.disable(GL.SCISSOR_TEST)
        context.depthMask(true)
        context.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT or GL.STENCIL_BUFFER_BIT)
        context.depthMask(false)
    }

    override fun createDynamicVertexBuffer(format: VertexFormat, vertexCount: Int, session: Session?): VertexBuffer {
        return VertexBufferWebGL.createDynamic(context, format, vertexCount, session)
    }

    override fun createDynamicIndexBuffer(elementCount: Int, type: IndexType, session: Session?): IndexBuffer {
        error("not supported")
    }

    override fun createShaderStorageBuffer(format: ShaderStorageFormat, session: Session?): ShaderStorageBuffer {
        error("not supported")
    }

    private fun setupFormat(vertexBuffer: List<VertexBuffer>, instanceAttributes: List<VertexBuffer>, shader: ShaderWebGL) {
        val scalarVectorTypes = setOf(
            VertexElementType.FLOAT32, VertexElementType.VECTOR2_FLOAT32, VertexElementType.VECTOR3_FLOAT32, VertexElementType.VECTOR4_FLOAT32)


        var attribute0Used = false

        fun setupBuffer(buffer: VertexBuffer, divisor: Int = 0) {
            val prefix = if (divisor == 0) "a" else "i"
            var attributeBindings = 0

            context.bindBuffer(GL.ARRAY_BUFFER, (buffer as VertexBufferWebGL).buffer)
            val format = buffer.vertexFormat
            for (item in format.items) {
                val attributeIndex = shader.attributeIndex("${prefix}_${item.attribute}")
                if (attributeIndex == 0) {
                    attribute0Used = true
                }
                if (attributeIndex != -1) {
                    when (item.type) {
                        in scalarVectorTypes -> {
                            for (i in 0 until item.arraySize) {
                                context.enableVertexAttribArray(attributeIndex + i)
                                val glType = item.type.glType()
                                if (glType == GL.FLOAT) {
                                    context.vertexAttribPointer(attributeIndex + i,
                                        item.type.componentCount,
                                        glType, false, format.size, item.offset.toInt() + i * item.type.sizeInBytes)
                                } else {
                                    error("integer attributes are not supported by WebGL")
                                }
                                extensions.instancedArrays?.vertexAttribDivisorANGLE(attributeIndex, divisor) ?: error("instancing not supported")
                                attributeBindings++
                            }
                        }
                        VertexElementType.MATRIX44_FLOAT32 -> {
                            for (i in 0 until item.arraySize) {
                                for (column in 0 until 4) {
                                    context.enableVertexAttribArray(attributeIndex + column + i * 4)
                                    context.vertexAttribPointer(attributeIndex + column + i * 4,
                                        4,
                                        item.type.glType(), false, format.size, item.offset.toInt() + column * 16 + i * 64)
                                    extensions.instancedArrays?.vertexAttribDivisorANGLE(attributeIndex + column + i * 4, divisor) ?: error("instancing not supported")
                                    attributeBindings++
                                }
                            }
                        }
                        VertexElementType.MATRIX33_FLOAT32 -> {
                            for (i in 0 until item.arraySize) {
                                for (column in 0 until 3) {
                                    context.enableVertexAttribArray(attributeIndex + column + i * 3)
                                    context.vertexAttribPointer(attributeIndex + column + i * 3,
                                        3,
                                        item.type.glType(), false, format.size, item.offset + column * 12 + i * 48)
                                    extensions.instancedArrays?.vertexAttribDivisorANGLE(attributeIndex + column + i * 3, divisor) ?: error("instancing not supported")
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
                error("Maximum vertex attributes exceeded $attributeBindings (limit is 16)")
            }
        }
        vertexBuffer.forEach {
            setupBuffer(it, 0)
        }
        instanceAttributes.forEach {
            setupBuffer(it, 1)
        }

        if (!attribute0Used) {
            println("attribute 0 is not used")
        }
    }

    override fun drawVertexBuffer(
        shader: Shader,
        vertexBuffers: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        vertexOffset: Int,
        vertexCount: Int,
        verticesPerPatch: Int
    ) {
        shader as ShaderWebGL
        setupFormat(vertexBuffers, emptyList(), shader)
        context.drawArrays(drawPrimitive.glType(), vertexOffset, vertexCount)
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
        shader as ShaderWebGL
        setupFormat(vertexBuffers, emptyList(), shader)
        //context.drawElements(drawPrimitive.glType(), indexCount, indexBuffer.type.glType(), )
        TODO()
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
        shader as ShaderWebGL
        setupFormat(vertexBuffers, instanceAttributes, shader)
        require(instanceOffset == 0) {
            "instance offsets are not supported"
        }
        //console.log("drawing instances", vertexOffset, vertexCount, instanceCount)
        extensions.instancedArrays?.drawArraysInstancedANGLE(drawPrimitive.glType(), vertexOffset, vertexCount, instanceCount) ?: error("instancing not supported")
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
        TODO()
    }

    private val cached = DrawStyle()
    private var dirty = true
    override fun setState(drawStyle: DrawStyle) {
        if (dirty || cached.clip != drawStyle.clip) {
            if (drawStyle.clip != null) {
                drawStyle.clip?.let {
                    val target = RenderTarget.active
                    context.scissor((it.x * target.contentScale).toInt(), (target.height * target.contentScale - it.y * target.contentScale - it.height * target.contentScale).toInt(), (it.width * target.contentScale).toInt(), (it.height * target.contentScale).toInt())
                    context.enable(GL.SCISSOR_TEST)
                }
            } else {
                context.disable(GL.SCISSOR_TEST)
            }
            cached.clip = drawStyle.clip
        }
        if (dirty || cached.channelWriteMask != drawStyle.channelWriteMask) {
            context.colorMask(drawStyle.channelWriteMask.red, drawStyle.channelWriteMask.green, drawStyle.channelWriteMask.blue, drawStyle.channelWriteMask.alpha)
            cached.channelWriteMask = drawStyle.channelWriteMask
        }
        if (dirty || cached.depthWrite != drawStyle.depthWrite) {
            when (drawStyle.depthWrite) {
                true -> context.depthMask(true)
                false ->context.depthMask(false)
            }
            context.enable(GL.DEPTH_TEST)
            cached.depthWrite = drawStyle.depthWrite
        }
        if (drawStyle.frontStencil === drawStyle.backStencil) {
            if (drawStyle.stencil.stencilTest === StencilTest.DISABLED) {
                context.disable(GL.STENCIL_TEST)
            } else {
                context.enable(GL.STENCIL_TEST)
                context.stencilFunc(glStencilTest(drawStyle.stencil.stencilTest), drawStyle.stencil.stencilTestReference, drawStyle.stencil.stencilTestMask)
                //debugGLErrors()
                context.stencilOp(glStencilOp(drawStyle.stencil.stencilFailOperation), glStencilOp(drawStyle.stencil.depthFailOperation), glStencilOp(drawStyle.stencil.depthPassOperation))
                //debugGLErrors()
                context.stencilMask(drawStyle.stencil.stencilWriteMask)
                //debugGLErrors()
            }
        } else {
            context.enable(GL.STENCIL_TEST)
            context.stencilFuncSeparate(GL.FRONT, glStencilTest(drawStyle.frontStencil.stencilTest), drawStyle.frontStencil.stencilTestReference, drawStyle.frontStencil.stencilTestMask)
            context.stencilFuncSeparate(GL.BACK, glStencilTest(drawStyle.backStencil.stencilTest), drawStyle.backStencil.stencilTestReference, drawStyle.backStencil.stencilTestMask)
            context.stencilOpSeparate(GL.FRONT, glStencilOp(drawStyle.frontStencil.stencilFailOperation), glStencilOp(drawStyle.frontStencil.depthFailOperation), glStencilOp(drawStyle.frontStencil.depthPassOperation))
            context.stencilOpSeparate(GL.BACK, glStencilOp(drawStyle.backStencil.stencilFailOperation), glStencilOp(drawStyle.backStencil.depthFailOperation), glStencilOp(drawStyle.backStencil.depthPassOperation))
            context.stencilMaskSeparate(GL.FRONT, drawStyle.frontStencil.stencilWriteMask)
            context.stencilMaskSeparate(GL.BACK, drawStyle.backStencil.stencilWriteMask)
        }
        if (dirty || cached.blendMode != drawStyle.blendMode) {
            when (drawStyle.blendMode) {
                BlendMode.OVER -> {
                    context.enable(GL.BLEND)
                    context.blendEquation(GL.FUNC_ADD)
                    context.blendFunc(GL.ONE, GL.ONE_MINUS_SRC_ALPHA)
                }
                BlendMode.BLEND -> {
                    context.enable(GL.BLEND)
                    context.blendEquation(GL.FUNC_ADD)
                    context.blendFunc(GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA)
                }
                BlendMode.ADD -> {
                    context.enable(GL.BLEND)
                    context.blendEquation(GL.FUNC_ADD)
                    context.blendFunc(GL.ONE, GL.ONE)
                }

                BlendMode.REPLACE -> {
                    context.disable(GL.BLEND)
                }
                BlendMode.SUBTRACT -> {
                    context.enable(GL.BLEND)
                    context.blendEquationSeparate(GL.FUNC_REVERSE_SUBTRACT, GL.FUNC_ADD)
                    context.blendFuncSeparate(GL.SRC_ALPHA, GL.ONE, GL.ONE, GL.ONE)
                }
                BlendMode.MULTIPLY -> {
                    context.enable(GL.BLEND)
                    context.blendEquation(GL.FUNC_ADD)
                    context.blendFunc(GL.DST_COLOR, GL.ONE_MINUS_SRC_ALPHA)
                }
                BlendMode.REMOVE -> {
                    context.enable(GL.BLEND)
                    context.blendEquation(GL.FUNC_ADD)
                    context.blendFunc(GL.ZERO, GL.ONE_MINUS_SRC_ALPHA)
                }
            }
            cached.blendMode = drawStyle.blendMode
        }
        if (dirty || cached.alphaToCoverage != drawStyle.alphaToCoverage) {
            if (drawStyle.alphaToCoverage) {
                context.enable(GL.SAMPLE_ALPHA_TO_COVERAGE)
                context.disable(GL.BLEND)
            } else {
                context.disable(GL.SAMPLE_ALPHA_TO_COVERAGE)
            }
            cached.alphaToCoverage = drawStyle.alphaToCoverage
        }
        if (dirty || cached.depthTestPass != drawStyle.depthTestPass) {
            when (drawStyle.depthTestPass) {
                DepthTestPass.ALWAYS -> {
                    context.depthFunc(GL.ALWAYS)
                }
                DepthTestPass.GREATER -> {
                    context.depthFunc(GL.GREATER)
                }
                DepthTestPass.GREATER_OR_EQUAL -> {
                    context.depthFunc(GL.GEQUAL)
                }
                DepthTestPass.LESS -> {
                    context.depthFunc(GL.LESS)
                }
                DepthTestPass.LESS_OR_EQUAL -> {
                    context.depthFunc(GL.LEQUAL)
                }
                DepthTestPass.EQUAL -> {
                    context.depthFunc(GL.EQUAL)
                }
                DepthTestPass.NEVER -> {
                    context.depthFunc(GL.NEVER)
                }
            }
            //debugGLErrors()
            cached.depthTestPass = drawStyle.depthTestPass
        }
        if (dirty || cached.cullTestPass != drawStyle.cullTestPass) {
            when (drawStyle.cullTestPass) {
                CullTestPass.ALWAYS -> {
                    context.disable(GL.CULL_FACE)
                }
                CullTestPass.FRONT -> {
                    context.enable(GL.CULL_FACE)
                    context.cullFace(GL.BACK)
                }
                CullTestPass.BACK -> {
                    context.enable(GL.CULL_FACE)
                    context.cullFace(GL.FRONT)
                }
                CullTestPass.NEVER -> {
                    context.enable(GL.CULL_FACE)
                    context.cullFace(GL.FRONT_AND_BACK)
                }
            }
            cached.cullTestPass = drawStyle.cullTestPass
        }
    }

    override fun destroyContext(context: Long) {
        TODO("Not yet implemented")
    }

    override val fontImageMapManager: FontMapManager
        get() = TODO("Not yet implemented")
    override val fontVectorMapManager: FontMapManager
        get() = TODO("Not yet implemented")
    override val shaderGenerators: ShaderGenerators by lazy {
        ShaderGeneratorsWebGL()
    }

    override val activeRenderTarget: RenderTargetWebGL
        get() = RenderTargetWebGL.activeRenderTarget

    override fun finish() {
        context.flush()
        context.finish()
    }

    override fun internalShaderResource(resourceId: String): String {
        return when (resourceId) {
            "filter.vert" -> {
                """
                attribute vec2 a_texCoord0;
                attribute vec2 a_position;

                uniform vec2 targetSize;
                uniform vec2 padding;
                uniform mat4 projectionMatrix;

                varying mediump vec2 v_texCoord0;

                void main() {
                    v_texCoord0 = a_texCoord0;
                    vec2 transformed = a_position * (targetSize - 2.0 * padding) + padding;
                    gl_Position = projectionMatrix * vec4(transformed, 0.0, 1.0);
                }
                """
            }
            else -> error("unknown resource '$resourceId'")
        }
    }

    override val shaderLanguage: ShaderLanguage
        get() = WebGLSL("100")


    override fun shaderConfiguration(): String {
        return """
            precision highp float;
            #define OR_GL_FRAGCOLOR
            #define OR_VARYINGS
            #define OR_WEBGL1
        """.trimIndent()
    }
}

internal fun ColorFormat.glFormat(): Int {
    return when (this) {
        ColorFormat.R -> GL.LUMINANCE
        ColorFormat.RG -> GL.LUMINANCE_ALPHA
        ColorFormat.RGB -> GL.RGB
        ColorFormat.RGBa -> GL.RGBA
        ColorFormat.sRGB -> GL.RGB
        ColorFormat.sRGBa -> GL.RGBA
        ColorFormat.BGR -> error("BGR not supported")
        ColorFormat.BGRa -> error("BGRa not supported")
    }
}

internal fun ColorType.glType(): Int {
    return when (this) {
        ColorType.UINT8, ColorType.UINT8_INT -> GL.UNSIGNED_BYTE
        ColorType.SINT8_INT -> GL.BYTE
        ColorType.UINT16, ColorType.UINT16_INT -> GL.UNSIGNED_SHORT
        ColorType.SINT16_INT -> GL.SHORT
        ColorType.UINT32_INT -> GL.UNSIGNED_INT
        ColorType.SINT32_INT -> GL.INT
        ColorType.FLOAT16 -> HALF_FLOAT_OES
        ColorType.FLOAT32 -> GL.FLOAT
        ColorType.DXT1, ColorType.DXT3, ColorType.DXT5,
        ColorType.BPTC_UNORM, ColorType.BPTC_FLOAT, ColorType.BPTC_UFLOAT -> throw RuntimeException("gl type of compressed types cannot be queried")
    }
}
