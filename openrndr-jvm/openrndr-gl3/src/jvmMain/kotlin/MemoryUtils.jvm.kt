package org.openrndr.internal.gl3

import java.nio.ByteBuffer

import org.lwjgl.system.MemoryUtil as MemoryUtil_

actual object MemoryUtil {
    actual fun memAlloc(size: Int): ByteBuffer {
        return MemoryUtil_.memAlloc(size)
    }

    actual fun memFree(buffer: ByteBuffer) {
        return MemoryUtil_.memFree(buffer)
    }
}