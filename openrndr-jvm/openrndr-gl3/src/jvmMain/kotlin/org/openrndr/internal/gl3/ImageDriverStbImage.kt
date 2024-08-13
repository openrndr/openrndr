package org.openrndr.internal.gl3

import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.stb.STBIWriteCallback
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImage.stbi_info_from_memory
import org.lwjgl.stb.STBImageWrite
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.tinyexr.*
import org.lwjgl.util.tinyexr.TinyEXR.*
import org.openrndr.dds.loadDDS
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ImageFileDetails
import org.openrndr.draw.ImageFileFormat
import org.openrndr.internal.CubemapImageData
import org.openrndr.internal.ImageData
import org.openrndr.internal.ImageDriver
import org.openrndr.utils.buffer.MPPBuffer
import org.openrndr.utils.url.resolveFileOrUrl
import java.net.MalformedURLException
import java.net.URL
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists

class ImageDataStb(
    width: Int,
    height: Int,
    format: ColorFormat,
    type: ColorType,
    flipV: Boolean,
    data: MPPBuffer?,
    mipmapData: List<MPPBuffer> = emptyList()
) :
    ImageData(
        width, height, format,
        type,
        flipV,
        data,
        mipmapData
    ) {
    override fun close() {
        MemoryUtil.memFree(data?.byteBuffer)
        for (data in mipmapData) {
            MemoryUtil.memFree(data.byteBuffer)
        }
    }
}

class ImageDataDds(
    width: Int,
    height: Int,
    format: ColorFormat,
    type: ColorType,
    flipV: Boolean,
    data: MPPBuffer?,
    mipmapData: List<MPPBuffer> = emptyList()
) :
    ImageData(
        width, height, format,
        type,
        flipV,
        data,
        mipmapData
    ) {
    override fun close() {

    }
}


class CubemapImageDataDds(
    width: Int, height: Int, format: ColorFormat, type: ColorType, mipmaps: Int,
    bdata: List<MPPBuffer>, bdata2: List<MPPBuffer>
) : CubemapImageData(
    width, height, format,
    type,
    mipmaps, bdata, bdata2
) {
    override fun close() {

    }
}


/**
 * Image driver based on stbi and tinyexr
 */
class ImageDriverStbImage : ImageDriver {
    override fun probeImage(fileOrUrl: String): ImageFileDetails? {
        val (file, url) = resolveFileOrUrl(fileOrUrl)

        val header = ByteArray(1024)
        url?.let { it.openStream().use { stream -> stream.read(header) } }
        file?.let { it.inputStream().use { stream -> stream.read(header) } }

        stackPush().use { stack ->
            val bb = stack.malloc(header.size)
            bb.put(header)
            bb.flip()
            return probeImage(MPPBuffer(bb), null)
        }
    }

    override fun probeImage(buffer: MPPBuffer, formatHint: ImageFileFormat?): ImageFileDetails? {
        stackPush().use { stack ->
            val x = stack.mallocInt(1)
            val y = stack.mallocInt(1)
            val channels = stack.mallocInt(1)
            val result = stbi_info_from_memory(buffer.byteBuffer, x, y, channels)

            if (result) {
                return ImageFileDetails(x.get(), y.get(), channels.get())
            }
        }
        return null
    }

