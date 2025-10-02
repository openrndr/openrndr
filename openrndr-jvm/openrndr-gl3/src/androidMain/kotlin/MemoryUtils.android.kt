package org.openrndr.internal.gl3

import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

actual class MemoryStack : AutoCloseable {
    actual fun mallocFloat(size: Int): FloatBuffer {
        TODO("Not yet implemented")
    }

    actual fun mallocInt(size: Int): IntBuffer {
        TODO("Not yet implemented")
    }

    actual companion object {
        actual fun stackPush(): MemoryStack {
            TODO("Not yet implemented")
        }
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}

actual object MemoryUtil {
    actual fun memAlloc(size: Int): ByteBuffer {
        return BufferUtils.createByteBuffer(size)
    }

    actual fun memFree(buffer: ByteBuffer) {
        // don't free the memory I guess
    }
}