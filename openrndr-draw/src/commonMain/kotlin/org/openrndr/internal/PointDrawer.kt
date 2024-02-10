package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import kotlin.jvm.JvmName

class PointDrawer {
    val vertices: VertexBuffer = VertexBuffer.createDynamic(VertexFormat().apply {
        position(3)
        normal(3)
    }, 1)

    internal var batch = PointBatch.create(10_000, session = Session.root)
    private var count = 0


    private val singleBatches = (0 until DrawerConfiguration.vertexBufferMultiBufferCount).map { PointBatch.create(1) }


    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators("point",
            vsGenerator = Driver.instance.shaderGenerators::pointVertexShader,
            fsGenerator = Driver.instance.shaderGenerators::pointFragmentShader)

    internal fun ensureBatchSize(size: Int) {
        if (batch.size < size) {
            batch.destroy()
            batch = PointBatch.create(size, session = Session.root)
        }
    }

    init {
        val w = vertices.shadow.writer()
        w.rewind()
        val x = 0.0
        val y = 0.0
        val pa = Vector3(x, y, 0.0)

        val n = Vector3(0.0, 0.0, -1.0)
        w.apply {
            write(pa)
            write(n)

        }
        vertices.shadow.upload()
    }

    @JvmName("drawPoints2D")
    fun drawPoints(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>) {
        ensureBatchSize(positions.size)
        batch.geometry.put {
            for (i in positions.indices) {
                write(Vector3(positions[i].x, positions[i].y, 0.0))
            }
        }
        batch.drawStyle.put {
            for (i in positions.indices) {
                write(drawStyle)
            }
        }
        drawPoints(drawContext, drawStyle, batch, positions.size)
    }

    @JvmName("drawPoints3D")
    fun drawPoints(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector3>) {
        ensureBatchSize(positions.size)
        batch.geometry.put {
            for (i in positions.indices) {
                write(positions[i])
            }
        }
        batch.drawStyle.put {
            for (i in positions.indices) {
                write(drawStyle)
            }
        }
        drawPoints(drawContext, drawStyle, batch, positions.size)
    }

    fun drawPoint(drawContext: DrawContext,
                  drawStyle: DrawStyle, x: Double, y: Double, z: Double) {
        ensureBatchSize(1)

        val batch = singleBatches[count.mod(singleBatches.size)]

        batch.geometry.put {
            write(Vector3(x, y, z))
        }
        batch.drawStyle.put {
            write(drawStyle)
        }
        drawPoints(drawContext, drawStyle, batch, 1)
    }


    fun drawPoints(drawContext: DrawContext, drawStyle: DrawStyle, batch: PointBatch, count: Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, listOf(vertices.vertexFormat), listOf(batch.geometry.vertexFormat, batch.drawStyle.vertexFormat))
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(shader, listOf(vertices), listOf(batch.drawStyle, batch.geometry) + (drawStyle.shadeStyle?.attributes
                ?: emptyList()), DrawPrimitive.POINTS, 0, 1, 0, count)
        shader.end()
    }
}