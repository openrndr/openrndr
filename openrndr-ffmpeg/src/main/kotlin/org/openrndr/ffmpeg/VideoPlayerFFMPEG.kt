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
import org.bytedeco.ffmpeg.avdevice.AVDeviceInfoList
import org.bytedeco.ffmpeg.avformat.AVInputFormat
import org.bytedeco.ffmpeg.avutil.AVHWDeviceContext
import org.bytedeco.ffmpeg.global.avdevice.*
import org.bytedeco.ffmpeg.global.avutil.*
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType


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

class AVFile(val fileName: String,
             val formatName:String? = null,
             val frameRate:Double? = null,
             val imageWidth:Int? = null,
             val imageHeight:Int? = null) {
    val context = avformat_alloc_context()

    init {
        val options = AVDictionary(null)
        val format: AVInputFormat?
        if (formatName != null) {
            avdevice_register_all()
            format = av_find_input_format("dshow")
        } else {
            format = null
        }

        if (frameRate != null) {
            val r = av_d2q(frameRate, 1001000)
            av_dict_set(options, "framerate", r.num().toString() + "/" + r.den(), 0)
        }

        if (imageWidth != null && imageHeight != null) {
            av_dict_set(options, "video_size", "${imageWidth}x$imageHeight",0)
        }

        avformat_open_input(context, fileName, format, options).checkAVError()
        avformat_find_stream_info(context, null as PointerPointer<*>?).checkAVError()
        av_dict_free(options)

    }

    fun dumpFormat() {
        av_dump_format(context, 0, fileName, 0)
    }

    fun dispose() {
        avformat_free_context(context)
    }
}

class Camera() {

    companion object {


        fun listDevices() {
            avdevice_register_all()
            avcodec_register_all()

            var inputFormat:AVInputFormat? = null
            do {
                inputFormat = av_input_video_device_next(inputFormat)
                if (inputFormat != null) {
                    println(inputFormat.name().getString())
                }
                    val list = PointerPointer<AVDeviceInfoList>(1)
                    val formatContext = avformat_alloc_context()
                println(inputFormat?.get_device_list()?.call(formatContext, list))

                val r = avdevice_list_input_sources(inputFormat, null, null, list)
                if (r >= 0) {
                    val rl = AVDeviceInfoList(list[0])
                    println(rl.devices())
                }

            } while (inputFormat != null)

            val format = av_find_input_format("dshow")
            println(format.name().getString())

            val list = PointerPointer<AVDeviceInfoList>(1)
            //av_input_video_device_next()
            //avdevice_list_input_sources(format, null, null, list).checkAVError()

//            val formatContext = avformat_alloc_context()
//            avformat_open_input(formatContext, null as String?, format, null).checkAVError()
//
//
//

//            avdevice_list_devices(formatContext, list)
        }


    }
}

class FrameEvent(val frame:ColorBuffer, val timeStamp:Double) {

}

class VideoEvent {

}

class VideoPlayerFFMPEG(val file: AVFile, val mode: PlayMode = PlayMode.VIDEO) {

    companion object {
        fun fromFile(fileName: String, mode: PlayMode = PlayMode.VIDEO): VideoPlayerFFMPEG {
            av_log_set_level(AV_LOG_ERROR)
            val file = AVFile(fileName)
            return VideoPlayerFFMPEG(file, mode)
        }

        fun fromDevice(deviceName:String = defaultDevice(), mode: PlayMode = PlayMode.VIDEO, frameRate:Double?=null, imageWidth: Int?=null, imageHeight: Int?=null) : VideoPlayerFFMPEG {
            val format = when(Platform.type) {
                PlatformType.WINDOWS -> "dshow"
                PlatformType.MAC -> "avfoundation"
                PlatformType.GENERIC -> "video4linux2"
            }

            val file = AVFile(deviceName, format, frameRate, imageWidth, imageHeight)
            return VideoPlayerFFMPEG(file, mode)
        }
        fun defaultDevice(): String {
            return when(Platform.type) {
                PlatformType.WINDOWS -> {
                    "video=Integrated Webcam"
                }
                PlatformType.MAC  -> {
                    "0"
                }
                PlatformType.GENERIC -> {
                    "/dev/video0"
                }
            }
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
    val ended = Event<VideoEvent>()

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

    fun restart() {
        decoder?.restart()
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
                if (peekFrame == null && (decoder?.done() == true)) {
                    println("video ended")
                    ended.trigger(VideoEvent())
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
fun main() {
    Camera.listDevices()
}