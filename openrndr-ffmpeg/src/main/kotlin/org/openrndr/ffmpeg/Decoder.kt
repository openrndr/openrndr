package org.openrndr.ffmpeg

import mu.KotlinLogging
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avutil.AVBufferRef
import org.bytedeco.ffmpeg.avutil.AVHWDeviceContext
import org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc
import org.bytedeco.ffmpeg.global.avcodec.av_packet_unref
import org.bytedeco.ffmpeg.global.avformat.*
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

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

internal val flushPacket = AVPacket().apply {
    data(BytePointer("FLUSH"))
}

internal class Decoder(val statistics: VideoStatistics,
                       val configuration: VideoPlayerConfiguration,
                       val formatContext: AVFormatContext,
                       val videoStreamIndex: Int,
                       val audioStreamIndex: Int,
                       val videoCodecContext: AVCodecContext?,
                       val audioCodecContext: AVCodecContext?,
                       val hwType: Int
) {
    companion object {
        fun fromContext(statistics: VideoStatistics, configuration: VideoPlayerConfiguration, context: AVFormatContext, useVideo: Boolean = true, useAudio: Boolean = true): Pair<Decoder, CodecInfo> {
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
            if (configuration.useHardwareDecoding && videoContext != null) {
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
                    logger.debug { "creating hw device context (type: $name)" }
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
            return Pair(Decoder(statistics, configuration, context, videoStreamIndex, audioStreamIndex, videoContext, audioContext, hwType), CodecInfo(video, audio))
        }
    }

    private var videoDecoder: VideoDecoder? = null
    private var audioDecoder: AudioDecoder? = null
    private var disposed = false
    private var packetReader: PacketReader? = null

    var displayQueueFull: () -> Boolean = { false }
    var audioOutQueueFull: () -> Boolean = { false }
    var seekCompleted: () -> Unit = { }


    fun start(videoOutput: VideoDecoderOutput?, audioOutput: AudioDecoderOutput?) {
        videoDecoder = videoCodecContext?.let { ctx ->
            videoOutput?.let { VideoDecoder(statistics, configuration, ctx, it, hwType) }
        }

        audioDecoder = audioCodecContext?.let { ctx ->
            audioOutput?.let {
                AudioDecoder(ctx, it)
            }
        }

        if (configuration.usePacketReaderThread) {
            packetReader = PacketReader(configuration, formatContext, statistics)
            thread(isDaemon = true) {
                packetReader?.start()
            }
        }
        while (!disposed) {
            decodeIfNeeded()
            Thread.sleep(0)
        }
    }

    fun restart() {
        logger.debug { "restart requested" }
        videoDecoder?.flushQueue()
        audioDecoder?.flushQueue()
        packetReader?.flushQueue()
        logger.debug { "seeking to frame 0" }
        av_seek_frame(formatContext, -1, formatContext.start_time(), AVSEEK_FLAG_FRAME or AVSEEK_FLAG_ANY)
    }

    private var needFlush = false
    private var seekRequested = true
    private var seekPosition: Double = 0.0
    fun seek(positionInSeconds: Double) {
        seekPosition = positionInSeconds
        seekRequested = true
    }

    fun done() = (packetReader?.endOfFile == true) && (packetReader?.isQueueEmpty() == true) && (videoDecoder?.isQueueEmpty()
            ?: true)

    fun dispose() {
        disposed = true
        videoDecoder?.dispose()
        audioDecoder?.dispose()
        packetReader?.dispose()
    }

    fun needMoreFrames(): Boolean =
            !seekRequested && ((videoDecoder?.needMoreFrames() ?: false) || (audioDecoder?.needMoreFrames() ?: false))
                    && !audioOutQueueFull() && !displayQueueFull()

    fun decodeIfNeeded() {
        val hasSeeked = seekRequested

        if (seekRequested) {
            videoDecoder?.flushQueue()
            audioDecoder?.flushQueue()
            packetReader?.flushQueue()

            logger.debug { "seeking to $seekPosition" }
            val seekTS = (seekPosition * AV_TIME_BASE).toLong()
            val seekMinTS = ((seekPosition - 4.0) * AV_TIME_BASE).toLong()
            val seekResult = avformat_seek_file(formatContext, -1, seekMinTS, seekTS, seekTS, AVSEEK_FLAG_ANY)
            if (seekResult != 0) {
                logger.error { "seek failed" }
            } else {
                logger.debug { "seek returned"}
            }
            needFlush = true
            seekRequested = false
//            Thread.sleep(5)
        }



        if (needFlush) {
            needFlush = false
            videoDecoder?.flushBuffers()
            audioDecoder?.flushBuffers()
        }

        var packetsReceived = 0
        while (needMoreFrames()) {
            if (videoDecoder?.isQueueAlmostFull() == true) {
                logger.warn { "video queue is almost full ${videoDecoder?.queueCount()} ${audioDecoder?.queueCount()}" }
                Thread.sleep(200)
                return
            }
            if (audioDecoder?.isQueueAlmostFull() == true) {
                logger.warn { "audio queue is almost full ${videoDecoder?.queueCount()} ${audioDecoder?.queueCount()}" }
                Thread.sleep(200)
                return
            }

            val packet = if (packetReader != null) packetReader?.nextPacket() else av_packet_alloc()
            av_read_frame(formatContext, packet)
            if (packet != null) {
                packetsReceived++
                if (hasSeeked && packetsReceived == 1) {
                    logger.debug { "seek completed"}
                    seekCompleted()
                }

                if (hasSeeked && packetsReceived == 2) {
                    val packetTime = packet.dts() / 1000.0
                    logger.debug { "seek error: ${packetTime - seekPosition}" }
                }
                when (packet.stream_index()) {
                    videoStreamIndex -> videoDecoder?.decodeVideoPacket(packet)
                    audioStreamIndex -> audioDecoder?.decodeAudioPacket(packet)
                }
                av_packet_unref(packet)
            } else {
                if (packetReader?.endOfFile == true) {
                    Thread.sleep(10)
                } else {
                    logger.debug { "more frames are needed but none are received" }
                    Thread.sleep(1)
                }
            }
        }
    }

    fun peekNextVideoFrame(): VideoFrame? {
        return videoDecoder?.peekNextFrame()
    }

    fun nextVideoFrame(): VideoFrame? {
        return videoDecoder?.nextFrame()
    }

    fun nextAudioFrame(): AudioFrame? {
        return audioDecoder?.nextFrame()
    }

    fun videoQueueSize(): Int {
        return videoDecoder?.queueCount() ?: 0
    }

    fun audioQueueSize(): Int {
        return audioDecoder?.queueCount() ?: 0
    }

    fun audioVideoSynced() = (audioDecoder?.isSynced() ?: true) || done()
}