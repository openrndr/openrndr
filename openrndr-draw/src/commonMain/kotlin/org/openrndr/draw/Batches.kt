package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.jvm.JvmName

/**
 * Writes the specified `DrawStyle` properties to the buffer. This includes
 * linearized fill color, linearized stroke color, and an adjusted stroke weight
 * based on the presence and transparency of the stroke.
 *
 * @param drawStyle the `DrawStyle` instance containing fill color, stroke color,
 * and stroke weight to be written to the buffer
 */
fun BufferWriter.write(drawStyle: DrawStyle) {
    write(DrawStyleAttributes(drawStyle))
}

/**
 * The per instance attributes of a [DrawStyle] in a batch, its colors converted to linear once for all instances
 */
internal class DrawStyleAttributes(drawStyle: DrawStyle) {
    val fill = drawStyle.fill?.toLinear() ?: ColorRGBa.TRANSPARENT
    val stroke = drawStyle.stroke?.toLinear() ?: ColorRGBa.TRANSPARENT
    val strokeWeight = (if (drawStyle.stroke == null || drawStyle.stroke?.alpha == 0.0) 0.0 else drawStyle.strokeWeight).toFloat()
}

internal fun BufferWriter.write(attributes: DrawStyleAttributes) {
    write(attributes.fill)
    write(attributes.stroke)
    write(attributes.strokeWeight)
}

/**
 * Converts the fill and stroke colors of draw styles to linear, like [DrawStyleAttributes] does. The conversions of
 * the last fill and stroke are reused, colors are immutable.
 */
internal class LinearColors {
    private var fill: ColorRGBa? = null
    private var linearFill: ColorRGBa? = null
    private var stroke: ColorRGBa? = null
    private var linearStroke: ColorRGBa? = null

    /** [color] converted to linear, null stays null */
    fun fill(color: ColorRGBa?): ColorRGBa? {
        if (color !== fill) {
            fill = color
            linearFill = color?.toLinear()
        }
        return linearFill
    }

    /** [color] converted to linear, null stays null */
    fun stroke(color: ColorRGBa?): ColorRGBa? {
        if (color !== stroke) {
            stroke = color
            linearStroke = color?.toLinear()
        }
        return linearStroke
    }

    /** writes the instance attributes of [drawStyle], those that [DrawStyleAttributes] holds */
    fun write(writer: BufferWriter, drawStyle: DrawStyle) {
        writer.write(fill(drawStyle.fill) ?: ColorRGBa.TRANSPARENT)
        writer.write(stroke(drawStyle.stroke) ?: ColorRGBa.TRANSPARENT)
        writer.write(strokeWeightAttribute(drawStyle))
    }
}

/** the stroke weight instance attribute of [drawStyle], 0 without a visible stroke */
internal fun strokeWeightAttribute(drawStyle: DrawStyle): Float =
    (if (drawStyle.stroke == null || drawStyle.stroke?.alpha == 0.0) 0.0 else drawStyle.strokeWeight).toFloat()

/**
 * Writes [count] elements from the start of the buffer with [write] and uploads them, like [VertexBuffer.put] does,
 * without making a lambda
 */
internal inline fun VertexBuffer.putElements(count: Int, write: BufferWriter.() -> Unit) {
    val writer = shadow.writer()
    writer.rewind()
    writer.write()
    shadow.uploadElements(0, count)
}

/** [instances] followed by the attributes of the shade style of [drawStyle] */
internal fun withShadeStyleAttributes(drawStyle: DrawStyle, instances: List<VertexBuffer>): List<VertexBuffer> {
    val attributes = drawStyle.shadeStyle?.attributes
    return if (attributes.isNullOrEmpty()) instances else instances + attributes
}

/** grows [array] to hold at least [size] elements */
private fun grow(array: DoubleArray, size: Int): DoubleArray =
    if (array.size >= size) array else array.copyOf(maxOf(size, array.size * 2))

/** grows [array] to hold at least [size] elements */
private fun grow(array: Array<ColorRGBa?>, size: Int): Array<ColorRGBa?> =
    if (array.size >= size) array else array.copyOf(maxOf(size, array.size * 2))

