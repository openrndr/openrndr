package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle

class RectangleDrawer {

    private val vertices: VertexBuffer = VertexBuffer.createDynamic(VertexFormat().apply {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, 6)

    private var instanceFormat = VertexFormat().apply {
        attribute("dimensions", VertexElementType.VECTOR2_FLOAT32)
        attribute("offset", VertexElementType.VECTOR3_FLOAT32)
    }
    private var instanceAttributes = VertexBuffer.createDynamic(instanceFormat, 100)

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(Driver.instance.shaderGenerators::rectangleVertexShader,
            Driver.instance.shaderGenerators::rectangleFragmentShader)

    init {
        val w = vertices.shadow.writer()

        w.rewind()
        val x = 0.0
        val y = 0.0
        val radius = 1.0
        val pa = Vector3(x, y, 0.0)
        val pb = Vector3(x + radius, y, 0.0)
        val pc = Vector3(x + radius, y + radius, 0.0)
        val pd = Vector3(x, y + radius, 0.0)

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
    }

    private fun assertInstanceSize(size: Int) {
        if (instanceAttributes.vertexCount < size) {
            instanceAttributes.destroy()
            instanceAttributes = vertexBuffer(instanceFormat, size)
        }
    }

    fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>, dimensions: List<Vector2>) {
        assertInstanceSize(positions.size)
        instanceAttributes.shadow.writer().apply {
            rewind()
            for (i in 0 until positions.size) {
                write(dimensions[i])
                write(Vector3(positions[i].x, positions[i].y, 0.0))
            }
        }
        instanceAttributes.shadow.uploadElements(0, positions.size)
        drawRectangles(drawContext, drawStyle, positions.size)
    }

    fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>, width: Double, height: Double) {
        assertInstanceSize(positions.size)
        instanceAttributes.shadow.writer().apply {
            rewind()
            positions.forEach {
                write(width.toFloat())
                write(height.toFloat())
                write(Vector3(it.x, it.y, 0.0))
            }
        }
        instanceAttributes.shadow.uploadElements(0, positions.size)
        drawRectangles(drawContext, drawStyle, positions.size)
    }

    fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, rectangles: List<Rectangle>) {
        assertInstanceSize(rectangles.size)
        instanceAttributes.shadow.writer().apply {
            rewind()
            rectangles.forEach {
                write(it.width.toFloat())
                write(it.height.toFloat())
                write(Vector3(it.x, it.y, 0.0))
            }
        }
        instanceAttributes.shadow.uploadElements(0, rectangles.size)
        drawRectangles(drawContext, drawStyle, rectangles.size)
    }

    fun drawRectangle(drawContext: DrawContext,
                      drawStyle: DrawStyle, x: Double, y: Double, width: Double, height: Double) {
        assertInstanceSize(1)
        instanceAttributes.shadow.writer().apply {
            rewind()
            write(width.toFloat())
            write(height.toFloat())
            write(Vector3(x, y, 0.0))
        }
        instanceAttributes.shadow.uploadElements(0, 1)
        drawRectangles(drawContext, drawStyle, 1)
    }

    private fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, count: Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, listOf(vertices.vertexFormat), listOf(instanceAttributes.vertexFormat))
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(shader, listOf(vertices), listOf(instanceAttributes) + (drawStyle.shadeStyle?.attributes
                ?: emptyList()), DrawPrimitive.TRIANGLES, 0, 6, count)
        shader.end()
    }

}