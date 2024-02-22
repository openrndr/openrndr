package org.openrndr.internal.nullgl

import org.openrndr.draw.*
import org.openrndr.internal.*
import java.nio.Buffer

class DriverNullGL: Driver {
    override val contextID: Long
    get() {
        return Thread.currentThread().id
    }

    override fun createShader(vsCode: String, tcsCode: String?, tesCode: String?, gsCode: String?, fsCode: String, name: String, session: Session?): Shader {
        return ShaderNullGL(session)
    }

    override fun createComputeShader(code: String, name: String, session: Session?): ComputeShader {
        TODO("Not yet implemented")
    }

    override fun createShadeStyleManager(name: String, vsGenerator: (ShadeStructure) -> String, tcsGenerator: ((ShadeStructure) -> String)?, tesGenerator: ((ShadeStructure) -> String)?, gsGenerator: ((ShadeStructure) -> String)?, fsGenerator: (ShadeStructure) -> String, session: Session?): ShadeStyleManager {
        return ShadeStyleManagerNullGL(name)
    }

    override fun createRenderTarget(width: Int, height: Int, contentScale: Double, multisample: BufferMultisample, session: Session?): RenderTarget {
        return RenderTargetNullGL(width, height, contentScale, multisample, session)
    }

    override fun createArrayCubemap(width: Int, layers: Int, format: ColorFormat, type: ColorType, levels: Int, session: Session?): ArrayCubemap {
        TODO("Not yet implemented")
    }

    override fun createArrayTexture(width: Int, height: Int, layers: Int, format: ColorFormat, type: ColorType, levels: Int, session: Session?): ArrayTexture {
        TODO("Not yet implemented")
    }

    override fun createAtomicCounterBuffer(counterCount: Int, session: Session?): AtomicCounterBuffer {
        TODO("Not yet implemented")
    }

    override fun createColorBuffer(width: Int, height: Int, contentScale: Double, format: ColorFormat, type: ColorType, multisample: BufferMultisample, levels: Int, session: Session?): ColorBuffer {
        return ColorBufferNullGL(width, height, contentScale, format, type, levels, multisample, session)
    }

    override fun createDepthBuffer(width: Int, height: Int, format: DepthFormat, multisample: BufferMultisample, session: Session?): DepthBuffer {
        return DepthBufferNullGL(width, height, format, multisample, session)
    }

    override fun createBufferTexture(elementCount: Int, format: ColorFormat, type: ColorType, session: Session?): BufferTexture {
        TODO("Not yet implemented")
    }

    override fun createCubemap(width: Int, format: ColorFormat, type: ColorType, levels: Int, session: Session?): Cubemap {
        TODO("Not yet implemented")
    }

    override fun createVolumeTexture(width: Int, height: Int, depth: Int, format: ColorFormat, type: ColorType, levels: Int, session: Session?): VolumeTexture {
        TODO("Not yet implemented")
    }

    override fun createResourceThread(session: Session?, f: () -> Unit): ResourceThread {
        TODO("Not yet implemented")
    }

    override fun createDrawThread(session: Session?): DrawThread {
        TODO("Not yet implemented")
    }

    override fun clear(r: Double, g: Double, b: Double, a: Double) {

    }

    override fun createDynamicVertexBuffer(format: VertexFormat, vertexCount: Int, session: Session?): VertexBuffer {
        return VertexBufferNullGL(format, vertexCount, session)
    }

    override fun createStaticVertexBuffer(format: VertexFormat, buffer: Buffer, session: Session?): VertexBuffer {
        return VertexBufferNullGL(format, 100, session)
    }

    override fun createDynamicIndexBuffer(elementCount: Int, type: IndexType, session: Session?): IndexBuffer {
        TODO("Not yet implemented")
    }

    override fun createShaderStorageBuffer(format: ShaderStorageFormat, session: Session?): ShaderStorageBuffer {
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

    }

    override fun setState(drawStyle: DrawStyle) {

    }

    override fun destroyContext(context: Long) {

    }

    override val fontImageMapManager: FontMapManager
        get() = TODO("Not yet implemented")
    override val fontVectorMapManager: FontMapManager
        get() = TODO("Not yet implemented")
    override val shaderLanguage: ShaderLanguage
        get() {
            return GLSL("330 core")
        }
    override val shaderGenerators: ShaderGenerators = ShaderGeneratorsNullGL()

    override val activeRenderTarget: RenderTarget
        get() = renderTarget(640, 480) {
            colorBuffer()
            depthBuffer()
        }

    override fun finish() {

    }

    override fun internalShaderResource(resourceId: String): String {
        return "mock"
    }

    override fun shaderConfiguration(): String = ""
    override fun createComputeStyleManager(session: Session?): ComputeStyleManager {
        TODO("Not yet implemented")
    }


}