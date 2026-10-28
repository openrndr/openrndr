package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bytedeco.ffmpeg.avdevice.AVDeviceInfoList
import org.bytedeco.ffmpeg.avformat.AVInputFormat
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.Callback_Pointer_int_String_Pointer
import org.bytedeco.ffmpeg.global.avdevice.avdevice_free_list_devices
import org.bytedeco.ffmpeg.global.avdevice.avdevice_list_input_sources
import org.bytedeco.ffmpeg.global.avdevice.avdevice_register_all
import org.bytedeco.ffmpeg.global.avformat.*
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Lists the names of the video devices. The devices are listed by FFmpeg's device list API, when the input format of
 * the platform does not implement it they are read from FFmpeg's log output (dshow, avfoundation) or from the device
 * files (v4l2).
 */
internal fun listVideoDeviceNames(): List<String> {
    avdevice_register_all()
    val formatName = when (Platform.type) {
        PlatformType.WINDOWS -> "dshow"
        PlatformType.MAC -> "avfoundation"
        PlatformType.GENERIC -> "v4l2"
        else -> return emptyList()
    }
    val format = av_find_input_format(formatName)
    if (format == null || format.isNull) {
        logger.warn { "input format $formatName is not available" }
        return emptyList()
    }

    listInputSources(format)?.let { return it }

    logger.debug { "$formatName does not list devices, using the fallback" }
    return when (Platform.type) {
        PlatformType.WINDOWS -> parseDshowVideoDevices(logLines { logDevices(format, "video=dummy") })
        PlatformType.MAC -> parseAVFoundationVideoDevices(logLines { logDevices(format, "") })
        else -> sortVideoDeviceFiles(File("/dev").list()?.toList() ?: emptyList())
    }
}

/**
 * Lists the video devices with avdevice_list_input_sources
 * @return the device names, or null when [format] does not list devices
 */
private fun listInputSources(format: AVInputFormat): List<String>? {
    val list = PointerPointer<AVDeviceInfoList>(1)
    try {
        val result = avdevice_list_input_sources(format, null as BytePointer?, null, list)
        if (result < 0) {
            logger.debug { "avdevice_list_input_sources failed: $result" }
            return null
        }
        val devices = AVDeviceInfoList(list.get(0))
        val names = (0 until devices.nb_devices()).mapNotNull { index ->
            val device = devices.devices(index)
            val mediaTypes = device.media_types()
            val video = if (device.nb_media_types() == 0 || mediaTypes == null) {
                // dshow lists the media types of devices, v4l2 lists video devices without them
                Platform.type != PlatformType.WINDOWS
            } else {
                (0 until device.nb_media_types()).any { mediaTypes.get(it.toLong()) == AVMEDIA_TYPE_VIDEO }
            }
            when {
                !video -> null
                // dshow names are unique device paths, devices are known by their descriptions
                Platform.type == PlatformType.WINDOWS -> device.device_description()?.string
                else -> device.device_name()?.string
            }
        }
        return if (Platform.type == PlatformType.GENERIC) sortVideoDeviceFiles(names) else names
    } finally {
        avdevice_free_list_devices(list)
        list.close()
    }
}

/**
 * Makes [format] log its devices
 */
private fun logDevices(format: AVInputFormat, url: String) {
    val options = AVDictionary()
    av_dict_set(options, "list_devices", "true", 0)
    val context = avformat_alloc_context()
    // listing the devices makes opening fail, which frees the context
    avformat_open_input(context, url, format, options)
    av_dict_free(options)
    avformat_close_input(context)
}

/**
 * Runs [block] and returns the lines that FFmpeg logs while it runs, without the "[name @ address]" prefixes
 */
private fun logLines(block: () -> Unit): List<String> {
    val text = StringBuilder()
    val line = BytePointer(1024)
    val printPrefix = IntPointer(1)
    val callback = object : Callback_Pointer_int_String_Pointer() {
        override fun call(source: Pointer?, level: Int, formatStr: String?, params: Pointer?) {
            synchronized(text) {
                printPrefix.put(0)
                // the length of the formatted line, which is truncated when it does not fit
                val length = av_log_format_line2(source, level, formatStr, params, line, 1024, printPrefix)
                if (length > 0) {
                    text.append(line.string.substring(0, minOf(length, 1023)))
                }
            }
        }
    }
    av_log_set_callback(callback)
    try {
        block()
    } finally {
        DefaultLogger.install()
        callback.close()
    }
    line.close()
    printPrefix.close()
    return text.lines()
}

/**
 * Returns the names of the video devices that dshow logs. Since FFmpeg 5 each device line ends with its media types,
 * like `"Integrated Camera" (video)`, before that devices were listed in video and audio sections.
 */
internal fun parseDshowVideoDevices(lines: List<String>): List<String> {
    val devicePattern = Regex("""^\s*"(.*)"(?:\s+\((.*)\))?\s*$""")
    var section = ""
    val names = mutableListOf<String>()
    for (line in lines) {
        when {
            line.contains("DirectShow video devices") -> section = "video"
            line.contains("DirectShow audio devices") -> section = "audio"
            else -> devicePattern.matchEntire(line)?.let { match ->
                val mediaTypes = match.groupValues[2]
                val video = if (mediaTypes.isNotEmpty()) {
                    mediaTypes.split(",").any { it.trim() == "video" }
                } else {
                    section == "video"
                }
                if (video) {
                    names.add(match.groupValues[1])
                }
            }
        }
    }
    return names
}

/**
 * Returns the names of the video devices that avfoundation logs, the lines between its video and audio device
 * headers, like `[0] FaceTime HD Camera`
 */
internal fun parseAVFoundationVideoDevices(lines: List<String>): List<String> {
    val start = lines.indexOfFirst { it.contains("AVFoundation video devices") }
    if (start < 0) {
        return emptyList()
    }
    val devicePattern = Regex("""^\[\d+] (.*)$""")
    return lines.drop(start + 1)
        .takeWhile { !it.contains("AVFoundation audio devices") }
        .mapNotNull { devicePattern.matchEntire(it.trim())?.groupValues?.get(1) }
}

/**
 * Returns the video device files in [names], `/dev/video<N>` or `video<N>`, as `/dev/video<N>` sorted by number
 */
internal fun sortVideoDeviceFiles(names: List<String>): List<String> {
    val pattern = Regex("""^(?:/dev/)?video(\d+)$""")
    return names.mapNotNull { name -> pattern.matchEntire(name)?.groupValues?.get(1)?.toIntOrNull() }
        .sorted()
        .map { "/dev/video$it" }
}
