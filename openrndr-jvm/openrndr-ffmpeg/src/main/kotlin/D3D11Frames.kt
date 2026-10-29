package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bytedeco.ffmpeg.avutil.AVBufferRef
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.avutil.AVHWDeviceContext
import org.bytedeco.ffmpeg.avutil.AVHWFramesContext
import org.bytedeco.ffmpeg.global.avutil.*
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.D3D11TextureImporter
import org.openrndr.draw.D3D11TextureImporterFactory
import kotlin.math.min

private val logger = KotlinLogging.logger { }

/**
 * The number of decoded D3D11VA frames that are queued, the frames are kept in the fixed size surface pool of the
 * decoder
 */
private const val MAX_QUEUED_FRAMES = 16

/** the D3D11VA frames the decoder needs besides the queued frames: the frame that is shown and spare frames */
private const val EXTRA_POOL_FRAMES = 4

internal fun d3d11QueueSize(configuration: VideoPlayerConfiguration) =
    min(configuration.videoFrameQueueSize, MAX_QUEUED_FRAMES)

/**
 * The surfaces to add to the pool of a D3D11VA decoder that queues its frames
 */
internal fun d3d11ExtraPoolFrames(configuration: VideoPlayerConfiguration) =
    d3d11QueueSize(configuration) + configuration.displayQueueSize + EXTRA_POOL_FRAMES

// DXGI_FORMAT
private const val DXGI_FORMAT_R10G10B10A2_UNORM = 24
private const val DXGI_FORMAT_B8G8R8A8_UNORM = 87
private const val DXGI_FORMAT_NV12 = 103
private const val DXGI_FORMAT_P010 = 104

// DXGI_COLOR_SPACE_TYPE
private const val RGB_FULL_G22_NONE_P709 = 0
private const val YCBCR_STUDIO_G22_LEFT_P601 = 6
private const val YCBCR_FULL_G22_LEFT_P601 = 7
private const val YCBCR_STUDIO_G22_LEFT_P709 = 8
private const val YCBCR_FULL_G22_LEFT_P709 = 9
private const val YCBCR_STUDIO_G22_LEFT_P2020 = 10
private const val YCBCR_FULL_G22_LEFT_P2020 = 11
private const val RGB_FULL_G2084_NONE_P2020 = 12
private const val YCBCR_STUDIO_G2084_LEFT_P2020 = 13
private const val RGB_FULL_G22_NONE_P2020 = 17

private const val D3D11_BIND_SHADER_RESOURCE = 0x8
private const val D3D11_BIND_RENDER_TARGET = 0x20
private const val D3D11_VIDEO_PROCESSOR_FORMAT_SUPPORT_INPUT = 0x1
private const val D3D11_VIDEO_PROCESSOR_FORMAT_SUPPORT_OUTPUT = 0x2
private const val D3D11_VPIV_DIMENSION_TEXTURE2D = 1
private const val D3D11_VPOV_DIMENSION_TEXTURE2D = 1

/** IID_ID3D11VideoContext1, a7f026da-a5f8-4487-a564-15e34357651e */
private val IID_ID3D11VideoContext1 = byteArrayOf(
    0xda.toByte(), 0x26, 0xf0.toByte(), 0xa7.toByte(), 0xf8.toByte(), 0xa5.toByte(), 0x87.toByte(), 0x44,
    0xa5.toByte(), 0x64, 0x15, 0xe3.toByte(), 0x43, 0x57, 0x65, 0x1e
)

/**
 * Calls of COM interfaces by their index in the vtable, see d3d11.h and d3d11_1.h
 */
private object COM {
    private fun method(self: Long, index: Int) = MemoryUtil.memGetAddress(MemoryUtil.memGetAddress(self) + index * 8L)

    fun queryInterface(self: Long, iid: Long, result: Long) = JNI.invokePPPI(self, iid, result, method(self, 0))
    fun release(self: Long) {
        if (self != 0L) JNI.invokePI(self, method(self, 2))
    }

    // ID3D11Device
    fun createTexture2D(device: Long, desc: Long, result: Long) =
        JNI.invokePPPPI(device, desc, 0L, result, method(device, 5))

