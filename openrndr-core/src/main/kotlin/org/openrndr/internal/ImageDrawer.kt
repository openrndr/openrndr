package org.openrndr.internal

import org.openrndr.math.Vector2
import org.openrndr.draw.*
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle

class ImageDrawer {

    private val vertices: VertexBuffer = VertexBuffer.createDynamic(VertexFormat().apply {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, 6)

    private val instanceFormat = vertexFormat {
        attribute("source", VertexElementType.VECTOR4_FLOAT32)
        attribute("target", VertexElementType.VECTOR4_FLOAT32)
    }

    private var instanceAttributes = vertexBuffer(instanceFormat, 10)

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
            Driver.instance.shaderGenerators::imageVertexShader,
            Driver.instance.shaderGenerators::imageFragmentShader)

    init {
        val w = vertices.shadow.writer()

        w.rewind()
        val pa = Vector3(0.0, 0.0, 0.0)
        val pb = Vector3(1.0, 0.0, 0.0)
        val pc = Vector3(1.0, 1.0, 0.0)
        val pd = Vector3(0.0, 1.0, 0.0)

        val u0 = 0.0
        val u1 = 1.0
        val v0 = 1.0
        val v1 = 0.0

        val ta = Vector2(u0, v1)
        val tb = Vector2(u1, v1)
        val tc = Vector2(u1, v0)
        val td = Vector2(u0, v0)

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
    }

    private fun assertInstanceSize(size:Int) {
        if (instanceAttributes.vertexCount < size) {
            instanceAttributes.destroy()
            instanceAttributes = vertexBuffer(instanceFormat, size)
        }
    }

    fun drawImage(drawContext: DrawContext, drawStyle: DrawStyle, colorBuffer: ColorBuffer,
                  rectangles: List<Pair<Rectangle, Rectangle>>) {

        assertInstanceSize(rectangles.size)

        val shader = shaderManager.shader(drawStyle.shadeStyle, listOf(vertices.vertexFormat), listOf(instanceAttributes.vertexFormat))

        val iw = instanceAttributes.shadow.writer()
        iw.rewind()

        rectangles.forEach {
            val (source, target) = it
            iw.write(Vector4(source.corner.x / colorBuffer.width, source.corner.y / colorBuffer.height, source.width / colorBuffer.width, source.height / colorBuffer.height))
            iw.write(Vector4(target.corner.x, target.corner.y, target.width, target.height))
        }
        instanceAttributes.shadow.uploadElements(0, rectangles.size)

        colorBuffer.bind(0)
        shader.begin()
        drawContext.applyToShader(shader)
        shader.uniform("u_flipV", if (colorBuffer.flipV) 1 else 0)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(shader, listOf(vertices),  listOf(instanceAttributes) + ( drawStyle.shadeStyle?.attributes?: emptyList() ), DrawPrimitive.TRIANGLES, 0, 6, rectangles.size)
        shader.end()
    }

    fun drawImage(drawContext: DrawContext,
                  drawStyle: DrawStyle, colorBuffer: ColorBuffer, x: Double, y: Double, width: Double, height: Double) {
        drawImage(drawContext, drawStyle, colorBuffer, listOf( colorBuffer.bounds to Rectangle(x, y, width, height)))
    }
}