    override fun loadImage(fileOrUrl: String, formatHint: ImageFileFormat?, allowSRGB: Boolean): ImageData {
        if (fileOrUrl.startsWith("data:")) {
            val decoder = Base64.getDecoder()
            val commaIndex = fileOrUrl.indexOf(",")
            val base64Data = fileOrUrl.drop(commaIndex + 1).replace("\n", "")
            val decoded = decoder.decode(base64Data)
            val buffer = ByteBuffer.allocateDirect(decoded.size)
            buffer.put(decoded)
            (buffer as Buffer).rewind()
            return loadImage(MPPBuffer(buffer), "data-url", formatHint, allowSRGB)
        } else {
            val url = try {
                URL(fileOrUrl)
            } catch (e: MalformedURLException) {
                null
            }

            if (url != null) {
                return url.openStream().use {
                    val byteArray = url.readBytes()
                    if (byteArray.isEmpty()) {
                        error("read 0 bytes from stream $fileOrUrl")
                    }
                    val buffer = MemoryUtil.memAlloc(byteArray.size)
                    buffer.put(byteArray)
                    buffer.flip()
                    try {
                        loadImage(MPPBuffer(buffer), fileOrUrl, formatHint, allowSRGB)
                    } finally {
                        MemoryUtil.memFree(buffer)
                    }
                }
            } else {
                require(Path.of(fileOrUrl).exists()) { "$fileOrUrl not found" }
                return FileChannel.open(Path.of(fileOrUrl)).use { channel ->
                    val buffer = MemoryUtil.memAlloc(channel.size().toInt())
                    channel.read(buffer)
                    buffer.flip()
                    try {
                        loadImage(MPPBuffer(buffer), fileOrUrl, formatHint, allowSRGB)
                    } finally {
                        MemoryUtil.memFree(buffer)
                    }
                }
            }
        }
    }

