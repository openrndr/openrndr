package org.openrndr.webgl

import js.buffer.ArrayBuffer
import js.core.JsPrimitives.toJsFloat
import js.typedarrays.Float32Array
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferWriter
import org.openrndr.math.*

class BufferWriterWebGL(val buffer: Float32Array<ArrayBuffer>, val elementSize: Int) : BufferWriter() {
    init {
        require(elementSize % 4 == 0) { "elementSize $elementSize must be a multiple of 4" }
    }

    override var position: Int = 0

    override fun write(vararg v: Vector3) {
        TODO("Not yet implemented")
    }

    override fun write(v: Byte) {
        error("only float types")
    }

    override fun write(v: Short) {
        error("only float types")
    }

    override fun write(v: Vector3) {
        buffer[position] = v.x.toFloat().toJsFloat()
        position++
        buffer[position] = v.y.toFloat().toJsFloat()
        position++
        buffer[position] = v.z.toFloat().toJsFloat()
        position++
    }

    override fun write(v: Vector2) {
        buffer[position] = v.x.toFloat().toJsFloat()
        position++
        buffer[position] = v.y.toFloat().toJsFloat()
        position++
    }

    override fun write(v: Vector4) {
        buffer[position] = v.x.toFloat().toJsFloat()
        position++
        buffer[position] = v.y.toFloat().toJsFloat()
        position++
        buffer[position] = v.z.toFloat().toJsFloat()
        position++
        buffer[position] = v.w.toFloat().toJsFloat()
        position++
    }

    override fun write(v: Int) {
        error("only float types")
    }

    override fun write(v: IntVector2) {
        error("only float types")
    }

    override fun write(v: IntVector3) {
        error("only float types")
    }

    override fun write(v: IntVector4) {
        error("only float types")
    }

    override fun write(v: Matrix33) {
        TODO("Not yet implemented")
    }

    override fun write(v: Matrix44) {
        TODO("Not yet implemented")
    }

    override fun write(v: Float) {
        buffer[position] = v.toJsFloat()
        position++
    }

    override fun write(x: Float, y: Float) {
        buffer[position] = x.toJsFloat()
        position++
        buffer[position] = y.toJsFloat()
        position++
    }

    override fun write(x: Float, y: Float, z: Float) {
        buffer[position] = x.toJsFloat()
        position++
        buffer[position] = y.toJsFloat()
        position++
        buffer[position] = z.toJsFloat()
        position++
    }

    override fun write(x: Float, y: Float, z: Float, w: Float) {
        buffer[position] = x.toJsFloat()
        position++
        buffer[position] = y.toJsFloat()
        position++
        buffer[position] = z.toJsFloat()
        position++
        buffer[position] = w.toJsFloat()
        position++
    }

    override fun write(v: ColorRGBa) {
        write(v.r.toFloat(), v.g.toFloat(), v.b.toFloat(), v.alpha.toFloat())
    }

    override fun write(a: FloatArray, offset: Int, size: Int) {
        for (i in 0 until size) {
            buffer[position] = a[offset + i].toJsFloat()
            position++
        }
    }

    override fun rewind() {
        position = 0
    }

    override var positionElements: Int
        get() = (position * 4) / elementSize
        set(value) {
            position = (elementSize * value) / 4
        }
}
