package org.openrndr.openal

import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.AL11
import org.lwjgl.openal.EXTFloat32.AL_FORMAT_MONO_FLOAT32
import org.lwjgl.openal.EXTFloat32.AL_FORMAT_STEREO_FLOAT32
import org.lwjgl.openal.EXTMCFormats.*
import java.nio.ByteBuffer


enum class AudioFormat(val alFormat: Int) {
    MONO_8(AL_FORMAT_MONO8),
    STEREO_8(AL_FORMAT_STEREO8),
    QUAD_8(AL_FORMAT_QUAD8),
    MONO_16(AL_FORMAT_MONO16),
    STEREO_16(AL_FORMAT_STEREO16),
    QUAD_16(AL_FORMAT_QUAD16),
    MONO_FLOAT32(AL_FORMAT_MONO_FLOAT32),
    STEREO_FLOAT32(AL_FORMAT_STEREO_FLOAT32),
    QUAD_FLOAT32(AL_FORMAT_QUAD32),
}


class AudioData(
    val format: AudioFormat = AudioFormat.STEREO_16,
    val rate: Int = 44100, val buffer: ByteBuffer
) {
    fun createBuffer(): Int {
        val buffer = AL11.alGenBuffers()
        checkALError("alGenBuffers")
        writeToBuffer(buffer)
        return buffer
    }

    fun writeToBuffer(alBuffer: Int) {
        checkALError("pre-existing")
        AL11.alBufferData(alBuffer, format.alFormat, this.buffer, rate)
        checkALError("alBufferData", exception = false)
    }
}
