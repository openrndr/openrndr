package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.Callback_Pointer_int_String_Pointer
import org.bytedeco.ffmpeg.global.avcodec.*
import org.bytedeco.ffmpeg.global.avdevice.avdevice_register_all
import org.bytedeco.ffmpeg.global.avformat.*
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.openal.AudioData
import org.openrndr.openal.AudioQueueSource
import org.openrndr.openal.AudioSystem
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import org.openrndr.shape.Rectangle
import java.io.File
import java.nio.ByteBuffer
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

enum class State {
    INITIALIZE,
    PLAYING,
    STOPPED,
    PAUSED,
    DISPOSED
    ;

    inline fun transition(from: State, to: State, block: () -> Unit): State =
            if (this == from) {
                block()
                to
            } else this

    inline fun transition(from: Set<State>, to: State, block: () -> Unit): State =
            if (this in from) {
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

@JvmRecord
internal data class Dimensions(val w: Int, val h: Int) {
    operator fun minus(other: Dimensions) = Dimensions(w - other.w, h - other.h)
    operator fun div(b: Int) = Dimensions(w / b, h / b)
}

internal class AVFile(configuration: VideoPlayerConfiguration,
                      private val fileName: String,
                      playMode: PlayMode,
                      formatName: String? = null,
                      frameRate: Double? = null,
                      imageWidth: Int? = null,
                      imageHeight: Int? = null) {

    val context: AVFormatContext = avformat_alloc_context()

    init {
        val options = AVDictionary(null)
        val isDevice = formatName != null

        val format = if (formatName != null) {
            avdevice_register_all()
            av_find_input_format(formatName)
        } else {
            null
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

        if (isDevice && frameRate == null) {
            val result = avformat_open_input(context, fileName, format, options)

            if (result != 0) {
                logger.info { "opening device with default frame rate failed." }
                logger.debug { result.toAVError() }
                val frameRates = listOf(60.0, 30.0, 29.97, 25.0, 24.0, 15.0, 10.0)
                var found = false
                for (candidate in frameRates) {
                    val r = av_d2q(candidate, 1001000)
                    av_dict_set(options, "framerate", r.num().toString() + "/" + r.den(), 0)

                    val retryResult = avformat_open_input(context, fileName, format, options)
                    if (retryResult != 0) {
                        logger.warn { retryResult.toAVError() }
                    }
                    if (retryResult == 0) {
                        found = true
                        logger.info { "fall-back to $candidate" }
                        break
                    }
                }
                require(found) {
                    "failed to open device ($formatName) at any frame rate."
                }
            }
        } else {
            avformat_open_input(context, fileName, format, options).checkAVError()
        }

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
    var videoFrameQueueSize = 10
    var packetQueueSize = 2500
    var displayQueueSize = 5
    var useHardwareDecoding = true
    var usePacketReaderThread = false
    var realtimeBufferSize = -1L
    var allowFrameSkipping = true
    var minimumSeekOffset = -0.1
    var maximumSeekOffset = 0.0
    var legacyStreamOpen = false
    var allowArbitrarySeek = false
    var synchronizeToClock = true
    var displayQueueCooldown = 10

    /**
     * Maximum time in seconds it may take before a new packet is received
     */
    var packetTimeout = 60.0
}

private object DefaultLogger : Callback_Pointer_int_String_Pointer() {
    override fun call(source: Pointer?, level: Int, formatStr: String?, params: Pointer?) {
        av_log_default_callback(source, level, formatStr, params)
    }

    fun install() = av_log_set_callback(this)
}

/**
 * Load a video
 * @param fileOrUrl a file name or an url to open
 * @param mode the play mode to use, determines if video and/or audio should be played
 */
fun Program.loadVideo(fileOrUrl: String, mode: PlayMode = PlayMode.BOTH, configuration: VideoPlayerConfiguration = VideoPlayerConfiguration()): VideoPlayerFFMPEG {
    return VideoPlayerFFMPEG.fromFile(fileOrUrl, clock = { seconds }, mode = mode, configuration = configuration)
}

fun loadVideoDevice(deviceName: String = VideoPlayerFFMPEG.defaultDevice(), mode: PlayMode = PlayMode.VIDEO, width: Int? = null, height: Int? = null, frameRate: Double? = null, configuration: VideoPlayerConfiguration = VideoPlayerConfiguration()): VideoPlayerFFMPEG {
    logger.info { """loading video device $deviceName (mode: $mode)""" }
    return VideoPlayerFFMPEG.fromDevice(deviceName, mode = mode, imageWidth = width, imageHeight = height, frameRate = frameRate, configuration = configuration)
}

/**
 * Video player based on FFMPEG
 */
class VideoPlayerFFMPEG private constructor(
        private val file: AVFile,
        private val mode: PlayMode = PlayMode.VIDEO,
        private val configuration: VideoPlayerConfiguration,
        private val clock: () -> Double = { System.currentTimeMillis() / 1000.0 }) {

    private val displayQueue = Queue<VideoFrame>(configuration.displayQueueSize)



    /**
     * The video duration in seconds
     */
    var duration: Double = file.context.duration() / 1E6
        private set

    companion object {
        // Setting optimized for screen recording, with lower latency
        private val screenRecordingConfiguration = VideoPlayerConfiguration()
        init {
            screenRecordingConfiguration.let {
                it.videoFrameQueueSize = 5
                it.packetQueueSize = 1250
                it.displayQueueSize = 2
                it.synchronizeToClock = false
            }
        }

        /**
         * Lists the available machine-specific device names
         */
        @Suppress("unused")
        fun listDeviceNames(): List<String> {
            logger.debug { """listing device names""" }
            val result = mutableListOf<String>()

            /*
            FFMPEG 4.1 still does not have any working interface to query devices.
            This mess is necessary because we need to parse the devices from the log output.
             */
            val texts = mutableListOf<String>()
            val callback = object : Callback_Pointer_int_String_Pointer() {
                override fun call(source: Pointer?, level: Int, formatStr: String?, params: Pointer?) {
                    val bp = BytePointer(1024)
                    val ip = IntPointer(1)

                    val length = av_log_format_line2(source, level, formatStr, params, bp, 1024, ip)
                    val text = bp.string.substring(0, length).trimEnd()
                    texts.add(text)
                }
            }
            avdevice_register_all()
            av_log_set_callback(callback)
            val context = avformat_alloc_context()

            if (Platform.type == PlatformType.WINDOWS) {
                val options = AVDictionary()
                av_dict_set(options, "list_devices", "true", 0)
                val format = av_find_input_format("dshow")
                avformat_open_input(context, "video=dummy", format, options)
                var lineIndex = 0
                all@ while (true) {
                    if (lineIndex >= texts.size || texts[lineIndex].contains("DirectShow audio devices")) {
                        break@all
                    }
                    val deviceNamePattern = Regex("\\[dshow @ [0-9a-f]*]\\s+\"(.*)\"")
                    val deviceTypePattern = Regex("\\[dshow @ [0-9a-f]*]\\s+[ ]+\\((.*)")

                    val nameText = texts.getOrNull(lineIndex)
                    val typeText = texts.getOrNull(lineIndex+1)
                    if (typeText != null) {
                        val match = deviceTypePattern.matchEntire(typeText)
                        val group = match?.groupValues?.getOrNull(1)
                        if (group == "video") {
                            if (nameText != null) {
                                val match = deviceNamePattern.matchEntire(nameText)
                                val group = match?.groupValues?.getOrNull(1)
                                if (group != null) {
                                    result.add(group)
                                }
                            }
                        }
                    }
                    lineIndex += 5

                }
                av_dict_free(options)
                avformat_close_input(context)
            }

            if (Platform.type == PlatformType.MAC) {
                val options = AVDictionary()
                av_dict_set(options, "list_devices", "true", 0)
                val format = av_find_input_format("avfoundation")
                avformat_open_input(context, "", format, options)
                var lineIndex = 0
                all@ while (true) {
                    if (texts[lineIndex].contains("AVFoundation video devices")) {
                        lineIndex++
                        while (true) {
                            if (lineIndex >= texts.size || texts[lineIndex].contains("AVFoundation audio devices")) {
                                break@all
                            }
                            val devicePattern = Regex("\\[AVFoundation .*] \\[[0-9]*] (?<name>.*)")
                            val matchResult = devicePattern.matchEntire(texts[lineIndex])
                            if (matchResult != null) {
                                val (name) = matchResult.destructured
                                result.add(name)
                            }
                            lineIndex += 1
                        }
                    }
                }
                av_dict_free(options)
                avformat_close_input(context)
            }

            /**
             * On linux this is even more of a mess, the advice is to use v4l2-ctl for enumeration, but guess what,
             * that software is likely not installed by the User. Best we can do is scan for /dev/video<N> files.
             */
            if (Platform.type == PlatformType.GENERIC) {
                var index = 0
                while (true) {
                    val f = File("/dev/video$index")
                    if (f.exists()) {
                        result.add("/dev/video$index")
                        index++
                    } else {
                        break
                    }
                }
            }
            avformat_free_context(context)

            // -- switch back to the default logger
            DefaultLogger.install()
            return result
        }

        /**
         * Opens a video from file or url
         * @return a ready-to-play video player on success
         */
        fun fromFile(fileName: String,
                     mode: PlayMode = PlayMode.BOTH,
                     configuration: VideoPlayerConfiguration = VideoPlayerConfiguration(),
                     clock: () -> Double = { System.currentTimeMillis() / 1000.0 }): VideoPlayerFFMPEG {
            av_log_set_level(AV_LOG_QUIET)
            val file = AVFile(configuration, fileName, mode)
            return VideoPlayerFFMPEG(file, mode, configuration, clock)
        }

        /**
         * Opens a webcam or video device
         * @param deviceName a machine-specific device name
         * @param mode which streams should be opened and played
         * @param frameRate optional frame rate (in Hz)
         * @param imageWidth optional image width
         * @param imageHeight optional image height
         * @param configuration optional video player configuration
         * @return a ready-to-play video player on success
         */
        fun fromDevice(deviceName: String = defaultDevice(),
                       mode: PlayMode = PlayMode.VIDEO,
                       frameRate: Double? = null,
                       imageWidth: Int? = null,
                       imageHeight: Int? = null,
                       configuration: VideoPlayerConfiguration = VideoPlayerConfiguration())
                : VideoPlayerFFMPEG {
            av_log_set_level(AV_LOG_QUIET)
            val (format, properDeviceName) = when (Platform.type) {
                PlatformType.WINDOWS -> ("dshow" to "video=$deviceName")
                PlatformType.MAC -> ("avfoundation" to deviceName)
                PlatformType.GENERIC -> ("video4linux2" to deviceName)
                else -> error("unsupported platform ${Platform.type}")
            }
            val file = AVFile(configuration, properDeviceName, mode, format, frameRate, imageWidth, imageHeight)
            logger.debug { file }
            return VideoPlayerFFMPEG(file, mode, configuration)
        }

        /**
         * Returns machine-specific default device
         */
        fun defaultDevice(): String {
            return when (Platform.type) {
                PlatformType.WINDOWS -> listDeviceNames()[0]
                PlatformType.MAC -> "0"
                PlatformType.GENERIC -> "/dev/video0"
                else -> error("unsupporte platform ${Platform.type}")
            }
        }

        /**
         * Opens the screen for grabbing frames
         * See https://trac.ffmpeg.org/wiki/Capture/Desktop
         * @param screenName a machine-specific device name
         *   Windows: "desktop" or "title=window_title"
         *   Mac:     something like "1:0"
         *   Linux:   run `echo $DISPLAY` to find out
         * @param mode which streams should be opened and played
         *   Reserved for future use, in case we want grabbing audio
         * @param frameRate optional frame rate (in Hz)
         * @param imageWidth optional image width
         * @param imageHeight optional image height
         * @param configuration optional video player configuration
         * @return a ready-to-play video player on success
         */
        fun fromScreen(screenName: String = defaultScreenDevice(),
                       mode: PlayMode = PlayMode.VIDEO,
                       frameRate: Double? = null,
                       imageWidth: Int? = null,
                       imageHeight: Int? = null,
                       configuration: VideoPlayerConfiguration = screenRecordingConfiguration)
                : VideoPlayerFFMPEG {
            val (format, properDeviceName) = when (Platform.type) {
                PlatformType.WINDOWS -> ("gdigrab" to screenName)
                PlatformType.MAC -> ("avfoundation" to screenName)
                PlatformType.GENERIC -> ("x11grab" to screenName)
                else -> error("unsupported platform ${Platform.type}")
            }
            val file = AVFile(configuration, properDeviceName, mode, format, frameRate, imageWidth, imageHeight)
            return VideoPlayerFFMPEG(file, mode, configuration)
        }

        /**
         * Returns machine-specific default device
         */
        fun defaultScreenDevice(): String {
            return when (Platform.type) {
                PlatformType.WINDOWS -> "desktop"
                PlatformType.MAC -> "1:0"
                PlatformType.GENERIC -> ":1"
                else -> error("unsupported platform ${Platform.type}")
            }
        }
    }

    val statistics = VideoStatistics()
    private var decoder: Decoder? = null
    private var info: CodecInfo? = null
    private var state = State.INITIALIZE
    private var startTimeMillis = -1L
    var colorBuffer: ColorBuffer? = null

    val width: Int get() = colorBuffer?.width ?: 0
    val height: Int get() = colorBuffer?.height ?: 0

    /**
     * Listenable event, emitted when a new frame arrived
     */
    val newFrame = Event<FrameEvent>("videoplayer-new-frame")

    /**
     * Listenable event, emitted when video ended
     */
    val ended = Event<VideoEvent>("videoplayer-ended")

    private var audioOut: AudioQueueSource? = null

    private var endOfFileReached = false

    private var disposed = false

    /**
     * Controls the gain of the audio
     */
    @Suppress("unused")
    var audioGain: Double
        set(value) {
            audioOut?.let {
                it.gain = value
            }
        }
        get() {
            return audioOut?.gain ?: 1.0
        }

    /**
     * Start playing the stream
     */
    @Suppress("unused")
    fun play() {
        require(!disposed)
        require(state == State.INITIALIZE)

        logger.debug { "start play" }
        file.dumpFormat()
        av_format_inject_global_side_data(file.context)

        val (decoder, info) = runBlocking {
            Decoder.fromContext(statistics, configuration, file.context, mode.useVideo, mode.useAudio)
        }

        this.decoder = decoder
        this.info = info

        this.info?.video?.let {
            colorBuffer = org.openrndr.draw.colorBuffer(it.size.w, it.size.h).apply {
                flipV = true
                fill(ColorRGBa.TRANSPARENT, 0)
            }
        }
        val videoOutput = VideoOutput(info.video?.size ?: Dimensions(0, 0), AV_PIX_FMT_RGB32)
        val audioOutput = if (mode.useAudio) AudioOutput(44100, 2, SampleFormat.S16) else null
        av_format_inject_global_side_data(file.context)

        if (mode.useAudio) {
            audioOut = AudioSystem.createQueueSource {
                if (decoder.audioQueue() != null) {
                    synchronized(decoder.audioQueue() ?: error("no queue")) {
                        logger.trace { "queuing audio for play. frames in queue: ${decoder.audioQueueSize()}" }
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
                            //logger.debug { "no audio packets from upstream: [audio queue size: ${decoder.audioQueueSize()}]" }
                            null
                        }
                    }
                } else {
                    null
                }
            }
            audioOut?.play()

            audioOut?.let { ao ->
                decoder.audioOutQueueFull = { ao.outputQueueFull }
            }
        }

        decoder.displayQueueFull = { displayQueue.size() >= displayQueue.maxSize - 1 }

        thread(isDaemon = true) {
            Thread.currentThread().name += "(decoder)"
            decoder.start(videoOutput.toVideoDecoderOutput(), audioOutput?.toAudioDecoderOutput())
        }
        startTimeMillis = System.currentTimeMillis()

        decoder.reachedEndOfFile = {
            endOfFileReached = true
        }

        if (mode.useVideo) {
            thread(isDaemon = true) {
                Thread.currentThread().name += "(display)"
                var nextFrame = 0.0

                decoder.seekCompleted = {
                    nextFrame = 0.0
                }

                while (!disposed) {
                    if (seekRequested) {
                        logger.debug { "performing seek" }
                        synchronized(displayQueue) {
                            logger.debug { "flushing display queue" }
                            while (!displayQueue.isEmpty()) {
                                displayQueue.pop().unref()
                            }
                        }
                        audioOut?.flush()
                        decoder.seek(seekPosition)
                        audioOut?.resume()
                        seekRequested = false
                    }

                    val rate = (info.video?.fps ?: 1.0)
                    val duration = 1.0 / rate

                    val now = clock()


                    if (state == State.PAUSED) {
                        nextFrame = now + 0.001
                    }


                    val delta = now - nextFrame
                    if (delta > duration * 2 && configuration.synchronizeToClock) {
                        logger.warn {
                            "resetting next frame time to ${now}, ${delta/duration} frame difference"
                        }
                        nextFrame = now
                    }

                    if (now >= nextFrame || !configuration.synchronizeToClock) {
                        val frame = decoder.nextVideoFrame()


                        if (frame != null) {
                            logger.trace { "time stamp: ${frame.timeStamp}" }


                            nextFrame += duration

                            while (displayQueue.size() >= displayQueue.maxSize - 1) {
                                logger.warn {
                                    "display queue is full (${displayQueue.size()} / ${displayQueue.maxSize})"
                                }
                                Thread.sleep(configuration.displayQueueCooldown.toLong())
                            }
                            synchronized(displayQueue) {
                                if (!frame.buffer.isNull) {
                                    displayQueue.push(frame)
                                } else {
                                    logger.error { "encountered frame with null buffer in play()" }
                                    frame.unref()
                                }
                            }
                        }
                    }
                    Thread.sleep(3)
                }
                logger.debug {
                    """display thread ended"""
                }
            }

        }
        state = State.PLAYING
    }

    @Suppress("unused")
    fun pause() {
        state = state.transition(State.PLAYING, State.PAUSED) {
            audioOut?.pause()
        }
    }

    @Suppress("unused")
    fun resume() {
        state = state.transition(State.PAUSED, State.PLAYING) {
            audioOut?.resume()
        }
    }

    @Suppress("unused")
    fun restart() {
        require(!disposed)
        endOfFileReached = false
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

    var position: Double = 0.0
        private set

    /**
     * Seek in the video
     * @param positionInSeconds the desired seeking time in seconds
     */
    @Suppress("unused")
    fun seek(positionInSeconds: Double) {
        require(!disposed)

        logger.debug { "video player seek requested" }
        seekRequested = true
        endOfFileReached = false
        seekPosition = positionInSeconds
    }

    private fun update() {
        if (state == State.PLAYING) {

            decoder?.let {
                if (it.videoQueueSize() == 0 && displayQueue.size() == 0 && !endOfFileReached) {
                    if (configuration.packetTimeout > 0) {
                        if (it.lastPacketReceived != 0L) {
                            if ((System.currentTimeMillis() - it.lastPacketReceived) / 1000.0 > configuration.packetTimeout) {
                                error("packet receive timeout")
                            }
                        }
                    }
                }
            }

            synchronized(displayQueue) {
                if (!configuration.allowFrameSkipping) {

                    val frame = displayQueue.peek()
                    if (frame != null) {
                        displayQueue.pop()
                        if (!frame.buffer.isNull) {
                            colorBuffer?.write(frame.buffer.data().capacity(frame.frameSize.toLong()).asByteBuffer())
                            colorBuffer?.let { lc ->
                                position = frame.timeStamp
                                newFrame.trigger(FrameEvent(lc, frame.timeStamp))
                            }
                        } else {
                            logger.error {
                                "encountered frame with null buffer"
                            }
                        }
                        frame.unref()
                    }
                } else {
                    var frame: VideoFrame?
                    while (!displayQueue.isEmpty()) {
                        frame = displayQueue.pop()
                        if (displayQueue.isEmpty()) {
                            if (!frame.buffer.isNull) {
                                colorBuffer?.write(frame.buffer.data().capacity(frame.frameSize.toLong()).asByteBuffer())
                                colorBuffer?.let { lc ->
                                    position = frame.timeStamp
                                    newFrame.trigger(FrameEvent(lc, frame.timeStamp))
                                }
                            } else {
                                logger.error {
                                    "encountered frame with null buffer"
                                }
                            }
                        }
                        frame.unref()
                    }
                }
            }
        }
        if (endOfFileReached && displayQueue.isEmpty() && (decoder?.videoQueueSize() ?: 0) == 0) {
            ended.trigger(VideoEvent())
        }
    }

    /**
     * Draw the current frame
     * @param blind only updates the frame data when true
     * @param update does not update the frame data when false
     */
    @Suppress("unused")
    fun draw(drawer: Drawer, blind: Boolean = false, update: Boolean = true) {
        require(!disposed)
        if (update) {
            update()
        }
        colorBuffer?.let {
            if (!blind) {
                drawer.image(it)
            }
        }
    }

    /**
     * Draw the current frame at given position and size
     * @param blind only updates the frame data when true
     * @param update does not update the frame data when false
     */
    @Suppress("unused")
    fun draw(drawer: Drawer,
             x: Double = 0.0,
             y: Double = 0.0,
             width: Double = this.width.toDouble(),
             height: Double = this.height.toDouble(),
             blind: Boolean = false, update: Boolean = true) {
        require(!disposed)
        if (update) {
            update()
        }
        colorBuffer?.let {
            if (!blind) {
                drawer.image(it, x, y, width, height)
            }
        }
    }

    /**
     * Draw the current frame using source and target rectangles
     * @param source the source rectangle
     * @param target the target rectangle
     * @param blind only updates the frame data when true
     * @param update does not update the frame data when false
     */
    @Suppress("unused")
    fun draw(drawer: Drawer,
             source: Rectangle,
             target: Rectangle,
             blind: Boolean = false, update: Boolean = true) {
        require(!disposed)
        if (update) {
            update()
        }
        colorBuffer?.let {
            if (!blind) {
                drawer.image(it, source, target)
            }
        }
    }

    /**
     * Destroy the video player, this releases all allocated resources
     */
    @Suppress("unused")
    fun dispose() {
        require(!disposed)

        audioOut?.dispose()
        decoder?.dispose()
        while (!displayQueue.isEmpty()) {
            val buffer = displayQueue.pop()
            buffer.unref()
        }
        disposed = true
    }
}

internal fun AVFormatContext.streamAt(index: Int): AVStream? =
        if (index < 0) null
        else this.streams(index)

internal val AVFormatContext.codecs: List<AVCodecParameters?>
    get() = List(nb_streams()) { streams(it).codecpar() }

internal fun AVStream.openCodec(): AVCodecContext {
    val codecPar = this.codecpar()
    val codec = avcodec_find_decoder(codecPar.codec_id())
    if (codec.isNull)
        throw Error("Unsupported codec with id ${codecPar.codec_id()}...")

    val dictionary = AVDictionary()

    av_dict_set(dictionary, "threads", "auto", 0)

    val codecContext = avcodec_alloc_context3(codec)
    avcodec_parameters_to_context(codecContext, codecPar)
    if (avcodec_open2(codecContext, codec, dictionary) < 0)
        throw Error("Couldn't open codec with id ${codecPar.codec_id()}")

    av_dict_free(dictionary)
    return codecContext
}

