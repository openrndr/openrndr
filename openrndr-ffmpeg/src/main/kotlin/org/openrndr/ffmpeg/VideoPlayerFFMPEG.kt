package org.openrndr.ffmpeg

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.avcodec.avcodec_find_decoder
import org.bytedeco.javacpp.avcodec.avcodec_open2
import org.bytedeco.javacpp.avformat.*
import org.bytedeco.javacpp.avutil.*
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.events.Event

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
        println("avformat_open_input)")
        avformat_open_input(context, fileName, null, null).checkAVError()
        val options = avutil.AVDictionary(null)
        println("avformat_find_stream_info")
        avformat_find_stream_info(context, null as PointerPointer<*>?)
    }

    fun dumpFormat() {
        av_dump_format(context, 0, fileName, 0)
    }

    fun dispose() {
        avformat_free_context(context)
    }
}


class FrameEvent(val frame:ColorBuffer, val timeStamp:Double) {

}

class VideoPlayerFFMPEG(val file: AVFile, val mode: PlayMode = PlayMode.VIDEO) {

    companion object {
        fun fromFile(fileName: String, mode: PlayMode = PlayMode.VIDEO): VideoPlayerFFMPEG {
            println("opening file")
            val file = AVFile(fileName)

            println("opened filed")
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

val AVFormatContext.codecs: List<avcodec.AVCodecContext?>
    //get() = List(nb_streams.toInt()) { streams?.get(it)?.pointed?.codec?.pointed }
    get() = List(nb_streams()) { streams(it).codec() }


fun AVStream.openCodec(tag: String): avcodec.AVCodecContext {
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

fun main() {
    av_register_all()
    val player = VideoPlayerFFMPEG.fromFile("/Users/edwin/Desktop/namer-trimmed.mp4")


    player.play()

    while (true) {
        Thread.sleep(10)
    }

    //VideoPlayerFFMPEG.fromFile("https://manifest.googlevideo.com/api/manifest/hls_playlist/expire/1561070996/ei/NLkLXYvmLNfcgAe7sJDYDw/ip/213.124.33.58/id/bp6MTKFNqa4.0/itag/96/source/yt_live_broadcast/requiressl/yes/ratebypass/yes/live/1/goi/160/sgoap/gir%3Dyes%3Bitag%3D140/sgovp/gir%3Dyes%3Bitag%3D137/hls_chunk_host/r2---sn-5hne6n7z.googlevideo.com/playlist_type/DVR/initcwndbps/13770/mm/44/mn/sn-5hne6n7z/ms/lva/mv/m/pl/18/dover/11/keepalive/yes/mt/1561049318/disable_polymer/true/sparams/expire,ei,ip,id,itag,source,requiressl,ratebypass,live,goi,sgoap,sgovp,playlist_type/sig/ALgxI2wwRAIgCtskXa72BZLBBVGIEWXbzZVVyov0cFPQVpk6kabOuvUCIArblznMcV7Uk17qQifd8fl70_cmWestAPBob3jtOC16/lsparams/hls_chunk_host,initcwndbps,mm,mn,ms,mv,pl/lsig/AHylml4wRgIhAP5DwpDrqWp4gPNf-1_RgC_8R1nK5tS6qrebCYy910OiAiEAyVJIUfLusF34FZ2qLEYg2La7bRSCQqTYeP-TTNeQfaU%3D/playlist/index.m3u8").play()
}