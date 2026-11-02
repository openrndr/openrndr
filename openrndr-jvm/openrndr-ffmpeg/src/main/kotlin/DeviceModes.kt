package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bytedeco.ffmpeg.avformat.AVInputFormat
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.global.avdevice.avdevice_register_all
import org.bytedeco.ffmpeg.global.avformat.av_find_input_format
import org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context
import org.bytedeco.ffmpeg.global.avformat.avformat_close_input
import org.bytedeco.ffmpeg.global.avformat.avformat_open_input
import org.bytedeco.ffmpeg.global.avutil.av_dict_free
import org.bytedeco.ffmpeg.global.avutil.av_dict_set
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType

private val logger = KotlinLogging.logger {}

/**
 * A mode a video device supports. What is known depends on the platform: AVFoundation (macOS) and dshow (Windows)
 * report frame rates, v4l2 (Linux) reports sizes and formats only.
 *
 * @property width the width of the images in pixels
 * @property height the height of the images in pixels
 * @property minFrameRate the lowest frame rate of the mode, null when unknown
 * @property maxFrameRate the highest frame rate of the mode, null when unknown
 * @property pixelFormat the FFmpeg name of the pixel format of uncompressed modes, like `yuyv422`, null when unknown
 * @property codec the FFmpeg name of the codec of compressed modes, like `mjpeg`
 */
data class VideoDeviceMode(
    val width: Int,
    val height: Int,
    val minFrameRate: Double? = null,
    val maxFrameRate: Double? = null,
    val pixelFormat: String? = null,
    val codec: String? = null
)

/**
 * Lists the modes of the video device [deviceName], a name from [listVideoDeviceNames]. On macOS the modes are the
 * formats AVFoundation lists for the device, screens have none. FFmpeg has no API for the modes of devices, on Windows
 * and Linux the modes are read from FFmpeg's log output: dshow lists them with `list_options`, v4l2 with `list_formats`.
 */
internal fun listVideoDeviceModes(deviceName: String): List<VideoDeviceMode> {
    if (Platform.type == PlatformType.MAC) {
        // screens are not cameras, opening them waits for frames
        return if (Regex("""^Capture screen \d+$""").matches(deviceName)) emptyList() else AVFoundationModes.list(deviceName)
    }
    avdevice_register_all()
    val (formatName, url, options) = when (Platform.type) {
        PlatformType.WINDOWS -> Triple("dshow", "video=$deviceName", mapOf("list_options" to "true"))
        PlatformType.GENERIC -> Triple("v4l2", deviceName, mapOf("list_formats" to "all"))
        else -> return emptyList()
    }
    val format = av_find_input_format(formatName)
    if (format == null || format.isNull) {
        logger.warn { "input format $formatName is not available" }
        return emptyList()
    }
    val lines = logLines { logModes(format, url, options) }
    return if (Platform.type == PlatformType.WINDOWS) parseDshowModes(lines) else parseV4l2Modes(lines)
}

/**
 * Opens [url] with [format] and [options] that make it log the modes of the device, opening fails or is closed
 */
private fun logModes(format: AVInputFormat, url: String, options: Map<String, String>) {
    val dictionary = AVDictionary()
    for ((key, value) in options) av_dict_set(dictionary, key, value, 0)
    val context = avformat_alloc_context()
    // a failed open frees the context
    avformat_open_input(context, url, format, dictionary)
    av_dict_free(dictionary)
    avformat_close_input(context)
}

/**
 * Returns the modes that dshow logs with `list_options`, one line per format, like
 * `pixel_format=yuyv422  min s=640x480 fps=5 max s=640x480 fps=30` or `vcodec=mjpeg  min s=...`. A format with
 * different minimum and maximum sizes gives a mode for both sizes.
 */
internal fun parseDshowModes(lines: List<String>): List<VideoDeviceMode> {
    val modePattern = Regex(
        """^\s*(pixel_format|vcodec)=(\S+)\s+min s=(\d+)x(\d+) fps=(\S+) max s=(\d+)x(\d+) fps=(\S+?)(?:\s.*|\(.*)?$"""
    )
    return lines.mapNotNull { modePattern.matchEntire(it)?.groupValues }.flatMap { match ->
        val pixelFormat = match[2].takeIf { match[1] == "pixel_format" }
        val codec = match[2].takeIf { match[1] == "vcodec" }
        val minFrameRate = match[5].toDoubleOrNull()
        val maxFrameRate = match[8].toDoubleOrNull()
        listOf(match[3].toInt() to match[4].toInt(), match[6].toInt() to match[7].toInt()).distinct().map { (width, height) ->
            VideoDeviceMode(width, height, minFrameRate, maxFrameRate, pixelFormat, codec)
        }
    }.distinct()
}

/**
 * Returns the modes that v4l2 logs with `list_formats`, one line per format with its sizes, like
 * `Raw       :     yuyv422 :           YUYV 4:2:2 : 640x480 1280x720`. Size ranges (`{min-max, step}x{...}`) are not
 * modes, v4l2 does not log frame rates.
 */
internal fun parseV4l2Modes(lines: List<String>): List<VideoDeviceMode> {
    val sizePattern = Regex("""(?<![\d{,])(\d+)x(\d+)(?![\d}])""")
    return lines.flatMap { line ->
        val kind = when {
            line.trimStart().startsWith("Raw") -> "raw"
            line.trimStart().startsWith("Compressed") -> "compressed"
            else -> return@flatMap emptyList()
        }
        // after "Raw       :" or "Compressed:" the fields are separated by " : ", the description can contain colons,
        // like "YUYV 4:2:2"
        val fields = line.substringAfter(":").split(" : ")
        if (fields.size < 3) return@flatMap emptyList()
        val name = fields[0].trim()
        sizePattern.findAll(fields.last()).map { size ->
            VideoDeviceMode(
                size.groupValues[1].toInt(), size.groupValues[2].toInt(),
                pixelFormat = name.takeIf { kind == "raw" },
                codec = name.takeIf { kind == "compressed" }
            )
        }.toList()
    }.distinct()
}
