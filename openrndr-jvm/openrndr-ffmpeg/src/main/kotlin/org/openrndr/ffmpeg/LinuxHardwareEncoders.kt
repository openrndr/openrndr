package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.jvm.JvmRecord

private val logger = KotlinLogging.logger { }

/**
 * A hardware H.264 encoder on Linux: [codec] is `h264_nvenc` or `h264_vaapi`, [vaapiDevice] is the DRM render node
 * that `h264_vaapi` encodes with
 */
@JvmRecord
internal data class LinuxH264Encoder(val codec: String, val vaapiDevice: String? = null)

/** the encoders found per ffmpeg executable, an empty optional when none works */
private val encoders = ConcurrentHashMap<String, java.util.Optional<LinuxH264Encoder>>()

/**
 * Finds a hardware H.264 encoder that [ffmpeg] can encode with on this system: NVENC on NVIDIA GPUs, VAAPI on
 * Intel and AMD GPUs. The encoders are tried by encoding a frame, ffmpeg lists encoders that the driver or GPU does
 * not support, for example NVENC of a newer ffmpeg needs a newer NVIDIA driver.
 * @return the encoder, or null when no hardware encoder works
 */
internal fun findLinuxH264Encoder(ffmpeg: String): LinuxH264Encoder? = encoders.getOrPut(ffmpeg) {
    val candidates = listOf(LinuxH264Encoder("h264_nvenc")) + renderNodes().map { LinuxH264Encoder("h264_vaapi", it) }
    val encoder = candidates.firstOrNull { encodes(ffmpeg, it) }
    if (encoder == null) {
        logger.info { "no hardware H.264 encoder works with '$ffmpeg', encoding in software" }
    } else {
        logger.info { "encoding H.264 with ${encoder.codec}${encoder.vaapiDevice?.let { " on $it" } ?: ""}" }
    }
    java.util.Optional.ofNullable(encoder)
}.orElse(null)

/**
 * The DRM render nodes, the devices VAAPI encodes with
 */
private fun renderNodes(): List<String> =
    File("/dev/dri").listFiles { file -> file.name.startsWith("renderD") }
        ?.map { it.path }?.sorted() ?: emptyList()

/**
 * Does [encoder] encode a frame with [ffmpeg]?
 */
private fun encodes(ffmpeg: String, encoder: LinuxH264Encoder): Boolean {
    val command = listOf(ffmpeg, "-v", "error", "-f", "lavfi", "-i", "color=black:s=256x256:r=1") +
            (encoder.vaapiDevice?.let { listOf("-vaapi_device", it, "-vf", "format=nv12,hwupload") } ?: emptyList()) +
            listOf("-frames:v", "1", "-c:v", encoder.codec, "-f", "null", "-")
    val output = File.createTempFile("openrndr-ffmpeg-encoder", ".txt")
    return try {
        val process = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output).start()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            logger.debug { "${encoder.codec} did not encode a frame within 10 seconds" }
            return false
        }
        if (process.exitValue() != 0) {
            logger.debug { "${encoder.codec} can not encode: ${output.readText().trim()}" }
        }
        process.exitValue() == 0
    } catch (e: Exception) {
        logger.debug(e) { "failed to run '$ffmpeg'" }
        false
    } finally {
        output.delete()
    }
}
