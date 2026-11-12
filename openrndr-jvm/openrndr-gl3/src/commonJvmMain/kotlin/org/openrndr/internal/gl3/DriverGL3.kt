package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.*
import org.openrndr.internal.gl3.org.openrndr.internal.gl3.CommandBufferGL3
import org.openrndr.internal.gl3.org.openrndr.internal.gl3.CommandGL3
import org.openrndr.internal.gl3.org.openrndr.internal.gl3.IndexedCommandGL3
import org.openrndr.internal.glcommon.ComputeStyleManagerGLCommon
import org.openrndr.internal.glcommon.FontImageMapManager
import org.openrndr.internal.glcommon.ShadeStyleManagerGLCommon
import org.openrndr.internal.glcommon.ShaderGeneratorsGLCommon
import org.openrndr.math.Matrix33
import org.openrndr.math.Matrix44
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import org.openrndr.shape.Rectangle
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.jvm.JvmRecord

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
/**
 * Opaque types that GLSL ES gives no default precision (or only a lowp one, sampler2D and samplerCube),
 * grouped by the GLSL ES version that introduced them. Declaring a precision for a type the version
 * does not know is a compile error, so each group is only emitted for versions that have it.
 */
private val glesOpaqueTypes30 = listOf(
    "sampler2D", "isampler2D", "usampler2D",
    "sampler3D", "isampler3D", "usampler3D",
    "samplerCube", "isamplerCube", "usamplerCube",
    "sampler2DArray", "isampler2DArray", "usampler2DArray",
    "sampler2DShadow", "samplerCubeShadow", "sampler2DArrayShadow"
)

private val glesOpaqueTypes31 = listOf(
    "sampler2DMS", "isampler2DMS", "usampler2DMS",
    "image2D", "iimage2D", "uimage2D",
    "image3D", "iimage3D", "uimage3D",
    "imageCube", "iimageCube", "uimageCube",
    "image2DArray", "iimage2DArray", "uimage2DArray"
)

private val glesOpaqueTypes32 = listOf(
    "samplerBuffer", "isamplerBuffer", "usamplerBuffer",
    "samplerCubeArray", "isamplerCubeArray", "usamplerCubeArray", "samplerCubeArrayShadow",
    "sampler2DMSArray", "isampler2DMSArray", "usampler2DMSArray",
    "imageBuffer", "iimageBuffer", "uimageBuffer",
    "imageCubeArray", "iimageCubeArray", "uimageCubeArray"
)

private fun glesPrecision(types: List<String>) = types.joinToString(" ") { "precision highp $it;" }

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

/**
 * The GL state [DriverGL3] last set in a context, to skip calls that would not change it
 */
private class ContextState {
    /** resource ids of the textures bound to the texture units */
    val cachedTextureBindings = LongArray(32) { -1 }

    /** is nothing known about the state of the context yet? */
    var dirty = true

    /** the draw style last applied */
    val cached = DrawStyle()

    /** the blend modes of the draw buffers, null when unknown */
    val blendModes = arrayOfNulls<BlendMode>(MAX_DRAW_BUFFERS)

    /** is blending turned off for alpha to coverage? */
    var blendOffForAlphaToCoverage = false

    /** the scissor box, valid while [scissorEnabled] */
    var scissorEnabled = false
    val scissor = IntArray(4)

    /** are the context wide capabilities enabled, sRGB framebuffers and point sizes from shaders */
    var capabilitiesEnabled = false
}

private const val MAX_DRAW_BUFFERS = 16

/** Byte sizes of the commands in a draw indirect buffer */
private const val DRAW_ARRAYS_INDIRECT_COMMAND_SIZE = 4 * 4
private const val DRAW_ELEMENTS_INDIRECT_COMMAND_SIZE = 5 * 4

