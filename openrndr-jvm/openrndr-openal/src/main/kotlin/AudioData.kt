package org.openrndr.openal

import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.AL11
import org.lwjgl.openal.EXTFloat32.AL_FORMAT_MONO_FLOAT32
import org.lwjgl.openal.EXTFloat32.AL_FORMAT_STEREO_FLOAT32
import org.lwjgl.openal.EXTMCFormats.*
import java.nio.ByteBuffer


/**
 * @property bytesPerFrame the size of one sample for all channels
 */
enum class AudioFormat(val alFormat: Int, val bytesPerFrame: Int) {
    MONO_8(AL_FORMAT_MONO8, 1),
    STEREO_8(AL_FORMAT_STEREO8, 2),
    QUAD_8(AL_FORMAT_QUAD8, 4),
    MONO_16(AL_FORMAT_MONO16, 2),
    STEREO_16(AL_FORMAT_STEREO16, 4),
    QUAD_16(AL_FORMAT_QUAD16, 8),
    MONO_FLOAT32(AL_FORMAT_MONO_FLOAT32, 4),
    STEREO_FLOAT32(AL_FORMAT_STEREO_FLOAT32, 8),
    QUAD_FLOAT32(AL_FORMAT_QUAD32, 16),
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
