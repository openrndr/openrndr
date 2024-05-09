package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL40C.*
import org.lwjgl.system.FunctionProvider
import org.openrndr.draw.*
import org.openrndr.internal.*
import org.openrndr.internal.gl3.extensions.AngleExtensions
import org.openrndr.internal.glcommon.ComputeStyleManagerGLCommon
import org.openrndr.internal.glcommon.ShadeStyleManagerGLCommon
import org.openrndr.internal.glcommon.ShaderGeneratorsGLCommon
import org.openrndr.math.Matrix33
import org.openrndr.math.Matrix44
import org.openrndr.shape.Rectangle
import java.nio.Buffer
import java.nio.FloatBuffer
import java.util.*
import kotlin.math.min

private val logger = KotlinLogging.logger {}


enum class GlesBackend {
    SYSTEM,
    ANGLE
}

enum class DriverTypeGL {
    GL,
    GLES
}

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
}

@Suppress("NOTHING_TO_INLINE")
inline fun DriverVersionGL.require(minimum: DriverVersionGL) {
    require(ordinal >= minimum.ordinal) {
        """Feature is not supported on current OpenGL configuration (configuration: ${this.versionString}, required: ${minimum.versionString})"""
    }
}

class DriverGL3(val version: DriverVersionGL) : Driver {

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


    var angleExtensions: AngleExtensions? = null

    fun setupExtensions(functionProvider: FunctionProvider) {
        val isAngle = version.type == DriverTypeGL.GLES && glGetString(GL_RENDERER)?.contains("ANGLE") == true
        if (isAngle) {
            angleExtensions = AngleExtensions(functionProvider)
        }
    }

    override val shaderLanguage: ShaderLanguage
        get() {
            return GLSL(version.glslVersion)
        }

    override fun shaderConfiguration(): String = """
        #version ${version.glslVersion}
        #define OR_IN_OUT
        ${
        when (version.type) {
            DriverTypeGL.GL -> "#define OR_GL"
            DriverTypeGL.GLES -> """#define OR_GLES
          |precision highp float;
      """.trimMargin()
        }
    }       
    """.trimIndent()

    override fun createComputeStyleManager(session: Session?): ComputeStyleManager {
        return ComputeStyleManagerGLCommon()
    }

    companion object {
        fun candidateVersions(type: DriverTypeGL = DriverGL3Configuration.driverType): List<DriverVersionGL> {
            val property = System.getProperty("org.openrndr.gl3.version", "all")
            return DriverVersionGL.entries.find { it.type == DriverTypeGL.GL && "${it.majorVersion}.${it.minorVersion}" == property }
                ?.let { listOf(it) }
                ?: DriverVersionGL.entries.filter { it.type == type }.reversed()
        }
    }

    override val contextID: Long
        get() {
            return GLFW.glfwGetCurrentContext()
        }

    override fun createResourceThread(session: Session?, f: () -> Unit): ResourceThread {
        return ResourceThreadGL3.create(f)
    }

