package org.openrndr.internal.gl3

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

class BufferUtils {
    companion object {
        fun createByteBuffer(size: Int): ByteBuffer {
            return ByteBuffer.allocateDirect(size).apply {
                order(ByteOrder.nativeOrder())
            }
        }
        fun createIntBuffer(size: Int): IntBuffer {
            val bb =  ByteBuffer.allocateDirect(size * 4)
            bb.order(ByteOrder.nativeOrder())
            return bb.asIntBuffer()
        }
    }

}