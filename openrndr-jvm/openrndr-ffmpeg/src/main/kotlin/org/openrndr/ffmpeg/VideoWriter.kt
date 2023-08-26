package org.openrndr.ffmpeg

import mu.KotlinLogging
import org.bytedeco.ffmpeg.ffmpeg
import org.bytedeco.javacpp.Loader
import org.lwjgl.BufferUtils
import org.openrndr.ExtensionDslMarker
import org.openrndr.draw.ColorBuffer
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

@Deprecated("Use H264Profile", replaceWith = ReplaceWith("H264Profile"))
typealias MP4Profile = H264Profile

class H264Profile : VideoWriterProfile() {

    /**
     * constant rate factor (default is 23)
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

    var videoCodec = CODEC_LIBX264 as String?
    var hwaccel = null as String?
    var preset = null as String?


    var pixelFormat = "yuv420p" as String?
    var userArguments = emptyArray<String>()

    val filters = mutableListOf("vflip")

    override fun arguments(): Array<String> {
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

        if (highPrecisionChroma) {
            filters.add("colorspace=bt709:iall=bt601-6-625:fast=1")
        }

        val hwaccelArguments = hwaccel?.let { arrayOf("-hwaccel", it) } ?: emptyArray()
        val pixelFormatArguments = pixelFormat?.let { arrayOf("-pix_fmt", it) } ?: emptyArray()
        val constantRateArguments = constantRateFactor?.let { arrayOf("-crf", it.toString()) } ?: emptyArray()
        val presetArguments = preset?.let { arrayOf("-preset", it) } ?: emptyArray()
        val videoCodecArguments = videoCodec?.let { arrayOf("-vcodec", it) } ?: emptyArray()
        val filterArguments = arrayOf("-vf", filters.joinToString(","))

        val arguments =
            hwaccelArguments + pixelFormatArguments + chromaArguments + filterArguments + videoCodecArguments + constantRateArguments + presetArguments + userArguments

        return arguments
    }
}

class VideoWriter {
    internal var ffmpegOutput = File("ffmpegOutput.txt")
    private var stopped = false

    var frameRate = 25
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

        frameBuffer = when (inputFormat) {
            "rgba" -> BufferUtils.createByteBuffer(width * height * 4)
            "rgba64le" -> BufferUtils.createByteBuffer(width * height * 8)
            else -> throw RuntimeException("unsupported format $inputFormat")
        }

        val preamble = arrayOf(
            "-y", "-f", "rawvideo", "-vcodec", "rawvideo",
            "-s", String.format("%dx%d", width, height), "-pix_fmt", inputFormat, "-r", "" + frameRate, "-i", "-"
        )

        val codec = profile.arguments()
        val arguments = ArrayList<String>()

        // Decide ffmpeg location
        val ffmpegExe = if (System.getProperty("os.name").contains("Windows")) "ffmpeg.exe" else "ffmpeg"
        when (val ffmpegPathArg = (System.getProperties()["org.openrndr.ffmpeg"] as? String)) {
            // `-Dorg.openrndr.ffmpeg` not provided by the user
            null -> {
                val localExecutable = File("./$ffmpegExe")
                if (localExecutable.exists()) {
                    // 1. Use ffmpeg from current working directory
                    arguments.add("./$ffmpegExe")
                } else {
                    // 2. Assume ffmpeg it's in the path. Let it fail if not found.
                    arguments.add(ffmpegExe)
                }
            }

            // 3. Use built-in ffmpeg from jar because user passed `-Dorg.openrndr.ffmpeg=jar`
            "jar" -> {
                arguments.add(builtInFfmpegBinary)
            }

            // 4. User requested specific ffmpeg binary with `-Dorg.openrndr.ffmpeg=/some/path/ffmpeg[.exe]`
            else -> {
                arguments.add(ffmpegPathArg)
            }
        }

        arguments.addAll(listOf(*preamble))
        arguments.addAll(listOf(*codec))

        arguments.add(finalFilename)

        logger.debug {
            "using arguments: ${arguments.joinToString()}"
        }

        val pb = ProcessBuilder().command(*arguments.toTypedArray())
        pb.redirectErrorStream(true)
        pb.redirectOutput(ffmpegOutput)

        try {
            ffmpeg = pb.start()
            movieStream = ffmpeg!!.outputStream
            channel = Channels.newChannel(movieStream)
            return this
        } catch (e: IOException) {
            logger.error { "system path: ${System.getenv("path")}" }
            logger.error { "command: ${arguments.joinToString(" ")}" }
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
     */
    fun frame(frame: ColorBuffer) {
        if (!stopped) {
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
        } else {
            logger.warn { "ignoring frame after VideoWriter stop" }
        }
    }

    /**
     * Stop writing to the video file. This closes the video, after calling stop() it is no longer possible to provide new frames.
     */
    fun stop() {
        if (!stopped) {
            stopped = true
            try {
                movieStream!!.close()
                try {
                    logger.info("waiting for ffmpeg to finish")
                    ffmpeg!!.waitFor()
                    logger.info("ffmpeg finished")
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            } catch (e: IOException) {
                throw RuntimeException("failed to close the movie stream", e)
            }
        }
    }
}
