package org.openrndr.openal

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.openal.*

private val logger = KotlinLogging.logger {  }

class AudioDevice(val alDevice: Long, val pan: Double)  : AutoCloseable {
    var alcCaps: ALCCapabilities? = null
    var alCaps: ALCapabilities? = null

    fun createContext(): AudioContext {
        logger.debug { "Creating context" }
        val attributes = IntArray(1)
        val context = ALC11.alcCreateContext(alDevice, attributes).apply {
            checkALCError(alDevice, "alc create context")
            logger.debug { "Making context ${this} current" }
            ALC11.alcMakeContextCurrent(this)
            checkALCError(alDevice,"alc make context current")
        }
        if (alcCaps == null) {
            alcCaps = ALC.createCapabilities(alDevice).also { checkALCError(alDevice) }
            ALC.setCapabilities(alcCaps)

        }

        checkALCError(alDevice,"setCapabilities")

        if (alCaps == null) {
            alCaps = AL.createCapabilities(alcCaps ?: error("no alcCaps")).apply {
                require(this.OpenAL10) {
                    "no OpenAL 1.0 support"
                }
            }
        }

        return AudioContext(this, context, pan)
    }

    override fun close() {
        ALC11.alcCloseDevice(alDevice)
    }
}