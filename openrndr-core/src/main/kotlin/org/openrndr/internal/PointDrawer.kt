package org.openrndr.internal

import mu.KotlinLogging
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle

private val logger = KotlinLogging.logger {}

class PointDrawer {
    val vertices: VertexBuffer = VertexBuffer.createDynamic(VertexFormat().apply {
        position(3)
        normal(3)
    }, 1)

    private val instanceFormat = VertexFormat().apply {
        attribute("offset", VertexElementType.VECTOR3_FLOAT32)
    }

    private var instanceAttributes = VertexBuffer.createDynamic(instanceFormat, 10_000)

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(Driver.instance.shaderGenerators::pointVertexShader,
            Driver.instance.shaderGenerators::pointFragmentShader)

    private fun assertInstanceSize(size: Int) {
        if (instanceAttributes.vertexCount < size) {
            logger.debug {
                "resizing buffer from ${instanceAttributes.vertexCount} to $size"
            }
            instanceAttributes.destroy()
            instanceAttributes = vertexBuffer(instanceFormat, size).apply {
                Session.active.untrack(this)
            }
        }
    }

    init {
        val w = vertices.shadow.writer()
        w.rewind()
        val x = 0.0
        val y = 0.0
        val pa = Vector3(x , y , 0.0)

        val n = Vector3(0.0, 0.0, -1.0)
        w.apply {
            write(pa)
            write(n)

        }
        vertices.shadow.upload()
    }

    fun drawPoints(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>) {
        assertInstanceSize(positions.size)
        instanceAttributes.shadow.writer().apply {
            rewind()
            for (i in 0 until positions.size) {
                write(Vector3(positions[i].x, positions[i].y, 0.0))
            }
        }
        instanceAttributes.shadow.uploadElements(0, positions.size)
        drawPoints(drawContext, drawStyle, positions.size)
    }


    fun drawPoint(drawContext: DrawContext,
                  drawStyle: DrawStyle, x: Double, y: Double, z: Double) {
        assertInstanceSize(1)
        instanceAttributes.shadow.writer().apply {
            rewind()
            write(Vector3(x, y, z))
        }
        instanceAttributes.shadow.uploadElements(0, 1)
        drawPoints(drawContext, drawStyle, 1)
    }

    private fun drawPoints(drawContext: DrawContext, drawStyle: DrawStyle, count: Int) {
        drawPoints(drawContext, drawStyle, instanceAttributes, count)
    }

    fun drawPoints(drawContext: DrawContext, drawStyle: DrawStyle, instanceAttributes: VertexBuffer, count: Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, listOf(vertices.vertexFormat), listOf(instanceAttributes.vertexFormat))
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(shader, listOf(vertices), listOf(instanceAttributes) + (drawStyle.shadeStyle?.attributes
                ?: emptyList()), DrawPrimitive.POINTS, 0, 1, count)
        shader.end()
    }
}