package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle

fun BufferWriter.write(drawStyle: DrawStyle) {
    write(drawStyle.fill ?: ColorRGBa.TRANSPARENT)
    write(drawStyle.stroke ?: ColorRGBa.TRANSPARENT)
    write(drawStyle.strokeWeight.toFloat())
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

/**
 * Stored circle batch
 */
class CircleBatch(val geometry: VertexBuffer, val drawStyle: VertexBuffer) {
    init {
        require(geometry.vertexFormat == circleFormat)
        require(drawStyle.vertexFormat == drawStyleFormat)
        require(geometry.vertexCount == drawStyle.vertexCount)
    }

    val size
        get() = geometry.vertexCount

    companion object {
        fun create(size: Int, session: Session? = Session.active): CircleBatch {
            return CircleBatch(vertexBuffer(circleFormat, size, session), vertexBuffer(drawStyleFormat, size, session))
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

    val entries = mutableListOf<Entry>()

    /**
     * Add a circle to the batch
     */
    fun circle(x: Double, y: Double, radius: Double) {
        entries.add(Entry(fill, stroke, strokeWeight, Vector3(x, y, 0.0), Vector2(radius, radius)))
    }

    /**
     * Add a circle to the batch
     */
    fun circle(position: Vector2, radius: Double) {
        entries.add(Entry(fill, stroke, strokeWeight, position.xy0, Vector2(radius, radius)))
    }

    /**
     * Add a circle to the batch
     */
    fun circle(circle: Circle) {
        entries.add(Entry(fill, stroke, strokeWeight, circle.center.xy0, Vector2(circle.radius, circle.radius)))
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
        for (center in centers) {
            entries.add(Entry(fill, stroke, strokeWeight, center.xy0, Vector2(radius, radius)))
        }
    }

    /**
     * Add circles to the batch
     */
    fun circles(centers: List<Vector2>, radii: List<Double>) {
        require(centers.size == radii.size)
        for (i in centers.indices) {
            entries.add(Entry(fill, stroke, strokeWeight, centers[i].xy0, Vector2(radii[i], radii[i])))
        }
    }

    /**
     * Generate the stored batch
     */
    fun batch(existingBatch: CircleBatch? = null): CircleBatch {
        val geometry = existingBatch?.geometry ?: vertexBuffer(circleFormat, entries.size)
        geometry.put {
            for (entry in entries) {
                write(entry.offset)
                write(entry.radius)
            }
        }

        val drawStyle = existingBatch?.drawStyle ?: vertexBuffer(drawStyleFormat, entries.size)
        drawStyle.put {
            for (entry in entries) {
                write(entry.fill ?: ColorRGBa.TRANSPARENT)
                write(entry.stroke ?: ColorRGBa.TRANSPARENT)
                write(if (entry.stroke == null) 0.0f else entry.strokeWeight.toFloat())
            }
        }
        return existingBatch ?: CircleBatch(geometry, drawStyle)
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


class RectangleBatch(val geometry: VertexBuffer, val drawStyle: VertexBuffer) {
    init {
        require(geometry.vertexFormat == rectangleFormat)
        require(drawStyle.vertexFormat == drawStyleFormat)
        require(geometry.vertexCount == drawStyle.vertexCount)
    }

    val size
        get() = geometry.vertexCount

    companion object {
        fun create(size: Int, session: Session? = Session.active): RectangleBatch {
            return RectangleBatch(vertexBuffer(rectangleFormat, size, session), vertexBuffer(drawStyleFormat, size, session))
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


class RectangleBatchBuilder(drawer: Drawer) : BatchBuilder(drawer) {
    class Entry(
            val fill: ColorRGBa?,
            val stroke: ColorRGBa?,
            val strokeWeight: Double,
            val offset: Vector3,
            val dimensions: Vector2,
            val rotation: Double
    )

    val entries = mutableListOf<Entry>()

    fun rectangle(x: Double, y: Double, width: Double, height: Double, rotationInDegrees: Double = 0.0) {
        entries.add(Entry(fill, stroke, strokeWeight, Vector3(x, y, 0.0), Vector2(width, height), rotationInDegrees))
    }

    /**
     * Generate the stored batch
     */
    fun batch(existingBatch: RectangleBatch? = null): RectangleBatch {
        val geometry = existingBatch?.geometry ?: vertexBuffer(rectangleFormat, entries.size)
        geometry.put {
            for (entry in entries) {
                write(entry.offset)
                write(entry.dimensions)
                write(entry.rotation.toFloat())
            }
        }

        val drawStyle = existingBatch?.drawStyle ?: vertexBuffer(drawStyleFormat, entries.size)
        drawStyle.put {
            for (entry in entries) {
                write(entry.fill ?: ColorRGBa.TRANSPARENT)
                write(entry.stroke ?: ColorRGBa.TRANSPARENT)
                write(if (entry.stroke == null) 0.0f else entry.strokeWeight.toFloat())
            }
        }
        return existingBatch ?: RectangleBatch(geometry, drawStyle)
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