/** The largest number of elements drawn from a pooled batch, larger lists are drawn from a batch of their own */
internal const val POOLED_BATCH_SIZE = 1024

/** The capacity of a pooled batch for [size] elements: a power of two of at least 16 */
internal fun pooledBatchCapacity(size: Int): Int {
    var capacity = 16
    while (capacity < size) capacity *= 2
    return capacity
}

val drawStyleFormat = vertexFormat {
    attribute("fill", VertexElementType.VECTOR4_FLOAT32)
    attribute("stroke", VertexElementType.VECTOR4_FLOAT32)
    attribute("strokeWeight", VertexElementType.FLOAT32)
}

val circleFormat = vertexFormat {
    attribute("offset", VertexElementType.VECTOR3_FLOAT32)
    attribute("radius", VertexElementType.VECTOR2_FLOAT32)
}

val rectangleFormat = vertexFormat {
    attribute("offset", VertexElementType.VECTOR3_FLOAT32)
    attribute("dimensions", VertexElementType.VECTOR2_FLOAT32)
    attribute("rotation", VertexElementType.FLOAT32)
}

val pointFormat = vertexFormat {
    attribute("offset", VertexElementType.VECTOR3_FLOAT32)
}

/**
 * Stored circle batch
 */
class CircleBatch(val geometry: VertexBuffer, val drawStyle: VertexBuffer) : AutoCloseable {
    init {
        require(geometry.vertexFormat == circleFormat)
        require(drawStyle.vertexFormat == drawStyleFormat)
        require(geometry.vertexCount == drawStyle.vertexCount)
    }

    /** the instance attributes of the batch, in the order the circle drawer draws them */
    internal val instanceAttributes = listOf(geometry, drawStyle)

    val size
        get() = geometry.vertexCount

    companion object {
        fun create(size: Int, session: Session? = Session.active): CircleBatch {
            return CircleBatch(vertexBuffer(circleFormat, size, session), vertexBuffer(drawStyleFormat, size, session))
        }
    }

    override fun close() {
        geometry.destroy()
        drawStyle.destroy()
    }
}


open class BatchBuilder(val drawer: Drawer) {
    /**
     * Active fill color
     */
    var fill = drawer.fill

    /**
     * Active stroke color
     */
    var stroke = drawer.stroke

    /**
     * Active stroke weight
     */
    var strokeWeight = drawer.strokeWeight

    /** the linear colors of [fill] and [stroke] */
    internal val linear = LinearColors()

    /** is the builder building a batch, see [Drawer] */
    internal var building = false

    /** prepares the builder to build a new batch, with the colors and stroke weight of the drawer */
    internal open fun reset() {
        fill = drawer.fill
        stroke = drawer.stroke
        strokeWeight = drawer.strokeWeight
    }
}

/**
 * Builder for stored circle batches
 */
class CircleBatchBuilder(drawer: Drawer) : BatchBuilder(drawer) {
    class Entry(
        val fill: ColorRGBa?,
        val stroke: ColorRGBa?,
        val strokeWeight: Double,
        val offset: Vector3,
        val radius: Vector2
    )

    private var count = 0

    /** per circle: the offset (x, y, z), the radius (x, y) and the stroke weight */
    private var geometry = DoubleArray(16 * STRIDE)

    /** the linear fill and stroke colors of the circles */
    private var fills = arrayOfNulls<ColorRGBa>(16)
    private var strokes = arrayOfNulls<ColorRGBa>(16)

    /**
     * The circles of the batch
     */
    val entries: List<Entry> = object : AbstractList<Entry>() {
        override val size: Int get() = count

        override fun get(index: Int): Entry {
            if (index !in 0 until count) throw IndexOutOfBoundsException("index $index, size $count")
            val o = index * STRIDE
            return Entry(
                fills[index], strokes[index], geometry[o + 5],
                Vector3(geometry[o], geometry[o + 1], geometry[o + 2]), Vector2(geometry[o + 3], geometry[o + 4])
            )
        }
    }

