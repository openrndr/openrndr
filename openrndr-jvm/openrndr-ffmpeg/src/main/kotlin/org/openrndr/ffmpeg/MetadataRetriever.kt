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

/**
 * @Serializable data classes exposing a subset of what ffprobe returns.
 *
 * The reason they are nullable is that video streams and audio streams
 * have each a subset of these properties. The missing ones will be null.
 *
 * The full JSON returned by `ffprobe` is logged in debug mode and can be studied
 * to add missing entries if needed.
 */

@Serializable
data class MetadataStream(
    val width: Int? = null,
    val height: Int? = null,
    val codec_type: String? = null,
    val codec_name: String? = null,
    val pix_fmt: String? = null,
    val bit_rate: Int? = null,
    val nb_frames: Int? = null,
    val sample_rate: Int? = null,
    val channels: Int? = null,
)

@Serializable
data class MetadataFormat(
    val filename: String? = null,
    val size: Int? = null,
    val start_time: Double? = null,
    val duration: Double? = null,
    val bit_rate: Int? = null,
    val nb_streams: Int? = null,
)

@Serializable
data class MetadataContainer(
    val streams: List<MetadataStream>,
    val format: MetadataFormat
)

private val jsonFormat = Json { ignoreUnknownKeys = true }

/**
 * A class to retrieve metadata, typically from video files, but compatible with any
 * media accepted by ffprobe.
 */
class MetadataRetriever {
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
     * Query video file metadata
     */
    fun query(filename: String): MetadataContainer {
        logger.debug { "Querying video metadata of $filename" }

        val ffprobeFile = findFfprobe()

        val arguments = listOf(
            ffprobeFile?.toString() ?: builtInFfprobeBinary,
            "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            filename
        )

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

            logger.debug {
                "json to parse: $output"
            }

            return jsonFormat.decodeFromString<MetadataContainer>(output)
        } catch (e: IOException) {
            logger.error { "system path: ${System.getenv("path")}" }
            logger.error { "command: ${arguments.joinToString(" ")}" }
            throw RuntimeException("failed to launch ffprobe", e)
        }
    }
}
