package org.openrndr.internal.gl3

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*
import java.nio.Buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder

class BufferWriterStd430GL3(
    val buffer: ByteBuffer,
    val elements: List<ShaderStorageElement>,
    val elementSize: Int = 1
) : BufferWriterStd430 {
    private var pointer = 0
    private var element: ShaderStorageElement = elements[pointer]
    private lateinit var member: ShaderStoragePrimitive

    init {
        buffer.order(ByteOrder.nativeOrder())
    }

    private fun next() {
        when (element){
            is ShaderStoragePrimitive -> {
                member = element as ShaderStoragePrimitive
                pointer++

                val index = elements.indexOf(element)

                if (pointer >= (index + element.arraySize)) {
                    pointer = index + 1
                    element = elements[pointer % elements.size]
                }
            }
            is ShaderStorageStruct -> {
                val struct = element as ShaderStorageStruct
                val index = elements.indexOf(element)

                if (pointer >= (index + struct.arraySize * struct.elements.size)) {
                    pointer = index + 1
                    element = elements[pointer % elements.size]
                    next()
                } else {
                    val memberIdx = (pointer - index) % (struct.elements.size)
                    member = struct.elements[memberIdx % struct.elements.size] as ShaderStoragePrimitive
                    pointer++
                }
            }
        }

    }

    private fun padding() {
        next()

        repeat(member.padding / 4) {
            buffer.putInt(0)
        }
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
        for (v3 in v) {
            write(v3)
        }
    }

    override fun write(v: Boolean) {
        buffer.putInt(if (v) 1 else 0)
        padding()
    }

    override fun write(v: Byte) {
        buffer.put(v)
        padding()
    }

    override fun write(v: Short) {
        buffer.putShort(v)
        padding()
    }

    override fun write(v: Int) {
        buffer.putInt(v)
        padding()
    }

    override fun write(v: IntVector2) {
        buffer.putInt(v.x)
        buffer.putInt(v.y)
        padding()
    }

    override fun write(v: IntVector3) {
        buffer.putInt(v.x)
        buffer.putInt(v.y)
        buffer.putInt(v.z)
        padding()
    }

    override fun write(v: IntVector4) {
        buffer.putInt(v.x)
        buffer.putInt(v.y)
        buffer.putInt(v.z)
        buffer.putInt(v.w)
        padding()
    }

    override fun write(v: Vector3) {
        buffer.putFloat(v.x.toFloat())
        buffer.putFloat(v.y.toFloat())
        buffer.putFloat(v.z.toFloat())
        padding()
    }

    override fun write(v: Vector2) {
        buffer.putFloat(v.x.toFloat())
        buffer.putFloat(v.y.toFloat())
        padding()
    }

    override fun write(v: Vector4) {
        buffer.putFloat(v.x.toFloat())
        buffer.putFloat(v.y.toFloat())
        buffer.putFloat(v.z.toFloat())
        buffer.putFloat(v.w.toFloat())
        padding()
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

        padding()
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

        padding()
    }

    override fun write(v: Double) {
        buffer.putDouble(v)
        padding()
    }

    override fun write(v: Float) {
        buffer.putFloat(v)
        padding()
    }

    override fun write(x: Float, y: Float, z: Float) {
        buffer.putFloat(x)
        buffer.putFloat(y)
        buffer.putFloat(z)
        padding()
    }

    override fun write(x: Float, y: Float, z: Float, w: Float) {
        buffer.putFloat(x)
        buffer.putFloat(y)
        buffer.putFloat(z)
        buffer.putFloat(w)
        padding()
    }

    override fun write(x: Float, y: Float) {
        buffer.putFloat(x)
        buffer.putFloat(y)
        padding()
    }

    override fun write(v: ColorRGBa) {
        buffer.putFloat(v.r.toFloat())
        buffer.putFloat(v.g.toFloat())
        buffer.putFloat(v.b.toFloat())
        buffer.putFloat(v.alpha.toFloat())
        padding()
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
