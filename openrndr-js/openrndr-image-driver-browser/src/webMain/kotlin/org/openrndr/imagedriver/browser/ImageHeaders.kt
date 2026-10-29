package org.openrndr.imagedriver.browser

import org.openrndr.draw.ImageFileDetails
import org.openrndr.draw.ImageFileFormat
import org.openrndr.utils.buffer.MPPBuffer

/**
 * Reads the bytes of an [MPPBuffer] by absolute index, without changing its position
 */
private class Bytes(private val buffer: MPPBuffer) {
    private val origin = buffer.position()
    val size = buffer.limit() - origin

    operator fun get(index: Int): Int {
        buffer.position(origin + index)
        val value = buffer.get().toInt() and 0xff
        buffer.position(origin)
        return value
    }

    fun u16be(index: Int) = (this[index] shl 8) or this[index + 1]
    fun u16le(index: Int) = this[index] or (this[index + 1] shl 8)
    fun u24le(index: Int) = u16le(index) or (this[index + 2] shl 16)
    fun s32le(index: Int) = u24le(index) or (this[index + 3] shl 24)
    fun u32be(index: Int) = (u16be(index) shl 16) or u16be(index + 2)

    fun ascii(index: Int, length: Int) = (0 until length).map { this[index + it].toChar() }.joinToString("")
}

/**
 * Guesses the format of the image in [buffer] from its first bytes
 */
internal fun imageFileFormat(buffer: MPPBuffer): ImageFileFormat? {
    val bytes = Bytes(buffer)
    if (bytes.size < 4) return null
    return when {
        bytes.ascii(0, 4) == "DDS " -> ImageFileFormat.DDS
        bytes[0] == 0x89 && bytes.ascii(1, 3) == "PNG" -> ImageFileFormat.PNG
        bytes[0] == 0xff && bytes[1] == 0xd8 -> ImageFileFormat.JPG
        bytes.ascii(0, 4) == "v/1\u0001" -> ImageFileFormat.EXR
        bytes.ascii(0, 2) == "#?" -> ImageFileFormat.HDR
        else -> null
    }
}

/**
 * Reads the dimensions and number of channels of a PNG, JPEG, GIF, WebP, BMP or DDS image from its header
 */
internal fun probeImageHeader(buffer: MPPBuffer): ImageFileDetails? {
    val bytes = Bytes(buffer)
    if (bytes.size < 12) return null
    return when {
        // PNG: the IHDR chunk follows the signature, the color type gives the channels
        bytes[0] == 0x89 && bytes.ascii(1, 3) == "PNG" && bytes.size >= 26 -> {
            val channels = when (bytes[25]) {
                0 -> 1
                2 -> 3
                3 -> 3
                4 -> 2
                6 -> 4
                else -> return null
            }
            ImageFileDetails(bytes.u32be(16), bytes.u32be(20), channels)
        }

        // JPEG: the dimensions are in the first start of frame segment
        bytes[0] == 0xff && bytes[1] == 0xd8 -> {
            var offset = 2
            while (offset + 9 < bytes.size) {
                if (bytes[offset] != 0xff) return null
                val marker = bytes[offset + 1]
                if (marker == 0xff) {
                    offset++
                    continue
                }
                val length = bytes.u16be(offset + 2)
                val startOfFrame = marker in 0xc0..0xcf && marker != 0xc4 && marker != 0xc8 && marker != 0xcc
                if (startOfFrame) {
                    return ImageFileDetails(bytes.u16be(offset + 7), bytes.u16be(offset + 5), bytes[offset + 9])
                }
                offset += 2 + length
            }
            null
        }

        // GIF: the logical screen size, palette images decode to RGB
        bytes.ascii(0, 3) == "GIF" -> ImageFileDetails(bytes.u16le(6), bytes.u16le(8), 3)

        // WebP: lossy (VP8), lossless (VP8L) or extended (VP8X) bitstreams
        bytes.ascii(0, 4) == "RIFF" && bytes.ascii(8, 4) == "WEBP" && bytes.size >= 30 -> when (bytes.ascii(12, 4)) {
            "VP8 " -> ImageFileDetails(bytes.u16le(26) and 0x3fff, bytes.u16le(28) and 0x3fff, 3)
            "VP8L" -> {
                val bits = bytes.s32le(21)
                val alpha = (bits shr 28) and 1
                ImageFileDetails((bits and 0x3fff) + 1, ((bits shr 14) and 0x3fff) + 1, 3 + alpha)
            }

            "VP8X" -> {
                val alpha = (bytes[20] shr 4) and 1
                ImageFileDetails(bytes.u24le(24) + 1, bytes.u24le(27) + 1, 3 + alpha)
            }

            else -> null
        }

        // BMP: the info header holds the size and bits per pixel, the height is negative for top-down images
        bytes.ascii(0, 2) == "BM" && bytes.size >= 30 -> {
            val bitsPerPixel = bytes.u16le(28)
            ImageFileDetails(bytes.s32le(18), kotlin.math.abs(bytes.s32le(22)), if (bitsPerPixel == 32) 4 else 3)
        }

        // DDS: the height precedes the width in the header
        bytes.ascii(0, 4) == "DDS " -> ImageFileDetails(bytes.s32le(16), bytes.s32le(12), 4)

        else -> null
    }
}
