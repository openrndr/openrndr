package org.openrndr.internal.gl3

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object ZeroCache {
    private var cached: ByteBuffer? = null
    fun buf(minCapacity: Int): ByteBuffer {
        val b = cached
        return if (b == null || b.capacity() < minCapacity) {
            val nb = ByteBuffer.allocateDirect(minCapacity).order(ByteOrder.nativeOrder())
            for (i in 0 until minCapacity) nb.put(0.toByte())
            nb.position(0)
            cached = nb
            nb
        } else {
            b.clear(); b
        }
    }
}