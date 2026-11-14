package org.openrndr.internal

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.min

/**
 * This class is responsible for efficiently managing and rendering 2D rectangles, supporting
 * various configurations, including batch rendering, custom dimensions, and draw styles.
 *
 * The rendering process is optimized by leveraging dynamic vertex buffers, shader management,
 * and batching mechanisms for handling multiple rectangles in a single draw pass.
 */
class RectangleDrawer {
    private val vertices: VertexBuffer = VertexBuffer.createDynamic(VertexFormat().apply {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, 6, Session.root)

    private var count = 0

    private val singleBatches = (0 until DrawerConfiguration.vertexBufferMultiBufferCount).map { RectangleBatch.create(1) }

    internal var batch = RectangleBatch.create(10_000, Session.root)

    /**
     * Batches for lists of up to [POOLED_BATCH_SIZE] elements. Each call writes the next one, so it does not wait for
     * the GPU to finish reading a batch an earlier call in the frame wrote.
     */
    private val pooledBatches = arrayOfNulls<RectangleBatch>(DrawerConfiguration.vertexBufferMultiBufferCount)
    private var pooledBatchIndex = 0

    /** A batch to write [size] elements into */
    internal fun batchFor(size: Int): RectangleBatch {
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
        pooled?.destroy()
        return RectangleBatch.create(pooledBatchCapacity(size), session = Session.root).also { pooledBatches[index] = it }
    }

    private val vertexFormats = listOf(vertices.vertexFormat)
    private val vertexBuffers = listOf(vertices)
    private val instanceFormats = listOf(rectangleFormat, drawStyleFormat)
    private val linear = LinearColors()

    /** writes the style attributes of [drawStyle] for [count] rectangles of [batch] */
    private fun writeStyles(batch: RectangleBatch, drawStyle: DrawStyle, count: Int) {
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
        val batch = batchFor(positions.size)
        require(positions.size == dimensions.size) {
            "`positions.size` and `dimensions.size` must be equal in drawRectangles()"
        }
        batch.geometry.putElements(dimensions.size) {
            for (i in dimensions.indices) {
                val sz = dimensions[i]
                write((positions[i].x + min(0.0, sz.x)).toFloat(), (positions[i].y + min(0.0, sz.y)).toFloat(), 0.0f)
                write(abs(sz.x).toFloat(), abs(sz.y).toFloat(), 0.0f)
            }
        }
        writeStyles(batch, drawStyle, positions.size)
        drawRectangles(drawContext, drawStyle, batch, positions.size)
    }

    fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>, width: Double, height: Double) {
        val batch = batchFor(positions.size)
        val w = abs(width).toFloat()
        val h = abs(height).toFloat()
        batch.geometry.putElements(positions.size) {
            for (i in positions.indices) {
                val position = positions[i]
                write((position.x + min(0.0, width)).toFloat(), (position.y + min(0.0, height)).toFloat(), 0.0f)
                write(w, h, 0.0f)
            }
        }
        writeStyles(batch, drawStyle, positions.size)
        drawRectangles(drawContext, drawStyle, batch, positions.size)
    }

    fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, rectangles: List<Rectangle>) {
        val batch = batchFor(rectangles.size)
        batch.geometry.putElements(rectangles.size) {
            for (i in rectangles.indices) {
                val it = rectangles[i]
                write((it.x + min(0.0, it.width)).toFloat(), (it.y + min(0.0, it.height)).toFloat(), 0.0f)
                write(abs(it.width).toFloat(), abs(it.height).toFloat(), 0.0f)
            }
        }
        writeStyles(batch, drawStyle, rectangles.size)
        drawRectangles(drawContext, drawStyle, batch, rectangles.size)
    }

    fun drawRectangle(drawContext: DrawContext,
                      drawStyle: DrawStyle, x: Double, y: Double, width: Double, height: Double) {
        val batch = singleBatches[count.mod(singleBatches.size)]

        batch.geometry.putElements(1) {
            write(
                (x + min(0.0, width)).toFloat(),
                (y + min(0.0, height)).toFloat(), 0.0f)
            write(abs(width).toFloat(), abs(height).toFloat(), 0.0f)
        }
        writeStyles(batch, drawStyle, 1)
        drawRectangles(drawContext, drawStyle, batch, 1)
        count++
    }

    fun drawRectangles(drawContext: DrawContext, drawStyle: DrawStyle, batch: RectangleBatch, count: Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexFormats, instanceFormats)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(shader, vertexBuffers, withShadeStyleAttributes(drawStyle, batch.instanceAttributes),
            DrawPrimitive.TRIANGLES, 0, 6, 0, count)
        shader.end()
    }
}