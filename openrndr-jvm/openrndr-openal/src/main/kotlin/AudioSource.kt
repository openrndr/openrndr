package org.openrndr.openal

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.AL11
import org.openrndr.math.Vector3
private val logger = KotlinLogging.logger {  }
open class AudioSource(protected val source: Int, val context: AudioContext) : AutoCloseable {
    var gain: Double = 1.0
        set(value) {
            context.makeCurrent()
            AL11.alSourcef(source, AL_GAIN, value.toFloat())
            checkALError()
            field = value
        }

    var position: Vector3 = Vector3(0.0, 0.0, 0.0)
        set(value) {
            context.makeCurrent()
            AL11.alSource3f(source, AL_POSITION, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
            checkALError()
            field = value
        }

    var velocity: Vector3 = Vector3(0.0, 0.0, 0.0)
        set(value) {
            context.makeCurrent()
            AL11.alSource3f(source, AL_VELOCITY, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
            checkALError()
            field = value
        }

    var direction: Vector3 = Vector3(0.0, 0.0, 0.0)
        set(value) {
            context.makeCurrent()
            AL11.alSource3f(source, AL_DIRECTION, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
            checkALError()
            field = value
        }


    override fun close() {
        context.makeCurrent()
        logger.debug { "Closing audio source"}
        AL11.alDeleteSources(source)
        checkALError("alDeleteSources")
    }
}
