package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bytedeco.ffmpeg.ffmpeg
import org.bytedeco.javacpp.Loader
import org.lwjgl.BufferUtils
import org.openrndr.ExtensionDslMarker
import org.openrndr.draw.ColorBuffer
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel

private val logger = KotlinLogging.logger {}

private val builtInFfmpegBinary by lazy { Loader.load(ffmpeg::class.java) }

@ExtensionDslMarker
abstract class VideoWriterProfile {
    abstract fun arguments(): Array<String>
    abstract val fileExtension: String

    /**
     * Arguments for the frames that are fed to the encoder, these are passed before the input
     */
    open fun inputArguments(): Array<String> = emptyArray()

    /**
     * The transfer function of HDR video, null for SDR video. HDR profiles take frames with linear colors in which
     * 1.0 is SDR white, the reference white of HDR video (203 nits), like the frames of HDR windows.
     */
    open val hdrTransfer: HDRTransfer? get() = null
}

val File.fileWithoutExtension: File
    get() {
        val p = parent
        return if (p == null) {
            File(nameWithoutExtension)
        } else {
            File(p, nameWithoutExtension)
        }
    }

/**
 * Rotations of the frames of a video, for example to store a portrait video in landscape for a screen that is mounted
 * on its side
 */
enum class VideoRotation(
    /** the FFmpeg filters that rotate upright frames */
    val filters: List<String>
) {
    NONE(emptyList()),

    /** rotate 90 degrees clockwise, the top of the frame is on the right side of the video */
    CLOCKWISE_90(listOf("transpose=clock")),

    /** rotate 90 degrees counterclockwise, the top of the frame is on the left side of the video */
    COUNTERCLOCKWISE_90(listOf("transpose=cclock")),

    /** rotate 180 degrees, the frame is upside down */
    ROTATE_180(listOf("hflip", "vflip"))
}

@Deprecated("Use H264Profile", replaceWith = ReplaceWith("H264Profile"))
typealias MP4Profile = H264Profile

class H264Profile : VideoWriterProfile() {

    /**
     * constant rate factor of the software encoder (default is 23)
     */
    var constantRateFactor = null as Int?

    @Deprecated("Use constantRateFactor property")
    fun constantRateFactor(factor: Int) {
        constantRateFactor = factor
    }

    override var fileExtension = "mp4"

    var highPrecisionChroma = true

    val CODEC_LIBX264 = "libx264"
    val CODEC_H264_NVENC = "h264_nvenc"
    val CODEC_H264_VIDEOTOOLBOX = "h264_videotoolbox"

    var videoCodec = CODEC_LIBX264 as String?

    @Deprecated("-hwaccel selects a decoder, it does not accelerate encoding, use hardwareAcceleration()")
    var hwaccel = null as String?

    /**
     * the preset of the software encoder
     */
    var preset = null as String?

    /**
     * constant quality of the VideoToolbox encoder, from 1 to 100 where higher is better, or null for its default
     * bitrate. The default of 60 is comparable to the default constant rate factor of the software encoder.
     */
    var quality = 60 as Int?

    /**
     * Encode with the VideoToolbox encoder on macOS, which uses a fraction of the CPU time of the software encoder.
     * On other platforms the encoder is not changed.
     */
    fun hardwareAcceleration() {
        if (Platform.type == PlatformType.MAC) {
            videoCodec = CODEC_H264_VIDEOTOOLBOX
        }
    }


    var pixelFormat = "yuv420p" as String?

    /**
     * Encode colors at full resolution (4:4:4, the yuv444p pixel format) instead of at half resolution (4:2:0). This
     * keeps the colors of thin lines and edges, but the video does not play in web browsers. The VideoToolbox encoder
     * does not encode 4:4:4, with full chroma the software encoder is used instead.
     */
    var fullChroma = false

    var userArguments = emptyArray<String>()

    /**
     * Rotates the frames, a 90 degree rotation swaps the width and height of the video. The pixels are rotated, the
     * video is not tagged with a rotation that players apply.
     */
    var rotation = VideoRotation.NONE

    val filters = mutableListOf("vflip")

