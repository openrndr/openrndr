package org.openrndr.ffmpeg

import kotlinx.coroutines.*
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.Pointer.memcpy
import org.bytedeco.javacpp.avcodec.*
import org.bytedeco.javacpp.avformat.av_read_frame
import org.bytedeco.javacpp.avutil.*
import org.bytedeco.javacpp.swscale.*
import java.nio.DoubleBuffer

internal data class CodecInfo(val video: VideoInfo?, val audio: AudioInfo?) {
    val hasVideo = video != null
    val hasAudio = audio != null
}

fun Int.checkAVError() {
    if (this != 0) {
        val buffer = ByteArray(1024)
        avutil.av_strerror(this, buffer, 1024L)
        throw Error("AVError: ${String(buffer)}")
    }
}

internal class Decoder(val formatContext: avformat.AVFormatContext,
                       val videoStreamIndex: Int,
                       val audioStreamIndex: Int,
                       val videoCodecContext: avcodec.AVCodecContext?,
                       val audioCodecContext: avcodec.AVCodecContext?,
                       val hwType: Int) {

    companion object {
        fun fromContext(context: avformat.AVFormatContext, useVideo: Boolean = true, useAudio: Boolean = true, useHW:Boolean = true): Pair<Decoder, CodecInfo> {
            // Find the first video/audio streams.
            val videoStreamIndex =
                    if (useVideo) context.codecs.indexOfFirst { it?.codec_type() == AVMEDIA_TYPE_VIDEO } else -1
            val audioStreamIndex =
                    if (useAudio) context.codecs.indexOfFirst { it?.codec_type() == AVMEDIA_TYPE_AUDIO } else -1

            val videoStream = context.streamAt(videoStreamIndex)
            val audioStream = context.streamAt(audioStreamIndex)

            val videoContext = videoStream?.openCodec("videoDecoder")
            val audioContext = audioStream?.openCodec("audio")


            var hwType = AV_HWDEVICE_TYPE_NONE
            if (useHW && videoContext != null) {

                var next = AV_HWDEVICE_TYPE_NONE
                do {
                    next = av_hwdevice_iterate_types(next)
                    println("found hwdevice ${next} ${next == AV_HWDEVICE_TYPE_NONE}")
                    val name = av_hwdevice_get_type_name(next)
                    if (next != 0) {
                        println((name.getString()))
                        hwType = next
                        break
                    }
                } while (next != AV_HWDEVICE_TYPE_NONE)

                if (hwType != AV_HWDEVICE_TYPE_NONE) {
                    val hwContextPtr = PointerPointer<AVHWDeviceContext>(1)
                    println("creating hw device context")
                    val result = av_hwdevice_ctx_create(hwContextPtr, hwType, null, null, 0)
                    val hwContext = AVHWDeviceContext(hwContextPtr[0])

                    println("result of creating $result")
                    println("testing ${hwContext.type()}")
                    println("assigning to codeccontext")
                    videoContext.hw_device_ctx(av_buffer_ref(AVBufferRef(hwContext)))
                }
            }

            // Extract video info.
            val video = videoContext?.run {
                VideoInfo(Dimensions(width(), height()), av_q2d(avformat.av_stream_get_r_frame_rate(videoStream)))
            }
            // Extract audio info.
            val audio = audioContext?.run {
                AudioInfo(sample_rate(), channels())
            }

            return Pair(Decoder(context, videoStreamIndex, audioStreamIndex, videoContext, audioContext, hwType), CodecInfo(video, audio))
        }
    }

    private var videoDecoder: VideoDecoder? = null
    private var audioDecoder: AudioDecoder? = null
    private var noMoreFrames = false

    suspend fun start(videoOutput: VideoDecoderOutput?, audioOutput: AudioDecoderOutput?) {
        println("starting decoder")
        videoDecoder = videoCodecContext?.let { ctx ->
            videoOutput?.let { VideoDecoder(ctx, it, hwType) }
        }

        audioDecoder = audioCodecContext?.let { ctx ->
            audioOutput?.let {
                AudioDecoder(ctx, it)
            }
        }

        noMoreFrames = false

        while (!done()) {
            decodeIfNeeded()
            kotlinx.coroutines.delay(1)
        }
    }

    fun done() = noMoreFrames && (videoDecoder?.isQueueEmpty() ?: true)

    fun dispose() {
        videoDecoder?.dispose()
    }

    fun needMoreFrames(): Boolean =
            (videoDecoder?.needMoreFrames() ?: false)

    suspend fun decodeIfNeeded() {
        if (!needMoreFrames()) {

        }
        if (videoDecoder?.isQueueAlmostFull() == true) {
            println("video queue is almost full")
            return
        }
        if (audioDecoder?.isQueueAlmostFull() == true) {
            println("audio queue is almost full")
            return
        }

//        GlobalScope.launch {
        val packet = av_packet_alloc()
        val frameFinished = IntPointer(1)

        while (needMoreFrames() && av_read_frame(formatContext, packet) >= 0) {
            when (packet.stream_index()) {
                videoStreamIndex -> videoDecoder?.decodeVideoPacket2(packet, frameFinished)
                audioStreamIndex -> audioDecoder?.decodeAudioPacket(packet, frameFinished)
            }
            av_packet_unref(packet)
        }
        if (needMoreFrames()) noMoreFrames = true
        frameFinished.deallocate()
//        }.join()
    }

    fun peekNextVideoFrame(): VideoFrame? {
        return videoDecoder?.peekNextFrame()
    }

    fun nextVideoFrame(): VideoFrame? {
        return videoDecoder?.nextFrame()
    }

    fun videoQueueSize(): Int {
        return videoDecoder?.queueCount() ?: 0
    }
}