    private fun add(x: Double, y: Double, z: Double, radiusX: Double, radiusY: Double, fill: ColorRGBa?, stroke: ColorRGBa?) {
        geometry = grow(geometry, (count + 1) * STRIDE)
        fills = grow(fills, count + 1)
        strokes = grow(strokes, count + 1)
        val o = count * STRIDE
        geometry[o] = x
        geometry[o + 1] = y
        geometry[o + 2] = z
        geometry[o + 3] = radiusX
        geometry[o + 4] = radiusY
        geometry[o + 5] = strokeWeight
        fills[count] = fill
        strokes[count] = stroke
        count++
    }

    override fun reset() {
        super.reset()
        count = 0
    }

    /**
     * Add a circle to the batch
     */
    fun circle(x: Double, y: Double, radius: Double) {
        add(x, y, 0.0, radius, radius, linear.fill(fill), linear.stroke(stroke))
    }

    /**
     * Add a circle to the batch
     */
    fun circle(position: Vector2, radius: Double) {
        add(position.x, position.y, 0.0, radius, radius, linear.fill(fill), linear.stroke(stroke))
    }

    /**
     * Add a circle to the batch
     */
    fun circle(circle: Circle) {
        add(circle.center.x, circle.center.y, 0.0, circle.radius, circle.radius, linear.fill(fill), linear.stroke(stroke))
    }

    /**
     * Add a circle to the batch
     */
    fun circles(circles: List<Circle>) {
        for (circle in circles) {
            circle(circle)
        }
    }

    /**
     * Add circles to the batch
     */
    fun circles(centers: List<Vector2>, radius: Double) {
        val lfill = linear.fill(fill)
        val lstroke = linear.stroke(stroke)
        for (i in centers.indices) {
            val center = centers[i]
            add(center.x, center.y, 0.0, radius, radius, lfill, lstroke)
        }
    }

    /**
     * Add circles to the batch
     */
    fun circles(centers: List<Vector2>, radii: List<Double>) {
        require(centers.size == radii.size)
        val lfill = linear.fill(fill)
        val lstroke = linear.stroke(stroke)
        for (i in centers.indices) {
            val center = centers[i]
            add(center.x, center.y, 0.0, radii[i], radii[i], lfill, lstroke)
        }
    }

    /**
     * Generate the stored batch
     */
    fun batch(existingBatch: CircleBatch? = null): CircleBatch {
        val geometry = existingBatch?.geometry ?: vertexBuffer(circleFormat, count)
        geometry.put {
            val g = this@CircleBatchBuilder.geometry
            for (i in 0 until count) {
                val o = i * STRIDE
                write(g[o].toFloat(), g[o + 1].toFloat(), g[o + 2].toFloat())
                write(g[o + 3].toFloat(), g[o + 4].toFloat())
            }
        }

        val drawStyle = existingBatch?.drawStyle ?: vertexBuffer(drawStyleFormat, count)
        drawStyle.put {
            for (i in 0 until count) {
                val stroke = strokes[i]
                write(fills[i] ?: ColorRGBa.TRANSPARENT)
                write(stroke ?: ColorRGBa.TRANSPARENT)
                write(if (stroke == null || stroke.alpha == 0.0) 0.0f else this@CircleBatchBuilder.geometry[i * STRIDE + 5].toFloat())
            }
        }
        return existingBatch ?: CircleBatch(geometry, drawStyle)
    }

    private companion object {
        const val STRIDE = 6
    }
}

/**
 * Create a stored batch of circles
 */
fun Drawer.circleBatch(build: CircleBatchBuilder.() -> Unit): CircleBatch {
    val circleBatchBuilder = CircleBatchBuilder(this)
    circleBatchBuilder.build()
    return circleBatchBuilder.batch()
}


class RectangleBatch(val geometry: VertexBuffer, val drawStyle: VertexBuffer): AutoCloseable {
    init {
        require(geometry.vertexFormat == rectangleFormat)
        require(drawStyle.vertexFormat == drawStyleFormat)
        require(geometry.vertexCount == drawStyle.vertexCount)
    }

    /** the instance attributes of the batch, in the order the rectangle drawer draws them */
    internal val instanceAttributes = listOf(drawStyle, geometry)

    val size
        get() = geometry.vertexCount

    companion object {
        fun create(size: Int, session: Session? = Session.active): RectangleBatch {
            return RectangleBatch(
                vertexBuffer(rectangleFormat, size, session),
                vertexBuffer(drawStyleFormat, size, session)
            )
        }
    }

