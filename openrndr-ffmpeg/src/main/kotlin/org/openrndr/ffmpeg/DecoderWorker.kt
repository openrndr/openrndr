package org.openrndr.ffmpeg

import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.Pointer.memcpy
import org.bytedeco.javacpp.avcodec.*
import org.bytedeco.javacpp.avformat.av_read_frame
import org.bytedeco.javacpp.avutil.*
import org.bytedeco.javacpp.swscale.*
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import java.util.*

data class VideoInfo(val size: Dimensions, val fps: Double)
data class AudioInfo(val sampleRate: Int, val channels: Int)

data class CodecInfo(val video: VideoInfo?, val audio: AudioInfo?) {
    val hasVideo = video != null
    val hasAudio = audio != null
}

class VideoFrame(val buffer: avutil.AVBufferRef, val lineSize: Int, val timeStamp: Double) {

    fun unref() = avutil.av_buffer_unref(buffer)

}

class AudioFrame(val buffer: avutil.AVBufferRef, var position: Int, val size: Int, val timeStamp: Double) {
    fun unref() = avutil.av_buffer_unref(buffer)
}



fun Int.checkAVError() {
    if (this != 0) {
        val buffer = ByteArray(1024)
        avutil.av_strerror(this, buffer, 1024L)
        throw Error("AVError: ${String(buffer)}")
    }
}

//private val avformat.AVFormatContext.codecs: List<avcodec.AVCodecContext?>
//    get() = List(nb_streams.toInt()) { streams?.get(it)?.pointed?.codec?.pointed }
//
//private fun avformat.AVFormatContext.streamAt(index: Int): avformat.AVStream? =
//        if (index < 0) null else streams?.get(index)?.pointed
//
//private fun PixelFormat.toAVPixelFormat(): AVPixelFormat? = when (this) {
//    PixelFormat.RGB24 -> AV_PIX_FMT_RGB24
//    PixelFormat.ARGB32 -> AV_PIX_FMT_RGB32
//    PixelFormat.INVALID -> null
//}


data class VideoDecoderOutput(val size: Dimensions, val avPixelFormat: Int)


data class AudioDecoderOutput(
        val sampleRate: Int,
        val channels: Int,
        val channelLayout: Long,
        val sampleFormat: Long)

private class VideoDecoder(
        private val videoCodecContext: avcodec.AVCodecContext,
        output: VideoDecoderOutput
) {
    private val windowSize = output.size
    private val avPixelFormat = output.avPixelFormat
    private val videoSize = Dimensions(videoCodecContext.width(), videoCodecContext.height())
    private val videoFrame = avutil.av_frame_alloc()

    private val scaledVideoFrame = avutil.av_frame_alloc()
    private val softwareScalingContext =
            sws_getContext(
                    videoSize.w, videoSize.h,
                    videoCodecContext.pix_fmt(),
                    windowSize.w, windowSize.h, avPixelFormat,
                    SWS_BILINEAR, null as swscale.SwsFilter?, null as swscale.SwsFilter?, null as DoubleBuffer?)


    //private val scaledFrameSize = avpicture_get_size(avPixelFormat, windowSize.w, windowSize.h)
    private val scaledFrameSize = av_image_get_buffer_size(avPixelFormat, windowSize.w, windowSize.h, 1)
    val image_ptr = arrayOf(BytePointer(av_malloc(scaledFrameSize.toLong())).capacity(scaledFrameSize.toLong()))
    val image_buf = arrayOf<Buffer>(image_ptr[0].asBuffer())

    private val buffer = ByteBuffer.allocateDirect(scaledFrameSize)

    private val videoQueue = Queue<VideoFrame>(100)

    private val minVideoFrames = 5

    init {




        av_image_fill_arrays(PointerPointer<AVFrame>(scaledVideoFrame), scaledVideoFrame.linesize(), image_ptr[0], avPixelFormat, windowSize.w, windowSize.h, 1)


        //avpicture_fill(scaledVideoFrame, buffer,
          //      avPixelFormat, windowSize.w, windowSize.h)

//        avpicture_fill()

    }

    fun dispose() {
        while (!videoQueue.isEmpty()) videoQueue.pop().unref()
    }

    fun isQueueEmpty() = videoQueue.isEmpty()
    fun isQueueAlmostFull() = videoQueue.size() > videoQueue.maxSize - 5
    fun needMoreFrames() = videoQueue.size() < minVideoFrames
    fun nextFrame() = videoQueue.popOrNull()

    fun decodeVideoPacket(packet: avcodec.AVPacket, frameFinished: IntPointer) {
        // Decode video frame.
        avcodec_decode_video2(videoCodecContext, videoFrame, frameFinished, packet)
        // Did we get a video frame?
        if (frameFinished.get() != 0) {
            // Convert the frame from its movie format to window pixel format.
            sws_scale(softwareScalingContext, videoFrame.data(),
                    videoFrame.linesize(), 0, videoSize.h,
                    scaledVideoFrame.data(), scaledVideoFrame.linesize())
            // TODO: reuse buffers!
            val buffer = av_buffer_alloc(scaledFrameSize)
            val ts = av_frame_get_best_effort_timestamp(videoFrame) *
                    av_q2d(videoCodecContext.time_base())
            memcpy(buffer.data(), scaledVideoFrame.data()[0], scaledFrameSize.toLong())
            videoQueue.push(VideoFrame(buffer, scaledVideoFrame.linesize()[0], ts))
        }
    }

}


class Decoder(val formatContext: avformat.AVFormatContext,
              val videoStreamIndex: Int,
              val audioStreamIndex: Int,
              val videoCodecContext: avcodec.AVCodecContext?,
              val audioCodecContext: avcodec.AVCodecContext?) {

    private var video: VideoDecoder? = null
    //private var audio: AudioDecoder? = null
    var noMoreFrames = false

    fun start(videoOutput: VideoDecoderOutput?, audioOutput: AudioDecoderOutput?) {
        video = videoCodecContext?.let { ctx ->
            videoOutput?.let { VideoDecoder(ctx, it) }
        }
        noMoreFrames = false
        decodeIfNeeded()
    }

    fun done() = noMoreFrames && (video?.isQueueEmpty()?:true)

    fun dispose() {
        video?.dispose()
    }

    fun needMoreFrames():Boolean =
            (video?.needMoreFrames()?:false)



    fun decodeIfNeeded() {
        if (!needMoreFrames()) {
            println("I need no more frames")
            return
        }
        if (video?.isQueueAlmostFull() == true) {
            println("queue is almost full")
            return
        }

        println("decoding frame")
        val packet = av_packet_alloc()
        val frameFinished = IntPointer(1)

        while (needMoreFrames() && av_read_frame(formatContext, packet) >= 0) {

            when (packet.stream_index()) {
                videoStreamIndex -> video?.decodeVideoPacket(packet, frameFinished)
            }
            av_packet_unref(packet)
        }
        if (needMoreFrames()) noMoreFrames = true
        frameFinished.deallocate()
    }

    fun nextVideoFrame(): VideoFrame? {
        decodeIfNeeded()
        return video?.nextFrame()
    }

}