    override fun arguments(): Array<String> {
        val videoCodec = if (fullChroma && videoCodec == CODEC_H264_VIDEOTOOLBOX) {
            logger.warn { "$CODEC_H264_VIDEOTOOLBOX does not encode full chroma, using $CODEC_LIBX264" }
            CODEC_LIBX264
        } else {
            videoCodec
        }
        val pixelFormat = if (fullChroma) "yuv444p" else pixelFormat

        val chromaArguments = if (highPrecisionChroma) {
            arrayOf(
                "-sws_flags",
                "spline+accurate_rnd+full_chroma_int",
                "-color_range",
                "1",
                "-colorspace",
                "1",
                "-color_primaries",
                "1",
                "-color_trc",
                "1"
            )
        } else {
            emptyArray()
        }

        // the filters are not changed, the arguments can be requested more than once
        val filters = filters + rotation.filters +
                (if (highPrecisionChroma) listOf("colorspace=bt709:iall=bt601-6-625:fast=1") else emptyList())

        @Suppress("DEPRECATION")
        val hwaccelArguments = hwaccel?.let { arrayOf("-hwaccel", it) } ?: emptyArray()
        val pixelFormatArguments = pixelFormat?.let { arrayOf("-pix_fmt", it) } ?: emptyArray()
        val videoCodecArguments = videoCodec?.let { arrayOf("-vcodec", it) } ?: emptyArray()
        val filterArguments = arrayOf("-vf", filters.joinToString(","))

        // VideoToolbox has no constant rate factor and presets, it encodes with a constant quality, and falls back
        // to its software encoder on Macs without a hardware encoder
        val encoderArguments = if (videoCodec?.endsWith("_videotoolbox") == true) {
            arrayOf("-allow_sw", "1") + (quality?.let { arrayOf("-q:v", it.toString()) } ?: emptyArray())
        } else {
            (constantRateFactor?.let { arrayOf("-crf", it.toString()) } ?: emptyArray()) +
                    (preset?.let { arrayOf("-preset", it) } ?: emptyArray())
        }

        val arguments =
            hwaccelArguments + pixelFormatArguments + chromaArguments + filterArguments + videoCodecArguments + encoderArguments + userArguments

        return arguments
    }
}

