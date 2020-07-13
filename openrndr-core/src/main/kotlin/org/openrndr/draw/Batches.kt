package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

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

    fun destroy() {
        geometry.destroy()
        drawStyle.destroy()
    }
}

class CircleBatchBuilder(val drawer: Drawer) {
    var fill = drawer.fill
    var stroke = drawer.stroke
    var strokeWeight = drawer.strokeWeight

    class Entry(
            val fill: ColorRGBa?,
            val stroke: ColorRGBa?,
            val strokeWeight: Double,
            val offset: Vector3,
            val radius: Vector2
    )

    val entries = mutableListOf<Entry>()

    fun circle(x: Double, y: Double, radius: Double) {
        entries.add(Entry(fill, stroke, strokeWeight, Vector3(x, y, 0.0), Vector2(radius, radius)))
    }

    fun batch(): CircleBatch {
        val geometry = vertexBuffer(circleFormat, entries.size)
        geometry.put {
            for (entry in entries) {
                write(entry.offset)
                write(entry.radius)
            }
        }

        val drawStyle = vertexBuffer(drawStyleFormat, entries.size)
        drawStyle.put {
            for (entry in entries) {
                write(entry.fill ?: ColorRGBa.TRANSPARENT)
                write(entry.stroke ?: ColorRGBa.TRANSPARENT)
                write(if (entry.stroke == null) 0.0f else entry.strokeWeight.toFloat())
            }
        }
        return CircleBatch(geometry, drawStyle)
    }
}

fun Drawer.circleBatch(build: CircleBatchBuilder.() -> Unit): CircleBatch {
    val circleBatchBuilder = CircleBatchBuilder(this)
    circleBatchBuilder.build()
    return circleBatchBuilder.batch()
}