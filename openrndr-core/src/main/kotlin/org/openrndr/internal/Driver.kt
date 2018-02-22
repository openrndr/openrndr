package org.openrndr.internal

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import java.nio.Buffer

/**
 *  built-in shader generators
 */
interface ShaderGenerators {
    fun vertexBufferFragmentShader(shadeStructure: ShadeStructure): String
    fun vertexBufferVertexShader(shadeStructure: ShadeStructure): String

    fun imageFragmentShader(shadeStructure: ShadeStructure): String
    fun imageVertexShader(shadeStructure: ShadeStructure): String

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

}

/**
 * Driver interface. This is the internal interface
 */
interface Driver {

    fun createShader(vsCode: String, fsCode: String): Shader

    fun createShadeStyleManager(vertexShaderGenerator: (ShadeStructure) -> String,
                                fragmentShaderGenerator: (ShadeStructure) -> String): ShadeStyleManager

    fun createRenderTarget(width: Int, height: Int, contentScale:Double=1.0): RenderTarget

    fun createColorBuffer(width: Int, height: Int, contentScale:Double, format: ColorFormat, type: ColorType): ColorBuffer
    fun createColorBufferFromUrl(url: String): ColorBuffer

    fun createDepthBuffer(width: Int, height: Int, format: DepthFormat): DepthBuffer
    fun createBufferTexture(elementCount: Int, format: ColorFormat, type: ColorType): BufferTexture

    fun clear(color: ColorRGBa)

    fun createDynamicVertexBuffer(format: VertexFormat, vertexCount: Int): VertexBuffer
    fun createStaticVertexBuffer(format: VertexFormat, buffer: Buffer): VertexBuffer
    fun drawVertexBuffer(shader: Shader, vertexBuffers: List<VertexBuffer>,
                         drawPrimitive: DrawPrimitive,
                         vertexOffset: Int, vertexCount: Int)

    fun drawInstances(shader: Shader, vertexBuffers: List<VertexBuffer>,

                      instanceAttributes: List<VertexBuffer>,
                      drawPrimitive: DrawPrimitive, vertexOffset: Int, vertexCount: Int, instanceCount: Int)

    fun setState(drawStyle: DrawStyle)

    val fontImageMapManager: FontMapManager
    val fontVectorMapManager: FontMapManager
    val shaderGenerators: ShaderGenerators

    fun internalShaderResource(resourceId: String): String

    companion object {
        lateinit var driver: Driver
        val instance: Driver get() = driver;
    }
}