package org.openrndr.internal.gl3

import android.opengl.GLES30
import android.opengl.GLES32
import android.opengl.GLU
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
import org.openrndr.draw.VertexFormat
import org.openrndr.draw.VolumeTexture
import org.openrndr.draw.vertexFormat
import org.openrndr.internal.Driver
import org.openrndr.internal.DriverProperties
import org.openrndr.internal.FontMapManager
import org.openrndr.internal.GLSL
import org.openrndr.internal.ResourceThread
import org.openrndr.internal.ShaderGenerators
import org.openrndr.internal.ShaderLanguage
import org.openrndr.internal.gl.BASIC_SOLID_FS
import org.openrndr.internal.gl.BASIC_SOLID_VS
import org.openrndr.internal.glcommon.ComputeStyleManagerGLCommon
import org.openrndr.internal.glcommon.ShadeStyleManagerGLCommon
import org.openrndr.internal.glcommon.ShaderGeneratorsGLCommon
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val logger = KotlinLogging.logger {}

class DriverAndroidGLES : Driver {

    private var currentWidth = 0
    private var currentHeight = 0
    private var currentFill = ColorRGBa.WHITE

    //    private var renderTarget = RenderTargetGLES.create(0, 0, 1.0, BufferMultisample.Disabled, null)
    private lateinit var renderTarget: RenderTargetGLES

    override val contextID: Long
        get() {
            return Thread.currentThread().id
        }

    private val vao = IntArray(1)
    private var vaoReady = false

    // dev: to test triangle display only
    private lateinit var vb: VertexBuffer

    private fun ensureVaoOrThrow() {
        // Clear any stale error so next check is meaningful
        while (GLES30.glGetError() != GLES30.GL_NO_ERROR) {
        }

        if (!vaoReady) {
            GLES30.glGenVertexArrays(1, vao, 0)
            if (vao[0] == 0) {
                throw RuntimeException("Failed to create VAO (id==0). Check GL context & version.")
            }
            vaoReady = true
        }
        GLES30.glBindVertexArray(vao[0])
        val err = GLES30.glGetError()
        if (err != GLES30.GL_NO_ERROR) {
            throw RuntimeException(
                "glBindVertexArray(${vao[0]}) failed with error $err. " +
                        "Ensure you really have a GLES 3.x context (setEGLContextClientVersion(3))."
            )
        }
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        logger.info { "onSurfaceChanged - width: $width - height: $height" }
        currentWidth = width
        currentHeight = height
        renderTarget = RenderTargetGLES.create(width, height, 1.0, BufferMultisample.Disabled, null)
        viewport(width, height) // make sure GL viewport matches display


        val verts = floatArrayOf(
            100f, 100f,
            300f, 100f,
            200f, 300f
        )
        val bb = ByteBuffer
            .allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder())

        // 2) Fill it via a FloatBuffer view
        bb.asFloatBuffer().put(verts)

        // 3) Rewind the ByteBuffer (important!)
        bb.position(0)

        val vf = vertexFormat { position(2) }
        vb = createDynamicVertexBuffer(vf, 3)
        (vb as VertexBufferGLES).write(bb, 0)
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
        // If explicit code is provided, compile it; otherwise use the solid fill shader.
        val vs = vsCode.ifBlank { BASIC_SOLID_VS }
        val fs = fsCode.ifBlank { BASIC_SOLID_FS }
        return ShaderGLES.fromSource(vs, fs, session)
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