    // ID3D11DeviceContext
    fun flush(context: Long) = JNI.invokePV(context, method(context, 111))

    // ID3D11VideoDevice
    fun createVideoProcessor(videoDevice: Long, enumerator: Long, result: Long) =
        JNI.invokePPPI(videoDevice, enumerator, 0, result, method(videoDevice, 4))

    fun createVideoProcessorInputView(videoDevice: Long, resource: Long, enumerator: Long, desc: Long, result: Long) =
        JNI.invokePPPPPI(videoDevice, resource, enumerator, desc, result, method(videoDevice, 8))

    fun createVideoProcessorOutputView(videoDevice: Long, resource: Long, enumerator: Long, desc: Long, result: Long) =
        JNI.invokePPPPPI(videoDevice, resource, enumerator, desc, result, method(videoDevice, 9))

    fun createVideoProcessorEnumerator(videoDevice: Long, desc: Long, result: Long) =
        JNI.invokePPPI(videoDevice, desc, result, method(videoDevice, 10))

    // ID3D11VideoProcessorEnumerator
    fun checkVideoProcessorFormat(enumerator: Long, format: Int, flags: Long) =
        JNI.invokePPI(enumerator, format, flags, method(enumerator, 8))

    // ID3D11VideoContext
    fun setOutputTargetRect(videoContext: Long, processor: Long, rect: Long) =
        JNI.invokePPPV(videoContext, processor, 1, rect, method(videoContext, 13))

    fun setStreamSourceRect(videoContext: Long, processor: Long, rect: Long) =
        JNI.invokePPPV(videoContext, processor, 0, 1, rect, method(videoContext, 30))

    fun setStreamDestRect(videoContext: Long, processor: Long, rect: Long) =
        JNI.invokePPPV(videoContext, processor, 0, 1, rect, method(videoContext, 31))

    fun setStreamAutoProcessingMode(videoContext: Long, processor: Long, enable: Boolean) =
        JNI.invokePPV(videoContext, processor, 0, if (enable) 1 else 0, method(videoContext, 37))

    fun videoProcessorBlt(videoContext: Long, processor: Long, outputView: Long, streams: Long) =
        JNI.invokePPPPI(videoContext, processor, outputView, 0, 1, streams, method(videoContext, 53))

    // ID3D11VideoContext1
    fun setOutputColorSpace1(videoContext: Long, processor: Long, colorSpace: Int) =
        JNI.invokePPV(videoContext, processor, colorSpace, method(videoContext, 70))

    fun setStreamColorSpace1(videoContext: Long, processor: Long, colorSpace: Int) =
        JNI.invokePPV(videoContext, processor, 0, colorSpace, method(videoContext, 74))
}

private fun Int.checkHResult(what: String) {
    check(this >= 0) { "$what failed: HRESULT 0x${toUInt().toString(16)}" }
}

private fun MemoryStack.rect(width: Int, height: Int) = MemoryUtil.memAddress(ints(0, 0, width, height))

/**
 * The D3D11VA device of a frame: the interfaces of its `AVD3D11VADeviceContext`
 */
private class D3D11Device(hwctx: Long) {
    val device = MemoryUtil.memGetAddress(hwctx)
    val deviceContext = MemoryUtil.memGetAddress(hwctx + 8)
    val videoDevice = MemoryUtil.memGetAddress(hwctx + 16)
    val videoContext = MemoryUtil.memGetAddress(hwctx + 24)
    private val lock = MemoryUtil.memGetAddress(hwctx + 32)
    private val unlock = MemoryUtil.memGetAddress(hwctx + 40)
    private val lockContext = MemoryUtil.memGetAddress(hwctx + 48)

    /**
     * Runs [block] while holding the lock that the decoder holds while it uses the device context
     */
    inline fun <T> locked(block: () -> T): T {
        JNI.invokePV(lockContext, lock)
        try {
            return block()
        } finally {
            JNI.invokePV(lockContext, unlock)
        }
    }
}

/**
 * Is [frame], a decoded D3D11VA frame, converted to RGB by [D3D11FrameConverter]?
 */
