package org.openrndr.openal

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.AL11
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.concurrent.thread import kotlin.math.min

private val logger = KotlinLogging.logger { }

class AudioQueueSource(
    context: AudioContext,
    source: Int,
    private val bufferCount: Int = 3,
    val queueSize: Int = 3,
    private val pullFunction: (() -> AudioData?)? = null
) : AudioSource(source, context) {
    private val inputQueue = Queue<AudioData>(queueSize)
    private var queued = 0
    private var outputQueue = mutableListOf<Pair<Int, Int>>()

    private val disposed = AtomicBoolean(false)

    fun queue(data: AudioData) {
        inputQueue.push(data)
    }

    val outputQueueFull: Boolean get() = outputQueue.size >= queueSize - 1

    var bufferOffset = 0L
        private set

    val sampleOffset: Long
        get() {
            context.makeCurrent()
            val o = bufferOffset + AL11.alGetSourcei(source, AL11.AL_SAMPLE_OFFSET).also { checkALError() }
            return o
        }


    private var playThread: Thread? = null
    fun play() {
        context.makeCurrent()
        playThread = thread(isDaemon = true) {
            var bufferIndex = 0

            Thread.currentThread().name = "AudioQueueSource-${context.alContext}"
            context.makeCurrent()

            logger.info { "starting play" }
            checkALError("play: pre-existing error")


            val buffers = (0 until bufferCount).map {
                alGenBuffers()
            }

            AL11.alSourcePlay(source)
            checkALError("alSourcePlay")

            var playing = true
            while (true) {
                if (disposed.get()) {
                    break
                }
                if (inputQueue.size() < inputQueue.maxSize - 1) {
                    val data = pullFunction?.invoke()
                    if (data != null) {
                        inputQueue.push(data)
                    }
                }

                if (queued == 0) {
                    if (disposed.get()) {
                        return@thread
                    }
                    val observation = AL11.alGetSourcei(source, AL11.AL_SOURCE_STATE) == AL11.AL_PLAYING
                    checkALError("alGetSourcei ${source}")
                    if (!observation && playing) {
                        logger.debug { "audio buffer underrun detected" }
                        playing = false
                    }
                }
                if (disposed.get()) {
                    break
                }
                val buffersProcessed = AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED)
                checkALError("alGetSourcei ${source} ${AL_BUFFERS_PROCESSED}")
                queued -= buffersProcessed
                queued = queued.coerceAtLeast(0)
                for (i in 0 until buffersProcessed) {
                    val unqueue = AL11.alSourceUnqueueBuffers(source)
                    checkALError("alSourceUnqueueBuffers", exception = false)
                    synchronized(outputQueue) {
                        if (outputQueue.isNotEmpty()) {
//                            AL11.alDeleteBuffers(unqueue)
//                            checkALError("alDeleteBuffers")
                            if (unqueue == outputQueue[0].first) {
                                bufferOffset += outputQueue[0].second
                                outputQueue.removeAt(0)
                            }
                        }
                    }
                }

                while (queued <= bufferCount && !inputQueue.isEmpty()) {
                    context.makeCurrent()
                    val data = inputQueue.pop()
                    val buffer = buffers[bufferIndex.mod(buffers.size)]
                    bufferIndex++
                    data.writeToBuffer(buffer)


                    synchronized(outputQueue) {
                        if (disposed.get() == false) {
                            outputQueue.add(Pair(buffer, data.buffer.capacity() / 4))
                            AL11.alSourceQueueBuffers(source, buffer)
                            checkALError()
                            queued++
                        }
                    }
                }

                if (!playing && queued > 0) {
                    logger.debug { "restarting play" }
                    playing = true
                    AL11.alSourcePlay(source)
                    checkALError()

                }
                Thread.sleep(0)
            }
            logger.info { "Stopped play" }
            context.removeCurrent()
//            for (buffer in buffers) {
//                alDeleteBuffers(buffer)
//                checkALError()
//            }
        }
    }

    fun stop() {
        checkALError("pre-existing error")
        flush()
    }

    fun pause() {
        checkALError("pre-existing error")
        context.makeCurrent()
        AL11.alSourcePause(source)
        checkALError()
    }

    fun flush() {
        context.makeCurrent()
        logger.info { "flushing" }
        checkALError("pre-existing error", exception = false)
        while (!inputQueue.isEmpty()) inputQueue.pop()
        AL11.alSourceStop(source)
        checkALError("alSourceStop")

        bufferOffset = 0L
        queued = 0
    }

    fun resume() {
        checkALError("pre-existing error")
        context.makeCurrent()
        AL11.alSourcePlay(source)
        checkALError()
    }

    fun dispose() {
        logger.info { "Disposing audio queue source" }
        if (disposed.get()) {
            error("Already disposed")
        }

        context.makeCurrent()
        checkALError("dispose: pre-existing errors", exception = false)
        disposed.set(true)

        if (playThread != null) {
            logger.info { "Waiting for play thread to stop" }
            playThread?.join()
        }

        flush()

        AL11.alDeleteSources(source)
        checkALError("alDeleteSources")

        synchronized(outputQueue) {
            for (i in outputQueue) {
                AL11.alDeleteBuffers(i.first)
                checkALError("alDeleteBuffers", exception = false)
            }
            outputQueue.clear()
        }

    }
}
