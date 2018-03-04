package org.openrndr.internal

import org.openrndr.math.Vector2
import org.openrndr.draw.*
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle

class ImageDrawer {

    private val vertices: VertexBuffer = VertexBuffer.createDynamic(VertexFormat().apply {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, 6)

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
            Driver.instance.shaderGenerators::imageVertexShader,
            Driver.instance.shaderGenerators::imageFragmentShader)

    fun drawImage(drawContext: DrawContext, drawStyle: DrawStyle, colorBuffer: ColorBuffer,
                  source:Rectangle, target:Rectangle) {

        fun flipV(v:Double):Double = if (colorBuffer.flipV) {
            1.0 - v
        } else {
            v
        }

        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        val w = vertices.shadow.writer()

        w.rewind()
        val pa = Vector3(target.x, target.y, 0.0)
        val pb = Vector3(target.x + target.width, target.y, 0.0)
        val pc = Vector3(target.x + target.width, target.y + target.height, 0.0)
        val pd = Vector3(target.x, target.y + target.height, 0.0)

        val u0 = source.x/colorBuffer.width
        val u1 = (source.x+source.width)/colorBuffer.width
        val v0 = 1.0 - (source.y+source.height)/colorBuffer.height
        val v1 = 1.0 - source.y/colorBuffer.height

        val ta = Vector2(u0, flipV(v1))
        val tb = Vector2(u1, flipV(v1))
        val tc = Vector2(u1, flipV(v0))
        val td = Vector2(u0, flipV(v0))

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
        colorBuffer.bind(0)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.TRIANGLES, 0, 6)
        shader.end()
    }

    fun drawImage(drawContext: DrawContext,
                  drawStyle: DrawStyle, colorBuffer: ColorBuffer, x: Double, y: Double, width: Double, height: Double) {

        fun flipV(v:Double):Double = if (colorBuffer.flipV) {
            1.0 - v
        } else {
            v
        }

        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        val w = vertices.shadow.writer()

        w.rewind()
        val pa = Vector3(x, y, 0.0)
        val pb = Vector3(x + width, y, 0.0)
        val pc = Vector3(x + width, y + height, 0.0)
        val pd = Vector3(x, y + height, 0.0)

        val ta = Vector2(0.0, flipV(1.0))
        val tb = Vector2(1.0, flipV(1.0))
        val tc = Vector2(1.0, flipV(0.0))
        val td = Vector2(0.0, flipV(0.0))

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
        colorBuffer.bind(0)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.TRIANGLES, 0, 6)
        shader.end()
    }

}