    override fun loadImage(buffer: MPPBuffer, name: String?, formatHint: ImageFileFormat?, allowSRGB: Boolean): ImageData {
        var assumedFormat = ImageFileFormat.PNG

        val inputIsDirect = buffer.byteBuffer.isDirect

        val buffer = if (inputIsDirect) buffer.byteBuffer else {
            MemoryUtil.memAlloc(buffer.byteBuffer.remaining()).also {
                it.put(buffer.byteBuffer)
                it.rewind()
            }
        }

        try {
            if (formatHint != null) {
                assumedFormat = formatHint
            }
            buffer.mark()

            when (assumedFormat) {
                ImageFileFormat.PNG, ImageFileFormat.JPG -> {
                    val wa = IntArray(1)
                    val ha = IntArray(1)
                    val ca = IntArray(1)

                    STBImage.stbi_set_flip_vertically_on_load(true)
                    STBImage.stbi_set_unpremultiply_on_load(false)

                    val bitsPerChannel = if (assumedFormat == ImageFileFormat.PNG) {
                        if (STBImage.stbi_is_16_bit_from_memory(buffer)) {
                            16
                        } else {
                            8
                        }
                    } else {
                        8
                    }
                    buffer.reset()

                    val targetType: ColorType
                    val mask: Int
                    val alphaOffset = 3
                    val redOffset = 0
                    val greenOffset = 1
                    val blueOffset = 2
                    when (bitsPerChannel) {
                        8 -> {
                            targetType = if (allowSRGB) ColorType.UINT8_SRGB else ColorType.UINT8
                            mask = 0xff
                        }

                        16 -> {
                            targetType = ColorType.UINT16
                            mask = 0xffff
                        }

                        else -> error("unsupported bits per channel: $bitsPerChannel")
                    }

                    val (tdata8, tdata16) = when (bitsPerChannel) {
                        8 -> Pair(
                            STBImage.stbi_load_from_memory(buffer, wa, ha, ca, 0)
                                ?: error("stbi_load returned null"), null as ShortBuffer?
                        )

                        16 -> Pair(
                            null as ByteBuffer?, STBImage.stbi_load_16_from_memory(buffer, wa, ha, ca, 0)
                                ?: error("stdi_load returned null")
                        )

                        else -> error("unsupported bits per channel: $bitsPerChannel")
                    }

                    if (tdata8 != null) {
                        var offset = 0
                        if (ca[0] == 4) {
                            for (y in 0 until ha[0]) {
                                for (x in 0 until wa[0]) {
                                    val a =
                                        (tdata8.get(offset + alphaOffset).toInt() and mask).toDouble() / mask.toDouble()
                                    val r = ((tdata8.get(offset + redOffset).toInt() and mask) * a)
                                    val g = ((tdata8.get(offset + greenOffset).toInt() and mask) * a)
                                    val b = ((tdata8.get(offset + blueOffset).toInt() and mask) * a)
                                    tdata8.put(offset + redOffset, r.toInt().toByte())
                                    tdata8.put(offset + greenOffset, g.toInt().toByte())
                                    tdata8.put(offset + blueOffset, b.toInt().toByte())
                                    offset += 4
                                }
                            }
                        }
                    }
                    val data8 = tdata8?.let {
                        // Convert grayscale images to RGB to avoid having them rendered in red.
                        if (ca[0] == 1) {
                            var roffset = 0
                            var woffset = 0
                            val data8 = ByteBuffer.allocateDirect(tdata8.capacity() * 3)
                            for (y in 0 until ha[0]) {
                                for (x in 0 until wa[0]) {
                                    val r = tdata8.get(roffset)
                                    data8.put(woffset + 0, r)
                                    data8.put(woffset + 1, r)
                                    data8.put(woffset + 2, r)
                                    roffset++
                                    woffset += 3
                                }
                            }
                            data8
                        } else {
                            tdata8
                        }
                    }

                    if (tdata16 != null) {
                        var offset = 0
                        if (ca[0] == 4) {
                            for (y in 0 until ha[0]) {
                                for (x in 0 until wa[0]) {
                                    val a =
                                        (tdata16.get(offset + alphaOffset)
                                            .toInt() and mask).toDouble() / mask.toDouble()
                                    val r = ((tdata16.get(offset + redOffset).toInt() and mask) * a)
                                    val g = ((tdata16.get(offset + greenOffset).toInt() and mask) * a)
                                    val b = ((tdata16.get(offset + blueOffset).toInt() and mask) * a)
                                    tdata16.put(offset + redOffset, r.toInt().toShort())
                                    tdata16.put(offset + greenOffset, g.toInt().toShort())
                                    tdata16.put(offset + blueOffset, b.toInt().toShort())
                                    offset += 4
                                }
                            }
                        }
                    }

                    val data16 = tdata16

                    val copyData = (data8?.let { MemoryUtil.memAlloc(it.capacity()) } ?: data16?.let {
                        MemoryUtil.memAlloc(
                            it.capacity() * 2
                        )
                    })
                        ?: error("alloc failed, data8: ${data8}, data16: ${data16}, $assumedFormat, $bitsPerChannel")

                    val source = data8?.let { MemoryUtil.memAddress(it) } ?: data16?.let { MemoryUtil.memAddress(it) }
                    ?: error("get address failed")
                    val dest = MemoryUtil.memAddress(copyData)
                    MemoryUtil.memCopy(source, dest, copyData.capacity().toLong())

                    tdata8?.let { STBImage.stbi_image_free(it) }
                    tdata16?.let { STBImage.stbi_image_free(it) }

                    return ImageDataStb(
                        wa[0], ha[0],
                        when (ca[0]) {
                            1 -> if (tdata16 != null) ColorFormat.R else ColorFormat.RGB
                            2 -> ColorFormat.RG
                            3 -> ColorFormat.RGB
                            4 -> ColorFormat.RGBa
                            else -> error("invalid component count ${ca[0]}")
                        },
                        targetType, false,
                        MPPBuffer(copyData)
                    )
                }

                ImageFileFormat.HDR -> {
                    STBImage.stbi_set_flip_vertically_on_load(true)
                    return stackPush().use { stack ->
                        require(STBImage.stbi_is_hdr_from_memory(buffer)) { "$name does not contain HDR content" }

                        val sx = stack.mallocInt(1)
                        val sy = stack.mallocInt(1)
                        val sc = stack.mallocInt(1)
                        val floats = STBImage.stbi_loadf_from_memory(buffer, sx, sy, sc, 3)
                        require(floats != null) {
                            "load from .hdr failed ($name)"
                        }
                        val copy = MemoryUtil.memAllocFloat(sx[0] * sy[0] * sc[0])
                        MemoryUtil.memCopy(floats, copy)
                        STBImage.stbi_image_free(floats)
                        ImageDataStb(
                            sx[0],
                            sy[0],
                            ColorFormat.RGB,
                            ColorType.FLOAT32,
                            false,
                            MPPBuffer(MemoryUtil.memByteBuffer(copy))
                        )
                    }
                }

                ImageFileFormat.DDS -> {
                    val data = loadDDS(buffer)
                    val readBuffer = data.image(0)
                    require(readBuffer.remaining() > 0) {
                        "image buffer $readBuffer has no remaining bytes"
                    }
                    return ImageDataDds(
                        data.width,
                        data.height,
                        data.format,
                        data.type,
                        data.flipV,
                        data.image(0)
                    )
                }

                ImageFileFormat.EXR -> {
                    val exrHeader = EXRHeader.create()
                    val exrVersion = EXRVersion.create()
                    val versionResult = ParseEXRVersionFromMemory(exrVersion, buffer)
                    buffer.rewind()

                    if (versionResult != TINYEXR_SUCCESS) {
                        error("failed to get version")
                    }

                    val errors = PointerBuffer.allocateDirect(1)

                    val parseResult = ParseEXRHeaderFromMemory(exrHeader, exrVersion, buffer, errors)
                    if (parseResult != TINYEXR_SUCCESS) {
                        error("failed to parse file")
                    }

                    for (i in 0 until exrHeader.num_channels()) {
                        exrHeader.requested_pixel_types().put(i, exrHeader.pixel_types().get(i))
                    }

                    val exrImage = EXRImage.create()
                    InitEXRImage(exrImage)
                    LoadEXRImageFromMemory(exrImage, exrHeader, buffer, errors)

                    val format =
                        when (val c = exrImage.num_channels()) {
                            1 -> ColorFormat.R
                            3 -> ColorFormat.RGB
                            4 -> ColorFormat.RGBa
                            else -> error("unsupported number of channels $c")
                        }

                    val type = when (val t = exrHeader.requested_pixel_types().get(0)) {
                        TINYEXR_PIXELTYPE_HALF -> ColorType.FLOAT16
                        TINYEXR_PIXELTYPE_FLOAT -> ColorType.FLOAT32
                        else -> error("unsupported pixel type [type=$t]")
                    }

                    val height = exrImage.height()
                    val width = exrImage.width()
                    val channels = exrImage.num_channels()

                    val data =
                        MemoryUtil.memAlloc(format.componentCount * type.componentSize * exrImage.width() * exrImage.height())

                    val channelNames =
                        (0 until exrHeader.num_channels()).map { exrHeader.channels().get(it).nameString() }
                    val images = exrImage.images()!!
                    val channelImages = (0 until exrHeader.num_channels()).map {
                        images.getByteBuffer(
                            it,
                            width * height * type.componentSize
                        )
                    }

                    val order = when (format) {
                        ColorFormat.R -> listOf("R").map { channelNames.indexOf(it) }
                        ColorFormat.RGB -> listOf("B", "G", "R").map { channelNames.indexOf(it) }
                        ColorFormat.RGBa -> listOf("B", "G", "R", "A").map { channelNames.indexOf(it) }
                        else -> error("unsupported channel layout")
                    }
                    require(order.none { it == -1 }) { "some channels are not found" }

                    val orderedImages = order.map { channelImages[it] }
                    orderedImages.forEach { (it as Buffer).rewind() }

                    for (y in 0 until exrImage.height()) {
                        val offset = (height - 1 - y) * format.componentCount * type.componentSize * width
                        (data as Buffer).position(offset)
                        for (x in 0 until exrImage.width()) {
                            for (c in 0 until channels) {
                                for (i in 0 until type.componentSize) {
                                    data.put(orderedImages[c].get())
                                }
                            }
                        }
                    }
                    (data as Buffer).rewind()

                    FreeEXRHeader(exrHeader)
                    FreeEXRImage(exrImage)
                    return ImageDataStb(exrImage.width(), exrImage.height(), format, type, false, MPPBuffer(data))
                }

                else -> {
                    error("format not supported")
                }
            }
        } finally {
            if (!inputIsDirect) {
                MemoryUtil.memFree(buffer)
            }
        }
    }

