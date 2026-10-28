package org.openrndr.ffmpeg

import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.avutil.*
import org.lwjgl.system.JNI
import org.lwjgl.system.macosx.MacOSXLibrary

/** kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange and FullRange, NV12 */
private const val PIXEL_FORMAT_420V = 0x34323076
private const val PIXEL_FORMAT_420F = 0x34323066

/** kCVPixelFormatType_420YpCbCr10BiPlanarVideoRange and FullRange, P010 */
private const val PIXEL_FORMAT_X420 = 0x78343230
private const val PIXEL_FORMAT_XF20 = 0x78663230

/**
 * The functions of the CoreVideo framework
 */
internal object CoreVideo {
    private val library = MacOSXLibrary.getWithIdentifier("com.apple.CoreVideo")
    private val getIOSurface = library.getFunctionAddress("CVPixelBufferGetIOSurface")
    private val getPixelFormatType = library.getFunctionAddress("CVPixelBufferGetPixelFormatType")

    /** VideoToolbox frames store their CVPixelBufferRef in data[3] */
    private fun pixelBuffer(frame: AVFrame) = frame.data(3).address()

    /**
     * Returns the `IOSurfaceRef` that backs a VideoToolbox [frame], or 0 when the frame is not backed by an IOSurface
     */
    fun ioSurface(frame: AVFrame): Long {
        val pixelBuffer = pixelBuffer(frame)
        return if (pixelBuffer == 0L) 0L else JNI.invokePP(pixelBuffer, getIOSurface)
    }

    /**
     * Returns the layout of a VideoToolbox [frame], or null when the frame has to be converted on the CPU
     */
    fun planarFormat(frame: AVFrame): PlanarFormat? {
        val pixelBuffer = pixelBuffer(frame)
        if (pixelBuffer == 0L) {
            return null
        }
        return when (JNI.invokePI(pixelBuffer, getPixelFormatType)) {
            PIXEL_FORMAT_420V -> PlanarFormat(bits = 8, fullRange = false)
            PIXEL_FORMAT_420F -> PlanarFormat(bits = 8, fullRange = true)
            PIXEL_FORMAT_X420 -> PlanarFormat(bits = 10, fullRange = false)
            PIXEL_FORMAT_XF20 -> PlanarFormat(bits = 10, fullRange = true)
            else -> null
        }
    }
}

/**
 * The layout of a biplanar 4:2:0 frame: a luma plane and an interleaved chroma plane, with [bits] per sample stored
 * in the most significant bits of 8 or 16 bit samples
 */
internal data class PlanarFormat(val bits: Int, val fullRange: Boolean) {
    /** scales normalized samples to code values */
    val codeScale: Double get() = if (bits == 8) 255.0 else 65535.0 / (1 shl (16 - bits))

    private val maxCode get() = ((1 shl bits) - 1).toDouble()
    private val scale get() = (1 shl (bits - 8)).toDouble()

    val lumaOffset: Double get() = if (fullRange) 0.0 else 16.0 * scale
    val lumaRange: Double get() = if (fullRange) maxCode else 219.0 * scale
    val chromaOffset: Double get() = 128.0 * scale
    val chromaRange: Double get() = if (fullRange) maxCode else 224.0 * scale
}

/**
 * Returns the luma coefficients (Kr, Kb) of the matrix that converts [frame] from YCbCr to RGB. Frames without a
 * matrix are assumed to use BT.709 when they are HD and BT.601 otherwise.
 */
internal fun lumaCoefficients(frame: AVFrame): Pair<Double, Double> = when (frame.colorspace()) {
    AVCOL_SPC_BT709 -> Pair(0.2126, 0.0722)
    AVCOL_SPC_BT470BG, AVCOL_SPC_SMPTE170M -> Pair(0.299, 0.114)
    AVCOL_SPC_BT2020_NCL, AVCOL_SPC_BT2020_CL -> Pair(0.2627, 0.0593)
    else -> if (frame.height() >= 720) Pair(0.2126, 0.0722) else Pair(0.299, 0.114)
}
