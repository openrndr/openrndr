package org.openrndr.internal

import org.openrndr.math.Vector2
import org.openrndr.draw.*
import org.openrndr.math.Vector3

class RectangleDrawer {

    val vertices: VertexBuffer = VertexBuffer.createDynamic(VertexFormat().apply {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, 6)

    val shaderManager: ShadeStyleManager

    init {
        shaderManager = ShadeStyleManager.fromGenerators(Driver.instance.shaderGenerators::rectangleVertexShader,
                Driver.instance.shaderGenerators::rectangleFragmentShader)
    }

    fun drawRectangle(drawContext: DrawContext,
                      drawStyle: DrawStyle, x: Double, y: Double, width: Double, height: Double) {

        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)

        val w = vertices.shadow.writer()

        w.rewind()
        val pa = Vector3(x, y, 0.0)
        val pb = Vector3(x + width, y, 0.0)
        val pc = Vector3(x + width, y + height, 0.0)
        val pd = Vector3(x, y + height, 0.0)

        val ta = Vector2(0.0, 0.0)
        val tb = Vector2(1.0, 0.0)
        val tc = Vector2(1.0, 1.0)
        val td = Vector2(0.0, 1.0)

        val n = Vector3(0.0, 0.0, -1.0)
        w.apply {
            write(pa); write(n); write(ta)
            write(pd); write(n); write(td)
            write(pc); write(n); write(tc)

            write(pc); write(n); write(tc)
            write(pb); write(n); write(tb)
            write(pa); write(n); write(ta)
        }
        vertices.shadow.upload()

        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.TRIANGLES, 0, 6)
        shader.end()
    }

}