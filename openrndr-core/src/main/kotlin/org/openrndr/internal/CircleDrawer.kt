package org.openrndr.internal

import mu.KotlinLogging
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle

private val logger = KotlinLogging.logger {}

class CircleDrawer {
    private val vertices: VertexBuffer = VertexBuffer.createDynamic(VertexFormat().apply {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, 6)

    private val instanceFormat = VertexFormat().apply {
        attribute("offset", VertexElementType.VECTOR3_FLOAT32)
        attribute("radius", VertexElementType.FLOAT32)
    }

    private var instanceAttributes = VertexBuffer.createDynamic(instanceFormat, 10_000)

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(Driver.instance.shaderGenerators::circleVertexShader,
            Driver.instance.shaderGenerators::circleFragmentShader)

    private fun assertInstanceSize(size: Int) {
        if (instanceAttributes.vertexCount < size) {
            logger.debug {
                "resizing buffer from ${instanceAttributes.vertexCount} to $size"
            }
            instanceAttributes.destroy()
            instanceAttributes = vertexBuffer(instanceFormat, size)
        }
    }

    init {
        val w = vertices.shadow.writer()

        w.rewind()
        val x = 0.0
        val y = 0.0
        val radius = 1.0
        val pa = Vector3(x - radius, y - radius, 0.0)
        val pb = Vector3(x + radius, y - radius, 0.0)
        val pc = Vector3(x + radius, y + radius, 0.0)
        val pd = Vector3(x - radius, y + radius, 0.0)

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

    fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>, radii: List<Double>) {
        assertInstanceSize(positions.size)
        instanceAttributes.shadow.writer().apply {
            rewind()
            for (i in positions.indices) {
                write(Vector3(positions[i].x, positions[i].y, 0.0))
                write(radii[i].toFloat())
            }
        }
        instanceAttributes.shadow.uploadElements(0, positions.size)
        drawCircles(drawContext, drawStyle, positions.size)
    }

    fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>, radius: Double) {
        assertInstanceSize(positions.size)
        instanceAttributes.shadow.writer().apply {
            rewind()
            positions.forEach {
                write(Vector3(it.x, it.y, 0.0))
                write(radius.toFloat())
            }
        }
        instanceAttributes.shadow.uploadElements(0, positions.size)
        drawCircles(drawContext, drawStyle, positions.size)
    }

    fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, circles: List<Circle>) {
        assertInstanceSize(circles.size)
        instanceAttributes.shadow.writer().apply {
            rewind()
            circles.forEach {
                write(Vector3(it.center.x, it.center.y, 0.0))
                write(it.radius.toFloat())
            }
        }
        instanceAttributes.shadow.uploadElements(0, circles.size)
        drawCircles(drawContext, drawStyle, circles.size)
    }

    fun drawCircle(drawContext: DrawContext,
                   drawStyle: DrawStyle, x: Double, y: Double, radius: Double) {
        assertInstanceSize(1)
        instanceAttributes.shadow.writer().apply {
            rewind()
            write(Vector3(x, y, 0.0))
            write(radius.toFloat())
        }
        instanceAttributes.shadow.uploadElements(0, 1)
        drawCircles(drawContext, drawStyle, 1)
    }

    private fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, count: Int) {
        drawCircles(drawContext, drawStyle, instanceAttributes, count)
    }

    fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, instanceAttributes: VertexBuffer, count: Int) {
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