package org.openrndr.internal

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

/**
 * Driver interface. This is the internal interface
 */
actual interface Driver {

    //    fun createColorBufferFromStream(
//        stream: InputStream,
//        name: String? = null,
//        formatHint: ImageFileFormat? = null,
//        session: Session? = Session.active
//    ): ColorBuffer

    //    fun createColorBufferFromBuffer(
//        buffer: ByteBuffer,
//        name: String? = null,
//        formatHint: ImageFileFormat? = null,
//        session: Session? = Session.active
//    ): ColorBuffer

    //    fun createResourceThread(session: Session? = Session.active, f: () -> Unit): ResourceThread
//    fun createDrawThread(session: Session? = Session.active): DrawThread

    //    fun clear(color: ColorRGBa) {
//        clear(color.r, color.g, color.b, color.a)
//    }

    //    fun createStaticVertexBuffer(format: VertexFormat, buffer: Buffer, session: Session? = Session.active): VertexBuffer
    actual val contextID: Long

    /**
     * Create a shader from code
     * @param vsCode vertex shader code
     * @param gsCode optional geometry shader code
     * @param fsCode fragment shader code
     */
    actual fun createShader(
        vsCode: String,
        tcsCode: String?,
        tesCode: String?,
        gsCode: String?,
        fsCode: String,
        name: String,
        session: Session?
    ): Shader

    actual fun createComputeShader(
        code: String,
        name: String,
        session: Session?
    ): ComputeShader

    actual fun createShadeStyleManager(
        name: String,
        vsGenerator: (ShadeStructure) -> String,
        tcsGenerator: ((ShadeStructure) -> String)?,
        tesGenerator: ((ShadeStructure) -> String)?,
        gsGenerator: ((ShadeStructure) -> String)?,
        fsGenerator: (ShadeStructure) -> String,
        session: Session?
    ): ShadeStyleManager

    actual fun createRenderTarget(
        width: Int,
        height: Int,
        contentScale: Double,
        multisample: BufferMultisample,
        session: Session?
    ): RenderTarget

    actual fun createArrayCubemap(
        width: Int,
        layers: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session?
    ): ArrayCubemap

    actual fun createArrayTexture(
        width: Int,
        height: Int,
        layers: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session?
    ): ArrayTexture

    actual fun createAtomicCounterBuffer(
        counterCount: Int,
        session: Session?
    ): AtomicCounterBuffer

    actual fun createColorBuffer(
        width: Int,
        height: Int,
        contentScale: Double,
        format: ColorFormat,
        type: ColorType,
        multisample: BufferMultisample,
        levels: Int,
        session: Session?
    ): ColorBuffer

    actual fun createDepthBuffer(
        width: Int,
        height: Int,
        format: DepthFormat,
        multisample: BufferMultisample,
        session: Session?
    ): DepthBuffer

    actual fun createBufferTexture(
        elementCount: Int,
        format: ColorFormat,
        type: ColorType,
        session: Session?
    ): BufferTexture

    actual fun createCubemap(
        width: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session?
    ): Cubemap

    actual fun createVolumeTexture(
        width: Int,
        height: Int,
        depth: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session?
    ): VolumeTexture

    actual fun clear(color: ColorRGBa)
    actual fun createDynamicVertexBuffer(
        format: VertexFormat,
        vertexCount: Int,
        session: Session?
    ): VertexBuffer

    actual fun createDynamicIndexBuffer(
        elementCount: Int,
        type: IndexType,
        session: Session?
    ): IndexBuffer

    actual fun createShaderStorageBuffer(
        format: ShaderStorageFormat,
        session: Session?
    ): ShaderStorageBuffer

    actual fun drawVertexBuffer(
        shader: Shader,
        vertexBuffers: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        vertexOffset: Int,
        vertexCount: Int,
        verticesPerPatch: Int
    )

    actual fun drawIndexedVertexBuffer(
        shader: Shader,
        indexBuffer: IndexBuffer,
        vertexBuffers: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        indexOffset: Int,
        indexCount: Int,
        verticesPerPatch: Int
    )

    actual fun drawInstances(
        shader: Shader,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        vertexOffset: Int,
        vertexCount: Int,
        instanceOffset: Int,
        instanceCount: Int,
        verticesPerPatch: Int
    )

    actual fun drawIndexedInstances(
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
    )

    actual fun setState(drawStyle: DrawStyle)
    actual fun destroyContext(context: Long)
    actual val fontImageMapManager: FontMapManager
    actual val fontVectorMapManager: FontMapManager
    actual val shaderGenerators: ShaderGenerators
    actual val activeRenderTarget: RenderTarget

    /**
     * waits for all drawing to complete
     */
    actual fun finish()
    actual fun internalShaderResource(resourceId: String): String

    actual fun shaderConfiguration(type: ShaderType): String

    actual companion object {
        actual var driver: Driver? = null
        actual val instance: Driver
            get() {
                return driver ?: error("no active driver")
            }
    }

    actual val shaderLanguage: ShaderLanguage
    actual fun createComputeStyleManager(session: Session?): ComputeStyleManager
    actual val properties: DriverProperties


}