package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.min

class RectangleDrawer {
    private val vertices: VertexBuffer = VertexBuffer.createDynamic(VertexFormat().apply {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, 6, Session.root)

    private var count = 0

    private val singleBatches = (0 until DrawerConfiguration.vertexBufferMultiBufferCount).map { RectangleBatch.create(1) }

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
        require(positions.size == dimensions.size) {
            "`positions.size` and `dimensions.size` must be equal in drawRectangles()"
        }
        batch.geometry.put {
            dimensions.forEachIndexed { i, sz ->
                write(Vector3(
                        positions[i].x + min(0.0, sz.x),
                        positions[i].y + min(0.0, sz.y), 0.0))
                write(Vector3(abs(sz.x), abs(sz.y), 0.0))

            }
        }
        batch.drawStyle.put {
            for (i in positions.indices) {
                write(drawStyle)
            }
        }
        drawRectangles(drawContext, drawStyle, batch, positions.size)
    }

    fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>, width: Double, height: Double) {
        ensureBatchSize(positions.size)
        batch.geometry.put {
            positions.forEach {
                write(Vector3(
                        it.x + min(0.0, width),
                        it.y + min(0.0, height), 0.0))
                write(Vector3(abs(width), abs(height), 0.0))
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
                write(Vector3(
                        it.x + min(0.0, it.width),
                        it.y + min(0.0, it.height), 0.0))
                write(Vector3(abs(it.width), abs(it.height), 0.0))
            }
        }
        batch.drawStyle.put {
            for (i in rectangles.indices) {
                write(drawStyle)
            }
        }
        drawRectangles(drawContext, drawStyle, batch, rectangles.size)
    }

    fun drawRectangle(drawContext: DrawContext,
                      drawStyle: DrawStyle, x: Double, y: Double, width: Double, height: Double) {
        ensureBatchSize(1)

        val batch = singleBatches[count.mod(singleBatches.size)]

        batch.geometry.put {
            write(
                (x + min(0.0, width)).toFloat(),
                (y + min(0.0, height)).toFloat(), 0.0f)
            write(abs(width).toFloat(), abs(height).toFloat(), 0.0f)
        }
        batch.drawStyle.put {
            write(drawStyle)
        }
        drawRectangles(drawContext, drawStyle, batch, 1)
        count++
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