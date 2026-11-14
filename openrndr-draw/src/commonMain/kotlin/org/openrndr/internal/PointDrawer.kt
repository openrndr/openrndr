package org.openrndr.internal

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.jvm.JvmName

/**
 * A utility class for rendering points in 2D and 3D space using custom styles, shaders, and batching.
 * Optimizes point rendering by handling vertex buffers and batching mechanisms.
 */
class PointDrawer {
    val vertices: VertexBuffer = VertexBuffer.createDynamic(VertexFormat().apply {
        position(3)
        normal(3)
    }, 1)

    internal var batch = PointBatch.create(10_000, session = Session.root)

    /**
     * Batches for lists of up to [POOLED_BATCH_SIZE] elements. Each call writes the next one, so it does not wait for
     * the GPU to finish reading a batch an earlier call in the frame wrote.
     */
    private val pooledBatches = arrayOfNulls<PointBatch>(DrawerConfiguration.vertexBufferMultiBufferCount)
    private var pooledBatchIndex = 0

    /** A batch to write [size] elements into */
    internal fun batchFor(size: Int): PointBatch {
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
        return PointBatch.create(pooledBatchCapacity(size), session = Session.root).also { pooledBatches[index] = it }
    }
    private var count = 0


    private val singleBatches = (0 until DrawerConfiguration.vertexBufferMultiBufferCount).map { PointBatch.create(1) }


    private val linear = LinearColors()

    /** writes the style attributes of [drawStyle] for [count] points of [batch] */
    private fun writeStyles(batch: PointBatch, drawStyle: DrawStyle, count: Int) {
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

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators("point",
            vsGenerator = Driver.instance.shaderGenerators::pointVertexShader,
            fsGenerator = Driver.instance.shaderGenerators::pointFragmentShader)

    internal fun ensureBatchSize(size: Int) {
        if (batch.size < size) {
            batch.destroy()
            batch = PointBatch.create(size, session = Session.root)
        }
    }

    private val vertexFormats = listOf(vertices.vertexFormat)
    private val vertexBuffers = listOf(vertices)
    private val instanceFormats = listOf(pointFormat, drawStyleFormat)

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

    /**
     * Draws a list of 2D points using the specified drawing context and style.
     *
     * @param drawContext The drawing context that provides transformation matrices and rendering parameters.
     * @param drawStyle The style that defines the visual appearance of the points, such as color and stroke settings.
     * @param positions The list of 2D points to be drawn, each represented as a `Vector2`.
     */
    @JvmName("drawPoints2D")
    fun drawPoints(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector2>) {
        val batch = batchFor(positions.size)
        batch.geometry.putElements(positions.size) {
            for (i in positions.indices) {
                val position = positions[i]
                write(position.x.toFloat(), position.y.toFloat(), 0.0f)
            }
        }
        writeStyles(batch, drawStyle, positions.size)
        drawPoints(drawContext, drawStyle, batch, positions.size)
    }

    /**
     * Draws a list of 3D points using the specified drawing context and style.
     *
     * @param drawContext The drawing context that provides transformation matrices and rendering parameters.
     * @param drawStyle The style that defines the visual appearance of the points, such as color and stroke settings.
     * @param positions The list of 3D points to be drawn, each represented as a `Vector3`.
     */
    @JvmName("drawPoints3D")
    fun drawPoints(drawContext: DrawContext, drawStyle: DrawStyle, positions: List<Vector3>) {
        val batch = batchFor(positions.size)
        batch.geometry.putElements(positions.size) {
            for (i in positions.indices) {
                write(positions[i])
            }
        }
        writeStyles(batch, drawStyle, positions.size)
        drawPoints(drawContext, drawStyle, batch, positions.size)
    }

    /**
     * Draws a single 3D point using the specified drawing context and style at the given coordinates.
     *
     * @param drawContext The drawing context that provides transformation matrices and rendering parameters.
     * @param drawStyle The style that defines the visual appearance of the point, such as color and stroke settings.
     * @param x The x-coordinate of the point in 3D space.
     * @param y The y-coordinate of the point in 3D space.
     * @param z The z-coordinate of the point in 3D space.
     */
    fun drawPoint(drawContext: DrawContext,
                  drawStyle: DrawStyle, x: Double, y: Double, z: Double) {
        val batch = singleBatches[count.mod(singleBatches.size)]

        batch.geometry.putElements(1) {
            write(x.toFloat(), y.toFloat(), z.toFloat())
        }
        writeStyles(batch, drawStyle, 1)
        drawPoints(drawContext, drawStyle, batch, 1)
        count++
    }


    /**
     * Draws a batch of points using the specified drawing context and style.
     *
     * @param drawContext The drawing context that provides transformation matrices and rendering parameters.
     * @param drawStyle The style that defines the visual appearance of the points, such as color and stroke settings.
     * @param batch A batch containing the geometry and associated drawing styles for the points.
     * @param count The number of points to be drawn from the batch.
     */
    fun drawPoints(drawContext: DrawContext, drawStyle: DrawStyle, batch: PointBatch, count: Int) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexFormats, instanceFormats)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(shader, vertexBuffers, withShadeStyleAttributes(drawStyle, batch.instanceAttributes),
            DrawPrimitive.POINTS, 0, 1, 0, count)
        shader.end()
    }
}