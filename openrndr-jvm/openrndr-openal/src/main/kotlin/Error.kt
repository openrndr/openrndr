package org.openrndr.openal

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.AL11
import org.lwjgl.openal.ALC10.*
import org.lwjgl.openal.ALC11

private val logger = KotlinLogging.logger { }

fun checkALError(taskName: String = "", exception: Boolean = true) {

    val error = AL11.alGetError()
    val context = ALC11.alcGetCurrentContext()

    //AL11.alGetBoolean(AL_DOPPLER_FACTOR)
    val errorName = when (error) {
        AL_INVALID_NAME -> "AL_INVALID_NAME"
        AL_INVALID_ENUM -> "AL_INVALID_ENUM"
        AL_INVALID_VALUE -> "AL_INVALID_VALUE"
        AL_INVALID_OPERATION -> "AL_INVALID_OPERATION"
        AL_OUT_OF_MEMORY -> "AL_OUT_OF_MEMORY"
        else -> "unknown error ${String.format("%x", error)}"
    }
    if (error != 0) {
        logger.error { "OpenAL error: <context: $context> [$taskName] $errorName" }
    }
    if (exception) {
        require(error == AL_NO_ERROR) {
            "OpenAL error: <context: $context> [$taskName] $errorName"
        }
    }
}

/**
 * Checks for OpenAL Context (ALC) errors on a given audio device and logs the error details if any are found.
 * The method uses `alcGetError` to determine if there are errors associated with the specified device.
 * If an error is found, it throws an `IllegalArgumentException` with the corresponding error message.
 *
 * @param device The handle to the audio device to check for errors.
 * @param taskName An optional name of the task or operation being performed, used for logging purposes.
 */
fun checkALCError(device: Long, taskName: String = "") {

    val error = alcGetError(device)
    require(error == ALC_NO_ERROR) {
        val errorName = when (error) {
            ALC_INVALID_DEVICE -> "ALC_INVALID_DEVICE"
            ALC_INVALID_CONTEXT -> "ALC_INVALID_CONTEXT"
            ALC_INVALID_ENUM -> "ALC_INVALID_ENUM"
            ALC_INVALID_VALUE -> "ALC_INVALID_VALUE"
            ALC_OUT_OF_MEMORY -> "ALC_OUT_OF_MEMORY"

            else -> "unknown error ${String.format("%x", error)}"
        }
        logger.error { "OpenALC error: [$taskName] $errorName" }
        "OpenALC error: [$taskName] $errorName"
    }
}