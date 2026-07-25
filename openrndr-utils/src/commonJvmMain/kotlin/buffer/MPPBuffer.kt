package org.openrndr.utils.buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    actual fun limit(): Int {
        return byteBuffer.limit()
    }

    actual fun limit(newLimit: Int) {
        byteBuffer.limit(newLimit)
    }

    actual fun position(): Int {
        return byteBuffer.position()
    }

    actual fun position(newPosition: Int) {
        byteBuffer.position(newPosition)
    }

    actual fun flip() {
        byteBuffer.flip()
    }

    actual companion object {
        actual fun allocate(size: Int) : MPPBuffer {
            val byteBuffer = ByteBuffer.allocateDirect(size)
            byteBuffer.order(ByteOrder.nativeOrder())
            return MPPBuffer(byteBuffer)
        }

        actual fun createFrom(fromBytes: ByteArray): MPPBuffer {
            val byteBuffer= ByteBuffer.allocateDirect(fromBytes.size)
            byteBuffer.order(ByteOrder.nativeOrder())
            byteBuffer.put(fromBytes)
            byteBuffer.flip()

            return MPPBuffer(byteBuffer)
        }
    }

    actual fun put(byte: Byte) {
        byteBuffer.put(byte)
    }

    actual fun put(ubyte: UByte) {
        byteBuffer.put(ubyte.toByte())
    }

    actual fun put(short: Short) {
        byteBuffer.putShort(short)
    }

    actual fun put(ushort: UShort) {
        byteBuffer.putShort(ushort.toShort())
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