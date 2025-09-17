package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVBufferRef
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.avcodec.*
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.ffmpeg.global.swscale
import org.bytedeco.ffmpeg.swscale.SwsContext
import org.bytedeco.ffmpeg.swscale.SwsFilter
import org.bytedeco.javacpp.*
import java.nio.DoubleBuffer

private val logger = KotlinLogging.logger {}

internal data class VideoOutput(val size: Dimensions, val pixelFormat: Int)

internal fun VideoOutput.toVideoDecoderOutput(): VideoDecoderOutput {
    val avPixelFormat = AV_PIX_FMT_BGR32
    return VideoDecoderOutput(size.copy(), avPixelFormat)
}

internal data class VideoFrame(val buffer: AVBufferRef, val lineSize: Int, val timeStamp: Double, val frameSize: Int) {
    fun unref() = av_buffer_unref(buffer)
}

internal data class VideoInfo(val size: Dimensions, val fps: Double)

private fun Int.pixFmtForHWType(): Int {
    return when (this) {
        AV_HWDEVICE_TYPE_VAAPI -> AV_PIX_FMT_VAAPI
        AV_HWDEVICE_TYPE_MEDIACODEC -> AV_PIX_FMT_MEDIACODEC
        AV_HWDEVICE_TYPE_DXVA2 -> AV_PIX_FMT_DXVA2_VLD
        AV_HWDEVICE_TYPE_D3D11VA -> AV_PIX_FMT_D3D11
        AV_HWDEVICE_TYPE_VDPAU -> AV_PIX_FMT_VDPAU
        AV_HWDEVICE_TYPE_VIDEOTOOLBOX -> AV_PIX_FMT_VIDEOTOOLBOX
        else -> AV_PIX_FMT_NONE
    }
}

internal data class VideoDecoderOutput(val size: Dimensions, val avPixelFormat: Int)

internal class VideoDecoder(
    val statistics: VideoStatistics,
    configuration: VideoPlayerConfiguration,
    private val videoCodecContext: AVCodecContext,
    output: VideoDecoderOutput,
    hwType: Int
) {
    private val windowSize = output.size
    private val avPixelFormat = output.avPixelFormat
    private val videoSize = Dimensions(videoCodecContext.width(), videoCodecContext.height())

    private val scaledVideoFrame = av_frame_alloc()
    private val hwPixFmt = hwType.pixFmtForHWType()
    private var softwareScalingContext: SwsContext? = null

    private val scaledFrameSize = av_image_get_buffer_size(avPixelFormat, windowSize.w, windowSize.h, 1)
    private val imagePointer =
        arrayOf(BytePointer(av_malloc(scaledFrameSize.toLong())).capacity(scaledFrameSize.toLong()))
    private val videoQueue = Queue<VideoFrame>(configuration.videoFrameQueueSize)
    private val minVideoFrames = 5 //configuration.videoFrameQueueSize

    init {
        av_image_fill_arrays(
            PointerPointer<AVFrame>(scaledVideoFrame),
            scaledVideoFrame.linesize(),
            imagePointer[0],
            avPixelFormat,
            windowSize.w,
            windowSize.h,
            1
        )
    }

    fun dispose() {
        while (!videoQueue.isEmpty()) videoQueue.pop().unref()
    }

    fun queueCount() = videoQueue.size()
    fun isQueueEmpty() = videoQueue.isEmpty()
    fun isQueueAlmostFull() = videoQueue.size() > videoQueue.maxSize - 2
    fun needMoreFrames() = (videoQueue.size() < minVideoFrames)

    fun peekNextFrame() = videoQueue.peek()
    fun nextFrame() = videoQueue.popOrNull()

    fun flushQueue() {
        while (!videoQueue.isEmpty()) {
            val a = videoQueue.popOrNull()
            a?.unref()
        }
    }

    fun flushBuffers() {
        avcodec_flush_buffers(videoCodecContext)
    }

    fun decodeVideoPacket(stream: AVStream, packet: AVPacket, seekPosition: Double) {
        val start = System.currentTimeMillis()
        var ret = avcodec_send_packet(videoCodecContext, packet)

        statistics.videoBytesReceived += packet.size()

        if (ret < 0) {
            if (ret != AVERROR_EOF)
                logger.debug { "error in avcodec_send_packet: $ret" }
            return
        }

        while (true) {
            val decodedFrame = av_frame_alloc()
            ret = avcodec_receive_frame(videoCodecContext, decodedFrame)

            val ptss = (decodedFrame.pts() - stream.start_time()).toDouble() * av_q2d(stream.time_base())
            logger.trace { "decoded frame pts: ${ptss}s" }

            if (ret == AVERROR_EAGAIN()) {
                logger.trace { "try again" }
                av_frame_free(decodedFrame)
                break
            }

            if (ret == 0) {
                val transferredFrame = av_frame_alloc()
                if (ptss >= seekPosition) {
                    val resultFrame: AVFrame
                    if (decodedFrame.format() == hwPixFmt) {
                        ret = av_hwframe_transfer_data(transferredFrame, decodedFrame, 0)
                        ret.checkAVError()
                        resultFrame = transferredFrame
                    } else {
                        resultFrame = decodedFrame
                    }
                    if (softwareScalingContext == null) {
                        softwareScalingContext = swscale.sws_getCachedContext(
                            null,
                            videoSize.w, videoSize.h,
                            resultFrame.format(),
                            windowSize.w, windowSize.h, avPixelFormat,
                            swscale.SWS_BILINEAR, null as SwsFilter?, null as SwsFilter?, null as DoubleBuffer?
                        )
                    }

                    swscale.sws_scale(
                        softwareScalingContext, resultFrame.data(),
                        resultFrame.linesize(), 0, resultFrame.height(),
                        scaledVideoFrame.data(), scaledVideoFrame.linesize()
                    )

                    val buffer = av_buffer_alloc(scaledFrameSize.toLong())
                    Pointer.memcpy(buffer.data(), scaledVideoFrame.data()[0], scaledFrameSize.toLong())
                    videoQueue.push(VideoFrame(buffer, scaledVideoFrame.linesize()[0], ptss, scaledFrameSize))
                    av_frame_free(transferredFrame)
                    statistics.videoQueueSize = videoQueue.size()
                    statistics.videoFramesDecoded += 1
                } else {
                    logger.trace { "skipping video frame: $ptss < $seekPosition" }
                    statistics.videoFrameErrors += 1
                }
            }
            av_frame_free(decodedFrame)
            break
        }
        val end = System.currentTimeMillis()
        statistics.videoDecodeDuration += (end - start)
    }
}