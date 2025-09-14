package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVBufferRef
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.ffmpeg.global.swresample.*
import org.bytedeco.ffmpeg.swresample.SwrContext
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.javacpp.Pointer.memcpy

private val logger = KotlinLogging.logger {}

internal enum class SampleFormat {
    INVALID,
    S16
}

internal data class AudioOutput(val sampleRate: Int, val channels: Int, val sampleFormat: SampleFormat)

internal fun AudioOutput.toAudioDecoderOutput(): AudioDecoderOutput? {
    val avSampleFormat = sampleFormat.toAVSampleFormat() ?: return null
    return AudioDecoderOutput(sampleRate, channels, AV_CH_LAYOUT_STEREO, avSampleFormat)
}

private fun SampleFormat.toAVSampleFormat() = when (this) {
    SampleFormat.S16 -> AV_SAMPLE_FMT_S16.toLong()
    SampleFormat.INVALID -> null
}

internal data class AudioInfo(val sampleRate: Int, val channels: Int)

internal class AudioFrame(val buffer: AVBufferRef, var position: Int, val size: Int, val timeStamp: Double) {
    fun unref() = av_buffer_unref(buffer)
}

internal data class AudioDecoderOutput(
    val sampleRate: Int,
    val channels: Int,
    val channelLayout: Long,
    val sampleFormat: Long
)

internal class AudioDecoder(
    private val audioCodecContext: AVCodecContext,
    val output: AudioDecoderOutput
) {
    private val audioFrame = av_frame_alloc()

    private val resampledAudioFrame = av_frame_alloc()
    private val resampleContext: SwrContext
    private val minAudioFrames = 10
    private val maxAudioFrames = 90

    internal val audioQueue = Queue<AudioFrame>(maxAudioFrames)

    init {
        resampleContext = swr_alloc()

        val result: Int = swr_alloc_set_opts2(
            /* ps = */ resampleContext,
            /* out_ch_layout = */ AV_CHANNEL_LAYOUT_STEREO,
            /* out_sample_fmt = */ AV_SAMPLE_FMT_S16,
            /* out_sample_rate = */ output.sampleRate,
            /* in_ch_layout = */ audioCodecContext.ch_layout(),
            /* in_sample_fmt = */ audioCodecContext.sample_fmt(),
            /* in_sample_rate = */ audioCodecContext.sample_rate(),
            /* log_offset = */ 0,
            /* log_ctx = */ null
        )
        require(result == 0)

        swr_init(resampleContext).checkAVError()
    }

    private fun setResampleOpt(name: String, value: Long) =
        av_opt_set_int(resampleContext, name, value, 0)

    private fun setResampleFmt(name: String, value: Int) =
        av_opt_set_sample_fmt(resampleContext, name, value, 0)


    fun dispose() {
        synchronized(audioQueue) {
            while (!audioQueue.isEmpty()) audioQueue.pop().unref()
        }
    }

    fun isSynced(): Boolean = audioQueue.size() < maxAudioFrames

    fun isQueueEmpty() = audioQueue.isEmpty()
    fun isQueueAlmostFull() = audioQueue.size() > audioQueue.maxSize - 2
    fun needMoreFrames() = (audioQueue.size() < minAudioFrames).apply {
        if (this) {
            //println("audio player needs more frames: q: ${audioQueue.size()} < ${minAudioFrames}")
        }
    }

    fun nextFrame(size: Int): AudioFrame? {
        val frame = audioQueue.peek() ?: return null
        val realSize = if (frame.position + size > frame.size) frame.size - frame.position else size
        return if (frame.position + realSize == frame.size) {
            audioQueue.pop()
        } else {
            val result = AudioFrame(av_buffer_ref(frame.buffer), frame.position, frame.size, frame.timeStamp)
            frame.position += realSize
            result
        }
    }

    fun nextFrame(): AudioFrame? {
        return audioQueue.popOrNull()
    }

    fun queueCount() = audioQueue.size()

    fun flushQueue() {
        logger.debug { "flushing audio queue" }
        try {
            synchronized(audioQueue) {
                while (!audioQueue.isEmpty()) audioQueue.popOrNull()?.unref()
            }
        } catch (e: Throwable) {
            logger.error { "audio Queue race condition fail" }
        }
    }

    fun flushBuffers() {
        avcodec.avcodec_flush_buffers(audioCodecContext)
    }

    var first = 0
    fun decodeAudioPacket(stream: AVStream, packet: AVPacket, seekPosition: Double) {
        var ret = avcodec.avcodec_send_packet(audioCodecContext, packet)
        if (ret < 0) {
            logger.error { "error in avcodec_send_packet: $ret" }
            if (ret == AVERROR_EAGAIN()) {
                logger.debug { "again" }
            }

            if (ret != AVERROR_EOF)
                logger.debug { "error in avcodec_send_packet: $ret" }
            return
        }

        val decodedFrame = av_frame_alloc()
        ret = avcodec.avcodec_receive_frame(audioCodecContext, decodedFrame)
        val ptss = (decodedFrame.pts() - stream.start_time()).toDouble() * av_q2d(stream.time_base())


        if (ret == 0 && ptss > seekPosition) {
//                println("channel layout ${decodedFrame.channel_layout()}")
//                println("channels ${decodedFrame.channels()}")
//                println("format ${decodedFrame.format()}")
//                println("rate ${decodedFrame.sample_rate()}")

            resampledAudioFrame.sample_rate(output.sampleRate)
            resampledAudioFrame.format(AV_SAMPLE_FMT_S16)
            resampledAudioFrame.ch_layout(AV_CHANNEL_LAYOUT_STEREO)


            swr_config_frame(resampleContext, resampledAudioFrame, decodedFrame)
            val result = swr_convert_frame(resampleContext, resampledAudioFrame, decodedFrame)
            if (result == 0) {
                with(resampledAudioFrame) {
                    val audioFrameSize =
                        av_samples_get_buffer_size(
                            null as IntPointer?,
                            ch_layout().nb_channels(),
                            nb_samples(),
                            format(),
                            1
                        )
                    val buffer = av_buffer_alloc(audioFrameSize.toLong())!!
                    val ts = (audioFrame.best_effort_timestamp()) * av_q2d(audioCodecContext.time_base())
                    memcpy(buffer.data(), data()[0], audioFrameSize.toLong())
                    synchronized(audioQueue) {
                        logger.trace { "queuing audio frame" }
                        audioQueue.push(AudioFrame(buffer, 0, audioFrameSize, ts))
                    }
                }
            } else {
                logger.error { "there was an error: $result" }
                result.checkAVError()
            }


        }
        av_frame_free(decodedFrame)

    }
}
