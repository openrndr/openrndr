package org.openrndr.internal.gl3


import org.lwjgl.opengl.GL43C.*
import org.lwjgl.opengl.GL44C.GL_DYNAMIC_STORAGE_BIT
import org.lwjgl.opengl.GL44C.glBufferStorage
import org.openrndr.draw.AtomicCounterBuffer
import org.openrndr.internal.Driver
import org.openrndr.utils.resettableLazy

class AtomicCounterBufferGL42(val buffer: Int, override val size: Int) : AtomicCounterBuffer {

    private var destroyed = false

    companion object {
        fun create(counterCount: Int): AtomicCounterBufferGL42 {
            val counterBuffer = glGenBuffers()
            glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, counterBuffer)
            val useBufferStorage = (Driver.instance as DriverGL3).version >= DriverVersionGL.VERSION_4_4
            if (useBufferStorage) {
                glBufferData(GL_ATOMIC_COUNTER_BUFFER, IntArray(counterCount), GL_DYNAMIC_DRAW)
            } else {
                glBufferStorage(GL_ATOMIC_COUNTER_BUFFER, counterCount * 4L, GL_DYNAMIC_STORAGE_BIT)
            }
            return AtomicCounterBufferGL42(counterBuffer, counterCount)
        }
    }

    override fun write(data: IntArray) {
        require(!destroyed)
        glBindBuffer(GL_COPY_WRITE_BUFFER, buffer)
        glBufferSubData(GL_COPY_WRITE_BUFFER, 0, data)
    }

    override fun read() : IntArray {
        require(!destroyed)
        glBindBuffer(GL_COPY_READ_BUFFER, buffer)
        val result = IntArray(size)
        glGetBufferSubData(GL_COPY_READ_BUFFER, 0, result)
        return result
    }



    override fun reset() {
        require(!destroyed)
        write(IntArray(size))
    }

    override fun destroy() {
        if (!destroyed) {
            destroyed = true
            glDeleteBuffers(buffer)
        }
    }
}