    override fun createDrawThread(session: Session?): DrawThread {
        return DrawThreadGL3.create()
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

    override val shaderGenerators: ShaderGenerators = ShaderGeneratorsGLCommon()
    private val vaos = mutableMapOf<ShaderVertexDescription, Int>()

    private fun hash(
        shader: ShaderGL3,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>
    ): Long {
        var hash = contextID
        hash = hash * 31 + shader.programObject.toLong()
        logger.hashCode()

        for (vb in vertexBuffers) {
            hash = hash * 31 + vb.vertexFormat.hashCode()
        }
        hash *= 31

        for (vb in instanceAttributes) {
            hash = hash * 31 + vb.vertexFormat.hashCode()
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

    override fun clear(r: Double, g: Double, b: Double, a: Double) {
        debugGLErrors()
        glClearColor(r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat())
        glClearDepth(1.0)
        val depthWriteMask = glGetInteger(GL_DEPTH_WRITEMASK) != 0
        glDisable(GL_SCISSOR_TEST)
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
        logger.trace { "creating cubemap $width" }
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
            val indexBuffer = IndexBufferGL3.create(elementCount, type)
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

        shader as ShaderGL3
        // -- find or create a VAO for our shader + vertex buffers combination
        val shaderVertexDescription = ShaderVertexDescription(
            Driver.instance.contextID,
            shader.programObject,
            IntArray(vertexBuffers.size) { (vertexBuffers[it] as VertexBufferGL3).buffer },
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
            GL42C.glDrawArraysInstancedBaseInstance(
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
            GL42C.glDrawElementsInstancedBaseInstance(
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

            fun setupBuffer(buffer: VertexBuffer, divisor: Int = 0) {
                val prefix = if (divisor == 0) "a" else "i"
                var attributeBindings = 0

                glBindBuffer(GL_ARRAY_BUFFER, (buffer as VertexBufferGL3).buffer)
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
                                            glType, false, format.size, item.offset.toLong() + i * item.type.sizeInBytes
                                        )
                                    } else {
                                        glVertexAttribIPointer(
                                            attributeIndex + i,
                                            item.type.componentCount,
                                            glType, format.size, item.offset.toLong() + i * item.type.sizeInBytes
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
                                            item.offset.toLong() + column * 16 + i * 64
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
                                            item.offset.toLong() + column * 12 + i * 48
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
                setupBuffer(it, 1)
            }
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

    private fun teardownFormat(
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        shader: ShaderGL3
    ) {
        vertexBuffers.forEach { teardownFormat(it.vertexFormat, shader) }
        instanceAttributes.forEach { teardownFormat(it.vertexFormat, shader) }
    }


    private val dirtyPerContext = mutableMapOf<Long, Boolean>()
    private val cachedPerContext = mutableMapOf<Long, DrawStyle>()

    //private val cached = DrawStyle()
//private var dirty = true
    override fun setState(drawStyle: DrawStyle) {

        val dirty = dirtyPerContext.getOrDefault(contextID, true)
        val cached = cachedPerContext.getOrPut(contextID) { DrawStyle() }

        if (dirty || cached.clip != drawStyle.clip) {
            if (drawStyle.clip != null) {
                drawStyle.clip?.let { it: Rectangle ->
                    val target = RenderTarget.active
                    glScissor(
                        (it.x * target.contentScale).toInt(),
                        (target.height * target.contentScale - it.y * target.contentScale - it.height * target.contentScale).toInt(),
                        (it.width * target.contentScale).toInt(),
                        (it.height * target.contentScale).toInt()
                    )
                    glEnable(GL_SCISSOR_TEST)
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

        if (dirty || cached.blendMode != drawStyle.blendMode) {
            when (drawStyle.blendMode) {
                BlendMode.OVER -> {
                    glEnable(GL_BLEND)
                    if (version >= DriverVersionGL.GL_VERSION_4_1 && version.type == DriverTypeGL.GL) {
                        glBlendEquationi(0, GL_FUNC_ADD)
                        glBlendFunci(0, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
                    } else {
                        glBlendEquation(GL_FUNC_ADD)
                        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
                    }
                }

                BlendMode.BLEND -> {
                    glEnable(GL_BLEND)
                    if (version >= DriverVersionGL.GL_VERSION_4_1 && version.type == DriverTypeGL.GL) {
                        glBlendEquationi(0, GL_FUNC_ADD)
                        glBlendFunci(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                    } else {
                        glBlendEquation(GL_FUNC_ADD)
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                    }
                }

                BlendMode.ADD -> {
                    glEnable(GL_BLEND)
                    if (version >= DriverVersionGL.GL_VERSION_4_1 && version.type == DriverTypeGL.GL) {
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
                    if (version >= DriverVersionGL.GL_VERSION_4_1 && version.type == DriverTypeGL.GL) {
                        glBlendEquationSeparatei(0, GL_FUNC_REVERSE_SUBTRACT, GL_FUNC_ADD)
                        glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE, GL_ONE, GL_ONE)
                    } else {
                        glBlendEquationSeparate(GL_FUNC_REVERSE_SUBTRACT, GL_FUNC_ADD)
                        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE, GL_ONE, GL_ONE)
                    }
                }

                BlendMode.MULTIPLY -> {
                    glEnable(GL_BLEND)
                    if (version >= DriverVersionGL.GL_VERSION_4_1 && version.type == DriverTypeGL.GL) {
                        glBlendEquationi(0, GL_FUNC_ADD)
                        glBlendFunci(0, GL_DST_COLOR, GL_ONE_MINUS_SRC_ALPHA)
                    } else {
                        glBlendEquation(GL_FUNC_ADD)
                        glBlendFunc(GL_DST_COLOR, GL_ONE_MINUS_SRC_ALPHA)
                    }
                }

                BlendMode.REMOVE -> {
                    glEnable(GL_BLEND)
                    if (version >= DriverVersionGL.GL_VERSION_4_1 && version.type == DriverTypeGL.GL) {
                        glBlendEquationi(0, GL_FUNC_ADD)
                        glBlendFunci(0, GL_ZERO, GL_ONE_MINUS_SRC_ALPHA)
                    } else {
                        glBlendEquation(GL_FUNC_ADD)
                        glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_ALPHA)
                    }
                }

                BlendMode.MIN -> {
                    glEnable(GL_BLEND)
                    if (version >= DriverVersionGL.GL_VERSION_4_1 && version.type == DriverTypeGL.GL) {
                        glBlendEquationi(0, GL_MIN)
                        glBlendFunci(0, GL_ONE, GL_ONE)
                    } else {
                        glBlendEquation(GL_MIN)
                        glBlendFunc(GL_ONE, GL_ONE)
                    }
                }

                BlendMode.MAX -> {
                    glEnable(GL_BLEND)
                    if (version >= DriverVersionGL.GL_VERSION_4_1 && version.type == DriverTypeGL.GL) {
                        glBlendEquationi(0, GL_MAX)
                        glBlendFunci(0, GL_ONE, GL_ONE)
                    } else {
                        glBlendEquation(GL_MAX)
                        glBlendFunc(GL_ONE, GL_ONE)
                    }
                }
            }
            cached.blendMode = drawStyle.blendMode
        }
        if (dirty || cached.alphaToCoverage != drawStyle.alphaToCoverage) {
            if (drawStyle.alphaToCoverage) {
                glEnable(GL13C.GL_SAMPLE_ALPHA_TO_COVERAGE)
                glDisable(GL11C.GL_BLEND)
            } else {
                glDisable(GL13C.GL_SAMPLE_ALPHA_TO_COVERAGE)
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

    fun destroyAllVAOs() {
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
        else -> throw RuntimeException("unsupported op")
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