package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import java.io.File
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger { }

/**
 * How frames reach an encoder: the pixel format they are converted to, or null when [filters] convert them, and
 * filters to append to the filter chain
 * @since openrndr 0.5.1
 */
data class EncoderInput(val pixelFormat: String?, val filters: List<String>)

/**
 * A hardware video encoder of FFmpeg, as found by [findHardwareEncoder]. [codec] is the name of the FFmpeg encoder,
 * for example `h264_nvenc` or `hevc_vaapi`. [device] is the device the encoder encodes with, the DRM render node of
 * VAAPI encoders, null for the other encoders.
 *
 * Supported are the NVENC (`_nvenc`), VAAPI (`_vaapi`), Quick Sync (`_qsv`), AMF (`_amf`), Media Foundation (`_mf`)
 * and VideoToolbox (`_videotoolbox`) encoders, the constructor throws an [IllegalArgumentException] for other
 * encoders, see [forCodec].
 * @since openrndr 0.5.1
 */
data class HardwareEncoder(val codec: String, val device: String? = null) {
    private val api = codec.substringAfterLast('_')

    init {
        require(api in APIS) { "'$codec' is not a hardware encoder" }
    }

    /**
     * Does the encoder encode full chroma (4:4:4)? Of the hardware encoders only NVENC does.
     */
    val encodesFullChroma: Boolean get() = api == "nvenc"

    /**
     * How frames in [pixelFormat] reach the encoder. [pixelFormat] is an FFmpeg pixel format: `yuv420p` for 8 bit
     * 4:2:0, `p010le` for 10 bit 4:2:0, `yuv444p` for full chroma. VAAPI encoders take frames that are uploaded to
     * the GPU by filters, Quick Sync, AMF and Media Foundation take 8 bit 4:2:0 frames as NV12.
     */
    fun input(pixelFormat: String = "yuv420p"): EncoderInput = when (api) {
        "vaapi" -> EncoderInput(null, listOf("format=${nv12(pixelFormat)}", "hwupload"))
        "qsv", "amf", "mf" -> EncoderInput(nv12(pixelFormat), emptyList())
        else -> EncoderInput(pixelFormat, emptyList())
    }

    /**
     * The arguments of the encoder for [quality], a constant quality from 1 to 100 where higher is better, or null
     * for the default bitrate of the encoder. A quality of 60 is comparable to the default constant rate factor of
     * libx264. The arguments include the [device] of VAAPI encoders, keep Media Foundation from falling back to its
     * software encoder, and let VideoToolbox fall back to its software encoder on Macs without a hardware encoder.
     */
    fun arguments(quality: Int?): List<String> {
        val quantizer = quality?.let { quantizer(it).toString() }
        return when (api) {
            // VideoToolbox encodes with a constant quality, and falls back to its software encoder on Macs without a
            // hardware encoder
            "videotoolbox" -> listOf("-allow_sw", "1") + (quality?.let { listOf("-q:v", it.toString()) } ?: emptyList())
            // NVENC, VAAPI and AMF encode with a constant quantizer, NVENC varies it per frame type
            "nvenc" -> listOf("-preset", "p5", "-tune", "hq") +
                    (quantizer?.let { listOf("-rc", "vbr", "-cq", it, "-b:v", "0") } ?: emptyList())

            "vaapi" -> listOf("-vaapi_device", device ?: DEFAULT_VAAPI_DEVICE) +
                    (quantizer?.let { listOf("-rc_mode", "CQP", "-qp", it) } ?: emptyList())

            // Quick Sync encodes with its intelligent constant quality
            "qsv" -> listOf("-preset", "slow") + (quantizer?.let { listOf("-global_quality", it) } ?: emptyList())
            // only the H.264 encoder of AMF has a quantizer for B-frames
            "amf" -> listOf("-quality", "quality") + (quantizer?.let {
                listOf("-rc", "cqp", "-qp_i", it, "-qp_p", it) + if (codec == "h264_amf") listOf("-qp_b", it) else emptyList()
            } ?: emptyList())

            // without -hw_encoding Media Foundation falls back to its software encoder, it encodes with a quality
            // from 1 to 100
            "mf" -> listOf("-hw_encoding", "1") +
                    (quality?.let { listOf("-rate_control", "quality", "-quality", it.toString()) } ?: emptyList())

            else -> emptyList()
        }
    }

    companion object {
        private val APIS = setOf("nvenc", "vaapi", "qsv", "amf", "mf", "videotoolbox")

        /** the DRM render node of the first GPU */
        const val DEFAULT_VAAPI_DEVICE = "/dev/dri/renderD128"

        /**
         * Returns the hardware encoder for [codec], an FFmpeg encoder name, or null when [codec] is not a hardware
         * encoder, like `libx264`. [device] is the device of VAAPI encoders.
         */
        fun forCodec(codec: String, device: String? = null): HardwareEncoder? =
            if (codec.substringAfterLast('_') in APIS) {
                HardwareEncoder(codec, device?.takeIf { codec.endsWith("_vaapi") })
            } else {
                null
            }
    }
}

