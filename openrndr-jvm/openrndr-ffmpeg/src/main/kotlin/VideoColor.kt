package org.openrndr.ffmpeg

import org.bytedeco.ffmpeg.avutil.AVContentLightMetadata
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.avutil.AVMasteringDisplayMetadata
import org.bytedeco.ffmpeg.global.avutil.*

/**
 * The dynamic range of the frames in [VideoPlayerFFMPEG.colorBuffer]
 */
enum class VideoDynamicRange {
    /**
     * An 8 bit color buffer, HDR video is tone mapped to SDR
     */
    SDR,

    /**
     * A 16 bit floating point color buffer that holds linear colors in which 1.0 is SDR white, the reference white
     * of HDR video (203 nits). Brighter colors of HDR video are larger than 1.0, they are shown brighter than SDR
     * white in HDR windows, see `Configuration.hdr`.
     */
    HDR
}

internal enum class TransferFunction {
    /** sRGB and BT.709 like transfer functions */
    SDR,

    /** SMPTE ST 2084 */
    PQ,

    /** ARIB STD-B67 */
    HLG
}

/**
 * The matrix that converts YCbCr to RGB, by its luma coefficients
 */
internal enum class YCbCrMatrix(val kr: Double, val kb: Double) {
    BT601(0.299, 0.114),
    BT709(0.2126, 0.0722),
    BT2020(0.2627, 0.0593)
}

/**
 * How the colors of a frame are encoded
 * @param matrix the matrix that converts YCbCr to RGB
 * @param bt2020 are the primaries BT.2020, otherwise they are assumed to be BT.709
 * @param peakLuminance the luminance of the brightest pixel in nits, when the frame has metadata for it
 */
internal data class ColorDescription(
    val matrix: YCbCrMatrix,
    val transfer: TransferFunction,
    val bt2020: Boolean,
    val peakLuminance: Double?
) {
    val isHdr: Boolean get() = transfer != TransferFunction.SDR

    companion object {
        fun of(frame: AVFrame) = ColorDescription(
            yCbCrMatrix(frame),
            when (frame.color_trc()) {
                AVCOL_TRC_SMPTE2084 -> TransferFunction.PQ
                AVCOL_TRC_ARIB_STD_B67 -> TransferFunction.HLG
                else -> TransferFunction.SDR
            },
            frame.color_primaries() == AVCOL_PRI_BT2020,
            peakLuminance(frame)
        )

        /**
         * Returns the maximum content light level of [frame], or the peak luminance of its mastering display
         */
        private fun peakLuminance(frame: AVFrame): Double? {
            av_frame_get_side_data(frame, AV_FRAME_DATA_CONTENT_LIGHT_LEVEL)?.takeUnless { it.isNull }?.let {
                val maxCLL = AVContentLightMetadata(it.data()).MaxCLL()
                if (maxCLL > 0) return maxCLL.toDouble()
            }
            av_frame_get_side_data(frame, AV_FRAME_DATA_MASTERING_DISPLAY_METADATA)?.takeUnless { it.isNull }?.let {
                val metadata = AVMasteringDisplayMetadata(it.data())
                if (metadata.has_luminance() != 0) {
                    val maxLuminance = av_q2d(metadata.max_luminance())
                    if (maxLuminance > 0.0) return maxLuminance
                }
            }
            return null
        }
    }
}

/**
 * Returns the matrix that converts [frame] from YCbCr to RGB. Frames without a matrix are assumed to use BT.709 when
 * they are HD and BT.601 otherwise.
 */
internal fun yCbCrMatrix(frame: AVFrame): YCbCrMatrix = when (frame.colorspace()) {
    AVCOL_SPC_BT709 -> YCbCrMatrix.BT709
    AVCOL_SPC_BT470BG, AVCOL_SPC_SMPTE170M -> YCbCrMatrix.BT601
    AVCOL_SPC_BT2020_NCL, AVCOL_SPC_BT2020_CL -> YCbCrMatrix.BT2020
    else -> if (frame.height() >= 720) YCbCrMatrix.BT709 else YCbCrMatrix.BT601
}
