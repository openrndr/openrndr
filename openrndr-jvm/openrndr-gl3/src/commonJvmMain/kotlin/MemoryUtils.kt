package org.openrndr.internal.gl3

import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

expect class MemoryStack: AutoCloseable {
    fun mallocFloat(size: Int): FloatBuffer
    fun mallocInt(size: Int): IntBuffer
    companion object {
        fun stackPush(): MemoryStack
    }
}

expect object MemoryUtil {
    fun memAlloc(size: Int): ByteBuffer
    fun memFree(buffer: ByteBuffer)
}