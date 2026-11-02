package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.avutil.AVHWFramesContext
import org.bytedeco.ffmpeg.global.avutil.*
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.linux.LinuxLibrary
import org.lwjgl.system.linux.UNISTD
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.DmaBufImporter
import kotlin.jvm.JvmRecord

private val logger = KotlinLogging.logger { }

private const val VA_STATUS_SUCCESS = 0

/** VA_SURFACE_ATTRIB_MEM_TYPE_DRM_PRIME_2 */
private const val VA_MEM_TYPE_DRM_PRIME_2 = 0x40000000

private const val VA_EXPORT_SURFACE_READ_ONLY = 0x0001
private const val VA_EXPORT_SURFACE_SEPARATE_LAYERS = 0x0004

/** the size of VADRMPRIMESurfaceDescriptor */
private const val DESCRIPTOR_SIZE = 308

/** the offsets of the objects and layers arrays in VADRMPRIMESurfaceDescriptor, and the sizes of their elements */
private const val OBJECTS_OFFSET = 16
private const val OBJECT_SIZE = 16
private const val NUM_LAYERS_OFFSET = 80
private const val LAYERS_OFFSET = 84
private const val LAYER_SIZE = 56

/**
 * The functions of libva. The library is the one ffmpeg loaded, VADisplays of ffmpeg can only be used with it.
 */
private object LibVA {
    private val library: LinuxLibrary? = try {
        LinuxLibrary("libva.so.2")
    } catch (e: Throwable) {
        logger.debug(e) { "libva could not be loaded" }
        null
    }

    private fun function(name: String) = library?.getFunctionAddress(name) ?: 0L

    private val exportSurfaceHandle = function("vaExportSurfaceHandle")
    private val syncSurface = function("vaSyncSurface")

    val isAvailable = exportSurfaceHandle != 0L && syncSurface != 0L

    /** vaExportSurfaceHandle(display, surface, memType, flags, descriptor) */
    fun exportSurfaceHandle(display: Long, surface: Int, memType: Int, flags: Int, descriptor: Long): Int =
        JNI.invokePPI(display, surface, memType, flags, descriptor, exportSurfaceHandle)

    /** vaSyncSurface(display, surface) */
    fun syncSurface(display: Long, surface: Int): Int = JNI.invokePI(display, surface, syncSurface)
}

/** VAAPI frames store their VASurfaceID in data[3] */
private fun surfaceId(frame: AVFrame) = frame.data(3).address().toInt()

/** the VADisplay of a VAAPI frame, the first field of its AVVAAPIDeviceContext */
private fun vaDisplay(frame: AVFrame): Long =
    MemoryUtil.memGetAddress(AVHWFramesContext(frame.hw_frames_ctx().data()).device_ctx().hwctx().address())

private fun swFormat(frame: AVFrame) = AVHWFramesContext(frame.hw_frames_ctx().data()).sw_format()

/**
 * Is [frame], a decoded VAAPI frame, shown by importing it as DMA-BUFs?
 */
internal fun isVaapiFrameSupported(frame: AVFrame): Boolean {
    if (frame.format() != AV_PIX_FMT_VAAPI || frame.hw_frames_ctx() == null || !LibVA.isAvailable) {
        return false
    }
    val swFormat = swFormat(frame)
    return swFormat == AV_PIX_FMT_NV12 || swFormat == AV_PIX_FMT_P010
}

/**
 * Waits until the decoder has finished writing [frame], a VAAPI frame
 * @return false when decoding the frame failed
 */
internal fun syncVaapiFrame(frame: AVFrame): Boolean {
    val status = LibVA.syncSurface(vaDisplay(frame), surfaceId(frame))
    if (status != VA_STATUS_SUCCESS) {
        logger.error { "vaSyncSurface failed: VAStatus $status" }
    }
    return status == VA_STATUS_SUCCESS
}