    /**
     * Destroy the stored batch
     */
    fun destroy() {
        geometry.destroy()
        drawStyle.destroy()
    }

    override fun close() {
        destroy()
    }
}


class RectangleBatchBuilder(drawer: Drawer) : BatchBuilder(drawer) {
    class Entry(
        val fill: ColorRGBa?,
        val stroke: ColorRGBa?,
        val strokeWeight: Double,
        val offset: Vector3,
        val dimensions: Vector2,
        val rotation: Double
    )

    private var count = 0

    /** per rectangle: the offset (x, y, z), the dimensions (x, y), the rotation and the stroke weight */
    private var geometry = DoubleArray(16 * STRIDE)

    /** the linear fill and stroke colors of the rectangles */
    private var fills = arrayOfNulls<ColorRGBa>(16)
    private var strokes = arrayOfNulls<ColorRGBa>(16)

    /**
     * The rectangles of the batch
     */
    val entries: List<Entry> = object : AbstractList<Entry>() {
        override val size: Int get() = count

        override fun get(index: Int): Entry {
            if (index !in 0 until count) throw IndexOutOfBoundsException("index $index, size $count")
            val o = index * STRIDE
            return Entry(
                fills[index], strokes[index], geometry[o + 6],
                Vector3(geometry[o], geometry[o + 1], geometry[o + 2]), Vector2(geometry[o + 3], geometry[o + 4]),
                geometry[o + 5]
            )
        }
    }

    private fun add(x: Double, y: Double, width: Double, height: Double, rotation: Double) {
        geometry = grow(geometry, (count + 1) * STRIDE)
        fills = grow(fills, count + 1)
        strokes = grow(strokes, count + 1)
        val o = count * STRIDE
        geometry[o] = x
        geometry[o + 1] = y
        geometry[o + 2] = 0.0
        geometry[o + 3] = width
        geometry[o + 4] = height
        geometry[o + 5] = rotation
        geometry[o + 6] = strokeWeight
        fills[count] = linear.fill(fill)
        strokes[count] = linear.stroke(stroke)
        count++
    }

    override fun reset() {
        super.reset()
        count = 0
    }

    fun rectangle(x: Double, y: Double, width: Double, height: Double, rotationInDegrees: Double = 0.0) {
        add(x, y, width, height, rotationInDegrees)
    }

    fun rectangle(corner: Vector2, width: Double, height: Double, rotationInDegrees: Double = 0.0) {
        add(corner.x, corner.y, width, height, rotationInDegrees)
    }

    fun rectangle(rectangle: Rectangle, rotationInDegrees: Double = 0.0) {
        add(rectangle.corner.x, rectangle.corner.y, rectangle.width, rectangle.height, rotationInDegrees)
    }

    fun rectangles(rectangles: List<Rectangle>) {
        for (rectangle in rectangles) {
            rectangle(rectangle)
        }
    }

    /**
     * Generate the stored batch
     */
    fun batch(existingBatch: RectangleBatch? = null): RectangleBatch {
        val geometry = existingBatch?.geometry ?: vertexBuffer(rectangleFormat, count)
        geometry.put {
            val g = this@RectangleBatchBuilder.geometry
            for (i in 0 until count) {
                val o = i * STRIDE
                write(g[o].toFloat(), g[o + 1].toFloat(), g[o + 2].toFloat())
                write(g[o + 3].toFloat(), g[o + 4].toFloat())
                write(g[o + 5].toFloat())
            }
        }

        val drawStyle = existingBatch?.drawStyle ?: vertexBuffer(drawStyleFormat, count)
        drawStyle.put {
            for (i in 0 until count) {
                val stroke = strokes[i]
                write(fills[i] ?: ColorRGBa.TRANSPARENT)
                write(stroke ?: ColorRGBa.TRANSPARENT)
                write(if (stroke == null) 0.0f else this@RectangleBatchBuilder.geometry[i * STRIDE + 6].toFloat())
            }
        }
        return existingBatch ?: RectangleBatch(geometry, drawStyle)
    }

