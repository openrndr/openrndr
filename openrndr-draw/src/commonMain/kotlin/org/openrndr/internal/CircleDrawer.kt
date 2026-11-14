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

    /**
     * Batches for lists of up to [POOLED_BATCH_SIZE] elements. Each call writes the next one, so it does not wait for
     * the GPU to finish reading a batch an earlier call in the frame wrote.
     */
    private val pooledBatches = arrayOfNulls<CircleBatch>(DrawerConfiguration.vertexBufferMultiBufferCount)
    private var pooledBatchIndex = 0

    /** A batch to write [size] elements into */
    internal fun batchFor(size: Int): CircleBatch {
        if (size > POOLED_BATCH_SIZE) {
            ensureBatchSize(size)
            return batch
        }
        val index = pooledBatchIndex
        pooledBatchIndex = (pooledBatchIndex + 1) % pooledBatches.size
        val pooled = pooledBatches[index]
        if (pooled != null && pooled.size >= size) {
            return pooled
        }
        pooled?.close()
        return CircleBatch.create(pooledBatchCapacity(size), session = Session.root).also { pooledBatches[index] = it }
    }
    private var count = 0

    private val singleBatches = (0 until DrawerConfiguration.vertexBufferMultiBufferCount).map { CircleBatch.create(1) }

    private val vertexFormats = listOf(vertices.vertexFormat)
    private val vertexBuffers = listOf(vertices)
    private val instanceFormats = listOf(circleFormat, drawStyleFormat)
    private val linear = LinearColors()

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
        val batch = batchFor(positions.size)
        batch.geometry.putElements(positions.size) {
            for (i in positions.indices) {
                val position = positions[i]
                val radius = abs(radii[i]).toFloat()
                write(position.x.toFloat(), position.y.toFloat(), 0.0f)
                write(radius, radius)
            }
        }
        writeStyles(batch, drawStyle, positions.size)
        drawCircles(drawContext, drawStyle, batch, positions.size)
    }

    fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>, radius: Double) {
        val batch = batchFor(positions.size)
        val r = abs(radius).toFloat()
        batch.geometry.putElements(positions.size) {
            for (i in positions.indices) {
                val position = positions[i]
                write(position.x.toFloat(), position.y.toFloat(), 0.0f)
                write(r, r)
            }
        }
        writeStyles(batch, drawStyle, positions.size)
        drawCircles(drawContext, drawStyle, batch, positions.size)
    }

    fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, circles: List<Circle>) {
        val batch = batchFor(circles.size)
        batch.geometry.putElements(circles.size) {
            for (i in circles.indices) {
                val circle = circles[i]
                val radius = abs(circle.radius).toFloat()
                write(circle.center.x.toFloat(), circle.center.y.toFloat(), 0.0f)
                write(radius, radius)
            }
        }
        writeStyles(batch, drawStyle, circles.size)
        drawCircles(drawContext, drawStyle, batch, circles.size)
    }

    /** writes the style attributes of [drawStyle] for [count] circles of [batch] */
    private fun writeStyles(batch: CircleBatch, drawStyle: DrawStyle, count: Int) {
        val fill = linear.fill(drawStyle.fill) ?: ColorRGBa.TRANSPARENT
        val stroke = linear.stroke(drawStyle.stroke) ?: ColorRGBa.TRANSPARENT
        val weight = strokeWeightAttribute(drawStyle)
        batch.drawStyle.putElements(count) {
            for (i in 0 until count) {
                write(fill)
                write(stroke)
                write(weight)
            }
        }
    }


    fun drawCircle(
        drawContext: DrawContext,
        drawStyle: DrawStyle, x: Double, y: Double, radius: Double
    ) {

        val batch = singleBatches[count.mod(singleBatches.size)]

        batch.geometry.putElements(1) {
            val r = abs(radius).toFloat()
            write(x.toFloat(), y.toFloat(), 0.0f)
            write(r, r)
        }
        writeStyles(batch, drawStyle, 1)

        drawCircles(drawContext, drawStyle, batch, 1)
        count++
    }


    fun drawCircles(drawContext: DrawContext, drawStyle: DrawStyle, circleBatch: CircleBatch, count: Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexFormats, instanceFormats)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(
            shader,
            vertexBuffers,
            withShadeStyleAttributes(drawStyle, circleBatch.instanceAttributes),
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