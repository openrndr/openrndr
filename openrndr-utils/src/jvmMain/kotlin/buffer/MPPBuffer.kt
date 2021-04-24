package org.openrndr.utils.buffer

import java.nio.ByteBuffer

actual class MPPBuffer(val byteBuffer: ByteBuffer)  {
    actual fun rewind() {
        byteBuffer.rewind()
    }

    actual val int: Int
        get() = byteBuffer.int
    actual val float: Float
        get() = byteBuffer.float
    actual val double: Double
        get() = byteBuffer.double
    actual val byte: Byte
        get() = byteBuffer.get()

    actual fun get(): Byte {
        return byteBuffer.get()
    }

    actual fun capacity(): Int {
        return byteBuffer.capacity()
    }

    actual fun get(target: ByteArray) {
        byteBuffer.get(target)
    }

    actual fun remaining(): Int {
        return byteBuffer.remaining()
    }

    actual companion object {
        actual fun allocate(size: Int) : MPPBuffer {
            return MPPBuffer(ByteBuffer.allocateDirect(size))
        }

        actual fun createFrom(fromBytes: ByteArray): MPPBuffer {
            val bb= ByteBuffer.allocateDirect(fromBytes.size)
            bb.put(fromBytes)
            return MPPBuffer(bb)
        }
    }

    actual fun put(byte: Byte) {
        byteBuffer.put(byte)
    }

    actual fun putFloat(float: Float) {
        byteBuffer.putFloat(float)
    }

    actual fun putDouble(double: Double) {
        byteBuffer.putDouble(double)
    }

    actual fun putInt(int: Int) {
        byteBuffer.putInt(int)
    }
}