/** The equations and factors of a blend mode, null for [BlendMode.REPLACE], which does not blend */
private fun blendFactors(blendMode: BlendMode): IntArray? = when (blendMode) {
    BlendMode.OVER -> intArrayOf(GL_FUNC_ADD, GL_FUNC_ADD, GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
    BlendMode.BLEND -> intArrayOf(GL_FUNC_ADD, GL_FUNC_ADD, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    BlendMode.ADD -> intArrayOf(GL_FUNC_ADD, GL_FUNC_ADD, GL_ONE, GL_ONE, GL_ONE, GL_ONE)
    BlendMode.REPLACE -> null
    BlendMode.SUBTRACT -> intArrayOf(GL_FUNC_REVERSE_SUBTRACT, GL_FUNC_ADD, GL_SRC_ALPHA, GL_ONE, GL_ONE, GL_ONE)
    BlendMode.MULTIPLY -> intArrayOf(GL_FUNC_ADD, GL_FUNC_ADD, GL_DST_COLOR, GL_ONE_MINUS_SRC_ALPHA, GL_DST_COLOR, GL_ONE_MINUS_SRC_ALPHA)
    BlendMode.REMOVE -> intArrayOf(GL_FUNC_ADD, GL_FUNC_ADD, GL_ZERO, GL_ONE_MINUS_SRC_ALPHA, GL_ZERO, GL_ONE_MINUS_SRC_ALPHA)
    BlendMode.MIN -> intArrayOf(GL_MIN, GL_MIN, GL_ONE, GL_ONE, GL_ONE, GL_ONE)
    BlendMode.MAX -> intArrayOf(GL_MAX, GL_MAX, GL_ONE, GL_ONE, GL_ONE, GL_ONE)
}
abstract class DriverGL3(val version: DriverVersionGL) : Driver, IOSurfaceImporterFactory,
    D3D11TextureImporterFactory, DmaBufImporterFactory {

    override fun enableErrorChecking() {

    }

    override fun createIOSurfaceImporter(): IOSurfaceImporter? = createIOSurfaceImporterGL3()

    override val supportsD3D11TextureImport: Boolean
        get() = supportsD3D11TextureImportGL3()

    override fun createD3D11TextureImporter(device: Long): D3D11TextureImporter? =
        createD3D11TextureImporterGL3(device)

    override fun createDmaBufImporter(): DmaBufImporter? = createDmaBufImporterGL3()

    override fun createCommandBuffer(size: UInt, session: Session?): CommandBuffer<Command> {
        val commandBuffer =  CommandBufferGL3<Command>(size, session)
        session?.track(commandBuffer)
        return commandBuffer
    }

    override fun createIndexedCommandBuffer(size: UInt, session: Session?): CommandBuffer<IndexedCommand> {
        return CommandBufferGL3(size, session)
    }


    override fun createCommand(
        vertexCount: UInt,
        instanceCount: UInt,
        baseVertex: Int,
        baseInstance: UInt
    ): Command = CommandGL3(vertexCount, instanceCount, baseVertex, baseInstance)

    override fun createIndexedCommand(
        vertexCount: UInt,
        instanceCount: UInt,
        firstIndex: UInt,
        baseVertex: Int,
        baseInstance: UInt
    ): IndexedCommand = IndexedCommandGL3(vertexCount, instanceCount, firstIndex, baseVertex, baseInstance)

    override fun drawCommandBuffer(
        shader: Shader,
        commandBuffer: CommandBuffer<Command>,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        commandCount: Int,
        commandBufferIndex: Int
    ) {
        shader as ShaderGL3
        commandBuffer as CommandBufferGL3
        commandBuffer.ssbo as ShaderStorageBufferGL43

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, commandBuffer.ssbo.buffer)

        // -- find or create a VAO for our shader + vertex buffers combination
        val context = contextID
        val vao = getVao(context, vertexBuffers, instanceAttributes, shader)

        glBindVertexArray(vao)
        debugGLErrors {
            when (it) {
                GL_INVALID_OPERATION -> "array ($vao) is not zero or the name of a vertex array object previously returned from a call to glGenVertexArrays"
                else -> "unknown error $it"
            }
        }

        glMultiDrawArraysIndirect(
            drawPrimitive.glType(),
            commandBufferIndex.toLong() * DRAW_ARRAYS_INDIRECT_COMMAND_SIZE,
            commandCount,
            DRAW_ARRAYS_INDIRECT_COMMAND_SIZE
        )

        debugGLErrors {
            when (it) {
                else -> null
            }
        }
        // -- restore defaultVAO binding
        glBindVertexArray(defaultVAO(context))
    }


    private val contextStates = HashMap<Long, ContextState>()

    /** The state of the [context] */
    private fun contextState(context: Long = contextID): ContextState =
        synchronized(contextStates) { contextStates.getOrPut(context) { ContextState() } }

    override fun drawIndexedCommandBuffer(
        shader: Shader,
        indexBuffer: IndexBuffer,
        commandBuffer: CommandBuffer<IndexedCommand>,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        primitiveType: DrawPrimitive,
        commandCount: Int,
        commandBufferIndex: Int
    ) {
        shader as ShaderGL3
        commandBuffer as CommandBufferGL3
        commandBuffer.ssbo as ShaderStorageBufferGL43

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, commandBuffer.ssbo.buffer)

        // -- find or create a VAO for our shader + vertex buffers combination
        val context = contextID
        val vao = getVao(context, vertexBuffers, instanceAttributes, shader)

        glBindVertexArray(vao)
        debugGLErrors {
            when (it) {
                GL_INVALID_OPERATION -> "array ($vao) is not zero or the name of a vertex array object previously returned from a call to glGenVertexArrays"
                else -> "unknown error $it"
            }
        }

        (indexBuffer as IndexBufferGL3).bind()
        glMultiDrawElementsIndirect(
            primitiveType.glType(),
            indexBuffer.type.glType(),
            commandBufferIndex.toLong() * DRAW_ELEMENTS_INDIRECT_COMMAND_SIZE,
            commandCount,
            DRAW_ELEMENTS_INDIRECT_COMMAND_SIZE
        )

        debugGLErrors {
            when (it) {
                else -> null
            }
        }
        // -- restore defaultVAO binding
        glBindVertexArray(defaultVAO(context))
    }

    /**
     * Applies the blend mode of [drawStyle], or the blend modes of the attachments of the active render target, for
     * each draw buffer whose blend mode changed. Each draw buffer is blended by its own mode when the driver supports
     * indexed blend state, otherwise all are blended by the mode of the last one.
     */
    fun applyBlendMode(drawStyle: DrawStyle) = applyBlendMode(drawStyle, contextState())

    private fun applyBlendMode(drawStyle: DrawStyle, state: ContextState) {
        val blendModes = RenderTarget.active.blendModes
        if (blendModes.isEmpty()) {
            return
        }
        if (Driver.glVersion.isAtLeast(DriverVersionGL.GL_VERSION_4_1, DriverVersionGL.GLES_VERSION_3_2)) {
            for (i in 0 until min(blendModes.size, MAX_DRAW_BUFFERS)) {
                val blendMode = drawStyle.blendMode ?: blendModes[i]
                if (state.blendModes[i] == blendMode) {
                    continue
                }
                val factors = blendFactors(blendMode)
                if (factors == null) {
                    glDisablei(GL_BLEND, i)
                } else {
                    glEnablei(GL_BLEND, i)
                    glBlendEquationSeparatei(i, factors[0], factors[1])
                    glBlendFuncSeparatei(i, factors[2], factors[3], factors[4], factors[5])
                }
                state.blendModes[i] = blendMode
            }
        } else {
            val blendMode = drawStyle.blendMode ?: blendModes.last()
            if (state.blendModes[0] == blendMode) {
                return
            }
            val factors = blendFactors(blendMode)
            if (factors == null) {
                glDisable(GL_BLEND)
            } else {
                glEnable(GL_BLEND)
                glBlendEquationSeparate(factors[0], factors[1])
                glBlendFuncSeparate(factors[2], factors[3], factors[4], factors[5])
            }
            state.blendModes[0] = blendMode
        }
    }

    fun applyTextureBindings(bindings: TextureBindings) = applyTextureBindings(bindings, contextState())

    private fun applyTextureBindings(bindings: TextureBindings, state: ContextState) {
        debugGLErrors { "Pre-existing error" }
        val cachedTextureBindings = state.cachedTextureBindings
        bindings.binding.forEach { i, texture ->
            val (resourceId, target, name) = when (texture) {
                is ColorBufferGL3 -> Triple(texture.resourceId, texture.target, texture.texture)
                is DepthBufferGL3 -> Triple(texture.resourceId, texture.target, texture.texture)
                is BufferTextureGL3 -> Triple(texture.resourceId, GL_TEXTURE_BUFFER, texture.texture)
                is ArrayTextureGL3 -> Triple(texture.resourceId, texture.target, texture.texture)
                is ArrayCubemapGL4 -> Triple(texture.resourceId, texture.target, texture.texture)
                is VolumeTextureGL3 -> Triple(texture.resourceId, GL_TEXTURE_3D, texture.texture)
                is CubemapGL3 -> Triple(texture.resourceId, GL_TEXTURE_CUBE_MAP, texture.texture)
                else -> error("unsupported texture type $texture")
            }
            if (cachedTextureBindings[i] != resourceId) {
                if (texture is ColorBufferGL3) {
                    require(!texture.destroyed)
                }
                glActiveTexture(GL_TEXTURE0 + i)
                debugGLErrors { "Failed to set active texture unit $i" }
                glBindTexture(target, name)
                debugGLErrors { "Failed to bind $texture to unit $i" }
                cachedTextureBindings[i] = resourceId
            }
        }
    }

    private val executionQueue = mutableListOf<() -> Unit>()

    /**
     * Called on the thread that queues a function for the main thread, so that a main loop that waits can wake up and
     * [processMainThreadExecutables]
     */
    @Volatile
    var onMainThreadWork: (() -> Unit)? = null

    fun executeOnMainThread(f: () -> Unit) {
        synchronized(executionQueue) {
            executionQueue.add(f)
        }
        onMainThreadWork?.invoke()
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

    @JvmRecord
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
            val rt = renderTarget(64, 64, 1.0) {
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

        /**
         * Some drivers do not encode the colors that glClearBuffer writes into sRGB attachments
         */
        val clearBufferIgnoresSRGB: Boolean by lazy {
            val rt = renderTarget(1, 1, 1.0) {
                colorBuffer(type = ColorType.UINT8_SRGB)
            }
            rt.bind()
            glClearBufferfv(GL_COLOR, 0, floatArrayOf(0.5f, 0.5f, 0.5f, 1.0f))
            rt.unbind()
            val buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
            rt.colorBuffer(0).read(buffer)
            // 0.5 is encoded as 188, unencoded it is 128
            val quirky = (buffer.get(0).toInt() and 0xff) < 160
            if (quirky) {
                logger.warn { "quirk: clearBuffer ignores sRGB setting" }
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
            maxTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE).let { if (it == 0) 16384 else it },
            hasComputeShaders = when (Driver.glType) {
                DriverTypeGL.GLES -> Driver.glVersion >= DriverVersionGL.GLES_VERSION_3_1
                DriverTypeGL.GL -> Driver.glVersion >= DriverVersionGL.GL_VERSION_4_3
            },
            hasCommandBuffers = when (Driver.glType) {
                DriverTypeGL.GLES -> Driver.glVersion >= DriverVersionGL.GLES_VERSION_3_1
                DriverTypeGL.GL -> Driver.glVersion >= DriverVersionGL.GL_VERSION_4_3
            }
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

    private fun onPremise(premise: Boolean, emit: String): String {
        return if (premise) emit else ""
    }
    @Suppress("SpellCheckingInspection")
    override fun shaderConfiguration(type: ShaderType): String = """
        #version ${version.glslVersion}
        #define OR_IN_OUT
        ${
        if (type == ShaderType.VERTEX) {
            """
            ${onPremise(this.version.type == DriverTypeGL.GLES && Platform.type == PlatformType.MAC, "#extension GL_ANGLE_multi_draw: enable")}
            """
        } else {
            ""
        }
    }
        
        ${
        when (version.type) {
            DriverTypeGL.GL -> "#define OR_GL"
            DriverTypeGL.GLES -> """#define OR_GLES
          |precision highp float;
          |${glesPrecision(glesOpaqueTypes30)}
          |${
                if (Driver.glVersion >= DriverVersionGL.GLES_VERSION_3_1) {
                    glesPrecision(glesOpaqueTypes31)
                } else {
                    ""
                }
            }
          |${
                if (Driver.glVersion >= DriverVersionGL.GLES_VERSION_3_2) {
                    glesPrecision(glesOpaqueTypes32)
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
        private val resourceId: AtomicLong = AtomicLong(0L)
        fun generateResourceId(): Long {
            return resourceId.getAndIncrement()
        }

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
        get() = defaultVAO(contextID)

    /** The VAO bound between draws in [context], which must be current */
    private fun defaultVAO(context: Long): Int = defaultVAOs.getOrPut(context) {
        val vaos = IntArray(1)
        synchronized(Driver.instance) {
            logger.debug { "[context=$context] creating default VAO" }
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
        debugGLErrors()
        withOpenWriteMasks {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
            debugGLErrors {
                when (it) {
                    GL_INVALID_VALUE -> "any bit other than the three defined bits is set in mask."
                    else -> null
                }
            }
        }
    }

    override fun clear(color: ColorRGBa) {
        val active = RenderTarget.active
        if (active is ProgramRenderTargetGL3 && active.colorType.isFloat) {
            // HDR windows hold linear colors, nothing encodes them
            clearImpl(color.toLinear())
        } else if (active.colorAttachments.firstOrNull()?.type?.isSRGB == true || active is ProgramRenderTargetGL3) {
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
        checkGLErrors {
            "pre-existing error in createColorBuffer"
        }
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
        val shaderStorageBuffer = ShaderStorageBufferGL43.create(format, session)
        session?.track(shaderStorageBuffer)
        return shaderStorageBuffer
    }

    override fun createDynamicVertexBuffer(format: VertexFormat, vertexCount: Int, session: Session?): VertexBuffer {
        synchronized(this) {
            val vertexBuffer = VertexBufferGL3.createDynamic(format, vertexCount, session)
            session?.track(vertexBuffer)
            return vertexBuffer
        }
    }

    /**
     * VAOs to delete in a context, which was not current when their shader or vertex buffer was destroyed. VAOs
     * belong to the context they were created in.
     */
    private val pendingVaoDeletions = HashMap<Long, MutableList<Int>>()

    /**
     * Finds or creates the VAO for [shader] and the [vertexBuffers] and [instanceAttributeBuffers] in [context],
     * which must be current
     */
    private fun getVao(
        context: Long,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributeBuffers: List<VertexBuffer>,
        shader: ShaderGL3
    ): Int {
        if (pendingVaoDeletions.isNotEmpty()) {
            pendingVaoDeletions.remove(context)?.forEach { glDeleteVertexArrays(it) }
        }
        val shaderVertexDescription = ShaderVertexDescription(
            context,
            shader.programObject,
            IntArray(vertexBuffers.size) { (vertexBuffers[it] as VertexBufferGL3).buffer },
            IntArray(instanceAttributeBuffers.size) { (instanceAttributeBuffers[it] as VertexBufferGL3).buffer }
        )
        return vaos.getOrPut(shaderVertexDescription) {
            logger.debug {
                "[context=$context] creating new VAO for hash $shaderVertexDescription"
            }

            val arrays = IntArray(1)
            synchronized(Driver.instance) {
                glGenVertexArrays(arrays)
                glBindVertexArray(arrays[0])
                setupFormat(vertexBuffers, instanceAttributeBuffers, shader)
                debugGLErrors()
                glBindVertexArray(defaultVAO(context))
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
        debugGLErrors {
            "a pre-existing GL error occurred before Driver.drawVertexBuffer "
        }

        val context = contextID
        applyTextureBindings(shader.textureBindings, contextState(context))
        debugGLErrors {
            "an error occurred while applying texture bindings to the shader: $it"
        }
        if (drawPrimitive == DrawPrimitive.PATCHES) {
            if (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_1) {
                glPatchParameteri(GL_PATCH_VERTICES, verticesPerPatch)
            }
        }

        shader as ShaderGL3
        // -- find or create a VAO for our shader + vertex buffers combination
        val vao = getVao(context, vertexBuffers, emptyList(), shader)

        glBindVertexArray(vao)
        debugGLErrors {
            when (it) {
                GL_INVALID_OPERATION -> "array ($vao) is not zero or the name of a vertex array object previously returned from a call to glGenVertexArrays"
                else -> "unknown error $it"
            }
        }

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
        glBindVertexArray(defaultVAO(context))
    }

    @JvmRecord
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
        val context = contextID
        applyTextureBindings(shader.textureBindings, contextState(context))
        if (drawPrimitive == DrawPrimitive.PATCHES) {
            if (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_1) {
                glPatchParameteri(GL_PATCH_VERTICES, verticesPerPatch)
            }
        }


        // -- find or create a VAO for our shader + vertex buffers combination
        val vao = getVao(context, vertexBuffers, emptyList(), shader)


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
        glBindVertexArray(defaultVAO(context))
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
        require(instanceOffset == 0 || Driver.glVersion.isAtLeast(DriverVersionGL.GL_VERSION_4_2)) {
            "non-zero instance offsets require OpenGL 4.2 (current config: ${Driver.glVersion.versionString})"
        }
        val context = contextID
        applyTextureBindings(shader.textureBindings, contextState(context))

        if (drawPrimitive == DrawPrimitive.PATCHES) {
            if (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_1) {
                glPatchParameteri(GL_PATCH_VERTICES, verticesPerPatch)
            }
        }

        // -- find or create a VAO for our shader + vertex buffers + instance buffers combination
        val vao = getVao(context, vertexBuffers, instanceAttributes, shader as ShaderGL3)

        debugGLErrors()
        glBindVertexArray(vao)
        debugGLErrors()

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
        glBindVertexArray(defaultVAO(context))
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
        val context = contextID
        applyTextureBindings(shader.textureBindings, contextState(context))
        require(instanceOffset == 0 || (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_2 && Driver.glVersion.type == DriverTypeGL.GL)) {
            "non-zero instance offsets require OpenGL 4.2 (current config: ${Driver.glVersion.versionString})"
        }

        if (drawPrimitive == DrawPrimitive.PATCHES) {
            if (Driver.glVersion >= DriverVersionGL.GL_VERSION_4_1) {
                glPatchParameteri(GL_PATCH_VERTICES, verticesPerPatch)
            }
        }


        // -- find or create a VAO for our shader + vertex buffers + instance buffers combination
        val vao = getVao(context, vertexBuffers, instanceAttributes, shader as ShaderGL3)

        glBindVertexArray(vao)
        indexBuffer as IndexBufferGL3
        indexBuffer.bind()

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
        glBindVertexArray(defaultVAO(context))
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
                                    glVertexAttribDivisor(attributeIndex + i, divisor)
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

    /**
     * Runs [function] without the scissor test, which clears and blits honor, and restores the scissor test afterwards
     */
    internal fun withoutScissor(function: () -> Unit) {
        val scissorTestEnabled = glIsEnabled(GL_SCISSOR_TEST)
        if (scissorTestEnabled) {
            glDisable(GL_SCISSOR_TEST)
        }
        function()
        if (scissorTestEnabled) {
            glEnable(GL_SCISSOR_TEST)
        }
    }

    /**
     * Runs [clear] without the scissor test and with all color, depth and stencil write masks open, clears are
     * scissored and masked like draws. The masks of the cached draw style are restored afterwards, the masks of the
     * first draw style otherwise.
     */
    internal fun withOpenWriteMasks(clear: () -> Unit) = withoutScissor {
        glColorMask(true, true, true, true)
        glDepthMask(true)
        glStencilMaskSeparate(GL_FRONT_AND_BACK, 0xff)
        clear()
        val state = contextState()
        if (!state.dirty) {
            val cached = state.cached
            val mask = cached.channelWriteMask
            glColorMask(mask.red, mask.green, mask.blue, mask.alpha)
            glDepthMask(cached.depthWrite)
            if (cached.frontStencil === cached.backStencil) {
                glStencilMaskSeparate(GL_FRONT_AND_BACK, cached.stencil.stencilWriteMask)
            } else {
                glStencilMaskSeparate(GL_FRONT, cached.frontStencil.stencilWriteMask)
                glStencilMaskSeparate(GL_BACK, cached.backStencil.stencilWriteMask)
            }
        }
    }

    override fun setState(drawStyle: DrawStyle) {
        val state = contextState()
        val dirty = state.dirty
        val cached = state.cached

        if (dirty || !state.capabilitiesEnabled) {
            if (Driver.glType == DriverTypeGL.GL) {
                glEnable(GL_FRAMEBUFFER_SRGB)
                glEnable(GL_VERTEX_PROGRAM_POINT_SIZE)
            }
            state.capabilitiesEnabled = true
        }

        if (dirty || cached.alphaToCoverage != drawStyle.alphaToCoverage) {
            if (drawStyle.alphaToCoverage) {
                glEnable(GL_SAMPLE_ALPHA_TO_COVERAGE)
            } else {
                glDisable(GL_SAMPLE_ALPHA_TO_COVERAGE)
            }
            cached.alphaToCoverage = drawStyle.alphaToCoverage
        }
        // alpha to coverage replaces blending
        if (drawStyle.alphaToCoverage) {
            if (dirty || !state.blendOffForAlphaToCoverage) {
                glDisable(GL_BLEND)
                state.blendModes.fill(null)
                state.blendOffForAlphaToCoverage = true
            }
        } else {
            if (dirty) {
                state.blendModes.fill(null)
            }
            state.blendOffForAlphaToCoverage = false
            applyBlendMode(drawStyle, state)
        }

        // the scissor box depends on the size and content scale of the render target, not only on the clip
        val clip = drawStyle.clip
        if (clip != null) {
            val target = RenderTarget.active
            val scale = target.contentScale
            // the pixels whose centers lie inside the clip
            val x0 = (min(clip.x, clip.x + clip.width) * scale).roundToInt()
            val x1 = (max(clip.x, clip.x + clip.width) * scale).roundToInt()
            val top = (min(clip.y, clip.y + clip.height) * scale).roundToInt()
            val bottom = (max(clip.y, clip.y + clip.height) * scale).roundToInt()
            val y0 = target.effectiveHeight - bottom
            val scissor = state.scissor
            if (dirty || !state.scissorEnabled) {
                glEnable(GL_SCISSOR_TEST)
                state.scissorEnabled = true
            }
            if (dirty || scissor[0] != x0 || scissor[1] != y0 || scissor[2] != x1 - x0 || scissor[3] != bottom - top) {
                glScissor(x0, y0, x1 - x0, bottom - top)
                scissor[0] = x0
                scissor[1] = y0
                scissor[2] = x1 - x0
                scissor[3] = bottom - top
            }
        } else if (dirty || state.scissorEnabled) {
            glDisable(GL_SCISSOR_TEST)
            state.scissorEnabled = false
        }
        cached.clip = clip

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

        state.dirty = false
        debugGLErrors()

    }

    /**
     * Forgets the VAOs and state of [context]. Its VAOs are deleted when it is current, otherwise they go with it.
     */
    override fun destroyContext(context: Long) {
        logger.debug { "destroying context: $context" }
        val current = contextID == context
        styleBlocks.remove(context)
        contextBlocks.remove(context)
        defaultVAOs.remove(context)?.let {
            if (current) {
                glDeleteVertexArrays(it)
                debugGLErrors()
            }
        }
        val keys = vaos.keys.filter { it.context == context }
        for (key in keys) {
            val vao = vaos.remove(key) ?: continue
            if (current) {
                glDeleteVertexArrays(vao)
                debugGLErrors()
            }
        }
        pendingVaoDeletions.remove(context)?.let { pending ->
            if (current) {
                pending.forEach { glDeleteVertexArrays(it) }
            }
        }
        synchronized(contextStates) {
            contextStates.remove(context)
        }
    }

    override val activeRenderTarget: RenderTarget
        get() = RenderTargetGL3.activeRenderTarget


    override fun finish() {
        glFlush()
        glFinish()
    }

    /**
     * Deletes the VAOs whose keys match [predicate], those of other contexts when that context is next used
     */
    private fun destroyVAOs(predicate: (ShaderVertexDescription) -> Boolean) {
        val context = contextID
        val candidates = vaos.keys.filter(predicate)
        for (candidate in candidates) {
            val value = vaos.remove(candidate) ?: continue
            logger.debug { "removing VAO $value for $candidate" }
            if (candidate.context == context) {
                glDeleteVertexArrays(value)
                debugGLErrors()
            } else {
                pendingVaoDeletions.getOrPut(candidate.context) { mutableListOf() }.add(value)
            }
        }
    }

    fun destroyVAOsForVertexBuffer(vertexBuffer: VertexBufferGL3) = destroyVAOs {
        it.vertexBuffers.contains(vertexBuffer.buffer) || it.instanceAttributeBuffers.contains(vertexBuffer.buffer)
    }

    fun destroyVAOsForShader(shader: ShaderGL3) = destroyVAOs { it.shader == shader.programObject }
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