    override fun saveImage(imageData: ImageData, filename: String, formatHint: ImageFileFormat?) {

        fun flipImage(inputBuffer: ByteBuffer) : ByteBuffer {
            return if (imageData.flipV) inputBuffer else {
                val flippedPixels =
                    MemoryUtil.memAlloc(imageData.width * imageData.height * imageData.format.componentCount * imageData.type.componentSize)
                val stride = imageData.width * imageData.format.componentCount
                val row = ByteArray(stride)
                flippedPixels.rewind()

                for (y in 0 until imageData.height) {
                    inputBuffer.position((imageData.height - y - 1) * stride)
                    inputBuffer.get(row)
                    flippedPixels.put(row)
                }

                inputBuffer.rewind()
                inputBuffer.put(flippedPixels)
                inputBuffer.rewind()
                flippedPixels.rewind()
                flippedPixels
            }
        }

        val format = formatHint ?: ImageFileFormat.PNG
        when (format) {
            ImageFileFormat.PNG -> {
                val buffer = flipImage(imageData.data?.byteBuffer ?: error("no data"))
                when (Pair(imageData.format, imageData.type)) {
                    Pair(ColorFormat.R, ColorType.UINT8),
                    Pair(ColorFormat.RGB, ColorType.UINT8),
                    Pair(ColorFormat.RGBa, ColorType.UINT8),
                    Pair(ColorFormat.RGB, ColorType.UINT8_SRGB),
                    Pair(ColorFormat.RGBa, ColorType.UINT8_SRGB) -> {
                        require(
                            STBImageWrite.stbi_write_png(
                                filename,
                                imageData.width,
                                imageData.height,
                                imageData.format.componentCount, buffer,
                                imageData.width * imageData.format.componentCount
                            )
                        ) {
                            "write to png failed"
                        }
                    }
                    else -> error("unsupported input for PNG (${imageData.type}/${imageData.type}")
                }
                if (!imageData.flipV) {
                    MemoryUtil.memFree(buffer)
                }
            }

            ImageFileFormat.JPG -> {
                val buffer = flipImage(imageData.data?.byteBuffer ?: error("no data"))
                when (Pair(imageData.format, imageData.type)) {
                    Pair(ColorFormat.R, ColorType.UINT8),
                    Pair(ColorFormat.RGB, ColorType.UINT8),
                    Pair(ColorFormat.RGBa, ColorType.UINT8),
                    Pair(ColorFormat.RGB, ColorType.UINT8),
                    Pair(ColorFormat.RGBa, ColorType.UINT8_SRGB) -> {
                        require(
                            STBImageWrite.stbi_write_jpg(
                                filename,
                                imageData.width,
                                imageData.height,
                                imageData.format.componentCount, buffer,
                                imageData.width * imageData.format.componentCount
                            )
                        ) {
                            "write to jpg failed"
                        }
                    }

                    else -> error("unsupported input for JPG (${imageData.type}/${imageData.type}")
                }
                if (!imageData.flipV) {
                    MemoryUtil.memFree(buffer)
                }
            }

            ImageFileFormat.HDR -> {
                when (Pair(imageData.format, imageData.type)) {
                    Pair(ColorFormat.RGB, ColorType.FLOAT32) -> {
                        require(
                            STBImageWrite.stbi_write_hdr(
                                filename,
                                imageData.width,
                                imageData.height,
                                imageData.format.componentCount,
                                imageData.data?.byteBuffer?.asFloatBuffer() ?: error("no data")
                            )
                        ) {
                            "write to hdr failed"
                        }
                    }

                    else -> error("unsupported input for JPG (${imageData.type}/${imageData.type}")
                }
            }

            ImageFileFormat.EXR -> {
                require(imageData.format == ColorFormat.RGB || imageData.format == ColorFormat.RGBa) { "can only save RGB and RGBa formats in EXR format" }

                val exrType =
                    if (imageData.type == ColorType.FLOAT16) TINYEXR_PIXELTYPE_HALF else TINYEXR_PIXELTYPE_FLOAT

                val exrImage = EXRImage.create()
                InitEXRImage(exrImage)

                val exrHeader = EXRHeader.create()
                InitEXRHeader(exrHeader)

                exrHeader.num_channels(3)

                val exrChannels = EXRChannelInfo.calloc(3)
                exrChannels[0].name(
                    ByteBuffer.allocateDirect(2)
                        .apply { put('B'.code.toByte()); put(0.toByte()); (this as Buffer).rewind() })
                exrChannels[1].name(
                    ByteBuffer.allocateDirect(2)
                        .apply { put('G'.code.toByte()); put(0.toByte()); (this as Buffer).rewind() })
                exrChannels[2].name(
                    ByteBuffer.allocateDirect(2)
                        .apply { put('R'.code.toByte()); put(0.toByte()); (this as Buffer).rewind() })

                exrHeader.channels(exrChannels)


                val bBuffer =
                    ByteBuffer.allocateDirect(imageData.width * imageData.height * 4).order(ByteOrder.nativeOrder())
                val gBuffer =
                    ByteBuffer.allocateDirect(imageData.width * imageData.height * 4).order(ByteOrder.nativeOrder())
                val rBuffer =
                    ByteBuffer.allocateDirect(imageData.width * imageData.height * 4).order(ByteOrder.nativeOrder())

                val data = imageData.data?.byteBuffer ?: error("no buffer")

                // -- de-interleave and flip data
                for (y in 0 until imageData.height) {
                    val row = if (!imageData.flipV) imageData.height - 1 - y else y
                    val offset = row * imageData.width * imageData.type.componentSize * 3

                    (data as Buffer).position(offset)

                    for (x in 0 until imageData.width) {
                        for (i in 0 until imageData.type.componentSize) {
                            val b = data.get()
                            bBuffer.put(b)
                        }
                        for (i in 0 until imageData.type.componentSize) {
                            val g = data.get()
                            gBuffer.put(g)
                        }
                        for (i in 0 until imageData.type.componentSize) {
                            val r = data.get()
                            rBuffer.put(r)
                        }
                    }
                }

                (bBuffer as Buffer).rewind()
                (gBuffer as Buffer).rewind()
                (rBuffer as Buffer).rewind()


                val pixelTypes = BufferUtils.createIntBuffer(4 * 3).apply {
                    put(exrType); put(exrType); put(exrType); (this as Buffer).rewind()
                }
                exrHeader.pixel_types(pixelTypes)
                (pixelTypes as Buffer).rewind()
                exrHeader.requested_pixel_types(pixelTypes)

                exrImage.width(imageData.width)
                exrImage.height(imageData.height)
                exrImage.num_channels(3)

                val images = PointerBuffer.allocateDirect(3)
                images.put(0, bBuffer)
                images.put(1, gBuffer)
                images.put(2, rBuffer)
                images.rewind()
                exrImage.images(images)

                val errors = PointerBuffer.allocateDirect(1)
                val result = SaveEXRImageToFile(exrImage, exrHeader, filename, errors)

                require(result == 0) {
                    "failed to save EXR to ${filename}, [result=$result]"
                }
                exrImage.images(null as PointerBuffer?)
                FreeEXRImage(exrImage)
            }

            else -> error("unsupported file format $format")
        }
    }