internal fun isD3D11FrameSupported(frame: AVFrame): Boolean {
    if (frame.format() != AV_PIX_FMT_D3D11 || frame.hw_frames_ctx() == null) {
        return false
    }
    val swFormat = AVHWFramesContext(frame.hw_frames_ctx().data()).sw_format()
    // there is no RGB color space with the HLG transfer function to convert to
    return (swFormat == AV_PIX_FMT_NV12 || swFormat == AV_PIX_FMT_P010) &&
            frame.color_trc() != AVCOL_TRC_ARIB_STD_B67
}

/**
 * Converts D3D11VA frames to R'G'B' with the Direct3D 11 video processor, on the GPU. The frames are converted to
 * a texture that is shared with the draw driver; the conversion keeps the transfer function and the primaries of
 * the frame, those are converted by [FrameConverter].
 */
internal class D3D11FrameConverter(private val importerFactory: D3D11TextureImporterFactory) : AutoCloseable {

    /** the conversion of frames of one size and format */
    private inner class Processor(
        val deviceRef: AVBufferRef,
        val width: Int,
        val height: Int,
        val swFormat: Int
    ) : AutoCloseable {
        val d3d = D3D11Device(AVHWDeviceContext(deviceRef.data()).hwctx().address())
        private var videoContext1 = 0L
        private var enumerator = 0L
        private var processor = 0L
        private val textures = mutableListOf<Long>()
        private val outputViews = mutableListOf<Long>()
        var importer: D3D11TextureImporter? = null

        /** an output texture, shared with the draw driver as [colorBuffer] */
        inner class Slot(val outputView: Long, val colorBuffer: ColorBuffer) {
            /** the frame that was converted to the slot */
            var frame: VideoFrame? = null
        }

        /** frames are converted ahead to one slot while the frame in the other slot is shown */
        val slots = mutableListOf<Slot>()

        /** input views by texture and array slice */
        private val inputViews = mutableMapOf<Pair<Long, Int>, Long>()

        private var streamColorSpace = -1
        private var outputColorSpace = -1

        /**
         * @return false when frames can not be converted
         */
        fun create(): Boolean = d3d.locked {
            MemoryStack.stackPush().use { stack ->
                val result = stack.mallocPointer(1)

                val iid = stack.bytes(*IID_ID3D11VideoContext1)
                if (COM.queryInterface(d3d.videoContext, MemoryUtil.memAddress(iid), result.address()) < 0) {
                    logger.info { "ID3D11VideoContext1 is not supported" }
                    return@locked false
                }
                videoContext1 = result[0]

                // D3D11_VIDEO_PROCESSOR_CONTENT_DESC
                val contentDesc = stack.ints(0, 1, 1, width, height, 1, 1, width, height, 0)
                COM.createVideoProcessorEnumerator(d3d.videoDevice, MemoryUtil.memAddress(contentDesc), result.address())
                    .checkHResult("CreateVideoProcessorEnumerator")
                enumerator = result[0]

                val inputFormat = if (swFormat == AV_PIX_FMT_P010) DXGI_FORMAT_P010 else DXGI_FORMAT_NV12
                if (!supports(inputFormat, D3D11_VIDEO_PROCESSOR_FORMAT_SUPPORT_INPUT)) {
                    logger.info { "the video processor does not convert DXGI format $inputFormat" }
                    return@locked false
                }
                // keep the precision of 10 bit video
                val outputFormat = if (inputFormat == DXGI_FORMAT_P010 &&
                    supports(DXGI_FORMAT_R10G10B10A2_UNORM, D3D11_VIDEO_PROCESSOR_FORMAT_SUPPORT_OUTPUT)
                ) DXGI_FORMAT_R10G10B10A2_UNORM else DXGI_FORMAT_B8G8R8A8_UNORM

                COM.createVideoProcessor(d3d.videoDevice, enumerator, result.address())
                    .checkHResult("CreateVideoProcessor")
                processor = result[0]

                // the driver could enhance the frame, which would change its colors
                COM.setStreamAutoProcessingMode(d3d.videoContext, processor, false)
                COM.setStreamSourceRect(d3d.videoContext, processor, stack.rect(width, height))
                COM.setStreamDestRect(d3d.videoContext, processor, stack.rect(width, height))
                COM.setOutputTargetRect(d3d.videoContext, processor, stack.rect(width, height))

                val importer = importerFactory.createD3D11TextureImporter(d3d.device) ?: return@locked false
                this.importer = importer
                val type = if (outputFormat == DXGI_FORMAT_R10G10B10A2_UNORM) ColorType.UINT16 else ColorType.UINT8

                repeat(2) {
                    // D3D11_TEXTURE2D_DESC
                    val textureDesc = stack.ints(
                        width, height, 1, 1, outputFormat, 1, 0, 0,
                        D3D11_BIND_RENDER_TARGET or D3D11_BIND_SHADER_RESOURCE, 0, 0
                    )
                    COM.createTexture2D(d3d.device, MemoryUtil.memAddress(textureDesc), result.address())
                        .checkHResult("CreateTexture2D")
                    val texture = result[0]
                    textures.add(texture)

                    // D3D11_VIDEO_PROCESSOR_OUTPUT_VIEW_DESC
                    val outputViewDesc = stack.ints(D3D11_VPOV_DIMENSION_TEXTURE2D, 0, 0, 0)
                    COM.createVideoProcessorOutputView(
                        d3d.videoDevice, texture, enumerator, MemoryUtil.memAddress(outputViewDesc), result.address()
                    ).checkHResult("CreateVideoProcessorOutputView")
                    val outputView = result[0]
                    outputViews.add(outputView)

                    val colorBuffer = importer.import(texture, width, height, ColorFormat.RGBa, type)
                        ?: return@locked false
                    slots.add(Slot(outputView, colorBuffer))
                }
                true
            }
        }

        private fun supports(format: Int, flag: Int): Boolean = MemoryStack.stackPush().use { stack ->
            val flags = stack.mallocInt(1)
            COM.checkVideoProcessorFormat(enumerator, format, MemoryUtil.memAddress(flags)) >= 0 &&
                    flags[0] and flag != 0
        }

        private fun inputView(texture: Long, slice: Int, stack: MemoryStack): Long =
            inputViews.getOrPut(texture to slice) {
                // D3D11_VIDEO_PROCESSOR_INPUT_VIEW_DESC
                val desc = stack.ints(0, D3D11_VPIV_DIMENSION_TEXTURE2D, 0, slice)
                val result = stack.mallocPointer(1)
                COM.createVideoProcessorInputView(
                    d3d.videoDevice, texture, enumerator, MemoryUtil.memAddress(desc), result.address()
                ).checkHResult("CreateVideoProcessorInputView")
                result[0]
            }

        /**
         * Converts [frame] to [slot], the conversion is queued on the device
         */
        fun convert(frame: AVFrame, slot: Slot) = d3d.locked {
            MemoryStack.stackPush().use { stack ->
                val (input, output) = colorSpaces(frame)
                if (input != streamColorSpace) {
                    COM.setStreamColorSpace1(videoContext1, processor, input)
                    streamColorSpace = input
                }
                if (output != outputColorSpace) {
                    COM.setOutputColorSpace1(videoContext1, processor, output)
                    outputColorSpace = output
                }
                // D3D11VA frames are slices of texture arrays
                val view = inputView(frame.data(0).address(), (frame.data(1)?.address() ?: 0L).toInt(), stack)

                // D3D11_VIDEO_PROCESSOR_STREAM
                val stream = stack.calloc(8, 72)
                stream.putInt(0, 1)
                stream.putLong(32, view)
                COM.videoProcessorBlt(d3d.videoContext, processor, slot.outputView, MemoryUtil.memAddress(stream))
                    .checkHResult("VideoProcessorBlt")
                // start the conversion now, it is waited for when the slot is locked
                COM.flush(d3d.deviceContext)
            }
        }

        override fun close() {
            slots.forEach { importer?.release(it.colorBuffer) }
            slots.clear()
            importer?.close()
            d3d.locked {
                inputViews.values.forEach { COM.release(it) }
                inputViews.clear()
                outputViews.forEach { COM.release(it) }
                textures.forEach { COM.release(it) }
                COM.release(processor)
                COM.release(enumerator)
                COM.release(videoContext1)
            }
            av_buffer_unref(deviceRef)
        }
    }

