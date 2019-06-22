package org.openrndr.ffmpeg

import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.avcodec.avcodec_receive_frame
import org.bytedeco.javacpp.avcodec.avcodec_send_packet
import org.bytedeco.javacpp.avutil.*
import java.nio.DoubleBuffer

private fun Int.toAVPixelFormat(): Int =
        avutil.AV_PIX_FMT_BGR32

internal data class VideoOutput(val size: Dimensions, val pixelFormat: Int)

internal fun VideoOutput.toVideoDecoderOutput(): VideoDecoderOutput? {
    val avPixelFormat = pixelFormat.toAVPixelFormat() ?: return null
    return VideoDecoderOutput(size.copy(), avPixelFormat)
}


internal data class VideoFrame(val buffer: avutil.AVBufferRef, val lineSize: Int, val timeStamp: Double, val frameSize:Int) {
    fun unref() = avutil.av_buffer_unref(buffer)
}

internal data class VideoInfo(val size: Dimensions, val fps: Double)

private fun Int.pixFmtForHWType():Int {
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
        private val videoCodecContext: avcodec.AVCodecContext,
        output: VideoDecoderOutput,
        hwType:Int
) {
    private val windowSize = output.size
    private val avPixelFormat = output.avPixelFormat
    private val videoSize = Dimensions(videoCodecContext.width(), videoCodecContext.height())
    private val videoFrame = avutil.av_frame_alloc()

    private val scaledVideoFrame = avutil.av_frame_alloc()
    private val hwPixFmt = hwType.pixFmtForHWType()
    private var softwareScalingContext: swscale.SwsContext? = null

    private val scaledFrameSize = avutil.av_image_get_buffer_size(avPixelFormat, windowSize.w, windowSize.h, 1)
    private val imagePointer = arrayOf(BytePointer(avutil.av_malloc(scaledFrameSize.toLong())).capacity(scaledFrameSize.toLong()))
    private val videoQueue = Queue<VideoFrame>(100)
    private val minVideoFrames =50



    init {
        avutil.av_image_fill_arrays(PointerPointer<avutil.AVFrame>(scaledVideoFrame), scaledVideoFrame.linesize(), imagePointer[0], avPixelFormat, windowSize.w, windowSize.h, 1)
    }

    fun dispose() {
        while (!videoQueue.isEmpty()) videoQueue.pop().unref()
    }

    fun queueCount() = videoQueue.size()
    fun isQueueEmpty() = videoQueue.isEmpty()
    fun isQueueAlmostFull() = videoQueue.size() > videoQueue.maxSize - 5
    fun needMoreFrames() = videoQueue.size() < minVideoFrames

    fun peekNextFrame() = videoQueue.peek()

    fun nextFrame() = videoQueue.popOrNull()

    fun decodeVideoPacket(packet: avcodec.AVPacket, frameFinished: IntPointer) {
        // Decode video frame.
        avcodec.avcodec_decode_video2(videoCodecContext, videoFrame, frameFinished, packet)
        // Did we get a video frame?
        if (frameFinished.get() != 0) {
            // Convert the frame from its movie format to window pixel format.
            swscale.sws_scale(softwareScalingContext, videoFrame.data(),
                    videoFrame.linesize(), 0, videoSize.h,
                    scaledVideoFrame.data(), scaledVideoFrame.linesize())
            // TODO: reuse buffers!
            val buffer = avutil.av_buffer_alloc(scaledFrameSize)
            val packetTimestamp = videoFrame.best_effort_timestamp()// avutil.av_frame_get_best_effort_timestamp(videoFrame)

            val timeStamp = packetTimestamp * avutil.av_q2d(videoCodecContext.time_base())
            Pointer.memcpy(buffer.data(), scaledVideoFrame.data()[0], scaledFrameSize.toLong())
            videoQueue.push(VideoFrame(buffer, scaledVideoFrame.linesize()[0], timeStamp, scaledFrameSize))
        }
    }

    var lowestTimeStamp = Long.MAX_VALUE
    fun decodeVideoPacket2(packet: avcodec.AVPacket, frameFinished: IntPointer) {
        var ret = avcodec_send_packet(videoCodecContext, packet)

        if (ret < 0) {
            println("error in avcodec_send_packet")
            return
        }


        while (ret >= 0) {
            val decodedFrame = av_frame_alloc()
            ret = avcodec_receive_frame(videoCodecContext, decodedFrame)

            if (ret == org.bytedeco.javacpp.presets.avutil.AVERROR_EAGAIN()) {
                av_frame_free(decodedFrame)
                return
            }

            val transferredFrame = av_frame_alloc()
            val resultFrame:AVFrame

            if (decodedFrame.format() == hwPixFmt) {
                ret = av_hwframe_transfer_data(transferredFrame, decodedFrame, 0)
                ret.checkAVError()
                resultFrame = transferredFrame
            } else {
                resultFrame = decodedFrame
            }

            if (softwareScalingContext == null) {
                softwareScalingContext = swscale.sws_getCachedContext(null,
                        videoSize.w, videoSize.h,
                        resultFrame.format(),
                        windowSize.w, windowSize.h, avPixelFormat,
                        swscale.SWS_BILINEAR, null as swscale.SwsFilter?, null as swscale.SwsFilter?, null as DoubleBuffer?)
            }

            val buffer = avutil.av_buffer_alloc(scaledFrameSize)

            swscale.sws_scale(softwareScalingContext, resultFrame.data(),
                    resultFrame.linesize(), 0, resultFrame.height(),
                    scaledVideoFrame.data(), scaledVideoFrame.linesize())

            lowestTimeStamp = Math.min(lowestTimeStamp, decodedFrame.best_effort_timestamp())
            val packetTimestamp = decodedFrame.best_effort_timestamp()// avutil.av_frame_get_best_effort_timestamp(videoFrame)
            val timeStamp = (packetTimestamp-lowestTimeStamp) * avutil.av_q2d(videoCodecContext.time_base())

            //println("timestamps: $timeStamp $lowestTimeStamp $packetTimestamp")

            Pointer.memcpy(buffer.data(), scaledVideoFrame.data()[0], scaledFrameSize.toLong())
            videoQueue.push(VideoFrame(buffer, scaledVideoFrame.linesize()[0], timeStamp/1000.0, scaledFrameSize))
            frameFinished.put(1)
            av_frame_free(decodedFrame)
            av_frame_free(transferredFrame)
        }
    }
}
