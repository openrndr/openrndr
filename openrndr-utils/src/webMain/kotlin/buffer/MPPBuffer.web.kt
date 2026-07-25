package org.openrndr.utils.buffer

//mport js.buffer.DataView

import js.buffer.ArrayBuffer
import js.buffer.DataView

actual class MPPBuffer(val dataView: DataView<ArrayBuffer>) {
    private var offset = 0
    private var limit = dataView.byteLength

    actual fun rewind() {
        offset = 0
    }

    actual fun limit(): Int {
        return limit
    }

    actual fun limit(newLimit: Int) {
        require(newLimit in 0..capacity()) { "newLimit: $newLimit, capacity: ${capacity()}" }
        limit = newLimit
        if (offset > limit) {
            offset = limit
        }
    }

    actual fun position(): Int {
        return offset
    }

    actual fun position(newPosition: Int) {
        require(newPosition in 0..limit) { "newPosition: $newPosition, limit: $limit" }
        offset = newPosition
    }

    actual fun flip() {
        limit = offset
        offset = 0
    }

    actual val int: Int
        get() {
            val v = dataView.getInt32(offset, littleEndian = true)
            offset += 4
            return v
        }

    actual val float: Float
        get() {
            val v = dataView.getFloat32(offset, littleEndian = true)
            offset += 4
            return v
        }

    actual val double: Double
        get() {
            val v = dataView.getFloat64(offset, littleEndian = true)
            offset += 8
            return v
        }

    actual val byte: Byte
        get() {
            val v = dataView.getInt8(offset)
            offset += 1
            return v
        }

    actual fun get(): Byte {
        val v = dataView.getInt8(offset)
        offset += 1
        return v
    }

    actual fun capacity(): Int {
        return dataView.byteLength
    }

    actual fun get(target: ByteArray) {
        for (i in 0 until target.size) {
            target[i] = dataView.getInt8(offset)
            offset++
        }
    }

    actual fun remaining(): Int {
        return limit - offset
    }


    actual companion object {
        actual fun allocate(size: Int): MPPBuffer {
            val ab = ArrayBuffer(size)
            val dv = DataView(ab)
            return MPPBuffer(dv)
        }

        actual fun createFrom(fromBytes: ByteArray): MPPBuffer {
            val ab = ArrayBuffer(fromBytes.size)
            val dv = DataView(ab)
            for ((index, i) in fromBytes.withIndex()) {
                dv.setInt8(index, i)
            }
            val buf = MPPBuffer(dv)
            buf.position(fromBytes.size)
            buf.flip()
            return buf
        }
    }

    actual fun put(byte: Byte) {
        dataView.setInt8(offset, byte)
        offset++
    }

    actual fun put(ubyte: UByte) {
        dataView.setUint8(offset, ubyte.toByte())
        offset++
    }
    actual fun put(short: Short) {
        dataView.setInt16(offset, short, littleEndian = true)
        offset += 2
    }
    actual fun put(ushort: UShort) {
        dataView.setUint16(offset, ushort.toShort(), littleEndian = true)
        offset += 2
    }

    actual fun putFloat(float: Float) {
        dataView.setFloat32(offset, float, littleEndian = true)
        offset += 4
    }

    actual fun putDouble(double: Double) {
        dataView.setFloat64(offset, double, littleEndian = true)
        offset += 8
    }

    actual fun putInt(int: Int) {
        dataView.setInt32(offset, int, littleEndian = true)
        offset += 4
    }


}