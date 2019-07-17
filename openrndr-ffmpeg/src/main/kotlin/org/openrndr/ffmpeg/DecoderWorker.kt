package org.openrndr.ffmpeg

import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avutil.AVBufferRef
import org.bytedeco.ffmpeg.avutil.AVHWDeviceContext
import org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc
import org.bytedeco.ffmpeg.global.avcodec.av_packet_unref
import org.bytedeco.ffmpeg.global.avformat.av_read_frame
import org.bytedeco.ffmpeg.global.avformat.av_seek_frame
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.javacpp.*
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import kotlin.concurrent.thread

internal data class CodecInfo(val video: VideoInfo?, val audio: AudioInfo?) {
    val hasVideo = video != null
    val hasAudio = audio != null
}

internal fun Int.checkAVError() {
    if (this != 0) {
        val buffer = ByteArray(1024)
        avutil.av_strerror(this, buffer, 1024L)
        throw Error("AVError: ${String(buffer)}")
    }
}

internal class Decoder(val statistics: VideoStatistics,
                       val formatContext: AVFormatContext,
                       val videoStreamIndex: Int,
                       val audioStreamIndex: Int,
                       val videoCodecContext: AVCodecContext?,
                       val audioCodecContext: AVCodecContext?,
                       val hwType: Int) {

    companion object {
        fun fromContext(statistics: VideoStatistics, context: AVFormatContext, useVideo: Boolean = true, useAudio: Boolean = true, useHW: Boolean = true): Pair<Decoder, CodecInfo> {
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

                val preferedHW = when (Platform.type) {
                    PlatformType.WINDOWS -> arrayListOf(AV_HWDEVICE_TYPE_D3D11VA, AV_HWDEVICE_TYPE_DXVA2, AV_HWDEVICE_TYPE_QSV)
                    PlatformType.MAC -> arrayListOf(AV_HWDEVICE_TYPE_VIDEOTOOLBOX)
                    PlatformType.GENERIC -> arrayListOf(AV_HWDEVICE_TYPE_VAAPI)
                }.reversed()

                val foundHW = mutableListOf<Int>()
                var next = AV_HWDEVICE_TYPE_NONE
                do {
                    next = av_hwdevice_iterate_types(next)
                    if (next != 0) {
                        foundHW.add(next)
                    }
                } while (next != AV_HWDEVICE_TYPE_NONE)

                hwType = (foundHW.map { Pair(it, preferedHW.indexOf(it)) })
                        .filter { it.second >= 0 }
                        .maxBy { it.second }
                        ?.first ?: AV_HWDEVICE_TYPE_NONE


                if (hwType != AV_HWDEVICE_TYPE_NONE) {
                    val hwContextPtr = PointerPointer<AVHWDeviceContext>(1)
                    val name = av_hwdevice_get_type_name(hwType).getString()
                    println("creating hw device context (type: $name)")
                    av_hwdevice_ctx_create(hwContextPtr, hwType, null, null, 0).checkAVError()
                    val hwContext = AVHWDeviceContext(hwContextPtr[0])
                    videoContext.hw_device_ctx(av_buffer_ref(AVBufferRef(hwContext)))
                }
            }

            // Extract video info.
            val video = videoContext?.run {
                VideoInfo(Dimensions(width(), height()), av_q2d((videoStream.r_frame_rate())))
            }
            // Extract audio info.
            val audio = audioContext?.run {
                AudioInfo(sample_rate(), channels())
            }

            return Pair(Decoder(statistics, context, videoStreamIndex, audioStreamIndex, videoContext, audioContext, hwType), CodecInfo(video, audio))
        }
    }

    private var videoDecoder: VideoDecoder? = null
    private var audioDecoder: AudioDecoder? = null
    private var noMoreFrames = false
    private var disposed = false
    private var packetReader: PacketReader? = null

    fun start(videoOutput: VideoDecoderOutput?, audioOutput: AudioDecoderOutput?) {
        videoDecoder = videoCodecContext?.let { ctx ->
            videoOutput?.let { VideoDecoder(statistics, ctx, it, hwType) }
        }

        audioDecoder = audioCodecContext?.let { ctx ->
            audioOutput?.let {
                AudioDecoder(ctx, it)
            }
        }

        noMoreFrames = false

        packetReader = PacketReader(formatContext, statistics)

        thread(isDaemon=true) {
            packetReader?.start()
        }

        while (!disposed) {
            decodeIfNeeded()
            Thread.sleep(1)
        }
    }

    fun restart() {
        noMoreFrames = false
        videoDecoder?.flushQueue()
        audioDecoder?.flushQueue()
        packetReader?.flushQueue()
        av_seek_frame(formatContext, -1, formatContext.start_time(), 0)
    }

    fun done() = noMoreFrames && (videoDecoder?.isQueueEmpty() ?: true)

    fun dispose() {
        disposed = true
        videoDecoder?.dispose()
        audioDecoder?.dispose()
        packetReader?.dispose()
    }

    fun needMoreFrames(): Boolean =
            (videoDecoder?.needMoreFrames() ?: false)

    fun decodeIfNeeded() {
        if (videoDecoder?.isQueueAlmostFull() == true) {
            println("video queue is almost full")
            return
        }
        if (audioDecoder?.isQueueAlmostFull() == true) {
            println("audio queue is almost full")
            return
        }

        //val packet = av_packet_alloc()

        while (needMoreFrames()) {
            val packet = packetReader?.nextPacket()
            if (packet != null) {
                when (packet.stream_index()) {
                    videoStreamIndex -> videoDecoder?.decodeVideoPacket(packet)
                    audioStreamIndex -> audioDecoder?.decodeAudioPacket(packet)
                }
                av_packet_unref(packet)
            }
        }
        if (needMoreFrames()) noMoreFrames = true
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