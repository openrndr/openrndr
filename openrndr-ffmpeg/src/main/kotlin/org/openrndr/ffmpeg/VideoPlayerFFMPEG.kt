package org.openrndr.ffmpeg

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.global.avcodec.*
import org.bytedeco.ffmpeg.global.avformat.*
import org.bytedeco.javacpp.*
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder
import org.bytedeco.ffmpeg.avcodec.AVCodec
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters
import org.bytedeco.ffmpeg.global.avutil.*


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

class AVFile(val fileName: String) {
    val context = avformat_alloc_context()

    init {
        avformat_open_input(context, fileName, null, null).checkAVError()
        avformat_find_stream_info(context, null as PointerPointer<*>?)
    }

    fun dumpFormat() {
        av_dump_format(context, 0, fileName, 0)
    }

    fun dispose() {
        avformat_free_context(context)
    }
}

class Camera() {

}

class FrameEvent(val frame:ColorBuffer, val timeStamp:Double) {

}

class VideoPlayerFFMPEG(val file: AVFile, val mode: PlayMode = PlayMode.VIDEO) {

    companion object {
        fun fromFile(fileName: String, mode: PlayMode = PlayMode.VIDEO): VideoPlayerFFMPEG {
            av_log_set_level(AV_LOG_ERROR)
            val file = AVFile(fileName)
            return VideoPlayerFFMPEG(file, mode)
        }
    }

    private var decoder: Decoder? = null
    private var info: CodecInfo? = null
    private var state = State.PLAYING
    private var startTimeMillis = -1L
    private var colorBuffer: ColorBuffer? = null
    private var firstFrame = true
    private var playOffsetSeconds = 0.0

    val newFrame = Event<FrameEvent>()

    fun play() {
        file.dumpFormat()

        val (decoder, info) = runBlocking {
            Decoder.fromContext(file.context, mode.useVideo, mode.useAudio)
        }
        this.decoder = decoder
        this.info = info


        this.info?.video?.let {
            println("allocating video buffer")
            colorBuffer = org.openrndr.draw.colorBuffer(it.size.w, it.size.h).apply {
                flipV = true
            }
        }


        val videoOutput = VideoOutput(info.video?.size ?: TODO(), AV_PIX_FMT_RGB32)
        val audioOutput = AudioOutput(44100, 2, SampleFormat.S16)

        GlobalScope.launch {
            decoder.start(videoOutput.toVideoDecoderOutput(), audioOutput.toAudioDecoderOutput())
        }

        println("starting loop")
        startTimeMillis = System.currentTimeMillis()
        println("framerate!: ${info.video.fps}")
    }

    fun update() {
        if (state == State.PLAYING) {
            info?.video.let {

                val playTimeSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000.0 + playOffsetSeconds
                val peekFrame = decoder?.peekNextVideoFrame()
                if (firstFrame && peekFrame != null) {
                    if (peekFrame.timeStamp > playTimeSeconds) {
                        playOffsetSeconds += peekFrame.timeStamp - playTimeSeconds
                        println("jumping in time $playOffsetSeconds")
                    }
                    firstFrame = false
                }

                if (peekFrame == null && !firstFrame) {
                    println("oh no ran out buffered frames")
                    firstFrame = true
                }

                if (playTimeSeconds >= (peekFrame?.timeStamp ?: Double.POSITIVE_INFINITY)) {
                    val frame = decoder?.nextVideoFrame()
                    frame?.let {
                        colorBuffer?.write(it.buffer.data().capacity(frame.frameSize.toLong()).asByteBuffer())
                        it.unref()
                        newFrame.trigger(FrameEvent(colorBuffer?:throw IllegalStateException("colorBuffer == null"), peekFrame?.timeStamp?:-1.0))
                    }
                    runBlocking {
                        if (decoder?.done() == true) {
                            println("decoder is done")
                        }
                    }
                }
            }
        }
    }

    fun draw(drawer: Drawer) {
        update()
        colorBuffer?.let {
            drawer.image(it)
        }
    }
}

fun AVFormatContext.streamAt(index: Int): AVStream? =
        if (index < 0) null
        else this.streams(index)

val AVFormatContext.codecs: List<AVCodecParameters?>
    //get() = List(nb_streams.toInt()) { streams?.get(it)?.pointed?.codec?.pointed }
    get() = List(nb_streams()) { streams(it).codecpar() }


fun AVStream.openCodec(tag: String): AVCodecContext {
    // Get codec context for the video stream.
    val codecPar = this.codecpar()

    val codec = avcodec_find_decoder(codecPar.codec_id())
    if (codec.isNull)
        throw Error("Unsupported $tag codec with id ${codecPar.codec_id()}...")

    val codecContext = avcodec_alloc_context3(codec)
    avcodec_parameters_to_context(codecContext, codecPar)
    if (avcodec_open2(codecContext, codec, null as AVDictionary?) < 0)
        throw Error("Couldn't open $tag codec with id ${codecPar.codec_id()}")

    return codecContext
}