package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVBufferRef
import org.bytedeco.ffmpeg.avutil.AVChannelLayout
import org.bytedeco.ffmpeg.avutil.AVFrame
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
    val layout = if (channels == 1) { AV_CHANNEL_LAYOUT_MONO } else { AV_CHANNEL_LAYOUT_STEREO }

    return AudioDecoderOutput(sampleRate, channels,layout, avSampleFormat)
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
    val channelLayout: AVChannelLayout,
    val sampleFormat: Long
)

internal class AudioDecoder(
    private val audioCodecContext: AVCodecContext,
    val output: AudioDecoderOutput
) {
    private var firstSwr = true
    private val audioFrame = av_frame_alloc()

    private val resampleContext: SwrContext = swr_alloc() //= swr_alloc()
    private val minAudioFrames = 10
    private val maxAudioFrames = 55

    internal val audioQueue = Queue<AudioFrame>(maxAudioFrames)

    init {
        val result: Int = swr_alloc_set_opts2(
            /* ps = */ resampleContext,
            /* out_ch_layout = */ output.channelLayout,
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

    private fun configureResampler(output: AVFrame, input: AVFrame) {
        swr_close(resampleContext)
        swr_config_frame(resampleContext, output, input).checkAVError()
        swr_init(resampleContext).checkAVError()
    }

    private fun setResampleOpt(name: String, value: Long) =
        av_opt_set_int(resampleContext, name, value, 0)

    private fun setResampleFmt(name: String, value: Int) =
        av_opt_set_sample_fmt(resampleContext, name, value, 0)

    fun dispose() {
        logger.debug { "disposing audio decoder" }
        synchronized(audioQueue) {
            while (!audioQueue.isEmpty()) audioQueue.pop().unref()
        }
        swr_close(resampleContext)
        swr_free(resampleContext)
        av_frame_free(audioFrame)
    }

    fun isSynced(): Boolean = audioQueue.size() < maxAudioFrames

    fun isQueueEmpty() = audioQueue.isEmpty()
    fun isQueueAlmostFull() = audioQueue.size() > audioQueue.maxSize - 2
    fun needMoreFrames() = (audioQueue.size() < minAudioFrames)

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

    fun decodeAudioPacket(stream: AVStream, packet: AVPacket, seekPosition: Double) {
        val ret = avcodec.avcodec_send_packet(audioCodecContext, packet)
        if (ret < 0) {
            if (ret == AVERROR_EAGAIN()) {
                logger.debug { "again" }
            } else if (ret != AVERROR_EOF) {
                logger.error { "error in avcodec_send_packet: $ret" }
            }
            return
        }

        // streams without a known start time, like WAV files, start at 0
        val startTime = stream.start_time().let { if (it == AV_NOPTS_VALUE) 0L else it }

        // a packet can decode to any number of frames
        while (true) {
            val decodedFrame = av_frame_alloc()
            if (avcodec.avcodec_receive_frame(audioCodecContext, decodedFrame) != 0) {
                av_frame_free(decodedFrame)
                break
            }
            // frames of some formats, like WAV, have a channel layout without channel order. The resampler replaces
            // it with the default layout for the channel count and would then consider every frame changed.
            if (decodedFrame.ch_layout().order() == AV_CHANNEL_ORDER_UNSPEC) {
                val channels = decodedFrame.ch_layout().nb_channels()
                av_channel_layout_uninit(decodedFrame.ch_layout())
                av_channel_layout_default(decodedFrame.ch_layout(), channels)
            }
            val pts = decodedFrame.best_effort_timestamp().let { if (it == AV_NOPTS_VALUE) decodedFrame.pts() else it }
            // the time stamp in seconds, frames without a time stamp are never skipped
            val timeStamp = if (pts == AV_NOPTS_VALUE) Double.NaN else (pts - startTime).toDouble() * av_q2d(stream.time_base())

            // frames that end before the seek position are skipped, the frame that contains it is played
            val frameDuration = decodedFrame.nb_samples().toDouble() / decodedFrame.sample_rate().coerceAtLeast(1)
            if (timeStamp.isNaN() || timeStamp + frameDuration > seekPosition) {
                val resampledAudioFrame = av_frame_alloc()

                resampledAudioFrame.sample_rate(output.sampleRate)
                resampledAudioFrame.format(AV_SAMPLE_FMT_S16)
                resampledAudioFrame.ch_layout(output.channelLayout)

                // the resampler is configured from the frames, their layout and format can differ from the codec
                // context, for example the channel order of WAV files
                if (firstSwr) {
                    configureResampler(resampledAudioFrame, decodedFrame)
                    firstSwr = false
                }
                var result = swr_convert_frame(resampleContext, resampledAudioFrame, decodedFrame)
                if (result == AVERROR_INPUT_CHANGED) {
                    logger.debug { "audio input changed, reconfiguring resampler" }
                    configureResampler(resampledAudioFrame, decodedFrame)
                    result = swr_convert_frame(resampleContext, resampledAudioFrame, decodedFrame)
                }
                if (result == 0) {
                    val audioFrameSize = resampledAudioFrame.ch_layout().nb_channels() * resampledAudioFrame.nb_samples() * av_get_bytes_per_sample(resampledAudioFrame.format())

                    val buffer = av_buffer_alloc(audioFrameSize.toLong())
                    memcpy(buffer.data(), resampledAudioFrame.data()[0], audioFrameSize.toLong())
                    synchronized(audioQueue) {
                        // the queue holds maxSize - 1 frames
                        if (audioQueue.size() < audioQueue.maxSize - 1) {
                            logger.trace { "queuing audio frame" }
                            audioQueue.push(AudioFrame(buffer, 0, audioFrameSize, timeStamp))
                        } else {
                            logger.debug { "audio queue full, dropping audio frame" }
                            av_buffer_unref(buffer)
                        }
                    }
                } else {
                    logger.error { "there was an error: $result" }
                }
                av_frame_free(resampledAudioFrame)
                result.checkAVError()
            }
            av_frame_free(decodedFrame)
        }
    }
}
