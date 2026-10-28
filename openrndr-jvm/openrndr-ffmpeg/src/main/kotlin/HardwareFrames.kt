package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avutil.AVBufferRef
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.avutil.AVHWFramesContext
import org.bytedeco.ffmpeg.global.avcodec.avcodec_default_get_format
import org.bytedeco.ffmpeg.global.avcodec.avcodec_get_hw_frames_parameters
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.javacpp.PointerPointer
import org.lwjgl.system.JNI
import org.lwjgl.system.macosx.MacOSXLibrary

private val logger = KotlinLogging.logger {}

/**
 * Selects VideoToolbox decoding into BGRA frames, which can be imported as color buffers without copying them.
 * Falls back to the default format selection when VideoToolbox is not offered.
 */
internal class VideoToolboxBGRAFormat : AVCodecContext.Get_format_AVCodecContext_IntPointer() {
    override fun call(context: AVCodecContext, formats: IntPointer): Int {
        var index = 0L
        while (true) {
            val format = formats.get(index++)
            if (format == AV_PIX_FMT_NONE) {
                break
            }
            if (format == AV_PIX_FMT_VIDEOTOOLBOX && setBGRAFramesContext(context)) {
                return AV_PIX_FMT_VIDEOTOOLBOX
            }
        }
        logger.debug { "VideoToolbox BGRA frames not available, using default format" }
        return avcodec_default_get_format(context, formats)
    }

    private fun setBGRAFramesContext(context: AVCodecContext): Boolean {
        val framesReference = PointerPointer<AVBufferRef>(1)
        try {
            if (avcodec_get_hw_frames_parameters(
                    context, context.hw_device_ctx(), AV_PIX_FMT_VIDEOTOOLBOX, framesReference
                ) < 0
            ) {
                return false
            }
            val frames = AVBufferRef(framesReference.get(0))
            AVHWFramesContext(frames.data()).sw_format(AV_PIX_FMT_BGRA)
            if (av_hwframe_ctx_init(frames) < 0) {
                av_buffer_unref(frames)
                return false
            }
            // the codec context takes ownership of the reference
            context.hw_frames_ctx(frames)
            // FFmpeg only maps BGRA to a VideoToolbox format for full range video. VideoToolbox converts using the
            // range of the bitstream, the range of the codec context only selects the output format.
            context.color_range(AVCOL_RANGE_JPEG)
            return true
        } finally {
            framesReference.close()
        }
    }
}

/**
 * The functions of the CoreVideo framework
 */
internal object CoreVideo {
    private val library = MacOSXLibrary.getWithIdentifier("com.apple.CoreVideo")
    private val getIOSurface = library.getFunctionAddress("CVPixelBufferGetIOSurface")

    /**
     * Returns the `IOSurfaceRef` that backs a VideoToolbox [frame], or 0 when the frame is not backed by an IOSurface
     */
    fun ioSurface(frame: AVFrame): Long {
        // VideoToolbox frames store their CVPixelBufferRef in data[3]
        val pixelBuffer = frame.data(3).address()
        return if (pixelBuffer == 0L) 0L else JNI.invokePP(pixelBuffer, getIOSurface)
    }
}
