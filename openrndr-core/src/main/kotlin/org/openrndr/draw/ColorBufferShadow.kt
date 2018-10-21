package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import java.nio.ByteBuffer

interface ColorBufferShadow {
    val colorBuffer: ColorBuffer
    val buffer: ByteBuffer

    fun upload()
    fun download()
    fun destroy()

    fun writer(): BufferWriter
    fun write(x: Int, y: Int, r: Double, g: Double, b: Double, a: Double)
    fun write(x: Int, y: Int, color: ColorRGBa) {
        write(x, y, color.r, color.g, color.b, color.a)
    }

    fun write(x: Int, y: Int, r: Float, g: Float, b: Float, a: Float) {
        write(x, y, r.toDouble(), g.toDouble(), b.toDouble(), a.toDouble())
    }

    fun read(x: Int, y: Int): ColorRGBa

    fun mapBoolean(mapper: (r: Double, g: Double, b: Double, a: Double) -> Boolean): Array<BooleanArray>
    fun mapDouble(mapper: (r: Double, g: Double, b: Double, a: Double) -> Double): Array<DoubleArray>
    fun mapFloat(mapper: (r: Double, g: Double, b: Double, a: Double) -> Float): Array<FloatArray>
    fun mapInt(mapper: (r: Double, g: Double, b: Double, a: Double) -> Int): Array<IntArray>

    fun <T> mapIndexed(xrange: IntProgression = 0 until this.colorBuffer.effectiveWidth,
                       yrange: IntProgression = 0 until this.colorBuffer.effectiveHeight,
                       mapper: (x: Int, y: Int, r: Double, g: Double, b: Double, a: Double) -> T): Array<List<T>>

    fun <T> flatMapIndexed(xrange: IntProgression = 0 until this.colorBuffer.effectiveWidth,
                           yrange: IntProgression = 0 until this.colorBuffer.effectiveHeight,
                           mapper: (x: Int, y: Int, r: Double, g: Double, b: Double, a: Double) -> T): List<T> {
        return mapIndexed(xrange, yrange, mapper).flatMap { it }
    }


    operator fun get(x: Int, y: Int): ColorRGBa {
        return read(x, y)
    }

    operator fun set(x: Int, y: Int, c: ColorRGBa) {
        write(x, y, c)
    }
}
