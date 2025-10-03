package org.openrndr.openal

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.AL11
import org.lwjgl.openal.ALC11
import org.lwjgl.openal.EXTThreadLocalContext

private val logger = KotlinLogging.logger { }

class AudioContext(val device: AudioDevice, val alContext: Long, val pan: Double) : AutoCloseable {

    private var disposed : Boolean = false

    fun removeCurrent() {
        synchronized(this) {
            EXTThreadLocalContext.alcSetThreadContext(0L)
        }
    }

    fun makeCurrent() {
        synchronized(this) {
            require(!disposed)
            val currentContext = EXTThreadLocalContext.alcGetThreadContext()
            if (currentContext != alContext) {
        //        logger.info { "making context ${alContext} current (was $currentContext)" }
                EXTThreadLocalContext.alcSetThreadContext(alContext)

                checkALCError(device.alDevice, "set alContext")
                val check = EXTThreadLocalContext.alcGetThreadContext()

                require(check == alContext) {
                    "expected ${alContext}, got $check"
                }
            }
        }
    }

    fun createQueueSource(
        bufferCount: Int = 100,
        queueSize: Int = 20,
        pullFunction: (() -> AudioData?)? = null
    ): AudioQueueSource {
        makeCurrent()
        checkALError("createQueueSource pre-existing errors", exception = false)

        val sources = IntArray(1)
        AL11.alGenSources(sources)
        val source = sources[0]
        checkALError("alGenSources")

        AL11.alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE)
        AL11.alSource3f(source, AL_POSITION, pan.toFloat(), 0.0f, 0.0f)

        return AudioQueueSource(this, source, bufferCount, queueSize, pullFunction)
    }

    override fun close() {
        synchronized(this) {
            logger.info { "closing AudioContext" }
            val currentContext = ALC11.alcGetCurrentContext()
            if (currentContext == alContext) {
                ALC11.alcMakeContextCurrent(0L)
                checkALCError(device.alDevice)
            }
            ALC11.alcDestroyContext(alContext)
            checkALCError(device.alDevice)
            ALC11.alcMakeContextCurrent(currentContext)
            disposed = true
        }
    }
}