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
        val lcolor = if (colorBuffer.type.isSRGB) {
            color.toSRGB()
        } else {
            color.toLinear()
        }

        write(x, y, lcolor.r, lcolor.g, lcolor.b, lcolor.alpha)
    }

    fun write(x: Int, y: Int, r: Float, g: Float, b: Float, a: Float) {
        write(x, y, r.toDouble(), g.toDouble(), b.toDouble(), a.toDouble())
    }

    fun read(x: Int, y: Int): ColorRGBa

    operator fun get(x: Int, y: Int): ColorRGBa {
        return read(x, y)
    }

    operator fun set(x: Int, y: Int, c: ColorRGBa) {
        write(x, y, c)
    }
}
