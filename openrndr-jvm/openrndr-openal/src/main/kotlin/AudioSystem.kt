package org.openrndr.openal

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.openal.*
import org.lwjgl.openal.ALC11.ALC_ALL_DEVICES_SPECIFIER
import org.lwjgl.system.MemoryUtil
import kotlin.math.log

private val logger = KotlinLogging.logger { }

object AudioSystem {


    fun listDevices(): List<String> {
        val devs = ALUtil.getStringList(MemoryUtil.NULL, ALC_ALL_DEVICES_SPECIFIER)?.map { it!! } ?: emptyList()
        return devs

    }

    val defaultDevice by lazy { createDefaultDevice() }

    val defaultDeviceName by lazy {  ALC11.alcGetString(0, ALC11.ALC_DEFAULT_DEVICE_SPECIFIER) ?: "" }

    fun createDefaultDevice(): AudioDevice {
        val defaultDevice = ALC11.alcGetString(0, ALC11.ALC_DEFAULT_DEVICE_SPECIFIER)!!
        return createDevice(defaultDevice, 0.0) ?: error("Failed to open device $defaultDevice")
    }

    fun createDevice(deviceName: String, pan: Double): AudioDevice? {

        val device = ALC11.alcOpenDevice(deviceName)

        if (device == 0L) {
            logger.info { "could not open device $deviceName with pan $pan" }
            return null
        } else {
            logger.info { "opening device $deviceName with pan $pan" }
            return AudioDevice(device, pan)
        }
    }
}