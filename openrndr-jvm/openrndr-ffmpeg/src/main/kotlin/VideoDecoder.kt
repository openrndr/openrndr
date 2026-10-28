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

/**
 * A decoded frame, either converted to BGR32 in [buffer] or kept on the GPU in [hardwareFrame]
 */
internal data class VideoFrame(
    val buffer: AVBufferRef?,
    val lineSize: Int,
    val timeStamp: Double,
    val frameSize: Int,
    val hardwareFrame: AVFrame? = null
) {
    fun unref() {
        buffer?.let { av_buffer_unref(it) }
        hardwareFrame?.let { av_frame_free(it) }
    }
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
    hwType: Int,
    /** queue VideoToolbox frames as they are, instead of converting them to BGR32 */
    private val outputHardwareFrames: Boolean = false
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
    private val minVideoFrames = configuration.videoFrameQueueSize

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
        logger.debug { "disposing video decoder" }
        synchronized(videoQueue) {
            while (!videoQueue.isEmpty()) videoQueue.pop().unref()
        }
        av_frame_free(scaledVideoFrame)
        imagePointer.forEach {
            av_free(it)
        }
        swscale.sws_freeContext(softwareScalingContext)
    }

    fun queueCount() = videoQueue.size()
    fun isQueueEmpty() = videoQueue.isEmpty()
    fun isQueueAlmostFull() = videoQueue.size() > videoQueue.maxSize - 2
    fun needMoreFrames() = (videoQueue.size() < minVideoFrames)

    fun peekNextFrame() = synchronized(videoQueue) { videoQueue.peek() }
    fun nextFrame() = synchronized(videoQueue) { videoQueue.popOrNull() }

    fun flushQueue() {
        synchronized(videoQueue) {
            while (!videoQueue.isEmpty()) {
                val a = videoQueue.popOrNull()
                a?.unref()
            }
        }
    }

    fun flushBuffers() {
        avcodec_flush_buffers(videoCodecContext)
    }

    fun decodeVideoPacket(stream: AVStream, packet: AVPacket, seekPosition: Double) {
        val start = System.currentTimeMillis()
        val ret = avcodec_send_packet(videoCodecContext, packet)

        statistics.videoBytesReceived += packet.size()

        if (ret < 0) {
            if (ret != AVERROR_EOF)
                logger.debug { "error in avcodec_send_packet: $ret" }
            return
        }
        receiveFrame(stream, seekPosition)
        val end = System.currentTimeMillis()
        statistics.videoDecodeDuration += (end - start)
    }

    /**
     * Signals the end of the stream to the decoder, the frames it delays are then received with [drainFrame]
     */
    fun startDraining() {
        avcodec_send_packet(videoCodecContext, null as AVPacket?)
    }

    /**
     * Receives one of the frames the decoder delays at the end of the stream
     * @return true when there may be more frames, false when the decoder is drained
     */
    fun drainFrame(stream: AVStream, seekPosition: Double): Boolean = receiveFrame(stream, seekPosition) == 0

    /**
     * Receives a decoded frame and queues it when it is not before [seekPosition]
     * @return the result of avcodec_receive_frame
     */
    private fun receiveFrame(stream: AVStream, seekPosition: Double): Int {
        val decodedFrame = av_frame_alloc()
        var ret = avcodec_receive_frame(videoCodecContext, decodedFrame)

        if (ret == AVERROR_EAGAIN()) {
            logger.trace { "try again" }
        } else if (ret == 0) {
            // streams without a known start time start at 0
            val startTime = stream.start_time().let { if (it == AV_NOPTS_VALUE) 0L else it }
            val pts = decodedFrame.best_effort_timestamp().let { if (it == AV_NOPTS_VALUE) decodedFrame.pts() else it }
            val ptss = if (pts == AV_NOPTS_VALUE) seekPosition else (pts - startTime).toDouble() * av_q2d(stream.time_base())
            logger.trace { "decoded frame pts: ${ptss}s" }

            if (ptss >= seekPosition && outputHardwareFrames && decodedFrame.format() == AV_PIX_FMT_VIDEOTOOLBOX) {
                // the clone references the frame's CVPixelBuffer, which returns to the decoder's pool when freed
                val hardwareFrame = av_frame_clone(decodedFrame)
                synchronized(videoQueue) {
                    videoQueue.push(VideoFrame(null, 0, ptss, 0, hardwareFrame))
                }
                statistics.videoQueueSize = videoQueue.size()
                statistics.videoFramesDecoded += 1
                av_frame_free(decodedFrame)
                return 0
            }

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
                synchronized(videoQueue) {
                    videoQueue.push(VideoFrame(buffer, scaledVideoFrame.linesize()[0], ptss, scaledFrameSize))
                }
                statistics.videoQueueSize = videoQueue.size()
                statistics.videoFramesDecoded += 1
            } else {
                logger.trace { "skipping video frame: $ptss < $seekPosition" }
                statistics.videoFrameErrors += 1
            }
            av_frame_free(transferredFrame)
            ret = 0
        }
        av_frame_free(decodedFrame)
        return ret
    }
}