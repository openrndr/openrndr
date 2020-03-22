package org.openrndr.internal

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import java.io.InputStream
import java.nio.Buffer
import java.nio.ByteBuffer

/**
 *  built-in shader generators
 */
interface ShaderGenerators {
    fun vertexBufferFragmentShader(shadeStructure: ShadeStructure): String
    fun vertexBufferVertexShader(shadeStructure: ShadeStructure): String

    fun imageFragmentShader(shadeStructure: ShadeStructure): String
    fun imageVertexShader(shadeStructure: ShadeStructure): String

    fun imageArrayTextureFragmentShader(shadeStructure: ShadeStructure): String
    fun imageArrayTextureVertexShader(shadeStructure: ShadeStructure): String

    fun pointFragmentShader(shadeStructure: ShadeStructure): String
    fun pointVertexShader(shadeStructure: ShadeStructure): String

    fun circleFragmentShader(shadeStructure: ShadeStructure): String
    fun circleVertexShader(shadeStructure: ShadeStructure): String

    fun fontImageMapFragmentShader(shadeStructure: ShadeStructure): String
    fun fontImageMapVertexShader(shadeStructure: ShadeStructure): String

    fun rectangleFragmentShader(shadeStructure: ShadeStructure): String
    fun rectangleVertexShader(shadeStructure: ShadeStructure): String

    fun expansionFragmentShader(shadeStructure: ShadeStructure): String
    fun expansionVertexShader(shadeStructure: ShadeStructure): String

    fun fastLineFragmentShader(shadeStructure: ShadeStructure): String
    fun fastLineVertexShader(shadeStructure: ShadeStructure): String

    fun meshLineFragmentShader(shadeStructure: ShadeStructure): String
    fun meshLineVertexShader(shadeStructure: ShadeStructure): String

    fun filterVertexShader(shadeStructure: ShadeStructure): String
    fun filterFragmentShader(shadeStructure: ShadeStructure): String
}


/**
 * Driver interface. This is the internal interface
 */
interface Driver {

    val contextID: Long


    fun createShader(vsCode: String, fsCode: String, session: Session? = Session.active): Shader

    fun createComputeShader(code: String, session: Session? = Session.active): ComputeShader

    fun createShadeStyleManager(vertexShaderGenerator: (ShadeStructure) -> String,
                                fragmentShaderGenerator: (ShadeStructure) -> String,
                                session: Session? = Session.root): ShadeStyleManager

    fun createRenderTarget(width: Int, height: Int, contentScale: Double = 1.0, multisample: BufferMultisample = BufferMultisample.Disabled, session: Session? = Session.active): RenderTarget


    fun createArrayTexture(width: Int, height: Int, layers: Int, format: ColorFormat, type: ColorType, levels: Int = 1, session: Session? = Session.active): ArrayTexture
    fun createAtomicCounterBuffer(counterCount: Int, session: Session? = Session.active): AtomicCounterBuffer

    fun createColorBuffer(width: Int, height: Int, contentScale: Double, format: ColorFormat, type: ColorType, multisample: BufferMultisample = BufferMultisample.Disabled, levels: Int = 1, session: Session? = Session.active): ColorBuffer
    fun createColorBufferFromUrl(url: String, session: Session? = Session.active): ColorBuffer
    fun createColorBufferFromFile(filename: String, session: Session? = Session.active): ColorBuffer
    fun createColorBufferFromStream(stream: InputStream, name: String? = null, formatHint: ImageFileFormat? = null, session: Session? = Session.active): ColorBuffer
    fun createColorBufferFromArray(array: ByteArray, offset: Int = 0, length: Int = 0, name: String? = null, formatHint: ImageFileFormat? = null, session: Session? = Session.active): ColorBuffer
    fun createColorBufferFromBuffer(buffer: ByteBuffer, name: String? = null, formatHint: ImageFileFormat? = null, session: Session? = Session.active): ColorBuffer

    fun createDepthBuffer(width: Int, height: Int, format: DepthFormat, multisample: BufferMultisample = BufferMultisample.Disabled, session: Session? = Session.active): DepthBuffer
    fun createBufferTexture(elementCount: Int, format: ColorFormat, type: ColorType, session: Session? = Session.active): BufferTexture

    fun createCubemap(width: Int, format: ColorFormat, type: ColorType, session: Session? = Session.active): Cubemap
    fun createCubemapFromUrls(urls: List<String>, session: Session? = Session.active): Cubemap

    fun createResourceThread(session: Session? = Session.active, f: () -> Unit): ResourceThread
    fun createDrawThread(session: Session? = Session.active): DrawThread

    fun clear(r: Double, g: Double, b: Double, a: Double)
    fun clear(color: ColorRGBa) {
        clear(color.r, color.g, color.b, color.a)
    }

    fun createDynamicVertexBuffer(format: VertexFormat, vertexCount: Int, session: Session? = Session.active): VertexBuffer
    fun createStaticVertexBuffer(format: VertexFormat, buffer: Buffer, session: Session? = Session.active): VertexBuffer

    fun createDynamicIndexBuffer(elementCount: Int, type: IndexType, session: Session? = Session.active): IndexBuffer

    fun drawVertexBuffer(shader: Shader, vertexBuffers: List<VertexBuffer>,
                         drawPrimitive: DrawPrimitive,
                         vertexOffset: Int, vertexCount: Int)

    fun drawIndexedVertexBuffer(shader: Shader, indexBuffer: IndexBuffer, vertexBuffers: List<VertexBuffer>,
                                drawPrimitive: DrawPrimitive,
                                indexOffset: Int, indexCount: Int)


    fun drawInstances(shader: Shader, vertexBuffers: List<VertexBuffer>,
                      instanceAttributes: List<VertexBuffer>,
                      drawPrimitive: DrawPrimitive, vertexOffset: Int, vertexCount: Int, instanceCount: Int)

    fun drawIndexedInstances(shader: Shader, indexBuffer: IndexBuffer, vertexBuffers: List<VertexBuffer>,
                             instanceAttributes: List<VertexBuffer>,
                             drawPrimitive: DrawPrimitive, indexOffset: Int, indexCount: Int, instanceCount: Int)

    fun setState(drawStyle: DrawStyle)

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
        var driver: Driver? = null
        val instance: Driver get() = driver ?: error("No graphical context has been set up yet.")
    }
}

/**
 * Wait for the [Driver] to finish drawing
 */
fun finish() {
    Driver.instance.finish()
}