class VideoWriter(
    val useVariableFrameRate: Boolean = false
) {
    internal var ffmpegOutput = File("ffmpegOutput.txt")
    private var stopped = false

    var frameRate: Number = 25
    var width = -1
        private set
    var height = -1
        private set

    private var filename: String = "openrndr.mp4"

    private lateinit var frameBuffer: ByteBuffer
    private lateinit var channel: WritableByteChannel
    private var ffmpeg: Process? = null
    private var movieStream: OutputStream? = null

    private var profile: VideoWriterProfile = H264Profile()

    /** encodes the frames of HDR profiles */
    private var hdrEncoder: HDRFrameEncoder? = null

    var inputFormat = "rgba"

    fun profile(profile: VideoWriterProfile) {
        this.profile = profile
    }

    fun advisedSize(width: Int, height: Int): Pair<Int, Int> {
        val advisedWidth = (width / 2) * 2
        val advisedHeight = (height / 2) * 2
        return Pair(advisedWidth, advisedHeight)
    }


    fun size(width: Int, height: Int) {
        if (width % 2 != 0 || height % 2 != 0) {
            throw IllegalArgumentException("width ($width) and height ($height) should be divisible by 2")
        }
        this.width = width
        this.height = height
    }

    /**
     * Set the output file, should be set before calling start()
     *
     * @param filename the filename of the output file
     */
    fun output(filename: String) {
        this.filename = filename
    }

    fun findFfmpeg(): File? {
        val ffmpegExe = if (System.getProperty("os.name").contains("Windows")) "ffmpeg.exe" else "ffmpeg"

        return when (val ffmpegPathArg = (System.getProperties()["org.openrndr.ffmpeg"] as? String)) {
            // 1, 2. `-Dorg.openrndr.ffmpeg` not provided by the user
            null -> {
                val directory = (listOf(File(".")) + Platform.path()).find { File(it, ffmpegExe).exists() }
                if (directory != null) {
                    logger.info { "ffmpeg found in '$directory'" }
                    File(directory, ffmpegExe)
                } else {
                    null
                }
            }
            // 3. Use built-in ffmpeg from jar because user passed `-Dorg.openrndr.ffmpeg=jar`
            "jar" -> {
                null
            }
            // 4. User requested specific ffmpeg binary with `-Dorg.openrndr.ffmpeg=/some/path/ffmpeg[.exe]`
            else -> {
                val specified = File(ffmpegPathArg)
                require(specified.exists()) {
                    "file '$ffmpegPathArg' does not exist"
                }
                specified
            }
        }
    }

    /**
     * Start writing to the video file
     */
    fun start(): VideoWriter {
        logger.debug { "starting video writer with $width x $height output using $inputFormat writing to $filename" }

        val file = File(filename)
        val finalFilename = if (file.extension != profile.fileExtension) {
            "${file.fileWithoutExtension}.${profile.fileExtension}"
        } else {
            filename
        }

        if (width <= 0) {
            throw RuntimeException("invalid width or width not set $width")
        }
        if (height <= 0) {
            throw RuntimeException("invalid height or height not set $height")
        }

        // HDR frames are encoded as Y'CbCr with 16 bits per component
        if (profile.hdrTransfer != null) {
            inputFormat = "ayuv64le"
        }

        frameBuffer = when (inputFormat) {
            "rgba" -> BufferUtils.createByteBuffer(width * height * 4)
            "rgba64le", "ayuv64le" -> BufferUtils.createByteBuffer(width * height * 8)
            else -> throw RuntimeException("unsupported format $inputFormat")
        }

        val preamble = arrayOf(
            "-y", "-f", "rawvideo", "-vcodec", "rawvideo",
            "-s", String.format("%dx%d", width, height), "-pix_fmt", inputFormat, "-r", "" + frameRate
        ) + profile.inputArguments() + arrayOf("-i", "-")

        var profileArguments = profile.arguments()

        if (useVariableFrameRate) {
            val div = (1E6 / frameRate.toDouble()).toInt()
            val setPtsFilter = "setpts=(RTCTIME-RTCSTART)/$div"
            if (profileArguments.contains("-vf")) {
                val index = profileArguments.indexOf("-vf")
                profileArguments[index + 1] += ",$setPtsFilter"
            } else {
                profileArguments += arrayOf("-vf", setPtsFilter)
            }
            profileArguments += arrayOf("-fps_mode", "vfr")
        }

        val command = mutableListOf<String>()

        val ffmpegFile = findFfmpeg()

        if (ffmpegFile != null) {
            command.add(ffmpegFile.toString())
        } else {
            command.add(builtInFfmpegBinary)
        }
        command.addAll(listOf(*preamble))
        command.addAll(listOf(*profileArguments))

        command.add(finalFilename)

        logger.debug {
            "using arguments: ${command.joinToString()}"
        }

        val pb = ProcessBuilder().command(*command.toTypedArray())
        pb.redirectErrorStream(true)
        pb.redirectOutput(ffmpegOutput)

        try {
            ffmpeg = pb.start()
            movieStream = ffmpeg!!.outputStream
            channel = Channels.newChannel(movieStream)
            return this
        } catch (e: IOException) {
            logger.error { "system path: ${System.getenv("path")}" }
            logger.error { "command: ${command.joinToString(" ")}" }
            throw RuntimeException("failed to launch ffmpeg", e)
        }
    }

    /**
     * Returns true if the video process was started
     *
     */
    val started get() = ffmpeg != null

    /**
     * Feed a frame to the video encoder
     *
     * @param frame a ColorBuffer (RGBA, 8bit) holding the image data to be written to the video. The ColorBuffer should have the same resolution as the VideoWriter.
     * For HDR profiles, see [VideoWriterProfile.hdrTransfer], the ColorBuffer holds linear colors in which 1.0 is SDR
     * white, typically a floating point ColorBuffer.
     */
    fun frame(frame: ColorBuffer) {
        if (!stopped) {
            val transfer = profile.hdrTransfer
            if (transfer != null) {
                val encoder = hdrEncoder ?: HDRFrameEncoder(width, height, transfer).also { hdrEncoder = it }
                writeFrame(encoder.encode(frame))
            } else {
                writeFrame(frame)
            }
        } else {
            logger.warn { "ignoring frame after VideoWriter stop" }
        }
    }

    private fun writeFrame(frame: ColorBuffer) {
        val frameBytes =
            frame.effectiveWidth * frame.effectiveHeight * frame.format.componentCount * frame.type.componentSize
        require(frameBytes == frameBuffer.capacity()) {
            "frame size/format/type mismatch. ($width x $height) vs ({${frame.effectiveWidth} x ${frame.effectiveHeight})"
        }
        (frameBuffer as Buffer).rewind()
        frame.read(frameBuffer)
        (frameBuffer as Buffer).rewind()
        try {
            channel.write(frameBuffer)
        } catch (e: IOException) {
            throw RuntimeException("failed to write frame", e)
        }
    }

    /**
     * Stop writing to the video file. This closes the video, after calling stop() it is no longer possible to provide new frames.
     */
    fun stop() {
        if (!stopped) {
            stopped = true
            hdrEncoder?.close()
            hdrEncoder = null
            try {
                movieStream!!.close()
                try {
                    logger.info { "waiting for ffmpeg to finish" }
                    ffmpeg!!.waitFor()
                    logger.info { "ffmpeg finished" }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            } catch (e: IOException) {
                throw RuntimeException("failed to close the movie stream", e)
            }
        }
    }
}