    override fun imageToDataUrl(imageData: ImageData, formatHint: ImageFileFormat?): String {
        val imageFileFormat = formatHint ?: ImageFileFormat.JPG
        val saveBuffer = ByteBuffer.allocate(1_024 * 1_024 * 2)
        val writeFunc = object : STBIWriteCallback() {
            override fun invoke(context: Long, data: Long, size: Int) {
                val sourceBuffer = MemoryUtil.memByteBuffer(data, size)
                saveBuffer?.put(sourceBuffer)
            }
        }

        var pixels = imageData.data!!.byteBuffer
        if (!imageData.flipV) {
            val flippedPixels =
                BufferUtils.createByteBuffer(imageData.width * imageData.height * imageData.format.componentCount)
            (flippedPixels as Buffer).rewind()
            val stride = imageData.width * imageData.format.componentCount
            val row = ByteArray(stride)

            for (y in 0 until imageData.height) {
                imageData.data!!.byteBuffer.position((imageData.height - y - 1) * stride)
                imageData.data!!.byteBuffer.get(row)
                flippedPixels.put(row)
            }

            flippedPixels.rewind()
            pixels = flippedPixels
        }

        when (imageFileFormat) {
            ImageFileFormat.JPG -> STBImageWrite.stbi_write_jpg_to_func(
                writeFunc, 0L,
                imageData.width, imageData.height,
                imageData.format.componentCount, pixels, 90
            )

            ImageFileFormat.PNG -> STBImageWrite.stbi_write_png_to_func(
                writeFunc, 0L,
                imageData.width, imageData.height,
                imageData.format.componentCount, pixels, imageData.width * imageData.format.componentCount
            )

            else -> {
                error("format not supported $imageFileFormat")
            }
        }

        val byteArray = ByteArray((saveBuffer as Buffer).position())
        (saveBuffer as Buffer).rewind()
        saveBuffer.get(byteArray)
        val base64Data = Base64.getEncoder().encodeToString(byteArray)

        return "data:${imageFileFormat.mimeType};base64,$base64Data"
    }

