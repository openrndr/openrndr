package org.openrndr.ffmpeg

import org.bytedeco.javacpp.avcodec
import org.bytedeco.javacpp.avcodec.avcodec_find_decoder
import org.bytedeco.javacpp.avcodec.avcodec_open2
import org.bytedeco.javacpp.avformat
import org.bytedeco.javacpp.avformat.*
import org.bytedeco.javacpp.avutil
import org.bytedeco.javacpp.avutil.*

enum class State {
    PLAYING,
    STOPPED,
    PAUSED;

    inline fun transition(from: State, to: State, block: () -> Unit): State =
            if (this == from) {
                block()
                to
            } else this
}

enum class PlayMode {
    VIDEO,
    AUDIO,
    BOTH;

    val useVideo: Boolean get() = this != AUDIO
    val useAudio: Boolean get() = this != VIDEO
}

data class Dimensions(val w: Int, val h: Int) {
    operator fun minus(other: Dimensions) = Dimensions(w - other.w, h - other.h)
    operator fun div(b: Int) = Dimensions(w / b, h / b)
}

data class VideoOutput(val size: Dimensions, val pixelFormat:Int)

private fun VideoOutput.toVideoDecoderOutput(): VideoDecoderOutput? {
    val avPixelFormat = pixelFormat.toAVPixelFormat() ?: return null
    return VideoDecoderOutput(size.copy(), avPixelFormat)
}



private fun Int.toAVPixelFormat(): Int =
    AV_PIX_FMT_RGB32




//private fun AudioOutput.toAudioDecoderOutput(): AudioDecoderOutput? {
//    val avSampleFormat = sampleFormat.toAVSampleFormat() ?: return null
//    if (channels != 2) return null // only stereo output is supported for now
//    return AudioDecoderOutput(sampleRate, channels, AV_CH_LAYOUT_STEREO, avSampleFormat)
//}
//

enum class SampleFormat {
    INVALID,
    S16
}

data class AudioOutput(val sampleRate: Int, val channels: Int, val sampleFormat: SampleFormat)


private fun AudioOutput.toAudioDecoderOutput(): AudioDecoderOutput? {
    val avSampleFormat = sampleFormat.toAVSampleFormat() ?: return null
    if (channels != 2) return null // only stereo output is supported for now
    return AudioDecoderOutput(sampleRate, channels, AV_CH_LAYOUT_STEREO, avSampleFormat)
}

private fun SampleFormat.toAVSampleFormat() =  when (this) {
    SampleFormat.S16 -> AV_SAMPLE_FMT_S16.toLong()
    SampleFormat.INVALID -> null
}


class AVFile(val fileName: String) {
    val context = avformat_alloc_context()

    init {
        avformat_open_input(context, fileName, null, null).checkAVError()
        val options = avutil.AVDictionary()
        avformat_find_stream_info(context, options)
    }

    fun dumpFormat() {
        av_dump_format(context, 0, fileName, 0)
    }

    fun dispose() {
        avformat_free_context(context)
    }
}

class VideoPlayerFFMPEG() {


    companion object {
        fun fromFile(fileName: String, mode: PlayMode = PlayMode.VIDEO) {
            val file = AVFile(fileName)

            file.dumpFormat()
            val worker = Worker()
            val dec = DecoderWorker(worker)
            val info = dec.initDecode(file.context, mode.useVideo, mode.useAudio)
            var state = State.PLAYING

            val videoOutput = VideoOutput(info?.video?.size?: TODO(), AV_PIX_FMT_RGB32)
            val audioOutput = AudioOutput(44100, 2, SampleFormat.S16)

            dec.start(videoOutput, audioOutput)
            dec.requestDecodeChunk()

            println("starting loop")
            while (state != State.STOPPED) {

                info.video?.let {
                    val frame = dec.nextVideoFrame()
                    frame?.let {

                        println("got a frame")

                        it.unref()

                    }

                }
                if (state == State.PLAYING) {

                }
                if (dec.done()) {
                    println("ok bye")
                }
                Thread.sleep(100)
            }


        }
    }
}

