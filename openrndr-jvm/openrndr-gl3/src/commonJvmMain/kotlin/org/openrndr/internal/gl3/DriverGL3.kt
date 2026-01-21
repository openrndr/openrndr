package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL14.glMultiDrawArrays
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.*
import org.openrndr.internal.glcommon.ComputeStyleManagerGLCommon
import org.openrndr.internal.glcommon.ShadeStyleManagerGLCommon
import org.openrndr.internal.glcommon.ShaderGeneratorsGLCommon
import org.openrndr.math.Matrix33
import org.openrndr.math.Matrix44
import org.openrndr.shape.Rectangle
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import kotlin.math.min

private val logger = KotlinLogging.logger {}


@Suppress("SpellCheckingInspection")
enum class GlesBackend {
    SYSTEM,
    ANGLE
}

@Suppress("SpellCheckingInspection")
enum class DriverTypeGL {
    GL,
    GLES
}

@Suppress("SpellCheckingInspection")
enum class DriverVersionGL(
    val type: DriverTypeGL,
    val glslVersion: String,
    val majorVersion: Int,
    val minorVersion: Int
) {
    GL_VERSION_3_3(DriverTypeGL.GL, "330 core", 3, 3),
    GL_VERSION_4_1(DriverTypeGL.GL, "410 core", 4, 1),
    GL_VERSION_4_2(DriverTypeGL.GL, "420 core", 4, 2),
    GL_VERSION_4_3(DriverTypeGL.GL, "430 core", 4, 3),
    GL_VERSION_4_4(DriverTypeGL.GL, "440 core", 4, 4),
    GL_VERSION_4_5(DriverTypeGL.GL, "450 core", 4, 5),
    GL_VERSION_4_6(DriverTypeGL.GL, "460 core", 4, 6),

    GLES_VERSION_3_0(DriverTypeGL.GLES, "300 es", 3, 0),
    GLES_VERSION_3_1(DriverTypeGL.GLES, "310 es", 3, 1),
    GLES_VERSION_3_2(DriverTypeGL.GLES, "320 es", 3, 2);

    val versionString
        get() = "$majorVersion.$minorVersion"

    fun isAtLeast(vararg version: DriverVersionGL): Boolean =
        version.any {
            (this >= it && it.type == type)
        }

    companion object {
        /**
         * Finds and returns a DriverVersionGL constant that matches the specified driver type,
         * major version, and minor version.
         *
         * @param type The type of driver (e.g., GL or GLES).
         * @param majorVersion The major version of the driver to find.
         * @param minorVersion The minor version of the driver to find.
         * @return The matching DriverVersionGL constant, or null if no match is found.
         */
        fun find(type: DriverTypeGL, majorVersion: Int, minorVersion: Int): DriverVersionGL? {
            return entries.find {
                it.type == type && it.majorVersion == majorVersion && it.minorVersion == minorVersion
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun DriverVersionGL.require(minimum: DriverVersionGL) {
    require(ordinal >= minimum.ordinal) {
        """Feature is not supported on current OpenGL configuration (configuration: ${this.versionString}, required: ${minimum.versionString})"""
    }
}

abstract class DriverGL3(val version: DriverVersionGL) : Driver {


    private val cachedTextureBindings = IntArray(32) { 0 }

    fun applyBlendMode(drawStyle: DrawStyle) {
        if (true) {

            // TODO introduce caching

            val rt = RenderTarget.active
            for (i in 0 until rt.blendModes.size) {

                val blendMode = drawStyle.blendMode ?: rt.blendModes[i]

                fun setAdvancedEq(buf: Int, eq: Int) {
                    glEnable(GL_BLEND)
                    if (Driver.glVersion.isAtLeast(DriverVersionGL.GL_VERSION_4_1, DriverVersionGL.GLES_VERSION_3_2)) {
                        glBlendEquationi(buf, eq)
                        glBlendFunci(buf, GL_ONE, GL_ONE)
                    } else {
                        glBlendEquation(eq)
                        glBlendFunc(GL_ONE, GL_ONE)
                    }
                }

                when (blendMode) {
                    BlendMode.OVER -> {
                        glEnable(GL_BLEND)
                        if (Driver.glVersion.isAtLeast(
                                DriverVersionGL.GL_VERSION_4_1,
                                DriverVersionGL.GLES_VERSION_3_2
                            )
                        ) {
                            glBlendEquationi(0, GL_FUNC_ADD)
                            glBlendFunci(0, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
                        } else {
                            glBlendEquation(GL_FUNC_ADD)
                            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
                        }
                    }

                    BlendMode.BLEND -> {
                        glEnable(GL_BLEND)
                        if (Driver.glVersion.isAtLeast(
                                DriverVersionGL.GL_VERSION_4_1,
                                DriverVersionGL.GLES_VERSION_3_2
                            )
                        ) {
                            glBlendEquationi(0, GL_FUNC_ADD)
                            glBlendFunci(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                        } else {
                            glBlendEquation(GL_FUNC_ADD)
                            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                        }
                    }

                    BlendMode.ADD -> {
                        glEnable(GL_BLEND)
                        if (Driver.glVersion.isAtLeast(
                                DriverVersionGL.GL_VERSION_4_1,
                                DriverVersionGL.GLES_VERSION_3_2
                            )
                        ) {
                            glBlendEquationi(0, GL_FUNC_ADD)
                            glBlendFunci(0, GL_ONE, GL_ONE)
                        } else {
                            glBlendEquation(GL_FUNC_ADD)
                            glBlendFunc(GL_ONE, GL_ONE)
                        }
                    }

                    BlendMode.REPLACE -> {
                        glDisable(GL_BLEND)
                    }

                    BlendMode.SUBTRACT -> {
                        glEnable(GL_BLEND)
                        if (Driver.glVersion.isAtLeast(
                                DriverVersionGL.GL_VERSION_4_1,
                                DriverVersionGL.GLES_VERSION_3_2
                            )
                        ) {
                            glBlendEquationSeparatei(0, GL_FUNC_REVERSE_SUBTRACT, GL_FUNC_ADD)
                            glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE, GL_ONE, GL_ONE)
                        } else {
                            glBlendEquationSeparate(GL_FUNC_REVERSE_SUBTRACT, GL_FUNC_ADD)
                            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE, GL_ONE, GL_ONE)
                        }
                    }

                    BlendMode.MULTIPLY -> {
                        glEnable(GL_BLEND)
                        if (Driver.glVersion.isAtLeast(
                                DriverVersionGL.GL_VERSION_4_1,
                                DriverVersionGL.GLES_VERSION_3_2
                            )
                        ) {
                            glBlendEquationi(0, GL_FUNC_ADD)
                            glBlendFunci(0, GL_DST_COLOR, GL_ONE_MINUS_SRC_ALPHA)
                        } else {
                            glBlendEquation(GL_FUNC_ADD)
                            glBlendFunc(GL_DST_COLOR, GL_ONE_MINUS_SRC_ALPHA)
                        }
                    }

                    BlendMode.REMOVE -> {
                        glEnable(GL_BLEND)
                        if (Driver.glVersion.isAtLeast(
                                DriverVersionGL.GL_VERSION_4_1,
                                DriverVersionGL.GLES_VERSION_3_2
                            )
                        ) {
                            glBlendEquationi(0, GL_FUNC_ADD)
                            glBlendFunci(0, GL_ZERO, GL_ONE_MINUS_SRC_ALPHA)
                        } else {
                            glBlendEquation(GL_FUNC_ADD)
                            glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_ALPHA)
                        }
                    }

                    BlendMode.MIN -> {
                        glEnable(GL_BLEND)
                        if (Driver.glVersion.isAtLeast(
                                DriverVersionGL.GL_VERSION_4_1,
                                DriverVersionGL.GLES_VERSION_3_2
                            )
                        ) {
                            glBlendEquationi(0, GL_MIN)
                            glBlendFunci(0, GL_ONE, GL_ONE)
                        } else {
                            glBlendEquation(GL_MIN)
                            glBlendFunc(GL_ONE, GL_ONE)
                        }
                    }

                    BlendMode.MAX -> {
                        glEnable(GL_BLEND)
                        if (Driver.glVersion.isAtLeast(
                                DriverVersionGL.GL_VERSION_4_1,
                                DriverVersionGL.GLES_VERSION_3_2
                            )
                        ) {
                            glBlendEquationi(0, GL_MAX)
                            glBlendFunci(0, GL_ONE, GL_ONE)
                        } else {
                            glBlendEquation(GL_MAX)
                            glBlendFunc(GL_ONE, GL_ONE)
                        }
                    }

                    BlendMode.SCREEN -> setAdvancedEq(i, GL_SCREEN_KHR)
                    BlendMode.OVERLAY -> setAdvancedEq(i, GL_OVERLAY_KHR)
                    BlendMode.DARKEN -> setAdvancedEq(i, GL_DARKEN_KHR)
                    BlendMode.LIGHTEN -> setAdvancedEq(i, GL_LIGHTEN_KHR)
                    BlendMode.COLOR_DODGE -> setAdvancedEq(i, GL_COLORDODGE_KHR)
                    BlendMode.COLOR_BURN -> setAdvancedEq(i, GL_COLORBURN_KHR)
                    BlendMode.HARD_LIGHT -> setAdvancedEq(i, GL_HARDLIGHT_KHR)
                    BlendMode.SOFT_LIGHT -> setAdvancedEq(i, GL_SOFTLIGHT_KHR)
                    BlendMode.DIFFERENCE -> setAdvancedEq(i, GL_DIFFERENCE_KHR)
                    BlendMode.EXCLUSION -> setAdvancedEq(i, GL_EXCLUSION_KHR)
                    BlendMode.HSL_HUE -> setAdvancedEq(i, GL_HSL_HUE_KHR)
                    BlendMode.HSL_SATURATION -> setAdvancedEq(i, GL_HSL_SATURATION_KHR)
                    BlendMode.HSL_COLOR -> setAdvancedEq(i, GL_HSL_COLOR_KHR)
                    BlendMode.HSL_LUMINOSITY -> setAdvancedEq(i, GL_HSL_LUMINOSITY_KHR)
                }
            }
            //cached.blendMode = drawStyle.blendMode
        }
    }

    fun applyTextureBindings(bindings: TextureBindings) {
        bindings.binding.forEach { i, texture ->
            glActiveTexture(GL_TEXTURE0 + i)

            when (texture) {
                is ColorBufferGL3 -> {
                    if (cachedTextureBindings[i] != texture.texture) {
                        glBindTexture(texture.target, texture.texture)
                        cachedTextureBindings[i] = texture.texture
                    }
                }

                is ArrayTextureGL3 -> {
                    if (cachedTextureBindings[i] != texture.texture) {
                        glBindTexture(texture.target, texture.texture)
                        cachedTextureBindings[i] = texture.texture
                    }
                }

                is ArrayCubemapGL4 -> {
                    if (cachedTextureBindings[i] != texture.texture) {
                        glBindTexture(texture.target, texture.texture)
                        cachedTextureBindings[i] = texture.texture
                    }
                }

                is VolumeTextureGL3 -> {
                    if (cachedTextureBindings[i] != texture.texture) {
                        glBindTexture(GL_TEXTURE_3D, texture.texture)
                        cachedTextureBindings[i] = texture.texture
                    }
                }

                is CubemapGL3 -> {
                    if (cachedTextureBindings[i] != texture.texture) {
                        glBindTexture(GL_TEXTURE_CUBE_MAP, texture.texture)
                        cachedTextureBindings[i] = texture.texture
                    }
                }

                else -> error("unsupported texture type $texture")
            }
        }
    }

    private val executionQueue = mutableListOf<() -> Unit>()

    fun executeOnMainThread(f: () -> Unit) {
        synchronized(executionQueue) {
            executionQueue.add(f)
        }
    }

    fun processMainThreadExecutables() {
        synchronized(executionQueue) {
            for (runnable in executionQueue) {
                try {
                    runnable()
                } catch (e: Throwable) {
                    logger.error(e) {
                        "an exception occurred during main thread execution"
                    }
                    throw e
                }
            }
            executionQueue.clear()
        }
    }

    data class Capabilities(
        val programUniform: Boolean,
        val textureStorage: Boolean,
        val textureMultisampleStorage: Boolean,
        val compute: Boolean,
    )

    val capabilities = Capabilities(
        programUniform = version.isAtLeast(DriverVersionGL.GL_VERSION_4_1, DriverVersionGL.GLES_VERSION_3_1),
        textureStorage = version.isAtLeast(DriverVersionGL.GL_VERSION_4_1, DriverVersionGL.GLES_VERSION_3_0),
        textureMultisampleStorage = version.isAtLeast(DriverVersionGL.GL_VERSION_4_3, DriverVersionGL.GLES_VERSION_3_1),
        compute = version.isAtLeast(DriverVersionGL.GL_VERSION_4_3, DriverVersionGL.GLES_VERSION_3_1)
    )

    class Quirks {
        val clearIgnoresSRGB: Boolean by lazy {
            val rt = renderTarget(64, 64) {
                colorBuffer()
            }
            rt.bind()
            (Driver.driver as DriverGL3).clearImpl(ColorRGBa(0.5, 0.5, 0.5, 1.0))
            rt.unbind()
            val buffer = ByteBuffer.allocateDirect(64 * 64 * 4).order(
                ByteOrder.nativeOrder()
            )
            buffer.rewind()
            (Driver.instance as DriverGL3).finish()
            rt.colorBuffer(0).read(buffer)
            (Driver.instance as DriverGL3).finish()
            val c = buffer.getInt(0).toUInt()
            val quirky = (c and (0xff.toUInt())) == (0x7f).toUInt()
            if (quirky) {
                logger.warn { "quirk: clear ignores sRGB setting" }
            }
            rt.destroy()
            quirky
        }
    }

    val quirks = Quirks()

    override val properties: DriverProperties by lazy {
        DriverProperties(
            maxRenderTargetSamples = when (Driver.glType) {
                DriverTypeGL.GLES -> 4
                DriverTypeGL.GL -> min(
                    glGetInteger(GL_MAX_COLOR_TEXTURE_SAMPLES),
                    glGetInteger(GL_MAX_DEPTH_TEXTURE_SAMPLES)
                )
            },
            maxTextureSamples = when (Driver.glType) {
                DriverTypeGL.GLES -> 4
                DriverTypeGL.GL -> glGetInteger(GL_MAX_COLOR_TEXTURE_SAMPLES)
            },
            maxTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE),
        )
    }


    //var angleExtensions: AngleExtensions? = null

    /*
    fun setupExtensions(functionProvider: FunctionProvider) {
        val isAngle = version.type == DriverTypeGL.GLES && glGetString(GL_RENDERER)?.contains("ANGLE") == true
        if (isAngle) {
            angleExtensions = AngleExtensions(functionProvider)
        }
    }

     */

    override val shaderLanguage: ShaderLanguage
        get() {
            return GLSL(version.glslVersion)
        }

    @Suppress("SpellCheckingInspection")
    override fun shaderConfiguration(type: ShaderType): String = """
        #version ${version.glslVersion}
        #define OR_IN_OUT
        ${
        if (type == ShaderType.FRAGMENT) {
            """#extension GL_KHR_blend_equation_advanced : enable
            |#ifdef GL_KHR_blend_equation_advanced
            |layout(blend_support_all_equations) out;
            |#endif
            |
        """.trimMargin()
        } else ""
    }
        
        ${
        when (version.type) {
            DriverTypeGL.GL -> "#define OR_GL"
            DriverTypeGL.GLES -> """#define OR_GLES
          |precision highp float;
          |precision highp sampler2DArray;
          |${
                if (Driver.glVersion >= DriverVersionGL.GLES_VERSION_3_1) {
                    "precision highp image2D; precision highp image3D; precision highp imageCube; precision highp image2DArray;"
                } else {
                    ""
                }
            }
          |${
                if (Driver.glVersion >= DriverVersionGL.GLES_VERSION_3_2) {
                    "precision highp imageCubeArray;"
                } else {
                    ""
                }
            }
      """.trimMargin()
        }
    }       
    """.trimIndent()

    override fun createComputeStyleManager(session: Session?): ComputeStyleManager {
        return ComputeStyleManagerGLCommon()
    }

    companion object {
        fun candidateVersions(type: DriverTypeGL = DriverGL3Configuration.driverType): List<DriverVersionGL> {
            @Suppress("SpellCheckingInspection") val property = System.getProperty("org.openrndr.gl3.version", "all")
            return DriverVersionGL.entries.find { it.type == DriverTypeGL.GL && "${it.majorVersion}.${it.minorVersion}" == property }
                ?.let { listOf(it) }
                ?: DriverVersionGL.entries.filter { it.type == type }.reversed()
        }
    }

    abstract override val contextID: Long
//        get() {
//            return GLFW.glfwGetCurrentContext()
//        }

    abstract override fun createResourceThread(session: Session?, f: () -> Unit): ResourceThread
//        return ResourceThreadGL3.create(f)
//    }

    abstract override fun createDrawThread(session: Session?): DrawThread
    //{
//        TODO()
//        //return DrawThreadGL3.create()
//    }

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

    override val shaderGenerators: ShaderGenerators = ShaderGeneratorsGLCommon()
    private val vaos = mutableMapOf<ShaderVertexDescription, Int>()

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

    private var fontImageMapManagerInstance: FontImageMapManager? = null

    override val fontImageMapManager: FontMapManager
        get() {
            if (fontImageMapManagerInstance == null) {
                fontImageMapManagerInstance = FontImageMapManager()
            }
            return fontImageMapManagerInstance!!
        }

    override val fontVectorMapManager: FontMapManager
        get() {
            TODO("not implemented")
        }

    fun clearImpl(color: ColorRGBa) {
        val targetColor = color
        debugGLErrors()
        if (Driver.glType == DriverTypeGL.GL) {
            glEnable(GL_FRAMEBUFFER_SRGB)
        }
        glClearColor(
            targetColor.r.toFloat(),
            targetColor.g.toFloat(),
            targetColor.b.toFloat(),
            targetColor.alpha.toFloat()
        )
        glClearDepth(1.0)
        val depthWriteMask = glGetInteger(GL_DEPTH_WRITEMASK) != 0
        val scissorTestEnabled = glIsEnabled(GL_SCISSOR_TEST)
        if (scissorTestEnabled) {
            glDisable(GL_SCISSOR_TEST)
        }

        debugGLErrors()
        glDepthMask(true)
        debugGLErrors()

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        debugGLErrors()

        debugGLErrors {
            when (it) {
                GL_INVALID_VALUE -> "any bit other than the three defined bits is set in mask."
                else -> null
            }
        }
        glDepthMask(depthWriteMask)
        if (scissorTestEnabled) {
            glEnable(GL_SCISSOR_TEST)
        }
    }

    override fun clear(color: ColorRGBa) {
        if (RenderTarget.active.colorAttachments.firstOrNull()?.type?.isSRGB == true || RenderTarget.active is ProgramRenderTargetGL3) {
            if (quirks.clearIgnoresSRGB) {
                clearImpl(color.toSRGB())
            } else {
                clearImpl(color.toLinear())
            }
        } else {
            clearImpl(color.toLinear())
        }
    }

    override fun createStaticVertexBuffer(format: VertexFormat, buffer: Buffer, session: Session?): VertexBuffer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
            return ShaderGL3.create(
                vertexShader,
                tcShader,
                teShader,
                geometryShader,
                fragmentShader,
                name,
                session
            )
        }
    }

    override fun createComputeShader(code: String, name: String, session: Session?): ComputeShader {
        version.require(DriverVersionGL.GL_VERSION_4_3)
        return ComputeShaderGL43.createFromCode(code, name)
    }

    override fun createAtomicCounterBuffer(counterCount: Int, session: Session?): AtomicCounterBuffer {
        version.require(DriverVersionGL.GL_VERSION_4_3)
        val atomicCounterBuffer = AtomicCounterBufferGL42.create(counterCount)
        session?.track(atomicCounterBuffer)
        return atomicCounterBuffer
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
        logger.trace { "creating array texture" }
        val arrayTexture = ArrayTextureGL3.create(width, height, layers, format, type, levels, session)
        session?.track(arrayTexture)
        return arrayTexture
    }

    override fun createBufferTexture(
        elementCount: Int,
        format: ColorFormat,
        type: ColorType,
        session: Session?
    ): BufferTexture {
        logger.trace { "creating buffer texture" }
        val bufferTexture = BufferTextureGL3.create(elementCount, format, type, session)
        session?.track(bufferTexture)
        return bufferTexture
    }

    override fun createCubemap(
        width: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session?
    ): Cubemap {
        logger.trace { "creating cube map $width" }
        val cubemap = CubemapGL3.create(width, format, type, levels, session)
        session?.track(cubemap)
        return cubemap
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
        val volumeTexture = VolumeTextureGL3.create(width, height, depth, format, type, levels, session)
        session?.track(volumeTexture)
        return volumeTexture
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
            val renderTarget = RenderTargetGL3.create(width, height, contentScale, multisample, session)
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
        logger.trace { "creating array texture" }
        version.require(DriverVersionGL.GL_VERSION_4_1)
        val arrayTexture = ArrayCubemapGL4.create(width, layers, format, type, levels, session)
        session?.track(arrayTexture)
        return arrayTexture
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
        synchronized(this) {
            val colorBuffer =
                ColorBufferGL3.create(width, height, contentScale, format, type, multisample, levels, session)
            session?.track(colorBuffer)
            return colorBuffer
        }
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
            val depthBuffer = DepthBufferGL3.create(width, height, format, multisample, session)
            return depthBuffer
        }
    }

    override fun createDynamicIndexBuffer(elementCount: Int, type: IndexType, session: Session?): IndexBuffer {
        synchronized(this) {
            val indexBuffer = IndexBufferGL3.create(elementCount, type, session)
            session?.track(indexBuffer)
            return indexBuffer
        }
    }

    override fun createShaderStorageBuffer(format: ShaderStorageFormat, session: Session?): ShaderStorageBuffer {
        return ShaderStorageBufferGL43.create(format, session)
    }

    override fun createDynamicVertexBuffer(format: VertexFormat, vertexCount: Int, session: Session?): VertexBuffer {
        synchronized(this) {
            val vertexBuffer = VertexBufferGL3.createDynamic(format, vertexCount, session)
            session?.track(vertexBuffer)
            return vertexBuffer
        }
    }

    private fun getVao(
        shaderVertexDescription: ShaderVertexDescription,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributeBuffers: List<VertexBuffer>,
        shader: ShaderGL3
    ): Int {
        return vaos.getOrPut(shaderVertexDescription) {
            logger.debug {
                "[context=$contextID] creating new VAO for hash $shaderVertexDescription"
            }

            val arrays = IntArray(1)
            synchronized(Driver.instance) {
                glGenVertexArrays(arrays)
                glBindVertexArray(arrays[0])
                setupFormat(vertexBuffers, instanceAttributeBuffers, shader)
                glBindVertexArray(defaultVAO)
            }
            arrays[0]
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
        applyTextureBindings(shader.textureBindings)
        debugGLErrors {
            "a pre-existing GL error occurred before Driver.drawVertexBuffer "
        }

        if (drawPrimitive == DrawPrimitive.PATCHES) {
            if (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_1) {
                glPatchParameteri(GL_PATCH_VERTICES, verticesPerPatch)
            }
        }

        shader as ShaderGL3
        // -- find or create a VAO for our shader + vertex buffers combination
        val shaderVertexDescription = ShaderVertexDescription(
            Driver.instance.contextID,
            shader.programObject,
            IntArray(vertexBuffers.size) { (vertexBuffers[it] as VertexBufferGL3).buffer },
            IntArray(0)
        )

        val vao = getVao(shaderVertexDescription, vertexBuffers, emptyList(), shader)

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

    data class ShaderVertexDescription(
        val context: Long,
        val shader: Int,
        val vertexBuffers: IntArray,
        val instanceAttributeBuffers: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ShaderVertexDescription

            if (context != other.context) return false
            if (shader != other.shader) return false
            if (!vertexBuffers.contentEquals(other.vertexBuffers)) return false
            if (!instanceAttributeBuffers.contentEquals(other.instanceAttributeBuffers)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = context.hashCode()
            result = 31 * result + shader
            result = 31 * result + vertexBuffers.contentHashCode()
            result = 31 * result + instanceAttributeBuffers.contentHashCode()
            return result
        }
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

        shader as ShaderGL3
        indexBuffer as IndexBufferGL3
        applyTextureBindings(shader.textureBindings)
        if (drawPrimitive == DrawPrimitive.PATCHES) {
            if (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_1) {
                glPatchParameteri(GL_PATCH_VERTICES, verticesPerPatch)
            }
        }


        // -- find or create a VAO for our shader + vertex buffers combination
        val shaderVertexDescription =
            ShaderVertexDescription(
                contextID,
                shader.programObject,
                IntArray(vertexBuffers.size) { (vertexBuffers[it] as VertexBufferGL3).buffer },
                IntArray(0)
            )

        val vao =
            vaos.getOrPut(shaderVertexDescription) {
                logger.debug {
                    "creating new VAO for hash $shaderVertexDescription"
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


        //logger.trace { "drawing vertex buffer with $drawPrimitive(${drawPrimitive.glType()}) and $indexCount indices with indexOffset $indexOffset " }
        indexBuffer.bind()


        glDrawElements(drawPrimitive.glType(), indexCount, indexBuffer.type.glType(), indexOffset.toLong())


        debugGLErrors {
            when (it) {
                GL_INVALID_ENUM -> "mode ($drawPrimitive) is not an accepted value."
                GL_INVALID_VALUE -> "count ($indexCount) is negative."
                GL_INVALID_OPERATION -> "a non-zero buffer object name is bound to an enabled array and the buffer object's data store is currently mapped."
                else -> null
            }
        }

        // -- restore defaultVAO binding
        glBindVertexArray(defaultVAO)
    }


    override fun drawMultiVertexBuffer(
        shader: Shader,
        vertexBuffers: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        counts: IntArray,
        offsets: IntArray
    ) {
        applyTextureBindings(shader.textureBindings)
        debugGLErrors {
            "a pre-existing GL error occurred before Driver.drawVertexBuffer "
        }


        shader as ShaderGL3
        // -- find or create a VAO for our shader + vertex buffers combination
        val shaderVertexDescription = ShaderVertexDescription(
            Driver.instance.contextID,
            shader.programObject,
            IntArray(vertexBuffers.size) { (vertexBuffers[it] as VertexBufferGL3).buffer },
            IntArray(0)
        )

        val vao = getVao(shaderVertexDescription, vertexBuffers, emptyList(), shader)

        glBindVertexArray(vao)
        debugGLErrors {
            when (it) {
                GL_INVALID_OPERATION -> "array ($vao) is not zero or the name of a vertex array object previously returned from a call to glGenVertexArrays"
                else -> "unknown error $it"
            }
        }

        //logger.trace { "drawing vertex buffer with $drawPrimitive(${drawPrimitive.glType()}) and $vertexCount vertices with vertexOffset $vertexOffset " }
        glMultiDrawArrays(drawPrimitive.glType(), offsets, counts)

        //glDrawArrays(drawPrimitive.glType(), vertexOffset, vertexCount)

//        debugGLErrors {
//            when (it) {
//                GL_INVALID_ENUM -> "mode ($drawPrimitive) is not an accepted value."
//                GL_INVALID_VALUE -> "count ($vertexCount) is negative."
//                GL_INVALID_OPERATION -> "a non-zero buffer object name is bound to an enabled array and the buffer object's data store is currently mapped."
//                else -> null
//            }
//        }
        // -- restore defaultVAO binding
        glBindVertexArray(defaultVAO)
    }

    @Suppress("DuplicatedCode")
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
        require(instanceOffset == 0 || Driver.glVersion >= DriverVersionGL.GL_VERSION_4_2) {
            "non-zero instance offsets require OpenGL 4.2 (current config: ${Driver.glVersion.versionString})"
        }
        applyTextureBindings(shader.textureBindings)

        if (drawPrimitive == DrawPrimitive.PATCHES) {
            if (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_1) {
                glPatchParameteri(GL_PATCH_VERTICES, verticesPerPatch)
            }
        }

        // -- find or create a VAO for our shader + vertex buffers + instance buffers combination
        val hash = ShaderVertexDescription(
            contextID,
            (shader as ShaderGL3).programObject,
            IntArray(vertexBuffers.size) { (vertexBuffers[it] as VertexBufferGL3).buffer },
            IntArray(instanceAttributes.size) { (instanceAttributes[it] as VertexBufferGL3).buffer }
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

    @Suppress("DuplicatedCode")
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
        applyTextureBindings(shader.textureBindings)
        require(instanceOffset == 0 || (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_2 && Driver.glVersion.type == DriverTypeGL.GL)) {
            "non-zero instance offsets require OpenGL 4.2 (current config: ${Driver.glVersion.versionString})"
        }

        if (drawPrimitive == DrawPrimitive.PATCHES) {
            if (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_1) {
                glPatchParameteri(GL_PATCH_VERTICES, verticesPerPatch)
            }
        }


        // -- find or create a VAO for our shader + vertex buffers + instance buffers combination
        val shaderVertexDescription = ShaderVertexDescription(
            contextID,
            (shader as ShaderGL3).programObject,
            IntArray(vertexBuffers.size) { (vertexBuffers[it] as VertexBufferGL3).buffer },
            IntArray(instanceAttributes.size) { (instanceAttributes[it] as VertexBufferGL3).buffer }
        )

        val vao = vaos.getOrPut(shaderVertexDescription) {
            logger.debug {
                "creating new instances VAO for hash $shaderVertexDescription"
            }
            val arrays = IntArray(1)
            synchronized(Driver.instance) {
                glGenVertexArrays(arrays)
                glBindVertexArray(arrays[0])
                setupFormat(vertexBuffers, instanceAttributes, shader)
                glBindVertexArray(defaultVAO)
            }
            arrays[0]
        }

        glBindVertexArray(vao)
        indexBuffer as IndexBufferGL3
        indexBuffer.bind()

        logger.trace { "drawing $instanceCount instances with $drawPrimitive(${drawPrimitive.glType()}) and $indexCount vertices with vertexOffset $indexOffset " }

        if (instanceOffset == 0) {
            glDrawElementsInstanced(
                drawPrimitive.glType(),
                indexCount,
                indexBuffer.type.glType(),
                indexOffset.toLong(),
                instanceCount
            )
        } else {
            glDrawElementsInstancedBaseInstance(
                drawPrimitive.glType(),
                indexCount,
                indexBuffer.type.glType(),
                indexOffset.toLong(),
                instanceCount,
                instanceOffset
            )
        }

        debugGLErrors {
            when (it) {
                GL_INVALID_ENUM -> "mode is not one of the accepted values."
                GL_INVALID_VALUE -> "count ($instanceCount) or primcount ($indexCount) are negative."
                GL_INVALID_OPERATION -> "a non-zero buffer object name is bound to an enabled array and the buffer object's data store is currently mapped."
                else -> null
            }
        }
        // -- restore default VAO binding
        glBindVertexArray(defaultVAO)
    }

    private fun setupFormat(
        vertexBuffer: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        shader: ShaderGL3
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

            fun setupBuffer(buffer: VertexBufferGL3, divisor: Int = 0) {
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

                                        glVertexAttribDivisor(attributeIndex + column + i * 4, divisor)
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

                                        glVertexAttribDivisor(attributeIndex + column + i * 3, divisor)
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
                require(!(it as VertexBufferGL3).isDestroyed)
                setupBuffer(it, 0)
            }

            instanceAttributes.forEach {
                setupBuffer(it as VertexBufferGL3, 1)
            }
        }
    }

    private val dirtyPerContext = mutableMapOf<Long, Boolean>()
    private val cachedPerContext = mutableMapOf<Long, DrawStyle>()

    override fun setState(drawStyle: DrawStyle) {
        if (Driver.glType == DriverTypeGL.GL) {
            glEnable(GL_FRAMEBUFFER_SRGB)
        }
        applyBlendMode(drawStyle)

        val dirty = dirtyPerContext.getOrDefault(contextID, true)
        val cached = cachedPerContext.getOrPut(contextID) { DrawStyle() }

        if (dirty || cached.clip != drawStyle.clip) {
            if (drawStyle.clip != null) {
                drawStyle.clip?.let { it: Rectangle ->
                    val target = RenderTarget.active
                    glEnable(GL_SCISSOR_TEST)
                    glScissor(
                        (it.x * target.contentScale).toInt(),
                        (target.height * target.contentScale - it.y * target.contentScale - it.height * target.contentScale).toInt(),
                        (it.width * target.contentScale).toInt(),
                        (it.height * target.contentScale).toInt()
                    )

                }
            } else {
                glDisable(GL_SCISSOR_TEST)
            }
            cached.clip = drawStyle.clip
        }

        if (dirty || cached.channelWriteMask != drawStyle.channelWriteMask) {
            glColorMask(
                drawStyle.channelWriteMask.red,
                drawStyle.channelWriteMask.green,
                drawStyle.channelWriteMask.blue,
                drawStyle.channelWriteMask.alpha
            )
            cached.channelWriteMask = drawStyle.channelWriteMask
        }

        if (dirty || cached.depthWrite != drawStyle.depthWrite) {
            when (drawStyle.depthWrite) {
                true -> glDepthMask(true)
                false -> glDepthMask(false)
            }
            glEnable(GL_DEPTH_TEST)
            debugGLErrors()
            cached.depthWrite = drawStyle.depthWrite
        }

        if (dirty || cached.stencil != drawStyle.stencil || cached.backStencil != drawStyle.backStencil || cached.frontStencil != drawStyle.frontStencil) {
            if (drawStyle.frontStencil === drawStyle.backStencil) {
                if (drawStyle.stencil.stencilTest == StencilTest.DISABLED) {
                    glDisable(GL_STENCIL_TEST)
                } else {
                    glEnable(GL_STENCIL_TEST)
                    glStencilFuncSeparate(
                        GL_FRONT_AND_BACK,
                        glStencilTest(drawStyle.stencil.stencilTest),
                        drawStyle.stencil.stencilTestReference,
                        drawStyle.stencil.stencilTestMask
                    )
                    debugGLErrors()
                    glStencilOpSeparate(
                        GL_FRONT_AND_BACK,
                        glStencilOp(drawStyle.stencil.stencilFailOperation),
                        glStencilOp(drawStyle.stencil.depthFailOperation),
                        glStencilOp(drawStyle.stencil.depthPassOperation)
                    )
                    debugGLErrors()
                    glStencilMaskSeparate(GL_FRONT_AND_BACK, drawStyle.stencil.stencilWriteMask)
                    debugGLErrors()
                }
            } else {
                require(drawStyle.frontStencil.stencilTest != StencilTest.DISABLED)
                require(drawStyle.backStencil.stencilTest != StencilTest.DISABLED)
                glEnable(GL_STENCIL_TEST)
                glStencilFuncSeparate(
                    GL_FRONT,
                    glStencilTest(drawStyle.frontStencil.stencilTest),
                    drawStyle.frontStencil.stencilTestReference,
                    drawStyle.frontStencil.stencilTestMask
                )
                glStencilFuncSeparate(
                    GL_BACK,
                    glStencilTest(drawStyle.backStencil.stencilTest),
                    drawStyle.backStencil.stencilTestReference,
                    drawStyle.backStencil.stencilTestMask
                )
                glStencilOpSeparate(
                    GL_FRONT,
                    glStencilOp(drawStyle.frontStencil.stencilFailOperation),
                    glStencilOp(drawStyle.frontStencil.depthFailOperation),
                    glStencilOp(drawStyle.frontStencil.depthPassOperation)
                )
                glStencilOpSeparate(
                    GL_BACK,
                    glStencilOp(drawStyle.backStencil.stencilFailOperation),
                    glStencilOp(drawStyle.backStencil.depthFailOperation),
                    glStencilOp(drawStyle.backStencil.depthPassOperation)
                )
                glStencilMaskSeparate(GL_FRONT, drawStyle.frontStencil.stencilWriteMask)
                glStencilMaskSeparate(GL_BACK, drawStyle.backStencil.stencilWriteMask)
            }
            cached.stencil = drawStyle.stencil.copy()
            cached.frontStencil = drawStyle.frontStencil.copy()
            cached.backStencil = drawStyle.backStencil.copy()
        }


        if (dirty || cached.alphaToCoverage != drawStyle.alphaToCoverage) {
            if (drawStyle.alphaToCoverage) {
                glEnable(GL_SAMPLE_ALPHA_TO_COVERAGE)
                glDisable(GL_BLEND)
            } else {
                glDisable(GL_SAMPLE_ALPHA_TO_COVERAGE)
            }
            cached.alphaToCoverage = drawStyle.alphaToCoverage
        }

        if (dirty || cached.depthTestPass != drawStyle.depthTestPass) {
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
            cached.depthTestPass = drawStyle.depthTestPass
        }

        if (dirty || cached.cullTestPass != drawStyle.cullTestPass) {
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
            cached.cullTestPass = drawStyle.cullTestPass
        }

        if (Driver.glType == DriverTypeGL.GL) {
            glEnable(GL_VERTEX_PROGRAM_POINT_SIZE)
        }

        dirtyPerContext[contextID] = false
        debugGLErrors()

    }

    override fun destroyContext(context: Long) {
        logger.debug { "destroying context: $context" }
        styleBlocks.remove(context)
        contextBlocks.remove(context)
        defaultVAOs[context]?.let {
            glDeleteVertexArrays(it)
            debugGLErrors()
            defaultVAOs.remove(context)
        }

        destroyAllVAOs()

        dirtyPerContext.remove(context)
    }

    override val activeRenderTarget: RenderTarget
        get() = RenderTargetGL3.activeRenderTarget


    override fun finish() {
        glFlush()
        glFinish()
    }

    @Suppress("DuplicatedCode")
    fun destroyVAOsForVertexBuffer(vertexBuffer: VertexBufferGL3) {
        val candidates = vaos.keys.filter {
            it.vertexBuffers.contains(vertexBuffer.buffer) || it.instanceAttributeBuffers.contains(vertexBuffer.buffer)
        }
        for (candidate in candidates) {
            val value = vaos[candidate] ?: error("no vao found")
            logger.debug { "removing VAO $value for $candidate" }
            glDeleteVertexArrays(value)
            debugGLErrors()
            vaos.remove(candidate)
        }
    }

    fun destroyVAOsForShader(shader: ShaderGL3) {
        val candidates = vaos.keys.filter {
            it.vertexBuffers.contains(shader.programObject) || it.instanceAttributeBuffers.contains(shader.programObject)
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
}

private fun IndexType.glType(): Int {
    return when (this) {
        IndexType.INT16 -> GL_UNSIGNED_SHORT
        IndexType.INT32 -> GL_UNSIGNED_INT
    }
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
    }
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

@Suppress("DuplicatedCode")
internal fun Matrix44.put(fb: FloatBuffer) {
    fb.put(c0r0.toFloat())
    fb.put(c0r1.toFloat())
    fb.put(c0r2.toFloat())
    fb.put(c0r3.toFloat())
    fb.put(c1r0.toFloat())
    fb.put(c1r1.toFloat())
    fb.put(c1r2.toFloat())
    fb.put(c1r3.toFloat())
    fb.put(c2r0.toFloat())
    fb.put(c2r1.toFloat())
    fb.put(c2r2.toFloat())
    fb.put(c2r3.toFloat())
    fb.put(c3r0.toFloat())
    fb.put(c3r1.toFloat())
    fb.put(c3r2.toFloat())
    fb.put(c3r3.toFloat())
}

internal fun Matrix44.toFloatArray(): FloatArray = floatArrayOf(
    c0r0.toFloat(), c0r1.toFloat(), c0r2.toFloat(), c0r3.toFloat(),
    c1r0.toFloat(), c1r1.toFloat(), c1r2.toFloat(), c1r3.toFloat(),
    c2r0.toFloat(), c2r1.toFloat(), c2r2.toFloat(), c2r3.toFloat(),
    c3r0.toFloat(), c3r1.toFloat(), c3r2.toFloat(), c3r3.toFloat()
)

internal fun Matrix33.put(fb: FloatBuffer) {
    fb.put(c0r0.toFloat())
    fb.put(c0r1.toFloat())
    fb.put(c0r2.toFloat())

    fb.put(c1r0.toFloat())
    fb.put(c1r1.toFloat())
    fb.put(c1r2.toFloat())

    fb.put(c2r0.toFloat())
    fb.put(c2r1.toFloat())
    fb.put(c2r2.toFloat())
}


internal fun Matrix33.toFloatArray(): FloatArray = floatArrayOf(
    c0r0.toFloat(), c0r1.toFloat(), c0r2.toFloat(),
    c1r0.toFloat(), c1r1.toFloat(), c1r2.toFloat(),
    c2r0.toFloat(), c2r1.toFloat(), c2r2.toFloat()
)

val Driver.Companion.glVersion
    get() = (instance as DriverGL3).version

val Driver.Companion.glType
    get() = (instance as DriverGL3).version.type

/**
 * Quick access to capabilities
 */
val Driver.Companion.capabilities
    get() = (instance as DriverGL3).capabilities