    override fun loadCubemapImage(fileOrUrl: String, formatHint: ImageFileFormat?): CubemapImageData {
        val (file, url) = resolveFileOrUrl(fileOrUrl)
        require(Path.of(fileOrUrl).exists()) { "$fileOrUrl not found" }

        return if (file != null) {
            FileChannel.open(Path.of(fileOrUrl)).use { channel ->
                val buffer = MemoryUtil.memAlloc(channel.size().toInt())
                channel.read(buffer)
                buffer.flip()
                try {
                    loadCubemapImage(MPPBuffer(buffer), fileOrUrl, formatHint)
                } finally {
                    MemoryUtil.memFree(buffer)
                }
            }
        } else if (url != null) {
            val byteArray = url.readBytes()
            if (byteArray.isEmpty()) {
                error("read 0 bytes from stream $fileOrUrl")
            }
            val buffer = MemoryUtil.memAlloc(byteArray.size)
            buffer.put(byteArray)
            buffer.flip()
            try {
                loadCubemapImage(MPPBuffer(buffer), fileOrUrl, formatHint)
            } finally {
                MemoryUtil.memFree(buffer)
            }
        } else {
            error("can't resolve $fileOrUrl")
        }
    }

    override fun loadCubemapImage(buffer: MPPBuffer, name: String?, formatHint: ImageFileFormat?): CubemapImageData {
        val ddsData = loadDDS(buffer)
        require(ddsData.cubeMap)
        require(ddsData.width == ddsData.height)
        return CubemapImageDataDds(
            ddsData.width,
            ddsData.height,
            ddsData.format,
            ddsData.type,
            ddsData.mipmaps,
            ddsData.bdata,
            ddsData.bdata2
        )
    }
}