private fun AVFormatContext.streamAt(index: Int): AVStream? =
        if (index < 0) null
        else this.streams(index)

private val AVFormatContext.codecs: List<avcodec.AVCodecContext?>
    //get() = List(nb_streams.toInt()) { streams?.get(it)?.pointed?.codec?.pointed }
    get() = List(nb_streams()) { streams(it).codec() }


private fun AVStream.openCodec(tag: String): avcodec.AVCodecContext {
    // Get codec context for the video stream.
    val codecContext = this.codec()
    val codec = avcodec_find_decoder(codecContext.codec_id())
    if (codec.isNull)
    throw Error("Unsupported $tag codec with id ${codecContext.codec_id()}...")
    // Open codec.
    if (avcodec_open2(codecContext, codec, null as avutil.AVDictionary?) < 0)
        throw Error("Couldn't open $tag codec with id ${codecContext.codec_id()}")
    return codecContext
}


enum class TransferMode {
    SAFE
}

class Future<T>(val result:T) {

}

class Worker {

    fun <T1, T2> execute(mode:TransferMode,producer:()->T1, job:(T1)->T2 ) :Future<T2>{
        val r = producer()
        val f = job(r)
        return Future(f)
    }

}



class DecoderWorker(val worker: Worker) {

    private var decoder: Decoder? = null

    fun initDecode(context: AVFormatContext, useVideo: Boolean = true, useAudio: Boolean = true): CodecInfo {
        // Find the first video/audio streams.
        val videoStreamIndex =
                if (useVideo) context.codecs.indexOfFirst { it?.codec_type() == AVMEDIA_TYPE_VIDEO } else -1
        val audioStreamIndex =
                if (useAudio) context.codecs.indexOfFirst { it?.codec_type() == AVMEDIA_TYPE_AUDIO } else -1

        val videoStream = context.streamAt(videoStreamIndex)
        val audioStream = context.streamAt(audioStreamIndex)

        val videoContext = videoStream?.openCodec("video")
        val audioContext = audioStream?.openCodec("audio")

        // Extract video info.
        val video = videoContext?.run {
            VideoInfo(Dimensions(width(), height()), av_q2d(av_stream_get_r_frame_rate(videoStream)))
        }
        // Extract audio info.
        val audio = audioContext?.run {
            AudioInfo(sample_rate(), channels())
        }

        // Pack all state and pass it to the worker.
        worker.execute(TransferMode.SAFE, {
            Decoder(context,
                    videoStreamIndex, audioStreamIndex,
                    videoContext, audioContext)
        }) { decoder = it }
        return CodecInfo(video, audio)
    }

    fun start(videoOutput: VideoOutput, audioOutput: AudioOutput) {
        worker.execute(TransferMode.SAFE,
                {
                    Pair(
                            videoOutput.toVideoDecoderOutput(),
                            audioOutput.toAudioDecoderOutput())
                }) {
            decoder?.start(it.first, it.second)
        }
    }

    fun stop() {
        worker.execute(TransferMode.SAFE, { null }) {
            decoder?.run {
                dispose()
                decoder = null
            }
        }.result
    }

    fun done(): Boolean =
            worker.execute(TransferMode.SAFE, { null }) { decoder?.done() ?: true }.result

    fun requestDecodeChunk() =
            worker.execute(TransferMode.SAFE, { null }) { decoder?.decodeIfNeeded() }.result

    fun nextVideoFrame(): VideoFrame? =
            worker.execute(TransferMode.SAFE, { null }) { decoder?.nextVideoFrame() }.result

//    fun nextAudioFrame(size: Int): AudioFrame? =
//            worker.execute(TransferMode.SAFE, { size }) { decoder?.nextAudioFrame(it) }.result
//
//    fun audioVideoSynced(): Boolean =
//            worker.execute(TransferMode.SAFE, { null }) { decoder?.audioVideoSynced() ?: true }.result
}

fun main() {
    VideoPlayerFFMPEG.fromFile("/Users/edwin/Desktop/namer-trimmed.mp4")
}