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
import kotlin.jvm.JvmRecord

private val logger = KotlinLogging.logger {}

@JvmRecord
internal data class VideoOutput(val size: Dimensions, val pixelFormat: Int)

internal fun VideoOutput.toVideoDecoderOutput(): VideoDecoderOutput {
    val avPixelFormat = AV_PIX_FMT_BGR32
    return VideoDecoderOutput(size.copy(), avPixelFormat)
}

/**
 * A decoded frame, either converted to BGR32 in [buffer] or kept on the GPU in [hardwareFrame]
 */
@JvmRecord
internal data class VideoFrame(
    val buffer: AVBufferRef?,
    val lineSize: Int,
    val timeStamp: Double,
    val frameSize: Int,
    val color: ColorDescription,
    val hardwareFrame: AVFrame? = null,
    /** does [buffer] hold 16 bit RGBA instead of 8 bit RGBA */
    val rgb16: Boolean = false
) {
    fun unref() {
        buffer?.let { av_buffer_unref(it) }
        hardwareFrame?.let { av_frame_free(it) }
    }
}

@JvmRecord
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

/**
 * Makes [context] convert with the matrix and range of [frame]
 */
private fun setColorspace(context: SwsContext, frame: AVFrame) {
    val colorspace = when (yCbCrMatrix(frame)) {
        YCbCrMatrix.BT601 -> swscale.SWS_CS_ITU601
        YCbCrMatrix.BT709 -> swscale.SWS_CS_ITU709
        YCbCrMatrix.BT2020 -> swscale.SWS_CS_BT2020
    }
    val fullRange = if (frame.color_range() == AVCOL_RANGE_JPEG) 1 else 0
    swscale.sws_setColorspaceDetails(
        context,
        swscale.sws_getCoefficients(colorspace), fullRange,
        swscale.sws_getCoefficients(swscale.SWS_CS_DEFAULT), 1,
        0, 1 shl 16, 1 shl 16
    )
}

@JvmRecord
internal data class VideoDecoderOutput(val size: Dimensions, val avPixelFormat: Int)

