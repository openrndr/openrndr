package org.openrndr.ffmpeg

import mu.KotlinLogging
import org.lwjgl.BufferUtils
import org.openrndr.draw.ColorBuffer
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel
import java.util.*

private val logger = KotlinLogging.logger {}

abstract class VideoWriterProfile {
    abstract fun arguments(): Array<String>
}

class MP4Profile : VideoWriterProfile() {
    private var mode = WriterMode.Normal
    private var constantRateFactor = 23

    enum class WriterMode {
        Normal,
        Lossless
    }

    fun mode(mode: WriterMode): MP4Profile {
        this.mode = mode
        return this
    }

    /**
     * Sets the constant rate factor
     * @param constantRateFactor the constant rate factor (default is 23)
     * @return
     */
    fun constantRateFactor(constantRateFactor: Int): MP4Profile {
        this.constantRateFactor = constantRateFactor
        return this
    }

    override fun arguments(): Array<String> {
        return when (mode) {
            WriterMode.Normal -> arrayOf("-pix_fmt", "yuv420p", // this will produce videos that are playable by quicktime
                    "-an", "-vcodec", "libx264", "-crf", "" + constantRateFactor)
            WriterMode.Lossless -> {
                arrayOf("-pix_fmt", "yuv420p", // this will produce videos that are playable by quicktime
                        "-an", "-vcodec", "libx264", "-preset", "ultrafast")
            }
        }
    }
}

class X265Profile : VideoWriterProfile() {
    internal var mode = WriterMode.Normal
    internal var constantRateFactor = 28
    var hlg = false

    enum class WriterMode {
        Normal,
        Lossless

    }

    fun mode(mode: WriterMode): X265Profile {
        this.mode = mode
        return this
    }

    /**
     * Sets the constant rate factor
     * @param constantRateFactor the constant rate factor (default is 28)
     * @return
     */
    fun constantRateFactor(constantRateFactor: Int): X265Profile {
        this.constantRateFactor = constantRateFactor
        return this
    }

    override fun arguments(): Array<String> {
        if (mode == WriterMode.Normal) {

            if (!hlg) {
                return arrayOf("-pix_fmt", "yuv420", // this will produce videos that are playable by quicktime
                        "-an", "-vcodec", "libx265", "-crf", "" + constantRateFactor)
            } else {
                return arrayOf( // this will produce videos that are playable by quicktime
                        "-an", "" +
                        "-vcodec", "libx265",
                        "-pix_fmt", "yuv420p10le",
                        "-color_primaries", "bt2020",
                        "-colorspace", "bt2020_ncl",
                        "-color_trc", "arib-std-b67",
                        "-crf", "" + constantRateFactor)
// transfer=arib-std-b67
            }

        } else if (mode == WriterMode.Lossless) {
            return arrayOf("-pix_fmt", "yuv420p10", // this will produce videos that are playable by quicktime
                    "-an", "-vcodec", "libx265", "-preset", "ultrafast")
        } else {
            throw RuntimeException("unsupported write mode")
        }
    }


}

class VideoWriter {

    internal var ffmpegOutput = File("ffmpegOutput.txt")

    private var frameRate = 25
    private var width = -1
    private var height = -1

    private var filename: String? = "rndr.mp4"


    private lateinit var frameBuffer: ByteBuffer
    private lateinit var channel: WritableByteChannel
    private var ffmpeg: Process? = null
    private var movieStream: OutputStream? = null

    private var profile: VideoWriterProfile = MP4Profile()

    var inputFormat = "rgba"

    fun profile(profile: VideoWriterProfile): VideoWriter {
        this.profile = profile
        return this
    }

    fun width(): Int {
        return width
    }

    fun height(): Int {
        return height
    }


    fun size(width: Int, height: Int): VideoWriter {
        if (width % 2 != 0 || height % 2 != 0) {
            throw IllegalArgumentException("width ($width) and height ($height) should be divisible by 2")
        }
        this.width = width
        this.height = height
        return this
    }


    /**
     * Set the output file, should be set before calling start()
     *
     * @param filename the filename of the output file
     */
    fun output(filename: String): VideoWriter {
        this.filename = filename
        return this
    }


    /**
     * Sets the framerate of the output video
     *
     * @param frameRate the frame rate in frames per second
     * @return this
     */
    fun frameRate(frameRate: Int): VideoWriter {
        this.frameRate = frameRate
        return this
    }

    /**
     * Start writing to the video file
     */
    fun start(): VideoWriter {
        logger.debug { "starting video writer with $width x $height output using $inputFormat writing to $filename" }

        if (filename == null) {
            throw RuntimeException("output not set")
        }

        if (width <= 0) {
            throw RuntimeException("invalid width or width not set $width")
        }
        if (height <= 0) {
            throw RuntimeException("invalid height or height not set $height")
        }

        //frameBufferArray = ByteArray(width * height * 4)
        frameBuffer = when (inputFormat) {
                "rgba" -> BufferUtils.createByteBuffer(width*height*4)
                "rgba64le"  -> BufferUtils.createByteBuffer(width*height*8)

               else -> throw RuntimeException("unsupported format $inputFormat")
        }

        val preamble = arrayOf("-y", "-f", "rawvideo", "-vcodec", "rawvideo",
                "-s", String.format("%dx%d", width, height), "-pix_fmt", inputFormat, "-r", "" + frameRate, "-i", "-", "-vf", "vflip")

        val codec = profile.arguments()
        val arguments = ArrayList<String>()

        if (System.getProperty("os.name").contains("Windows")) {
            arguments.add("ffmpeg.exe")
        } else {
            arguments.add("ffmpeg")
        }
        arguments.addAll(Arrays.asList(*preamble))
        arguments.addAll(Arrays.asList(*codec))

        arguments.add(filename!!)

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
            System.err.println("system path: ${System.getenv("path")}")
            System.err.println("command: ${arguments.joinToString(" ")}")
            throw RuntimeException("failed to launch ffmpeg", e)
        }


    }

    /**
     * Feed a frame to the video encoder
     *
     * @param frame a ColorBuffer (RGBA, 8bit) holding the image data to be written to the video. The ColorBuffer should have the same resolution as the VideoWriter.
     */
    fun frame(frame: ColorBuffer): VideoWriter {
        if (! ((frame.width == width) && frame.height == height)) {
            throw RuntimeException("frame size mismatch")
        } else {
            (frameBuffer as Buffer).rewind()
            frameBuffer.order(ByteOrder.nativeOrder())
            frame.read(frameBuffer)
            (frameBuffer as Buffer).rewind()
            try {
                channel.write(frameBuffer)
                movieStream!!.flush()
            } catch (e: IOException) {
                e.printStackTrace()
                throw RuntimeException("failed to write frame", e)
            }
        }
        return this
    }

    /**
     * Stop writing to the video file. This closes the video, after calling stop() it is no longer possible to provide new frames.
     */
    fun stop(): VideoWriter {
        try {
            movieStream!!.close()
            try {
//                logger.info("waiting for ffmpeg to finish")
                ffmpeg!!.waitFor()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        } catch (e: IOException) {
            throw RuntimeException("failed to close the movie stream")
        }

        return this
    }

    companion object {
        //internal val logger = LogManager.getLogger(VideoWriter::class.java)
        fun create(): VideoWriter {
            return VideoWriter()
        }
    }


}
