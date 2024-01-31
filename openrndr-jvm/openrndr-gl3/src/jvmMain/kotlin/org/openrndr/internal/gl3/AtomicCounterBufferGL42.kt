package org.openrndr.internal.gl3


import org.lwjgl.opengl.GL43C.*
import org.lwjgl.opengl.GL44C.GL_DYNAMIC_STORAGE_BIT
import org.lwjgl.opengl.GL44C.glBufferStorage
import org.lwjgl.opengl.GL45C.glGetNamedBufferSubData
import org.lwjgl.opengl.GL45C.glNamedBufferSubData
import org.openrndr.draw.AtomicCounterBuffer
import org.openrndr.internal.Driver

class AtomicCounterBufferGL42(val buffer: Int, override val size: Int) : AtomicCounterBuffer {

    private var destroyed = false

    companion object {
        fun create(counterCount: Int): AtomicCounterBufferGL42 {
            val counterBuffer = glGenBuffers()
            glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, counterBuffer)
            val useBufferStorage = (Driver.instance as DriverGL3).version >= DriverVersionGL.GL_VERSION_4_4
            if (useBufferStorage) {
                glBufferData(GL_ATOMIC_COUNTER_BUFFER, IntArray(counterCount), GL_DYNAMIC_DRAW)
            } else {
                glBufferStorage(GL_ATOMIC_COUNTER_BUFFER, counterCount * 4L, GL_DYNAMIC_STORAGE_BIT)
            }
            return AtomicCounterBufferGL42(counterBuffer, counterCount)
        }
    }

    val useNamedBuffers = (Driver.instance as DriverGL3).version >= DriverVersionGL.GL_VERSION_4_5


    override fun write(data: IntArray) {
        require(!destroyed)
        if (useNamedBuffers) {
            glNamedBufferSubData(buffer,0, data)
        } else {
            glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, buffer)
            glBufferSubData(GL_ATOMIC_COUNTER_BUFFER, 0, data)
        }
    }


    override fun read() : IntArray {
        require(!destroyed)
        val result = IntArray(size)
        if (useNamedBuffers) {
            glGetNamedBufferSubData(buffer, 0, result)
        } else {
            glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, buffer)
            glGetBufferSubData(GL_ATOMIC_COUNTER_BUFFER, 0, result)
        }
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