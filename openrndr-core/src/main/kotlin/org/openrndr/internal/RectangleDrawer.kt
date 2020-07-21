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
    }, 6, Session.root)

    internal var batch = RectangleBatch.create(10_000, Session.root)

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators("rectangle",
            vsGenerator = Driver.instance.shaderGenerators::rectangleVertexShader,
            fsGenerator = Driver.instance.shaderGenerators::rectangleFragmentShader)

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

    internal fun ensureBatchSize(size: Int) {
        if (batch.size < size) {
            batch.destroy()
            batch = RectangleBatch.create(size, Session.root)
        }
    }

    fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>, dimensions: List<Vector2>) {
        ensureBatchSize(positions.size)
        batch.geometry.put {
            for (i in positions.indices) {
                write(Vector3(positions[i].x, positions[i].y, 0.0))
                write(dimensions[i])
                write(0.0f)
            }
        }
        batch.drawStyle.put {
            write(drawStyle)
        }
        drawRectangles(drawContext, drawStyle, batch, positions.size)
    }

    fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>, width: Double, height: Double) {
        ensureBatchSize(positions.size)
        batch.geometry.put {
            positions.forEach {
                write(it.x.toFloat(), it.y.toFloat(), 0.0f)
                write(width.toFloat())
                write(height.toFloat())
                write(0.0f)
            }
        }
        batch.drawStyle.put {
            for (i in positions.indices) {
                write(drawStyle)
            }
        }
        drawRectangles(drawContext, drawStyle, batch, positions.size)
    }

    fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, rectangles: List<Rectangle>) {
        ensureBatchSize(rectangles.size)
        batch.geometry.put {
            rectangles.forEach {
                write(Vector3(it.x, it.y, 0.0))
                write(it.width.toFloat())
                write(it.height.toFloat())
                write(0.0f)
            }
        }
        batch.drawStyle.put {
            rectangles.forEach {
                write(drawStyle)
            }
        }
        drawRectangles(drawContext, drawStyle, batch, rectangles.size)
    }

    fun drawRectangle(drawContext: DrawContext,
                      drawStyle: DrawStyle, x: Double, y: Double, width: Double, height: Double) {
        ensureBatchSize(1)

        batch.geometry.put {
            write(Vector3(x, y, 0.0))
            write(width.toFloat())
            write(height.toFloat())
            write(0.0f)
        }
        batch.drawStyle.put {
            write(drawStyle)
        }
        drawRectangles(drawContext, drawStyle,  batch,1)
    }

    fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, batch: RectangleBatch, count: Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, listOf(vertices.vertexFormat), listOf(batch.geometry.vertexFormat, batch.drawStyle.vertexFormat))
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(shader, listOf(vertices), listOf(batch.drawStyle, batch.geometry) + (drawStyle.shadeStyle?.attributes
                ?: emptyList()), DrawPrimitive.TRIANGLES, 0, 6, 0, count)
        shader.end()
    }
}