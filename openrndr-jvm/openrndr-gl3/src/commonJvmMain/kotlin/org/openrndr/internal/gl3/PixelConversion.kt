package org.openrndr.internal.gl3

import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import java.nio.ByteBuffer

/**
 * Converts [value] to a half precision float, rounding to the nearest even value
 */
internal fun floatToHalf(value: Float): Short {
    val bits = value.toRawBits()
    val sign = (bits ushr 16) and 0x8000
    val floatExponent = (bits ushr 23) and 0xff
    val mantissa = bits and 0x7fffff

    if (floatExponent == 0xff) {
        // infinity or NaN
        return (sign or 0x7c00 or (if (mantissa != 0) 0x200 else 0)).toShort()
    }
    val exponent = floatExponent - 127 + 15
    if (exponent >= 0x1f) {
        // too large, infinity
        return (sign or 0x7c00).toShort()
    }
    if (exponent <= 0) {
        // subnormal, or too small and zero
        if (exponent < -10) {
            return sign.toShort()
        }
        val fullMantissa = mantissa or 0x800000
        val shift = 14 - exponent
        var half = fullMantissa ushr shift
        val remainder = fullMantissa and ((1 shl shift) - 1)
        val halfway = 1 shl (shift - 1)
        if (remainder > halfway || (remainder == halfway && (half and 1) != 0)) {
            half++
        }
        return (sign or half).toShort()
    }
    var half = (exponent shl 10) or (mantissa ushr 13)
    val remainder = mantissa and 0x1fff
    // a carry into the exponent rounds up to the next exponent, or to infinity
    if (remainder > 0x1000 || (remainder == 0x1000 && (half and 1) != 0)) {
        half++
    }
    return (sign or half).toShort()
}

/**
 * The components of RGBA pixels that make up the pixels of [format]
 */
private fun rgbaComponents(format: ColorFormat): IntArray = when (format) {
    ColorFormat.R -> intArrayOf(0)
    ColorFormat.RG -> intArrayOf(0, 1)
    ColorFormat.RGB -> intArrayOf(0, 1, 2)
    ColorFormat.BGR -> intArrayOf(2, 1, 0)
    ColorFormat.RGBa -> intArrayOf(0, 1, 2, 3)
    ColorFormat.BGRa -> intArrayOf(2, 1, 0, 3)
}

/**
 * Converts [pixelCount] RGBA pixels in [source], with [ColorType.UINT8] or [ColorType.FLOAT32] components, to
 * [targetFormat] and [targetType] in [target]. Normalized values are not encoded or decoded for sRGB types.
 */
internal fun convertRGBAPixels(
    source: ByteBuffer,
    sourceType: ColorType,
    target: ByteBuffer,
    targetFormat: ColorFormat,
    targetType: ColorType,
    pixelCount: Int
) {
    require(sourceType == ColorType.UINT8 || sourceType == ColorType.FLOAT32) {
        "can only convert from UINT8 or FLOAT32 RGBA pixels, not $sourceType"
    }
    require(
        targetType == ColorType.UINT8 || targetType == ColorType.UINT8_SRGB || targetType == ColorType.UINT16 ||
                targetType.isFloat && !targetType.compressed
    ) {
        "can not convert pixels to $targetType"
    }
    val components = rgbaComponents(targetFormat)

    fun read(index: Int): Float = when (sourceType) {
        ColorType.UINT8 -> (source.get(index).toInt() and 0xff) / 255.0f
        else -> source.getFloat(index * 4)
    }

    var offset = 0
    for (pixel in 0 until pixelCount) {
        for (component in components) {
            val value = read(pixel * 4 + component)
            when (targetType) {
                ColorType.UINT8, ColorType.UINT8_SRGB -> {
                    target.put(offset, (value.coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt().toByte())
                }

                ColorType.UINT16 -> {
                    target.putShort(offset, (value.coerceIn(0.0f, 1.0f) * 65535.0f + 0.5f).toInt().toShort())
                }

                ColorType.FLOAT16 -> target.putShort(offset, floatToHalf(value))
                else -> target.putFloat(offset, value)
            }
            offset += targetType.componentSize
        }
    }
}