internal class VideoDecoder(
    val statistics: VideoStatistics,
    configuration: VideoPlayerConfiguration,
    private val videoCodecContext: AVCodecContext,
    output: VideoDecoderOutput,
    hwType: Int,
    /** queue NV12 and P010 VideoToolbox and D3D11VA frames as they are, instead of converting them to BGR32 */
    outputHardwareFrames: Boolean = false
) {
    private val windowSize = output.size
    private val avPixelFormat = output.avPixelFormat
    private val videoSize = Dimensions(videoCodecContext.width(), videoCodecContext.height())

    private val hwPixFmt = hwType.pixFmtForHWType()

    /**
     * Converts decoded frames to packed RGB in [pixelFormat] on the CPU
     */
    private inner class RGBConversion(private val pixelFormat: Int) {
        private val frame = av_frame_alloc()
        val frameSize = av_image_get_buffer_size(pixelFormat, windowSize.w, windowSize.h, 1)
        private val image = BytePointer(av_malloc(frameSize.toLong())).capacity(frameSize.toLong())
        private var context: SwsContext? = null

        init {
            av_image_fill_arrays(
                PointerPointer<AVFrame>(frame), frame.linesize(), image, pixelFormat, windowSize.w, windowSize.h, 1
            )
        }

        val lineSize: Int get() = frame.linesize()[0]

        /**
         * Converts [source] using the matrix and range of [decodedFrame]
         * @return a buffer that holds the converted frame
         */
        fun convert(source: AVFrame, decodedFrame: AVFrame): AVBufferRef {
            val context = context ?: swscale.sws_getCachedContext(
                null,
                videoSize.w, videoSize.h,
                source.format(),
                windowSize.w, windowSize.h, pixelFormat,
                swscale.SWS_BILINEAR, null as SwsFilter?, null as SwsFilter?, null as DoubleBuffer?
            ).also {
                setColorspace(it, decodedFrame)
                context = it
            }
            swscale.sws_scale(context, source.data(), source.linesize(), 0, source.height(), frame.data(), frame.linesize())
            val buffer = av_buffer_alloc(frameSize.toLong())
            Pointer.memcpy(buffer.data(), frame.data()[0], frameSize.toLong())
            return buffer
        }

        fun dispose() {
            av_frame_free(frame)
            av_free(image)
            swscale.sws_freeContext(context)
        }
    }

    private val rgbConversion = RGBConversion(avPixelFormat)

    /** keeps the precision of HDR video, created for the first HDR frame */
    private var rgb16Conversion: RGBConversion? = null

    /** queued D3D11VA frames hold surfaces of the fixed size pool of the decoder */
    private val queueSize = if (outputHardwareFrames && hwType == AV_HWDEVICE_TYPE_D3D11VA) {
        d3d11QueueSize(configuration)
    } else {
        configuration.videoFrameQueueSize
    }

    private val videoQueue = Queue<VideoFrame>(queueSize)
    private val minVideoFrames = queueSize

    @Volatile
    private var keepFramesOnGpu = outputHardwareFrames

    /**
     * Converts frames that are decoded from now on on the CPU, for when hardware frames can not be shown
     */
    fun stopKeepingFramesOnGpu() {
        keepFramesOnGpu = false
    }

    fun dispose() {
        logger.debug { "disposing video decoder" }
        synchronized(videoQueue) {
            while (!videoQueue.isEmpty()) videoQueue.pop().unref()
        }
        rgbConversion.dispose()
        rgb16Conversion?.dispose()
        frameBeforeSeek?.let { av_frame_free(it) }
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
        frameBeforeSeek?.let { av_frame_free(it) }
        frameBeforeSeek = null
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
     * The last frame before the seek position. Frames can last long in variable frame rate video, this frame is shown
     * at the seek position when the first frame after the seek position starts after it.
     */
    private var frameBeforeSeek: AVFrame? = null

    /**
     * Receives a decoded frame and queues it when it is not before [seekPosition]
     * @return the result of avcodec_receive_frame
     */
    private fun receiveFrame(stream: AVStream, seekPosition: Double): Int {
        val decodedFrame = av_frame_alloc()
        val ret = avcodec_receive_frame(videoCodecContext, decodedFrame)

        if (ret == AVERROR_EAGAIN()) {
            logger.trace { "try again" }
        } else if (ret == 0) {
            // streams without a known start time start at 0
            val startTime = stream.start_time().let { if (it == AV_NOPTS_VALUE) 0L else it }
            val pts = decodedFrame.best_effort_timestamp().let { if (it == AV_NOPTS_VALUE) decodedFrame.pts() else it }
            val ptss = if (pts == AV_NOPTS_VALUE) seekPosition else (pts - startTime).toDouble() * av_q2d(stream.time_base())
            logger.trace { "decoded frame pts: ${ptss}s" }

            if (ptss < seekPosition) {
                logger.trace { "skipping video frame: $ptss < $seekPosition" }
                statistics.videoFrameErrors += 1
                frameBeforeSeek?.let { av_frame_free(it) }
                frameBeforeSeek = av_frame_clone(decodedFrame)
            } else {
                frameBeforeSeek?.let {
                    if (ptss > seekPosition) {
                        queueFrame(it, seekPosition)
                    }
                    av_frame_free(it)
                    frameBeforeSeek = null
                }
                queueFrame(decodedFrame, ptss)
            }
        } else if (ret == AVERROR_EOF) {
            // the seek position is after the start of the last frame
            frameBeforeSeek?.let {
                queueFrame(it, seekPosition)
                av_frame_free(it)
                frameBeforeSeek = null
            }
        }
        av_frame_free(decodedFrame)
        return ret
    }

    /**
     * Queues [decodedFrame] to be shown at [timeStamp]
     */
    private fun queueFrame(decodedFrame: AVFrame, timeStamp: Double) {
        val keepOnGpu = keepFramesOnGpu && when (decodedFrame.format()) {
            AV_PIX_FMT_VIDEOTOOLBOX -> CoreVideo.planarFormat(decodedFrame) != null
            AV_PIX_FMT_D3D11 -> isD3D11FrameSupported(decodedFrame)
            else -> false
        }
        if (keepOnGpu) {
            // the clone references the frame's CVPixelBuffer or D3D11 surface, which returns to the decoder's pool
            // when freed
            val hardwareFrame = av_frame_clone(decodedFrame)
            synchronized(videoQueue) {
                videoQueue.push(VideoFrame(null, 0, timeStamp, 0, ColorDescription.of(decodedFrame), hardwareFrame))
            }
        } else {
            val transferredFrame = av_frame_alloc()
            val resultFrame = if (decodedFrame.format() == hwPixFmt) {
                av_hwframe_transfer_data(transferredFrame, decodedFrame, 0).checkAVError()
                transferredFrame
            } else {
                decodedFrame
            }
            val color = ColorDescription.of(decodedFrame)
            val conversion = if (color.isHdr) {
                rgb16Conversion ?: RGBConversion(AV_PIX_FMT_RGBA64LE).also { rgb16Conversion = it }
            } else {
                rgbConversion
            }
            val buffer = conversion.convert(resultFrame, decodedFrame)
            av_frame_free(transferredFrame)
            synchronized(videoQueue) {
                videoQueue.push(
                    VideoFrame(
                        buffer, conversion.lineSize, timeStamp, conversion.frameSize, color,
                        rgb16 = conversion !== rgbConversion
                    )
                )
            }
        }
        statistics.videoQueueSize = videoQueue.size()
        statistics.videoFramesDecoded += 1
    }
}