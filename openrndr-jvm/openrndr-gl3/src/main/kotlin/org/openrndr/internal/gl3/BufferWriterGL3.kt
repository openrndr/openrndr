package org.openrndr.internal.gl3

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferWriter
import org.openrndr.math.*
import java.nio.Buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder

class BufferWriterGL3(val buffer: ByteBuffer, val elementSize: Int = 1) : BufferWriter() {
    init {
        buffer.order(ByteOrder.nativeOrder())
    }

    override fun copyBuffer(sourceBuffer: ByteBuffer, sourceOffset: Int, sourceSizeInBytes: Int) {
        require(sourceBuffer.isDirect) {
            "can only copy from direct buffers"
        }
        (sourceBuffer as Buffer).limit(sourceBuffer.capacity())
        (sourceBuffer as Buffer).position(sourceOffset)
        (sourceBuffer as Buffer).limit(sourceOffset + sourceSizeInBytes)
        buffer.put(sourceBuffer)
        (sourceBuffer as Buffer).limit(sourceBuffer.capacity())
    }

    override fun write(vararg v: Vector3) {
        for (i in v) {
            write(i)
        }
    }

    override fun write(v: Byte) {
        buffer.put(v)
    }

    override fun write(v: Short) {
        buffer.putShort(v)
    }

    override fun write(v: Int) {
        buffer.putInt(v)
    }

    override fun write(v: IntVector2) {
        buffer.putInt(v.x)
        buffer.putInt(v.y)
    }

    override fun write(v: IntVector3) {
        buffer.putInt(v.x)
        buffer.putInt(v.y)
        buffer.putInt(v.z)
    }

    override fun write(v: IntVector4) {
        buffer.putInt(v.x)
        buffer.putInt(v.y)
        buffer.putInt(v.z)
        buffer.putInt(v.w)
    }

    override fun write(a: FloatArray, offset: Int, size: Int) {
        buffer.asFloatBuffer().put(a, offset, size)
        (buffer as Buffer).position(buffer.position() + size * 4)
    }

    override fun write(v: Vector3) {
        buffer.putFloat(v.x.toFloat())
        buffer.putFloat(v.y.toFloat())
        buffer.putFloat(v.z.toFloat())
    }

    override fun write(v: Vector2) {
        buffer.putFloat(v.x.toFloat())
        buffer.putFloat(v.y.toFloat())
    }

    override fun write(v: Vector4) {
        buffer.putFloat(v.x.toFloat())
        buffer.putFloat(v.y.toFloat())
        buffer.putFloat(v.z.toFloat())
        buffer.putFloat(v.w.toFloat())
    }

    override fun write(v: Matrix33) {
        buffer.putFloat(v.c0r0.toFloat())
        buffer.putFloat(v.c0r1.toFloat())
        buffer.putFloat(v.c0r2.toFloat())

        buffer.putFloat(v.c1r0.toFloat())
        buffer.putFloat(v.c1r1.toFloat())
        buffer.putFloat(v.c1r2.toFloat())

        buffer.putFloat(v.c2r0.toFloat())
        buffer.putFloat(v.c2r1.toFloat())
        buffer.putFloat(v.c2r2.toFloat())

    }

    override fun write(v: Matrix44) {
        buffer.putFloat(v.c0r0.toFloat())
        buffer.putFloat(v.c0r1.toFloat())
        buffer.putFloat(v.c0r2.toFloat())
        buffer.putFloat(v.c0r3.toFloat())

        buffer.putFloat(v.c1r0.toFloat())
        buffer.putFloat(v.c1r1.toFloat())
        buffer.putFloat(v.c1r2.toFloat())
        buffer.putFloat(v.c1r3.toFloat())

        buffer.putFloat(v.c2r0.toFloat())
        buffer.putFloat(v.c2r1.toFloat())
        buffer.putFloat(v.c2r2.toFloat())
        buffer.putFloat(v.c2r3.toFloat())

        buffer.putFloat(v.c3r0.toFloat())
        buffer.putFloat(v.c3r1.toFloat())
        buffer.putFloat(v.c3r2.toFloat())
        buffer.putFloat(v.c3r3.toFloat())
    }

    override fun write(v: Float) {
        buffer.putFloat(v)
    }

    override fun write(x: Float, y: Float, z: Float) {
        buffer.putFloat(x)
        buffer.putFloat(y)
        buffer.putFloat(z)
    }

    override fun write(x: Float, y: Float, z: Float, w: Float) {
        buffer.putFloat(x)
        buffer.putFloat(y)
        buffer.putFloat(z)
        buffer.putFloat(w)
    }

    override fun write(x: Float, y: Float) {
        buffer.putFloat(x)
        buffer.putFloat(y)
    }

    override fun write(v: ColorRGBa) {
        buffer.putFloat(v.r.toFloat())
        buffer.putFloat(v.g.toFloat())
        buffer.putFloat(v.b.toFloat())
        buffer.putFloat(v.a.toFloat())
    }

    override var position: Int
        get() = (buffer as Buffer).position()
        set(value) {
            (buffer as Buffer).position(value)
        }

    override var positionElements: Int
        get() = (buffer as Buffer).position() / elementSize
        set(value) {
            (buffer as Buffer).position(value * elementSize)
        }

    override fun rewind() {
        (buffer as Buffer).rewind()
    }
}