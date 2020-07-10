package org.openrndr.internal

import org.openrndr.draw.*

class VertexBufferDrawer {

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators("vertex-buffer",
            vsGenerator = Driver.instance.shaderGenerators::vertexBufferVertexShader,
            fsGenerator = Driver.instance.shaderGenerators::vertexBufferFragmentShader)

    fun drawVertexBuffer(drawContext: DrawContext, drawStyle: DrawStyle, primitive: DrawPrimitive, vertexBuffers:List<VertexBuffer>, offset:Int, vertexCount:Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexBuffers.map { it.vertexFormat })
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, vertexBuffers, primitive, offset, vertexCount)
        shader.end()
    }

    fun drawVertexBuffer(drawContext: DrawContext, drawStyle: DrawStyle, primitive: DrawPrimitive, indexBuffer: IndexBuffer, vertexBuffers:List<VertexBuffer>, offset:Int, indexCount:Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexBuffers.map { it.vertexFormat })
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawIndexedVertexBuffer(shader, indexBuffer, vertexBuffers, primitive, offset, indexCount)
        shader.end()
    }

    fun drawVertexBufferInstances(drawContext: DrawContext, drawStyle: DrawStyle, primitive: DrawPrimitive, vertexBuffers:List<VertexBuffer>, instanceAttributes:List<VertexBuffer>,  offset:Int, vertexCount:Int, instanceCount:Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexBuffers.map { it.vertexFormat }, instanceAttributes.map { it.vertexFormat })
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(shader, vertexBuffers, instanceAttributes + (drawStyle.shadeStyle?.attributes?: emptyList()), primitive, offset, vertexCount, instanceCount)
        shader.end()
    }


    fun drawVertexBufferInstances(drawContext: DrawContext, drawStyle: DrawStyle, primitive: DrawPrimitive, indexBuffer: IndexBuffer, vertexBuffers:List<VertexBuffer>, instanceAttributes:List<VertexBuffer>,  offset:Int, indexCount:Int, instanceCount:Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexBuffers.map { it.vertexFormat }, instanceAttributes.map { it.vertexFormat })
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawIndexedInstances(shader, indexBuffer, vertexBuffers, instanceAttributes + (drawStyle.shadeStyle?.attributes?: emptyList()), primitive, offset, indexCount, instanceCount)
        shader.end()
    }
}