@file:Suppress("unused")

package org.openrndr.openal

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.AL11
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC11
import org.lwjgl.openal.EXTFloat32.AL_FORMAT_MONO_FLOAT32
import org.lwjgl.openal.EXTFloat32.AL_FORMAT_STEREO_FLOAT32
import org.lwjgl.openal.EXTMCFormats.*
import org.openrndr.math.Vector3
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import kotlin.math.min

private val logger = KotlinLogging.logger {}

object AudioSystem {
    init {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                ALC11.alcDestroyContext(context)
                ALC11.alcCloseDevice(device)
            }
        })
    }

    private val defaultDevice = ALC11.alcGetString(0, ALC11.ALC_DEFAULT_DEVICE_SPECIFIER).apply {
        logger.debug { this }
    }
    private val device = ALC11.alcOpenDevice(defaultDevice)
    private val attributes = IntArray(1)
    private val context = ALC11.alcCreateContext(device, attributes).apply {
        ALC11.alcMakeContextCurrent(this)
    }

    private val alcCaps = ALC.createCapabilities(device)
    private val alCaps = AL.createCapabilities(alcCaps).apply {
        require(this.OpenAL10) {
            "no OpenAL 1.0 support"
        }
    }

    fun createQueueSource(bufferCount: Int = 2, queueSize: Int = 20, pullFunction: (() -> AudioData?)? = null): AudioQueueSource {
        val source = AL11.alGenSources()
        return AudioQueueSource(source, bufferCount, queueSize, pullFunction)
    }

    fun destroy() {
        ALC11.alcDestroyContext(context)
        ALC11.alcCloseDevice(device)
    }
}

class AudioBuffer(val buffer: Int)

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

class AudioData(val format: AudioFormat = AudioFormat.STEREO_16,
                val rate: Int = 48000, val buffer: ByteBuffer) {
    fun createBuffer(): AudioBuffer {
        val buffer = AL11.alGenBuffers()
        AL11.alBufferData(buffer, format.alFormat, this.buffer, rate)
        return AudioBuffer(buffer)
    }
}

open class AudioSource(protected val source: Int) {
    var gain: Double = 1.0
        set(value) {
            AL11.alSourcef(source, AL_GAIN, value.toFloat())
            field = value
        }

    var position: Vector3 = Vector3(0.0, 0.0, 0.0)
        set(value) {
            AL11.alSource3f(source, AL_POSITION, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
            field = value
        }

    var velocity: Vector3 = Vector3(0.0, 0.0, 0.0)
        set(value) {
            AL11.alSource3f(source, AL_VELOCITY, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
            field = value
        }

    var direction: Vector3 = Vector3(0.0, 0.0, 0.0)
        set(value) {
            AL11.alSource3f(source, AL_DIRECTION, value.x.toFloat(), value.y.toFloat(), value.z.toFloat())
            field = value
        }
}

class AudioQueueSource(source: Int, private val bufferCount: Int = 2, val queueSize: Int = 20, private val pullFunction: (() -> AudioData?)? = null) : AudioSource(source) {
    private val inputQueue = Queue<AudioData>(queueSize)
    private var queued = 0
    private var outputQueue = mutableListOf<Pair<Int, Int>>()

    fun queue(data: AudioData) {
        inputQueue.push(data)
    }

    val outputQueueFull: Boolean get() = outputQueue.size >= queueSize - 1

    var bufferOffset = 0L
        private set

    val sampleOffset: Long
        get() = bufferOffset + AL11.alGetSourcei(source, AL11.AL_SAMPLE_OFFSET)

    fun play() {
        val startBufferCount = min(bufferCount, inputQueue.size())
        for (i in 0 until startBufferCount) {
            val data = inputQueue.pop()
            val buffer = data.createBuffer()
            AL11.alSourcef(source, AL_PITCH, 1.0f)
            synchronized(outputQueue) {
                AL11.alSourceQueueBuffers(source, buffer.buffer)
                outputQueue.add(Pair(buffer.buffer, data.buffer.capacity() / 4))
                queued++
            }
        }

        AL11.alSourcePlay(source)
        thread(isDaemon = true) {
            var playing = true
            while (true) {
                if (inputQueue.size() < inputQueue.maxSize - 1) {
                    val data = pullFunction?.invoke()
                    if (data != null) {
                        inputQueue.push(data)
                    }
                }


                if (queued == 0) {

                    val observation = AL11.alGetSourcei(source, AL11.AL_SOURCE_STATE) == AL11.AL_PLAYING
                    if (!observation && playing) {
                        logger.debug { "audio buffer underrun detected" }
                        playing = false
                    }
                }



                val buffersProcessed = AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED)
                queued -= buffersProcessed
                queued = queued.coerceAtLeast(0)
                for (i in 0 until buffersProcessed) {
                    val unqueue = AL11.alSourceUnqueueBuffers(source)
                    synchronized(outputQueue) {
                        if (outputQueue.isNotEmpty()) {
                            AL11.alDeleteBuffers(unqueue)
                            if (unqueue == outputQueue[0].first) {
                                bufferOffset += outputQueue[0].second
                                outputQueue.removeAt(0)
                            }
                        }
                    }
                }

                while (queued <= bufferCount && !inputQueue.isEmpty()) {
                    val data = inputQueue.pop()
                    val buffer = data.createBuffer()
                    synchronized(outputQueue) {
                        outputQueue.add(Pair(buffer.buffer, data.buffer.capacity() / 4))
                        AL11.alSourceQueueBuffers(source, buffer.buffer)
                        queued++
                    }
                }

                if (!playing && queued > 0) {
                    logger.debug { "restarting play" }
                    playing = true
                    AL11.alSourcePlay(source)

                }
                Thread.sleep(0)
            }
        }
    }

    fun stop() {
        flush()
    }

    fun pause() {
        AL11.alSourcePause(source)
    }

    fun flush() {
        while (!inputQueue.isEmpty()) inputQueue.pop()
        AL11.alSourceStop(source)

        synchronized(outputQueue) {
            for (i in outputQueue) {
                AL11.alDeleteBuffers(i.first)
            }
            outputQueue.clear()
        }
        bufferOffset = 0L
        queued = 0
    }

    fun resume() {
        AL11.alSourcePlay(source)
    }

    fun dispose() {
        flush()
        AL11.alDeleteSources(source)
    }
}