    override fun clear(color: ColorRGBa) {
        // Bind the on-screen framebuffer (default)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        // Clear entire surface
        GLES30.glDisable(GLES30.GL_SCISSOR_TEST)
        // Set the clear color
        GLES30.glClearColor(
            color.r.toFloat(),
            color.g.toFloat(),
            color.b.toFloat(),
            color.a.toFloat()
        )
        // Clear color (and optionally depth/stencil)
        GLES30.glClear(
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
        return VertexBufferGLES(format, vertexCount, session)
    }

    override fun createStaticVertexBuffer(
        format: VertexFormat,
        buffer: Buffer,
        session: Session?
    ): VertexBuffer {
        return VertexBufferGLES(format, 100, session)
    }

    override fun createDynamicIndexBuffer(
        elementCount: Int,
        type: IndexType,
        session: Session?
    ): IndexBuffer {
        TODO("Not yet implemented")
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
        ensureVaoOrThrow()

        val sh = (shader as ShaderGLES)// ?: solidShader
        sh.begin()
        sh.uniform2f("u_resolution", currentWidth.toFloat(), currentHeight.toFloat())
        sh.uniform4f(
            "u_fill",
            currentFill.r.toFloat(),
            currentFill.g.toFloat(),
            currentFill.b.toFloat(),
            currentFill.a.toFloat()
        )

        // per-vertex (divisor 0), no base shift; we pass vertexOffset in draw
        vertexBuffers.forEach { vb ->
            vb as VertexBufferGLES
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vb.id)
            bindFormatAttributesForProgram(
                sh.programId,
                vb.vertexFormat,
                baseShiftBytes = 0,
                divisor = 0
            )
        }

        val mode = glMode(drawPrimitive)

        GLES30.glDrawArrays(mode, vertexOffset, vertexCount)

        // cleanup
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        sh.end()

        debugGLErrors("drawVertexBuffer")

//        logger.info { "draw mode=${mode}, count=$vertexCount, res=${currentWidth}x${currentHeight}, fill=$currentFill" }
//        println("draw mode=${mode}, count=$vertexCount, res=${currentWidth}x${currentHeight}, fill=$currentFill")
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

        val progId = (shader as ShaderGLES).programId

        // Build a VAO key
        val vbIds = IntArray(vertexBuffers.size) { (vertexBuffers[it] as VertexBufferGLES).id }
        val instIds =
            IntArray(instanceAttributes.size) { (instanceAttributes[it] as VertexBufferGLES).id }
        val key = VaoKey(
            contextID = contextID,
            programId = progId,
            vertexBuffers = vbIds.toList(),
            instanceAttributeBuffers = instIds.toList(),
            indexBuffer = null
        )

        // Find or create the VAO
        val vao = vaos.getOrPut(key) {
            logger.debug { "creating new instances VAO for key=$key" }
            val arr = IntArray(1)
            GLES30.glGenVertexArrays(1, arr, 0)
            val vaoId = arr[0]
            GLES30.glBindVertexArray(vaoId)

            // Setup per-vertex attributes (divisor 0)
            vertexBuffers.forEach {
                bindAttributesForBuffer(
                    it as VertexBufferGLES,
                    divisor = 0,
                    progId = progId
                )
            }
            // Setup per-instance attributes (divisor 1)
            instanceAttributes.forEach {
                bindAttributesForBuffer(
                    it as VertexBufferGLES,
                    divisor = 1,
                    progId = progId
                )
            }

            GLES30.glBindVertexArray(defaultVAO)
            vaoId
        }

        // Bind VAO and draw
        GLES30.glBindVertexArray(vao)
        val mode = glMode(drawPrimitive)
        GLES30.glDrawArraysInstanced(mode, vertexOffset, vertexCount, instanceCount)

        // Optional debug
        debugGLErrors("drawInstances")

        // Restore default VAO
        GLES30.glBindVertexArray(defaultVAO)
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
        GLES30.glViewport(0, 0, width, height)
    }

    override fun destroyContext(context: Long) {

    }

    fun destroyVAOsForVertexBuffer(vertexBuffer: VertexBufferGLES) {
        val candidates = vaos.keys.filter { key ->
            key.vertexBuffers.contains(vertexBuffer.id) || key.instanceAttributeBuffers.contains(
                vertexBuffer.id
            )
        }

        for (key in candidates) {
            val vaoId = vaos[key] ?: error("no vao found for key: $key")
            logger.debug { "removing VAO $vaoId for $key" }
            val arr = intArrayOf(vaoId)
            GLES30.glDeleteVertexArrays(1, arr, 0)
            debugGLErrors("destroyVAOsForVertexBuffer")
            vaos.remove(key)
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

    private val vaos = mutableMapOf<VaoKey, Int>() // Key -> VAO id

    // Create a default VAO once (ES requires a bound VAO when setting pointers)
    private val defaultVAO: Int by lazy {
        val a = IntArray(1)
        GLES30.glGenVertexArrays(1, a, 0)
        a[0]
    }

    // Map DrawPrimitive → GLES mode
    private fun glMode(p: DrawPrimitive): Int = when (p) {
        DrawPrimitive.POINTS -> GLES30.GL_POINTS
        DrawPrimitive.LINES -> GLES30.GL_LINES
        DrawPrimitive.LINE_STRIP -> GLES30.GL_LINE_STRIP
        DrawPrimitive.TRIANGLES -> GLES30.GL_TRIANGLES
        DrawPrimitive.TRIANGLE_STRIP -> GLES30.GL_TRIANGLE_STRIP
        DrawPrimitive.TRIANGLE_FAN -> GLES30.GL_TRIANGLE_FAN
        else -> GLES30.GL_TRIANGLES
    }

    private fun debugGLErrors(context: String = "DriverAndroidGLES") {
        val err = GLES30.glGetError()
        if (err != GLES30.GL_NO_ERROR) {
            val errorString = GLU.gluErrorString(err)
            println("$context: glGetError() = $err - errorString: $errorString")
//            logger.warn { "$context: glGetError() = $err" }
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
            var loc = GLES30.glGetAttribLocation(programId, elem.attribute)
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
                if (alias != null) loc = GLES30.glGetAttribLocation(programId, alias)
            }
            if (loc < 0) {
                // Attribute not used by the current shader – skip it
                return@forEachIndexed
            }

            val (size, glType, normalized, isInteger) = glAttribOf(elem.type)
            val pointer = elem.offset + baseShiftBytes

            GLES30.glEnableVertexAttribArray(loc)
            val err0 = GLES30.glGetError()
            if (err0 != GLES30.GL_NO_ERROR) {
                throw RuntimeException("glEnableVertexAttribArray($loc) error=$err0 (attr='${elem.attribute}')")
            }

            if (isInteger) {
                GLES30.glVertexAttribIPointer(loc, size, glType, stride, pointer)
            } else {
                GLES30.glVertexAttribPointer(loc, size, glType, normalized, stride, pointer)
            }
            val err1 = GLES30.glGetError()
            if (err1 != GLES30.GL_NO_ERROR) {
                throw RuntimeException(
                    "glVertexAttribPointer/IPointer(loc=$loc,size=$size,stride=$stride,ptr=$pointer) error=$err1 " +
                            "(attr='${elem.attribute}', type=${elem.type})"
                )
            }

            GLES30.glVertexAttribDivisor(loc, divisor)
            val err2 = GLES30.glGetError()
            if (err2 != GLES30.GL_NO_ERROR) {
                throw RuntimeException("glVertexAttribDivisor($loc,$divisor) error=$err2")
            }
        }
    }

    // Bind all attributes of a VBO with the requested divisor (0 = per-vertex, 1 = per-instance)
    private fun bindAttributesForBuffer(vb: VertexBufferGLES, divisor: Int, progId: Int) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vb.id)
        val stride = vb.vertexFormat.size