/** 8 bit 4:2:0 frames are encoded as NV12 by the encoders that take NV12 */
private fun nv12(pixelFormat: String) = if (pixelFormat == "yuv420p") "nv12" else pixelFormat

/**
 * The quantizer of NVENC, VAAPI and AMF, and the ICQ quality of Quick Sync, for [quality], from 51 to 4 where lower
 * is better. A quality of 60 maps to 23, NVENC and Quick Sync then encode with about the structural similarity of
 * libx264 at its default constant rate factor.
 */
internal fun quantizer(quality: Int) = (51 - 0.47 * quality).roundToInt().coerceIn(0, 51)

private data class EncoderKey(val ffmpeg: String, val codecName: String, val pixelFormat: String)

/** the encoders found, an empty optional when none works */
private val encoders = ConcurrentHashMap<EncoderKey, Optional<HardwareEncoder>>()

/**
 * Finds a hardware encoder for [codecName] that [ffmpeg] can encode frames in [pixelFormat] with on this system.
 * [codecName] is the FFmpeg name of the codec, for example `h264`, `hevc` or `av1`, [pixelFormat] is the pixel format
 * the frames are encoded in, see [HardwareEncoder.input].
 *
 * The encoders that are tried are, in order:
 * - macOS: VideoToolbox, which is not tried, it falls back to its software encoder on Macs without a hardware encoder
 * - Linux: NVENC on NVIDIA GPUs, VAAPI on Intel and AMD GPUs, on each DRM render node
 * - Windows: NVENC on NVIDIA GPUs, Quick Sync on Intel GPUs, AMF on AMD GPUs, Media Foundation on the others
 *
 * The encoders are tried by encoding a frame with [ffmpeg], with the arguments of [HardwareEncoder.input] and
 * [HardwareEncoder.arguments]. FFmpeg lists encoders that the GPU or its driver does not support, for example NVENC of
 * a newer FFmpeg needs a newer NVIDIA driver. Trying an encoder takes up to a second, the result is cached for
 * [ffmpeg], [codecName] and [pixelFormat].
 * @param ffmpeg the FFmpeg executable the frames are encoded with, by default the one [VideoWriter] runs
 * @return the encoder, or null when no hardware encoder works
 * @since openrndr 0.5.1
 */
fun findHardwareEncoder(
    codecName: String,
    pixelFormat: String = "yuv420p",
    ffmpeg: String = ffmpegExecutable()
): HardwareEncoder? {
    if (Platform.type == PlatformType.MAC) {
        return HardwareEncoder("${codecName}_videotoolbox")
    }
    return encoders.computeIfAbsent(EncoderKey(ffmpeg, codecName, pixelFormat)) { key ->
        val encoder = candidates(codecName).firstOrNull { encodes(key.ffmpeg, it, key.pixelFormat) }
        if (encoder == null) {
            logger.info { "no hardware $codecName encoder for $pixelFormat works with '$ffmpeg'" }
        } else {
            logger.info { "encoding $codecName with ${encoder.codec}${encoder.device?.let { " on $it" } ?: ""}" }
        }
        Optional.ofNullable(encoder)
    }.orElse(null)
}

/**
 * The hardware encoders for [codecName] on this platform, in the order they are tried
 */
private fun candidates(codecName: String): List<HardwareEncoder> = when (Platform.type) {
    PlatformType.WINDOWS -> listOf("nvenc", "qsv", "amf", "mf").map { HardwareEncoder("${codecName}_$it") }
    PlatformType.GENERIC -> listOf(HardwareEncoder("${codecName}_nvenc")) +
            renderNodes().map { HardwareEncoder("${codecName}_vaapi", it) }

    else -> emptyList()
}

/**
 * The DRM render nodes, the devices VAAPI encodes with
 */
private fun renderNodes(): List<String> =
    File("/dev/dri").listFiles { file -> file.name.startsWith("renderD") }
        ?.map { it.path }?.sorted() ?: emptyList()

/**
 * Does [encoder] encode a frame in [pixelFormat] with [ffmpeg]?
 */
private fun encodes(ffmpeg: String, encoder: HardwareEncoder, pixelFormat: String): Boolean {
    val input = encoder.input(pixelFormat)
    val command = listOf(ffmpeg, "-v", "error", "-f", "lavfi", "-i", "color=black:s=256x256:r=1") +
            (input.pixelFormat?.let { listOf("-pix_fmt", it) } ?: emptyList()) +
            (if (input.filters.isNotEmpty()) listOf("-vf", input.filters.joinToString(",")) else emptyList()) +
            listOf("-c:v", encoder.codec) + encoder.arguments(null) + listOf("-frames:v", "1", "-f", "null", "-")
    val output = File.createTempFile("openrndr-ffmpeg-encoder", ".txt")
    return try {
        val process = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output).start()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            logger.debug { "${encoder.codec} did not encode a frame within 10 seconds" }
            return false
        }
        if (process.exitValue() != 0) {
            logger.debug { "${encoder.codec} can not encode $pixelFormat: ${output.readText().trim()}" }
        }
        process.exitValue() == 0
    } catch (e: Exception) {
        logger.debug(e) { "failed to run '$ffmpeg'" }
        false
    } finally {
        output.delete()
    }
}
