package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bytedeco.ffmpeg.ffprobe
import org.bytedeco.javacpp.Loader
import org.openrndr.platform.Platform
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

private val logger = KotlinLogging.logger {}

private val builtInFfprobeBinary by lazy { Loader.load(ffprobe::class.java) }

@Serializable
data class VideoMetadataStream(
    val width: Int = 0,
    val height: Int = 0,
    val codec_type: String,
    val pix_fmt: String = "",
    val bit_rate: Int,
    val nb_frames: Int
)

@Serializable
data class VideoMetadataFormat(
    val duration: Double,
)

@Serializable
data class VideoMetadata(
    val streams: List<VideoMetadataStream>,
    val format: VideoMetadataFormat
)

private val jsonFormat = Json { ignoreUnknownKeys = true }

class VideoQuery {
    private var ffprobe: Process? = null

    fun findFfprobe(): File? {
        val ffprobeExe = if (System.getProperty("os.name").contains("Windows")) "ffprobe.exe" else "ffprobe"

        return when (val ffprobePathArg = (System.getProperties()["org.openrndr.ffprobe"] as? String)) {
            // 1, 2. `-Dorg.openrndr.ffprobe` not provided by the user
            null -> {
                val directory = (listOf(File(".")) + Platform.path()).find { File(it, ffprobeExe).exists() }
                if (directory != null) {
                    logger.info { "ffprobe found in '$directory'" }
                    File(directory, ffprobeExe)
                } else {
                    null
                }
            }
            // 3. Use built-in ffprobe from jar because user passed `-Dorg.openrndr.ffprobe=jar`
            "jar" -> {
                null
            }
            // 4. User requested specific ffprobe binary with `-Dorg.openrndr.ffprobe=/some/path/ffprobe[.exe]`
            else -> {
                val specified = File(ffprobePathArg)
                require(specified.exists()) {
                    "file '$ffprobePathArg' does not exist"
                }
                specified
            }
        }
    }

    /**
     * Start writing to the video file
     */
    fun query(filename: String): VideoMetadata {
        logger.debug { "Querying video metadata of $filename" }

        val arguments = mutableListOf(
            "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            "-show_streams"
        )

        val ffprobeFile = findFfprobe()

        if (ffprobeFile != null) {
            arguments.add(0, ffprobeFile.toString())
        } else {
            arguments.add(0, builtInFfprobeBinary)
        }
        arguments.add(filename)

        logger.debug {
            "using arguments: ${arguments.joinToString()}"
        }

        val pb = ProcessBuilder().command(*arguments.toTypedArray())

        try {
            ffprobe = pb.start()

            val output = buildString {
                BufferedReader(InputStreamReader(ffprobe!!.inputStream)).use { reader ->
                    reader.lines().forEach { line -> appendLine(line) }
                }
            }

            val error = buildString {
                BufferedReader(InputStreamReader(ffprobe!!.errorStream)).use { reader ->
                    reader.lines().forEach { line -> appendLine(line) }
                }
            }

            if (ffprobe!!.waitFor() != 0) {
                throw IOException("ffprobe failed with exit code ${ffprobe!!.exitValue()}: $error")
            }

            return jsonFormat.decodeFromString<VideoMetadata>(output)
        } catch (e: IOException) {
            logger.error { "system path: ${System.getenv("path")}" }
            logger.error { "command: ${arguments.joinToString(" ")}" }
            throw RuntimeException("failed to launch ffprobe", e)
        }
    }
}