    private var processor: Processor? = null

    /** frames are converted on the CPU after the processor failed */
    var failed = false
        private set

    /** the slot of the frame that was shown last */
    private var shownSlot: Processor.Slot? = null

    /**
     * Converts [frame], which holds a D3D11VA frame, to the returned color buffer, or returns the color buffer it
     * was converted to by [prepare]. The color buffer has to be used between [D3D11TextureImporter.lock] and
     * [D3D11TextureImporter.unlock] of the returned importer.
     * @return the color buffer and its importer, or null when [frame] could not be converted
     */
    fun convert(frame: VideoFrame): Pair<ColorBuffer, D3D11TextureImporter>? {
        val slot = slot(frame) ?: return null
        shownSlot = slot
        return slot.colorBuffer to processor!!.importer!!
    }

    /**
     * Converts [frame], the frame that is shown next, ahead of time: the conversion runs on the GPU while the
     * current frame is shown, instead of being waited for when [frame] is shown
     */
    fun prepare(frame: VideoFrame) {
        if (frame.hardwareFrame?.format() == AV_PIX_FMT_D3D11) {
            slot(frame)
        }
    }

    /**
     * Returns the slot that [frame] was converted to, converting it to a slot that is not shown when it was not
     */
    private fun slot(frame: VideoFrame): Processor.Slot? {
        val hardwareFrame = frame.hardwareFrame
        if (failed || hardwareFrame == null) {
            return null
        }
        return try {
            val processor = processor(hardwareFrame) ?: return null
            processor.slots.firstOrNull { it.frame === frame } ?: processor.slots.first { it !== shownSlot }.also {
                processor.convert(hardwareFrame, it)
                it.frame = frame
            }
        } catch (e: IllegalStateException) {
            logger.error(e) { "failed to convert D3D11 frame" }
            failed = true
            null
        }
    }

