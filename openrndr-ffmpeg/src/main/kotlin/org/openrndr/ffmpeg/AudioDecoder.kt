package org.openrndr.ffmpeg

import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avutil.AVBufferRef
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avcodec.avcodec_decode_audio4
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.ffmpeg.global.swresample.*
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.javacpp.Pointer.memcpy


internal enum class SampleFormat {
    INVALID,
    S16
}

internal data class AudioOutput(val sampleRate: Int, val channels: Int, val sampleFormat: SampleFormat)


internal fun AudioOutput.toAudioDecoderOutput(): AudioDecoderOutput? {
    val avSampleFormat = sampleFormat.toAVSampleFormat() ?: return null
    if (channels != 2) return null // only stereo output is supported for now
    return AudioDecoderOutput(sampleRate, channels, AV_CH_LAYOUT_STEREO, avSampleFormat)
}

private fun SampleFormat.toAVSampleFormat() = when (this) {
    SampleFormat.S16 -> AV_SAMPLE_FMT_S16.toLong()
    SampleFormat.INVALID -> null
}

internal data class AudioInfo(val sampleRate: Int, val channels: Int)

internal class AudioFrame(val buffer: AVBufferRef, var position: Int, val size: Int, val timeStamp: Double) {
    fun unref() = avutil.av_buffer_unref(buffer)
}

internal data class AudioDecoderOutput(
        val sampleRate: Int,
        val channels: Int,
        val channelLayout: Long,
        val sampleFormat: Long)

internal class AudioDecoder(
        private val audioCodecContext: AVCodecContext,
        output: AudioDecoderOutput
) {
    private val audioFrame = av_frame_alloc()
    private val resampledAudioFrame = av_frame_alloc()
    private val resampleContext = swr_alloc()

    private val audioQueue = Queue<AudioFrame>(100)

    private val minAudioFrames = 2
    private val maxAudioFrames = 5

    init {
        with(resampledAudioFrame) {
            channels(output.channels)
            sample_rate(output.sampleRate)
            format(output.sampleFormat.toInt())
            channel_layout(output.channelLayout)
        }

        with(audioCodecContext) {
            setResampleOpt("in_channel_layout", channel_layout())
            setResampleOpt("out_channel_layout", output.channelLayout)
            setResampleOpt("in_sample_rate", sample_rate().toLong())
            setResampleOpt("out_sample_rate", output.sampleRate.toLong())
            setResampleOpt("in_sample_fmt", sample_fmt().toLong())
            setResampleOpt("out_sample_fmt", output.sampleFormat)
        }
        swr_init(resampleContext)
    }

    private fun setResampleOpt(name: String, value: Long) =
            av_opt_set_int(resampleContext, name, value, 0)

    fun dispose() {
        while (!audioQueue.isEmpty()) audioQueue.pop().unref()
    }

    fun isSynced(): Boolean = audioQueue.size() < maxAudioFrames

    fun isQueueEmpty() = audioQueue.isEmpty()
    fun isQueueAlmostFull() = audioQueue.size() > audioQueue.maxSize - 20
    fun needMoreFrames() = audioQueue.size() < minAudioFrames

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

    fun flushQueue() {
        while (!audioQueue.isEmpty()) audioQueue.pop().unref()
    }

    fun decodeAudioPacket(packet: AVPacket) {
        val frameFinished = IntArray(1)
        while (packet.size() > 0) {
            val size = avcodec_decode_audio4(audioCodecContext, audioFrame, frameFinished, packet)
            if (frameFinished[0] != 0) {
                // Put audio frame to decoder's queue.
                swr_convert_frame(resampleContext, resampledAudioFrame, audioFrame).checkAVError()
                with(resampledAudioFrame) {
                    val audioFrameSize = av_samples_get_buffer_size(null as IntPointer?, channels(), nb_samples(), format(), 1)
                    val buffer = av_buffer_alloc(audioFrameSize)!!
                    val ts = (audioFrame.best_effort_timestamp()) * av_q2d(audioCodecContext.time_base())
                    memcpy(buffer.data(), data()[0], audioFrameSize.toLong())
                    audioQueue.push(AudioFrame(buffer, 0, audioFrameSize, ts))
                }
            }
            packet.size(packet.size() - size)
            packet.data(packet.data().position(size.toLong()))
        }
    }
}
