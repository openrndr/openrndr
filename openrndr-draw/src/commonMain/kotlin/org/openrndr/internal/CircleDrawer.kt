package org.openrndr.internal

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import kotlin.math.abs


private val logger = KotlinLogging.logger {  }

/**
 * A utility class designed for efficient rendering of circles.
 *
 * This class manages the creation and maintenance of vertex buffers, shader programs,
 * and batch rendering for drawing circles. It provides various methods to render
 * single or multiple circles with configurable positions, radii, and styles.
 *
 * The `CircleDrawer` class is optimized for performance by batching rendering
 * operations and dynamically resizing buffers as needed.
 */
class CircleDrawer {
    private val vertices: VertexBuffer = VertexBuffer.createDynamic(VertexFormat().apply {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, 6, Session.root)

    internal var batch = CircleBatch.create(10_000)
    private var count = 0

    private val singleBatches = (0 until DrawerConfiguration.vertexBufferMultiBufferCount).map { CircleBatch.create(1) }

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "circle",
        vsGenerator = Driver.instance.shaderGenerators::circleVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::circleFragmentShader
    )

    internal fun ensureBatchSize(size: Int) {
        if (batch.size < size) {
            batch.close()
            batch = CircleBatch.create(size, session = Session.root)
        }
    }

    init {
        logger.debug { "Number of batches: ${singleBatches.size}" }
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
        ensureBatchSize(positions.size)
        batch.geometry.shadow.writer().apply {
            rewind()
            for (i in positions.indices) {
                write(Vector3(positions[i].x, positions[i].y, 0.0))
                write(Vector2(abs(radii[i])))
            }
        }
        batch.geometry.shadow.uploadElements(0, positions.size)

        batch.drawStyle.shadow.writer().apply {
            rewind()
            for (i in positions.indices) {
                write(drawStyle)
            }
        }
        batch.drawStyle.shadow.uploadElements(0, positions.size)
        drawCircles(drawContext, drawStyle, positions.size)
    }

    fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>, radius: Double) {
        ensureBatchSize(positions.size)
        batch.geometry.shadow.writer().apply {
            rewind()
            for (i in positions.indices) {
                write(Vector3(positions[i].x, positions[i].y, 0.0))
                write(Vector2(abs(radius)))
            }
        }
        batch.geometry.shadow.uploadElements(0, positions.size)

        batch.drawStyle.shadow.writer().apply {
            rewind()
            for (i in positions.indices) {
                write(drawStyle)
            }
        }
        batch.drawStyle.shadow.uploadElements(0, positions.size)
        drawCircles(drawContext, drawStyle, positions.size)
    }

    fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, circles: List<Circle>) {
        ensureBatchSize(circles.size)
        batch.geometry.shadow.writer().apply {
            rewind()
            for (i in circles.indices) {
                write(circles[i].center.xy0)
                write(Vector2(abs(circles[i].radius)))
            }
        }
        batch.geometry.shadow.uploadElements(0, circles.size)

        batch.drawStyle.shadow.writer().apply {
            rewind()
            for (i in circles.indices) {
                write(drawStyle)
            }
        }
        batch.drawStyle.shadow.uploadElements(0, circles.size)

        drawCircles(drawContext, drawStyle, circles.size)
    }


    fun drawCircle(
        drawContext: DrawContext,
        drawStyle: DrawStyle, x: Double, y: Double, radius: Double
    ) {

        val batch = singleBatches[count.mod(singleBatches.size)]

        batch.geometry.shadow.writer().apply {
            rewind()
            write(Vector3(x, y, 0.0))
            write(Vector2(abs(radius)))
        }
        batch.geometry.shadow.uploadElements(0, 1)

        batch.drawStyle.shadow.writer().apply {
            rewind()
            write(drawStyle.fill?.toLinear() ?: ColorRGBa.TRANSPARENT)
            write(drawStyle.stroke?.toLinear() ?: ColorRGBa.TRANSPARENT)
            val weight = if (drawStyle.stroke == null || drawStyle.stroke?.alpha == 0.0) 0.0 else
                drawStyle.strokeWeight
            write(weight.toFloat())
        }
        batch.drawStyle.shadow.uploadElements(0, 1)

        drawCircles(drawContext, drawStyle, batch, 1)
        count++
    }

    private fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, count: Int) {
        drawCircles(drawContext, drawStyle, batch, count)
    }

    fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, circleBatch: CircleBatch, count: Int) {
        val instanceAttributes = listOf(circleBatch.geometry, circleBatch.drawStyle)
        val instanceAttributeFormats = listOf(circleBatch.geometry.vertexFormat, circleBatch.drawStyle.vertexFormat)

        val shader = shaderManager.shader(
            drawStyle.shadeStyle,
            listOf(vertices.vertexFormat),
            instanceAttributeFormats
        )
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(
            shader,
            listOf(vertices),
            instanceAttributes + (drawStyle.shadeStyle?.attributes.orEmpty()),
            DrawPrimitive.TRIANGLES,
            0,
            6,
            0,
            count,
            verticesPerPatch = 0
        )
        shader.end()
    }
}