        vb.vertexFormat.items.forEachIndexed { rawIndex, elem ->
            if (elem.attribute == "_") return@forEachIndexed
            // Resolve location from shader — safer than hardcoding indices
            val loc = GLES30.glGetAttribLocation(progId, elem.attribute)
            if (loc < 0) return@forEachIndexed // shader doesn't use it

            val (size, glType, normalized, isInteger) = glAttribOf(elem.type)

            GLES30.glEnableVertexAttribArray(loc)

            if (isInteger) {
                // ONLY use IPointer if shader input is ivec*/uvec*
                GLES30.glVertexAttribIPointer(loc, size, glType, stride, elem.offset)
            } else {
                GLES30.glVertexAttribPointer(loc, size, glType, normalized, stride, elem.offset)
            }
            GLES30.glVertexAttribDivisor(loc, divisor)
            // Optional: check error per attribute during bring-up
            // val err = GLES30.glGetError(); if (err != GLES30.GL_NO_ERROR) log...
        }
        // DO NOT glBindBuffer(GL_ARRAY_BUFFER, 0) here — harmless to keep bound.
    }

    private data class GlAttrib(
        val size: Int,
        val glType: Int,
        val normalized: Boolean,
        val isInteger: Boolean
    )

    private fun glAttribOf(type: org.openrndr.draw.VertexElementType): GlAttrib = when (type) {
        org.openrndr.draw.VertexElementType.FLOAT32 -> GlAttrib(1, GLES30.GL_FLOAT, false, false)
        org.openrndr.draw.VertexElementType.VECTOR2_FLOAT32 -> GlAttrib(
            2,
            GLES30.GL_FLOAT,
            false,
            false
        )

        org.openrndr.draw.VertexElementType.VECTOR3_FLOAT32 -> GlAttrib(
            3,
            GLES30.GL_FLOAT,
            false,
            false
        )

        org.openrndr.draw.VertexElementType.VECTOR4_FLOAT32 -> GlAttrib(
            4,
            GLES30.GL_FLOAT,
            false,
            false
        )

        org.openrndr.draw.VertexElementType.INT32 -> GlAttrib(1, GLES30.GL_INT, false, true)
        org.openrndr.draw.VertexElementType.VECTOR2_INT32 -> GlAttrib(2, GLES30.GL_INT, false, true)
        org.openrndr.draw.VertexElementType.VECTOR3_INT32 -> GlAttrib(3, GLES30.GL_INT, false, true)
        org.openrndr.draw.VertexElementType.VECTOR4_INT32 -> GlAttrib(4, GLES30.GL_INT, false, true)

        org.openrndr.draw.VertexElementType.UINT32 -> GlAttrib(
            1,
            GLES30.GL_UNSIGNED_INT,
            false,
            true
        )

        org.openrndr.draw.VertexElementType.VECTOR2_UINT32 -> GlAttrib(
            2,
            GLES30.GL_UNSIGNED_INT,
            false,
            true
        )

        org.openrndr.draw.VertexElementType.VECTOR3_UINT32 -> GlAttrib(
            3,
            GLES30.GL_UNSIGNED_INT,
            false,
            true
        )

        org.openrndr.draw.VertexElementType.VECTOR4_UINT32 -> GlAttrib(
            4,
            GLES30.GL_UNSIGNED_INT,
            false,
            true
        )

        org.openrndr.draw.VertexElementType.UINT8 -> GlAttrib(
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