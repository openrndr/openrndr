package org.openrndr.openal

import org.lwjgl.openal.ALC11.ALC_ALL_DEVICES_SPECIFIER
import org.lwjgl.openal.ALUtil
import org.lwjgl.system.MemoryUtil

data class AudioDeviceDescription(val name: String, val pan: Double = 0.0)

class AudioDeviceService(val deviceAliases: Map<String, AudioDeviceDescription>): AutoCloseable {
    private val audioDevices: MutableMap<String, AudioDevice?> = mutableMapOf()

    var deviceNames = ALUtil.getStringList(MemoryUtil.NULL, ALC_ALL_DEVICES_SPECIFIER)!!

    fun audioDevice(alias: String): AudioDevice? {

        val deviceDescription = deviceAliases[alias]

        return if (deviceDescription != null) {
            val fullName = deviceNames.find { it.contains(deviceDescription.name) }
            if (fullName != null) {
                var fullAlias = alias

                if (deviceDescription.pan == -1.0) {
                    fullAlias += "-left"
                } else if (deviceDescription.pan == 1.0) {
                    fullAlias += "-right"
                }

                audioDevices.getOrPut(fullAlias) {
                    println("opening device $fullAlias")
                    AudioSystem.createDevice(fullName, deviceDescription.pan)
                }
            } else null

        } else {
            null
        }
    }

    override fun close() {
        for (device in audioDevices.values) {
            device?.close()
        }
    }
}