/**
 * Returns the layout of [frame], a VAAPI frame for which [isVaapiFrameSupported] is true
 */
internal fun vaapiPlanarFormat(frame: AVFrame) = PlanarFormat(
    bits = if (swFormat(frame) == AV_PIX_FMT_P010) 10 else 8,
    fullRange = frame.color_range() == AVCOL_RANGE_JPEG
)

/**
 * A plane of an exported VAAPI surface
 */
@JvmRecord
private data class DmaBufPlane(
    val fd: Int,
    val width: Int,
    val height: Int,
    val fourcc: Int,
    val offset: Int,
    val pitch: Int,
    val modifier: Long
)

/**
 * The luma and chroma planes of a VAAPI frame, imported as DMA-BUFs
 */
internal class ImportedVaapiFrame(
    private val importer: DmaBufImporter,
    val luma: ColorBuffer,
    val chroma: ColorBuffer
) {
    fun release() {
        importer.release(luma)
        importer.release(chroma)
    }
}

/**
 * Imports the luma and chroma planes of [frame], a VAAPI frame for which [isVaapiFrameSupported] is true
 * @return the imported planes, or null when the frame could not be exported or imported
 */
internal fun importVaapiFrame(frame: AVFrame, importer: DmaBufImporter): ImportedVaapiFrame? {
    MemoryStack.stackPush().use { stack ->
        val descriptor = stack.calloc(8, DESCRIPTOR_SIZE)
        val address = MemoryUtil.memAddress(descriptor)
        val status = LibVA.exportSurfaceHandle(
            vaDisplay(frame), surfaceId(frame), VA_MEM_TYPE_DRM_PRIME_2,
            VA_EXPORT_SURFACE_READ_ONLY or VA_EXPORT_SURFACE_SEPARATE_LAYERS, address
        )
        if (status != VA_STATUS_SUCCESS) {
            logger.error { "vaExportSurfaceHandle failed: VAStatus $status" }
            return null
        }

        val width = descriptor.getInt(4)
        val height = descriptor.getInt(8)
        val objectCount = descriptor.getInt(12).coerceIn(0, 4)
        val fds = IntArray(objectCount) { descriptor.getInt(OBJECTS_OFFSET + it * OBJECT_SIZE) }
        try {
            val layerCount = descriptor.getInt(NUM_LAYERS_OFFSET)
            if (layerCount != 2) {
                logger.error { "exported VAAPI surface has $layerCount layers instead of 2" }
                return null
            }
            val planes = (0 until 2).map { layer ->
                val base = LAYERS_OFFSET + layer * LAYER_SIZE
                val objectIndex = descriptor.getInt(base + 8)
                if (descriptor.getInt(base + 4) != 1 || objectIndex !in 0 until objectCount) {
                    logger.error { "unexpected layout of exported VAAPI surface" }
                    return null
                }
                DmaBufPlane(
                    fd = fds[objectIndex],
                    // the chroma plane of 4:2:0 frames has half the resolution of the luma plane
                    width = if (layer == 0) width else (width + 1) / 2,
                    height = if (layer == 0) height else (height + 1) / 2,
                    fourcc = descriptor.getInt(base),
                    offset = descriptor.getInt(base + 24),
                    pitch = descriptor.getInt(base + 40),
                    modifier = descriptor.getLong(OBJECTS_OFFSET + objectIndex * OBJECT_SIZE + 8)
                )
            }
            val luma = planes[0].import(importer) ?: return null
            val chroma = planes[1].import(importer) ?: run {
                importer.release(luma)
                return null
            }
            return ImportedVaapiFrame(importer, luma, chroma)
        } finally {
            // the imported images reference the DMA-BUFs, not the file descriptors
            for (fd in fds) {
                UNISTD.close(null, fd)
            }
        }
    }
}

private fun DmaBufPlane.import(importer: DmaBufImporter) =
    importer.import(fd, width, height, fourcc, offset, pitch, modifier)