    private companion object {
        const val STRIDE = 7
    }
}

/**
 * Create a stored batch of rectangles
 */
fun Drawer.rectangleBatch(build: RectangleBatchBuilder.() -> Unit): RectangleBatch {
    val rectangleBatchBuilder = RectangleBatchBuilder(this)
    rectangleBatchBuilder.build()
    return rectangleBatchBuilder.batch()
}

class PointBatch(val geometry: VertexBuffer, val drawStyle: VertexBuffer) {
    init {
        require(geometry.vertexFormat == pointFormat)
        require(drawStyle.vertexFormat == drawStyleFormat)
        require(geometry.vertexCount == drawStyle.vertexCount)
    }

    /** the instance attributes of the batch, in the order the point drawer draws them */
    internal val instanceAttributes = listOf(drawStyle, geometry)

    val size
        get() = geometry.vertexCount

    companion object {
        fun create(size: Int, session: Session? = Session.active): PointBatch {
            return PointBatch(vertexBuffer(pointFormat, size, session), vertexBuffer(drawStyleFormat, size, session))
        }
    }

    /**
     * Destroy the stored batch
     */
    fun destroy() {
        geometry.destroy()
        drawStyle.destroy()
    }
}

/**
 * Create a stored batch of points
 */
fun Drawer.pointBatch(build: PointBatchBuilder.() -> Unit): PointBatch {
    val pointBatchBuilder = PointBatchBuilder(this)
    pointBatchBuilder.build()
    return pointBatchBuilder.batch()
}


class PointBatchBuilder(drawer: Drawer) : BatchBuilder(drawer) {
    class Entry(
        val fill: ColorRGBa?,
        val offset: Vector3
    )

    private var count = 0

    /** per point: the offset (x, y, z) */
    private var geometry = DoubleArray(16 * 3)

    /** the linear fill colors of the points */
    private var fills = arrayOfNulls<ColorRGBa>(16)

    /**
     * The points of the batch
     */
    val entries: List<Entry> = object : AbstractList<Entry>() {
        override val size: Int get() = count

        override fun get(index: Int): Entry {
            if (index !in 0 until count) throw IndexOutOfBoundsException("index $index, size $count")
            val o = index * 3
            return Entry(fills[index], Vector3(geometry[o], geometry[o + 1], geometry[o + 2]))
        }
    }

    private fun add(x: Double, y: Double, z: Double) {
        geometry = grow(geometry, (count + 1) * 3)
        fills = grow(fills, count + 1)
        val o = count * 3
        geometry[o] = x
        geometry[o + 1] = y
        geometry[o + 2] = z
        fills[count] = linear.fill(fill)
        count++
    }

    override fun reset() {
        super.reset()
        count = 0
    }

    fun point(x: Double, y: Double) {
        add(x, y, 0.0)
    }

    fun point(x: Double, y: Double, z: Double) {
        add(x, y, z)
    }

    fun point(position: Vector2) {
        add(position.x, position.y, 0.0)
    }

    fun point(position: Vector3) {
        add(position.x, position.y, position.z)
    }

    @JvmName("points3D")
    fun points(positions: List<Vector3>) {
        for (i in positions.indices) {
            point(positions[i])
        }
    }

    @JvmName("points2D")
    fun points(positions: List<Vector2>) {
        for (i in positions.indices) {
            point(positions[i])
        }
    }


    /**
     * Generate the stored batch
     */
    fun batch(existingBatch: PointBatch? = null): PointBatch {
        val geometry = existingBatch?.geometry ?: vertexBuffer(pointFormat, count)
        geometry.put {
            val g = this@PointBatchBuilder.geometry
            for (i in 0 until count) {
                write(g[i * 3].toFloat(), g[i * 3 + 1].toFloat(), g[i * 3 + 2].toFloat())
            }
        }

        val drawStyle = existingBatch?.drawStyle ?: vertexBuffer(drawStyleFormat, count)
        drawStyle.put {
            for (i in 0 until count) {
                val fill = fills[i] ?: ColorRGBa.TRANSPARENT
                write(fill)
                write(fill)
                write(1.0f)
            }
        }
        return existingBatch ?: PointBatch(geometry, drawStyle)
    }
}