    private fun processor(frame: AVFrame): Processor? {
        val framesContext = AVHWFramesContext(frame.hw_frames_ctx().data())
        val deviceRef = framesContext.device_ref()
        processor?.let {
            if (it.width == frame.width() && it.height == frame.height() && it.swFormat == framesContext.sw_format() &&
                it.deviceRef.data().address() == deviceRef.data().address()
            ) {
                return it
            }
            it.close()
            processor = null
        }
        // keeps the device alive as long as the processor uses it
        val processor = Processor(av_buffer_ref(deviceRef), frame.width(), frame.height(), framesContext.sw_format())
        if (!processor.create()) {
            processor.close()
            failed = true
            return null
        }
        logger.debug { "converting D3D11 frames on the GPU" }
        this.processor = processor
        return processor
    }

    override fun close() {
        processor?.close()
        processor = null
    }
}

/**
 * Returns the DXGI color spaces to convert [frame] from and to, keeping its transfer function and primaries
 */
private fun colorSpaces(frame: AVFrame): Pair<Int, Int> {
    val fullRange = frame.color_range() == AVCOL_RANGE_JPEG
    if (frame.color_trc() == AVCOL_TRC_SMPTE2084) {
        return YCBCR_STUDIO_G2084_LEFT_P2020 to RGB_FULL_G2084_NONE_P2020
    }
    return when (yCbCrMatrix(frame)) {
        YCbCrMatrix.BT601 ->
            (if (fullRange) YCBCR_FULL_G22_LEFT_P601 else YCBCR_STUDIO_G22_LEFT_P601) to RGB_FULL_G22_NONE_P709

        YCbCrMatrix.BT709 ->
            (if (fullRange) YCBCR_FULL_G22_LEFT_P709 else YCBCR_STUDIO_G22_LEFT_P709) to RGB_FULL_G22_NONE_P709

        YCbCrMatrix.BT2020 ->
            (if (fullRange) YCBCR_FULL_G22_LEFT_P2020 else YCBCR_STUDIO_G22_LEFT_P2020) to RGB_FULL_G22_NONE_P2020
    }
}
