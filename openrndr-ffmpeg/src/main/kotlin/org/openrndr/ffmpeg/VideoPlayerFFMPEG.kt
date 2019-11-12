package org.openrndr.ffmpeg

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVInputFormat
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.global.avcodec.*
import org.bytedeco.ffmpeg.global.avdevice.avdevice_register_all
import org.bytedeco.ffmpeg.global.avformat.*
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.javacpp.PointerPointer
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import java.nio.ByteBuffer
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

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

internal data class Dimensions(val w: Int, val h: Int) {
    operator fun minus(other: Dimensions) = Dimensions(w - other.w, h - other.h)
    operator fun div(b: Int) = Dimensions(w / b, h / b)
}

internal class AVFile(val configuration: VideoPlayerConfiguration,
                      val fileName: String,
                      val playMode: PlayMode,
                      val formatName: String? = null,
                      val frameRate: Double? = null,
                      val imageWidth: Int? = null,
                      val imageHeight: Int? = null) {
    val context = avformat_alloc_context()

    init {
        val options = AVDictionary(null)
        val format: AVInputFormat?
        if (formatName != null) {
            avdevice_register_all()
            format = av_find_input_format(formatName)
        } else {
            format = null
        }

        if (configuration.realtimeBufferSize != -1L) {
            av_dict_set(options, "rtbufsize", "${configuration.realtimeBufferSize}", 0)
        }

        if (frameRate != null) {
            val r = av_d2q(frameRate, 1001000)
            av_dict_set(options, "framerate", r.num().toString() + "/" + r.den(), 0)
        }

        if (imageWidth != null && imageHeight != null) {
            av_dict_set(options, "video_size", "${imageWidth}x$imageHeight", 0)
        }

        if (fileName.startsWith("rtsp://")) {
            av_dict_set(options, "max_delay", "0", 0)
            if (playMode == PlayMode.VIDEO) {
                av_dict_set(options, "allowed_media_types", "video", 0)
            }
        }
        av_dict_set(options, "user_agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36", 0)
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

class FrameEvent(val frame: ColorBuffer, val timeStamp: Double)

class VideoEvent

class VideoStatistics {
    var videoFramesDecoded = 0L
    var videoFrameErrors = 0L
    var videoQueueSize = 0
    var packetQueueSize = 0
    var videoBytesReceived = 0L
    var videoDecodeDuration = 0L
    var videoLastFrame = System.currentTimeMillis()
}

class VideoPlayerConfiguration {
    var videoFrameQueueSize = 50
    var packetQueueSize = 2500
    var useHardwareDecoding = true
    var usePacketReaderThread = false
    var realtimeBufferSize = -1L
    var allowFrameSkipping = true
}

class VideoPlayerFFMPEG private constructor(
        private val file: AVFile,
        private val mode: PlayMode = PlayMode.VIDEO,
        private val configuration: VideoPlayerConfiguration) {

    var lastFrameTime = 0.0
    fun getTime(): Double {
        return System.currentTimeMillis() / 1000.0
    }

    private val displayQueue = Queue<VideoFrame>(20)

    companion object {
        fun fromFile(fileName: String, mode: PlayMode = PlayMode.BOTH, configuration: VideoPlayerConfiguration = VideoPlayerConfiguration()): VideoPlayerFFMPEG {
            av_log_set_level(AV_LOG_ERROR)
            val file = AVFile(configuration, fileName, mode)
            return VideoPlayerFFMPEG(file, mode, configuration)
        }

        fun fromDevice(deviceName: String = defaultDevice(), mode: PlayMode = PlayMode.VIDEO, frameRate: Double? = null, imageWidth: Int? = null, imageHeight: Int? = null, configuration: VideoPlayerConfiguration = VideoPlayerConfiguration()): VideoPlayerFFMPEG {
            av_log_set_level(AV_LOG_QUIET)
            val format = when (Platform.type) {
                PlatformType.WINDOWS -> "dshow"
                PlatformType.MAC -> "avfoundation"
                PlatformType.GENERIC -> "video4linux2"
            }
            val file = AVFile(configuration, deviceName, mode, format, frameRate, imageWidth, imageHeight)
            return VideoPlayerFFMPEG(file, mode, configuration)
        }

        fun defaultDevice(): String {
            return when (Platform.type) {
                PlatformType.WINDOWS -> {
                    "video=Integrated Webcam"
                }
                PlatformType.MAC -> {
                    "0"
                }
                PlatformType.GENERIC -> {
                    "/dev/video0"
                }
            }
        }
    }

    val statistics = VideoStatistics()
    private var decoder: Decoder? = null
    private var info: CodecInfo? = null
    private var state = State.PLAYING
    private var startTimeMillis = -1L
    var colorBuffer: ColorBuffer? = null

    val newFrame = Event<FrameEvent>()
    val ended = Event<VideoEvent>()
    private var audioOut: AudioQueueSource? = null

    var audioGain: Double
        set(value: Double) {
            audioOut?.let {
                it.gain = value
            }
        }
        get() {
            return audioOut?.gain ?: 1.0
        }

    fun play() {
        logger.debug { "start play" }
        file.dumpFormat()

        val (decoder, info) = runBlocking {
            Decoder.fromContext(statistics, configuration, file.context, mode.useVideo, mode.useAudio)
        }

        this.decoder = decoder
        this.info = info

        this.info?.video?.let {
            colorBuffer = org.openrndr.draw.colorBuffer(it.size.w, it.size.h).apply {
                flipV = true
            }
        }
        val videoOutput = VideoOutput(info.video?.size ?: Dimensions(0,0), AV_PIX_FMT_RGB32)
        val audioOutput = AudioOutput(48000, 2, SampleFormat.S16)

        if (mode.useAudio) {
            audioOut = AudioSystem.createQueueSource {
                val frame = decoder.nextAudioFrame()
                if (frame != null) {
                    val data = frame.buffer.data()
                    data.capacity(frame.size.toLong())
                    val bb = ByteBuffer.allocateDirect(frame.size)
                    bb.put(data.asByteBuffer())
                    bb.rewind()
                    val ad = AudioData(buffer = bb)
                    frame.unref()
                    ad
                } else {
                    null
                }
            }
            audioOut?.play()

            audioOut?.let { ao ->
                decoder.audioOutQueueFull = { ao.outputQueue.size >= ao.queueSize-1 }
            }
        }

        decoder.displayQueueFull = { displayQueue.size() >= displayQueue.maxSize-1 }

        thread(isDaemon = true) {
            Thread.currentThread().name += "(decoder)"
            decoder.start(videoOutput.toVideoDecoderOutput(), audioOutput.toAudioDecoderOutput())
        }
        startTimeMillis = System.currentTimeMillis()

        if (mode.useVideo) {
            thread(isDaemon = true) {
                Thread.currentThread().name += "(display)"
                var nextFrame = 0.0

                decoder.seekCompleted = {
                    nextFrame = 0.0
                }

                while (true) {
                    if (seekRequested) {
                        logger.debug { "performing seek" }
                        synchronized(displayQueue) {
                            while (!displayQueue.isEmpty()) {
                                displayQueue.pop().unref()
                            }
                        }
                        audioOut?.flush()
                        decoder.seek(seekPosition)
                        audioOut?.resume()
                        seekRequested = false
                    }

                    val duration = (info.video?.fps ?: 1.0)

                    val now = getTime()
                    if (now - nextFrame > duration * 2) {
                        logger.debug {
                            "resetting next frame time"
                        }
                        nextFrame = now
                    }
                    if (now >= nextFrame) {
                        val frame = decoder.nextVideoFrame()
                        if (frame != null) {
                            nextFrame += 1.0 / duration

                            while (displayQueue.size() >= displayQueue.maxSize - 1) {
                                logger.debug {
                                    "display queue is full (${displayQueue.size()} / ${displayQueue.maxSize})"
                                }
                                Thread.sleep(10)
                            }
                            displayQueue.push(frame)
                        }
                    }
                    Thread.sleep(3)
                }
            }
        }
    }

    fun restart() {
        logger.debug { "video player restart requested" }
        synchronized(displayQueue) {
            while (!displayQueue.isEmpty()) {
                displayQueue.pop().unref()
            }
        }
        decoder?.restart()
        audioOut?.flush()
    }

    private var seekRequested = false
    private var seekPosition = -1.0

    fun seek(positionInSeconds: Double) {
        logger.debug { "video player seek requested" }
        seekRequested = true
        seekPosition = positionInSeconds
    }

    fun draw(drawer: Drawer, blind:Boolean = false) {

        synchronized(displayQueue) {
            if (!configuration.allowFrameSkipping) {
                val frame = displayQueue.peek()
                if (frame != null) {
                    displayQueue.pop()
                    colorBuffer?.write(frame.buffer.data().capacity(frame.frameSize.toLong()).asByteBuffer())
                    frame.unref()
                }

            } else {
                var frame: VideoFrame? = null
                while (!displayQueue.isEmpty()) {
                    frame = displayQueue.pop()
                    if (displayQueue.isEmpty()) {
                        colorBuffer?.write(frame.buffer.data().capacity(frame.frameSize.toLong()).asByteBuffer())
                    }
                    frame.unref()
                }
            }
        }
        colorBuffer?.let {
            if (!blind) {
                drawer.image(it)
            }
        }
    }
}

internal fun AVFormatContext.streamAt(index: Int): AVStream? =
        if (index < 0) null
        else this.streams(index)

internal val AVFormatContext.codecs: List<AVCodecParameters?>
    get() = List(nb_streams()) { streams(it).codecpar() }

internal fun AVStream.openCodec(tag: String): AVCodecContext {
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