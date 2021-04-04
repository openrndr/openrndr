@file:JvmName("DriverJVM")
package org.openrndr.internal

import mu.KotlinLogging
import org.openrndr.draw.*
import kotlin.jvm.JvmName


/**
 * Driver interface. This is the internal interface
 */
expect interface Driver {

    val contextID: Long

    /**
     * Create a shader from code
     * @param vsCode vertex shader code
     * @param gsCode optional geometry shader code
     * @param fsCode fragment shader code
     */

    fun createShader(
        vsCode: String,
        tcsCode: String?,
        tesCode: String?,
        gsCode: String?,
        fsCode: String,
        name: String,
        session: Session? = Session.active
    ): Shader

    fun createComputeShader(code: String, session: Session? = Session.active): ComputeShader

    fun createShadeStyleManager(
        name: String,
        vsGenerator: (ShadeStructure) -> String,
        tcsGenerator: ((ShadeStructure) -> String)? = null,
        tesGenerator: ((ShadeStructure) -> String)? = null,
        gsGenerator: ((ShadeStructure) -> String)? = null,
        fsGenerator: (ShadeStructure) -> String,
        session: Session? = Session.root
    ): ShadeStyleManager

    fun createRenderTarget(
        width: Int,
        height: Int,
        contentScale: Double = 1.0,
        multisample: BufferMultisample = BufferMultisample.Disabled,
        session: Session? = Session.active
    ): RenderTarget

    fun createArrayCubemap(
        width: Int,
        layers: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int = 1,
        session: Session? = Session.active
    ): ArrayCubemap

    fun createArrayTexture(
        width: Int,
        height: Int,
        layers: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int = 1,
        session: Session? = Session.active
    ): ArrayTexture

    fun createAtomicCounterBuffer(counterCount: Int, session: Session? = Session.active): AtomicCounterBuffer

    fun createColorBuffer(
        width: Int,
        height: Int,
        contentScale: Double,
        format: ColorFormat,
        type: ColorType,
        multisample: BufferMultisample = BufferMultisample.Disabled,
        levels: Int = 1,
        session: Session? = Session.active
    ): ColorBuffer

    suspend fun createColorBufferFromUrl(
        url: String,
        formatHint: ImageFileFormat? = null,
        session: Session? = Session.active
    ): ColorBuffer

    fun createColorBufferFromFile(
        filename: String,
        formatHint: ImageFileFormat? = null,
        session: Session? = Session.active
    ): ColorBuffer

//    fun createColorBufferFromStream(
//        stream: InputStream,
//        name: String? = null,
//        formatHint: ImageFileFormat? = null,
//        session: Session? = Session.active
//    ): ColorBuffer

    fun createColorBufferFromArray(
        array: ByteArray,
        offset: Int = 0,
        length: Int = 0,
        name: String? = null,
        formatHint: ImageFileFormat? = null,
        session: Session? = Session.active
    ): ColorBuffer

//    fun createColorBufferFromBuffer(
//        buffer: ByteBuffer,
//        name: String? = null,
//        formatHint: ImageFileFormat? = null,
//        session: Session? = Session.active
//    ): ColorBuffer

    fun createDepthBuffer(
        width: Int,
        height: Int,
        format: DepthFormat,
        multisample: BufferMultisample = BufferMultisample.Disabled,
        session: Session? = Session.active
    ): DepthBuffer

    fun createBufferTexture(
        elementCount: Int,
        format: ColorFormat,
        type: ColorType,
        session: Session? = Session.active
    ): BufferTexture

    fun createCubemap(
        width: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session? = Session.active
    ): Cubemap

    fun createCubemapFromUrls(
        urls: List<String>,
        formatHint: ImageFileFormat? = null,
        session: Session? = Session.active
    ): Cubemap

    fun createCubemapFromFiles(
        filenames: List<String>,
        formatHint: ImageFileFormat? = null,
        session: Session? = Session.active
    ): Cubemap

    fun createVolumeTexture(
        width: Int,
        height: Int,
        depth: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session? = Session.active
    ): VolumeTexture

//    fun createResourceThread(session: Session? = Session.active, f: () -> Unit): ResourceThread
//    fun createDrawThread(session: Session? = Session.active): DrawThread

    fun clear(r: Double, g: Double, b: Double, a: Double)

//    fun clear(color: ColorRGBa) {
//        clear(color.r, color.g, color.b, color.a)
//    }

    fun createDynamicVertexBuffer(
        format: VertexFormat,
        vertexCount: Int,
        session: Session? = Session.active
    ): VertexBuffer

//    fun createStaticVertexBuffer(format: VertexFormat, buffer: Buffer, session: Session? = Session.active): VertexBuffer

    fun createDynamicIndexBuffer(elementCount: Int, type: IndexType, session: Session? = Session.active): IndexBuffer

    fun createShaderStorageBuffer(format: ShaderStorageFormat, session: Session? = Session.active): ShaderStorageBuffer

    fun drawVertexBuffer(
        shader: Shader, vertexBuffers: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        vertexOffset: Int, vertexCount: Int, verticesPerPatch: Int = 0
    )

    fun drawIndexedVertexBuffer(
        shader: Shader, indexBuffer: IndexBuffer, vertexBuffers: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        indexOffset: Int, indexCount: Int, verticesPerPatch: Int = 0
    )


    fun drawInstances(
        shader: Shader, vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive, vertexOffset: Int, vertexCount: Int,
        instanceOffset: Int, instanceCount: Int, verticesPerPatch: Int = 0
    )

    fun drawIndexedInstances(
        shader: Shader, indexBuffer: IndexBuffer, vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive, indexOffset: Int, indexCount: Int,
        instanceOffset: Int, instanceCount: Int, verticesPerPatch: Int = 0
    )

    fun setState(drawStyle: DrawStyle)

    fun destroyContext(context: Long)

    val fontImageMapManager: FontMapManager
    val fontVectorMapManager: FontMapManager
    val shaderGenerators: ShaderGenerators
    val activeRenderTarget: RenderTarget

    /**
     * waits for all drawing to complete
     */
    fun finish()

    fun internalShaderResource(resourceId: String): String

    companion object {
        var driver: Driver?

//        = null
//            set(value) {
//                logger.debug {"setting driver instance to $value" }
//                field = value
//            }

        val instance: Driver
//        get() = driver ?: error("No graphical context has been set up yet.")
    }
}

/**
 * Wait for the [Driver] to finish drawing
 */
fun finish() {
    Driver.instance.finish()
}