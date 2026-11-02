package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.system.JNI.invokePI
import org.lwjgl.system.JNI.invokePP
import org.lwjgl.system.JNI.invokePPD
import org.lwjgl.system.JNI.invokePPP
import org.lwjgl.system.JNI.invokePPPP
import org.lwjgl.system.JNI.invokePPPPP
import org.lwjgl.system.JNI.invokePPPPPP
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memGetAddress
import org.lwjgl.system.MemoryUtil.memUTF8Safe
import org.lwjgl.system.macosx.DynamicLinkLoader.RTLD_NOW
import org.lwjgl.system.macosx.DynamicLinkLoader.dlopen
import org.lwjgl.system.macosx.DynamicLinkLoader.dlsym
import org.lwjgl.system.macosx.ObjCRuntime.objc_getClass
import org.lwjgl.system.macosx.ObjCRuntime.sel_getUid

private val logger = KotlinLogging.logger {}

/**
 * The FFmpeg names of the pixel formats that FFmpeg's avfoundation input supports, by their Core Video pixel format
 * type, as in FFmpeg's avfoundation.m
 */
private val pixelFormats = mapOf(
    0x00000001 to "monob",
    0x00000010 to "rgb555be",
    fourCC("L555") to "rgb555le",
    fourCC("B565") to "rgb565be",
    fourCC("L565") to "rgb565le",
    0x00000018 to "rgb24",
    fourCC("24BG") to "bgr24",
    0x00000020 to "0rgb",
    fourCC("BGRA") to "bgr0",
    fourCC("ABGR") to "0bgr",
    fourCC("RGBA") to "rgb0",
    fourCC("b48r") to "bgr48be",
    fourCC("2vuy") to "uyvy422",
    fourCC("r408") to "yuva444p",
    fourCC("y416") to "yuva444p16le",
    fourCC("v308") to "yuv444p",
    fourCC("v216") to "yuv422p16",
    fourCC("v210") to "yuv422p10",
    fourCC("v410") to "yuv444p10",
    fourCC("y420") to "yuv420p",
    fourCC("420v") to "nv12",
    fourCC("yuvs") to "yuyv422",
    fourCC("L008") to "gray"
)

private fun fourCC(code: String): Int = code.fold(0) { value, c -> (value shl 8) or c.code }

/**
 * Lists the modes of video devices with AVFoundation, through the Objective-C runtime. The modes are the formats of a
 * device, it is not opened: devices that do not deliver frames, such as Continuity Cameras that are not in use, would
 * make FFmpeg wait for a frame forever.
 */
internal object AVFoundationModes {
    private val avFoundation = dlopen("/System/Library/Frameworks/AVFoundation.framework/AVFoundation", RTLD_NOW)
    private val coreMedia = dlopen("/System/Library/Frameworks/CoreMedia.framework/CoreMedia", RTLD_NOW)
    private val objcMsgSend = org.lwjgl.system.macosx.ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend")
    private val getDimensions = dlsym(coreMedia, "CMVideoFormatDescriptionGetDimensions")
    private val getMediaSubType = dlsym(coreMedia, "CMFormatDescriptionGetMediaSubType")

    private fun send(receiver: Long, selector: String): Long = invokePPP(receiver, sel_getUid(selector), objcMsgSend)
    private fun send(receiver: Long, selector: String, a: Long): Long =
        invokePPPP(receiver, sel_getUid(selector), a, objcMsgSend)

    private fun send(receiver: Long, selector: String, a: Long, b: Long): Long =
        invokePPPPP(receiver, sel_getUid(selector), a, b, objcMsgSend)

    private fun send(receiver: Long, selector: String, a: Long, b: Long, c: Long): Long =
        invokePPPPPP(receiver, sel_getUid(selector), a, b, c, objcMsgSend)

    private fun sendDouble(receiver: Long, selector: String): Double =
        invokePPD(receiver, sel_getUid(selector), objcMsgSend)

    /** the value of an `NSString *` constant of AVFoundation, 0 when this version of macOS does not have it */
    private fun constant(name: String): Long = dlsym(avFoundation, name).let { if (it == 0L) 0L else memGetAddress(it) }

    private fun string(nsString: Long): String? = if (nsString == 0L) null else memUTF8Safe(send(nsString, "UTF8String"))

    private fun elements(nsArray: Long): List<Long> =
        if (nsArray == 0L) emptyList() else (0 until send(nsArray, "count")).map { send(nsArray, "objectAtIndex:", it) }

    private fun array(objects: List<Long>): Long = stackPush().use { stack ->
        val buffer = stack.mallocPointer(objects.size)
        objects.forEachIndexed { index, value -> buffer.put(index, value) }
        send(objc_getClass("NSArray"), "arrayWithObjects:count:", buffer.address(), objects.size.toLong())
    }

    /**
     * The devices of [mediaType], found like FFmpeg's avfoundation input finds them
     */
    private fun devices(mediaType: String, deviceTypes: List<String>): List<Long> {
        val types = deviceTypes.map { constant(it) }.filter { it != 0L }
        val media = constant(mediaType)
        if (types.isEmpty() || media == 0L) return emptyList()
        val session = send(
            objc_getClass("AVCaptureDeviceDiscoverySession"),
            "discoverySessionWithDeviceTypes:mediaType:position:", array(types), media, 0L
        )
        return if (session == 0L) emptyList() else elements(send(session, "devices"))
    }

    /** the video and muxed devices, in the order of FFmpeg's device list */
    private fun videoDevices(): List<Long> {
        val modern = constant("AVCaptureDeviceTypeExternal") != 0L
        val external = if (modern) listOf("AVCaptureDeviceTypeContinuityCamera", "AVCaptureDeviceTypeExternal")
        else listOf("AVCaptureDeviceTypeExternalUnknown")
        val video = devices(
            "AVMediaTypeVideo",
            listOf("AVCaptureDeviceTypeBuiltInWideAngleCamera", "AVCaptureDeviceTypeDeskViewCamera") + external
        )
        val muxed = devices(
            "AVMediaTypeMuxed",
            listOf(if (modern) "AVCaptureDeviceTypeExternal" else "AVCaptureDeviceTypeExternalUnknown")
        )
        return (video + muxed).distinct()
    }

    /**
     * Lists the modes of the device [name]: its name, its index, or the start of its name, like FFmpeg finds it
     */
    fun list(name: String): List<VideoDeviceMode> {
        val pool = send(send(objc_getClass("NSAutoreleasePool"), "alloc"), "init")
        try {
            val devices = videoDevices().map { it to string(send(it, "localizedName")) }
            val device = devices.firstOrNull { it.second == name }?.first
                ?: name.toIntOrNull()?.let { devices.getOrNull(it)?.first }
                ?: devices.firstOrNull { it.second?.startsWith(name) == true }?.first
            if (device == null) {
                logger.warn { "no AVFoundation video device '$name'" }
                return emptyList()
            }
            return elements(send(device, "formats")).flatMap { format ->
                val description = send(format, "formatDescription")
                // CMVideoDimensions is two int32s, returned in one register
                val dimensions = invokePP(description, getDimensions)
                val width = (dimensions and 0xffffffffL).toInt()
                val height = (dimensions ushr 32).toInt()
                val pixelFormat = pixelFormats[invokePI(description, getMediaSubType)]
                elements(send(format, "videoSupportedFrameRateRanges")).map { range ->
                    VideoDeviceMode(
                        width, height,
                        sendDouble(range, "minFrameRate"), sendDouble(range, "maxFrameRate"),
                        pixelFormat = pixelFormat
                    )
                }
            }.distinct()
        } finally {
            send(pool, "drain")
        }
    }
}
