package org.openrndr.internal.gl3

import java.nio.FloatBuffer
import java.nio.IntBuffer

import org.lwjgl.system.MemoryStack as MemoryStack_
actual class MemoryStack: AutoCloseable {

    lateinit var ms: MemoryStack
    actual fun mallocFloat(size: Int): FloatBuffer {
        return ms.mallocFloat(size)

    }
    actual fun mallocInt(size: Int): IntBuffer {
        return ms.mallocInt(size)
    }

    override fun close() {
        ms.close()
    }
    actual companion object {
        actual fun stackPush(): MemoryStack {
            return MemoryStack().apply { this.ms = MemoryStack.stackPush() }
        }
    }


}