package org.openrndr.internal

import org.openrndr.draw.*

class VertexBufferDrawer {

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(Driver.instance.shaderGenerators::vertexBufferVertexShader,
            Driver.instance.shaderGenerators::vertexBufferFragmentShader)

    fun drawVertexBuffer(drawContext: DrawContext, drawStyle: DrawStyle, primitive: DrawPrimitive, vertexBuffers:List<VertexBuffer>, offset:Int, vertexCount:Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexBuffers.map { it.vertexFormat })
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, vertexBuffers, primitive, offset, vertexCount)
        shader.end()
    }

    fun drawVertexBufferInstances(drawContext: DrawContext, drawStyle: DrawStyle, primitive: DrawPrimitive, vertexBuffers:List<VertexBuffer>, instanceAttributes:List<VertexBuffer>,  offset:Int, vertexCount:Int, instanceCount:Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexBuffers.map { it.vertexFormat }, instanceAttributes.map { it.vertexFormat })
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        shader.begin()
        Driver.instance.drawInstances(shader, vertexBuffers, instanceAttributes, primitive, offset, vertexCount, instanceCount)
        shader.end()
    }
}