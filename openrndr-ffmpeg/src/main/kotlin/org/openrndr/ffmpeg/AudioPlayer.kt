package org.openrndr.ffmpeg

import mu.KotlinLogging
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL11
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC11
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import kotlin.math.min

private val logger = KotlinLogging.logger {}

object AudioSystem {
    val defaultDevice = ALC11.alcGetString(0, ALC11.ALC_DEFAULT_DEVICE_SPECIFIER).apply {
        println(this)
    }
    val device = ALC11.alcOpenDevice(defaultDevice)
    val attributes = IntArray(1)
    val context = ALC11.alcCreateContext(device, attributes).apply {
        ALC11.alcMakeContextCurrent(this)
    }

    val alcCaps = ALC.createCapabilities(device)
    val alCaps = AL.createCapabilities(alcCaps).apply {
        require(this.OpenAL10) {
            "no OpenAL 1.0 support"
        }
    }

    fun createSource(): AudioSource {
        val source = AL11.alGenSources()
        return AudioSource(source)
    }

    fun createQueueSource(bufferCount: Int = 2, bufferSize: Int = 8192, pullFunction: (() -> AudioData?)? = null): AudioQueueSource {
        val source = AL11.alGenSources()
        return AudioQueueSource(source, bufferCount, pullFunction)
    }

    fun destroy() {
        ALC11.alcDestroyContext(context)
        ALC11.alcCloseDevice(device)
    }
}

class AudioBuffer(val buffer: Int)

class AudioData(val format: Int = AL11.AL_FORMAT_STEREO16, val rate: Int = 44100, val buffer: ByteBuffer) {
    fun createBuffer(): AudioBuffer {
        val buffer = AL11.alGenBuffers()
        AL11.alBufferData(buffer, format, this.buffer, rate)
        return AudioBuffer(buffer)
    }
}

class AudioQueueSource(val source: Int, val bufferCount: Int = 4, val pullFunction: (() -> AudioData?)? = null) {
    internal val inputQueue = Queue<AudioData>(2)
    internal var queued = 0
    internal var outputQueue = mutableListOf<Pair<Int, Int>>()

    fun queue(data: AudioData) {
        inputQueue.push(data)
    }

    var bufferOffset = 0L
        private set


    val sampleOffset: Long
        get() = bufferOffset + AL11.alGetSourcei(source, AL11.AL_SAMPLE_OFFSET)

    fun play() {
        val startBufferCount = min(bufferCount, inputQueue.size())
        for (i in 0 until startBufferCount) {
            val data = inputQueue.pop()
            val buffer = data.createBuffer()
            AL11.alSourceQueueBuffers(source, buffer.buffer)
            outputQueue.add(Pair(buffer.buffer, data.buffer.capacity() / 4))
            queued++
        }

        AL11.alSourcePlay(source)
        thread(isDaemon = true) {
            while (true) {
                if (inputQueue.size() < inputQueue.maxSize - 1) {
                    val data = pullFunction?.invoke()
                    if (data != null) {
                        inputQueue.push(data)
                    }
                }

                var playing = true
                if (queued == 0) {
                    playing = AL11.alGetSourcei(source, AL11.AL_SOURCE_STATE) == AL11.AL_PLAYING
                }

                val buffersProcessed = AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED)
                queued -= buffersProcessed
                for (i in 0 until buffersProcessed) {
                    val unqueue = AL11.alSourceUnqueueBuffers(source)

                    AL11.alDeleteBuffers(unqueue)
                    require(unqueue == outputQueue[0].first)
                    bufferOffset += outputQueue[0].second
                    outputQueue.removeAt(0)
                }

                while (queued <= bufferCount && !inputQueue.isEmpty()) {
                    val data = inputQueue.pop()
                    val buffer = data.createBuffer()
                    outputQueue.add(Pair(buffer.buffer, data.buffer.capacity() / 4))
                    AL11.alSourceQueueBuffers(source, buffer.buffer)
                    queued++
                }

                if (!playing && queued > 0) {
                    logger.debug { "restarting play" }
                    AL11.alSourcePlay(source)
                }
                Thread.sleep(1)
            }
        }
    }

    fun flush() {
        while (!inputQueue.isEmpty()) inputQueue.pop()
        AL11.alSourceStop(source)
        AL11.alSourcePlay(source)
        for (i in outputQueue) {
            AL11.alDeleteBuffers(i.first)
        }
        bufferOffset = 0
    }
}

class AudioSource(val source: Int) {
    fun playBuffer(buffer: AudioBuffer) {
        AL11.alSourcePlay(buffer.buffer)
    }
}