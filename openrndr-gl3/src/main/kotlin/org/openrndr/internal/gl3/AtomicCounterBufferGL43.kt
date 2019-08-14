package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL43C.*
import org.openrndr.draw.AtomicCounterBuffer

class AtomicCounterBufferGL43(val buffer: Int, val size: Int) : AtomicCounterBuffer {

    companion object {
        fun create(counterCount: Int): AtomicCounterBufferGL43 {
            val counterBuffer = glGenBuffers()
            glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, counterBuffer)
            glBufferData(GL_ATOMIC_COUNTER_BUFFER, IntArray(counterCount), GL_DYNAMIC_DRAW)
            return AtomicCounterBufferGL43(counterBuffer, counterCount)
        }
    }

    override fun write(data: IntArray) {
        glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, buffer)
        glBufferSubData(GL_ATOMIC_COUNTER_BUFFER, 0, data)
    }

    override fun read() : IntArray {
        glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, buffer)
        val result = IntArray(size)
        glGetBufferSubData(GL_ATOMIC_COUNTER_BUFFER, 0, result)
        return result
    }

    override fun reset() {
        write(IntArray(size))
    }
}