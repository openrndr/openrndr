package org.openrndr.color

import org.openrndr.math.Matrix33
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.abs
import kotlin.math.pow

/** the profile connection space illuminant of ICC profiles */
private val D50 = Vector3(0.9642029, 1.0, 0.8249054)

/** the Bradford cone response matrix */
private val BRADFORD = Matrix33(
    0.8951, 0.2664, -0.1614,
    -0.7502, 1.7135, 0.0367,
    0.0389, -0.0685, 1.0296
)

/**
 * Reads the big-endian values of an ICC profile, reads outside of the data throw [IndexOutOfBoundsException]
 */
private class IccReader(val bytes: ByteArray) {
    fun u8(offset: Int): Int = bytes[offset].toInt() and 0xff
    fun u16(offset: Int): Int = (u8(offset) shl 8) or u8(offset + 1)
    fun u32(offset: Int): Long = (u16(offset).toLong() shl 16) or u16(offset + 2).toLong()
    fun s32(offset: Int): Int = u32(offset).toInt()
    fun s15Fixed16(offset: Int): Double = s32(offset) / 65536.0
    fun signature(offset: Int): String = CharArray(4) { u8(offset + it).toChar() }.concatToString()
    fun xyz(offset: Int) = Vector3(s15Fixed16(offset), s15Fixed16(offset + 4), s15Fixed16(offset + 8))
}

private class IccTag(val offset: Int, val size: Int)

/**
 * Reads an RGB matrix/TRC ICC profile, returns null when the profile is not one or is malformed
 */
internal fun readIccProfile(bytes: ByteArray): ColorProfile? {
    return try {
        readIccProfileOrThrow(bytes)
    } catch (e: IndexOutOfBoundsException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}

private fun readIccProfileOrThrow(bytes: ByteArray): ColorProfile? {
    val reader = IccReader(bytes)
    if (bytes.size < 132 || reader.signature(36) != "acsp" || reader.signature(16) != "RGB ") {
        return null
    }
    // the header holds the size of the profile, a profile with less data is truncated
    require(reader.u32(0) <= bytes.size)
    val tagCount = reader.u32(128).toInt()
    require(tagCount in 0..1024)
    val tags = (0 until tagCount).associate {
        val entry = 132 + it * 12
        reader.signature(entry) to IccTag(reader.u32(entry + 4).toInt(), reader.u32(entry + 8).toInt())
    }
    fun tag(signature: String) = tags[signature]?.also { require(it.offset >= 0 && it.offset + it.size <= bytes.size) }

    val red = tag("rXYZ") ?: return null
    val green = tag("gXYZ") ?: return null
    val blue = tag("bXYZ") ?: return null
    val trc = tag("rTRC") ?: return null

    // the colorants are adapted to the D50 illuminant of the profile connection space
    val colorants = Matrix33.fromColumnVectors(readXYZ(reader, red), readXYZ(reader, green), readXYZ(reader, blue))
    val whitePoint = tag("wtpt")?.let { readXYZ(reader, it) } ?: D50
    val chad = tag("chad")?.let { readChad(reader, it) }

    // the matrix that undoes the adaptation, and the native white point
    val (toNative, nativeWhite) = when {
        chad != null -> chad.inversed.let { it to it * D50 }
        // without chad (version 2 profiles) the white point is native and the colorants are Bradford adapted
        else -> bradford(D50, whitePoint) to whitePoint
    }
    val native = toNative * colorants

    return ColorProfile(
        name = tag("desc")?.let { readDescription(reader, it) },
        red = chromaticity(native[0]),
        green = chromaticity(native[1]),
        blue = chromaticity(native[2]),
        white = chromaticity(nativeWhite),
        transfer = readTransfer(reader, trc),
        icc = bytes.copyOf()
    )
}

private fun chromaticity(xyz: Vector3): Vector2 {
    val sum = xyz.x + xyz.y + xyz.z
    require(sum != 0.0)
    return Vector2(xyz.x / sum, xyz.y / sum)
}

/**
 * The Bradford chromatic adaptation from white point [from] to white point [to]
 */
private fun bradford(from: Vector3, to: Vector3): Matrix33 {
    val source = BRADFORD * from
    val target = BRADFORD * to
    val scale = Matrix33(c0r0 = target.x / source.x, c1r1 = target.y / source.y, c2r2 = target.z / source.z)
    return BRADFORD.inversed * scale * BRADFORD
}

private fun readXYZ(reader: IccReader, tag: IccTag): Vector3 {
    require(reader.signature(tag.offset) == "XYZ ")
    return reader.xyz(tag.offset + 8)
}

private fun readChad(reader: IccReader, tag: IccTag): Matrix33 {
    require(reader.signature(tag.offset) == "sf32")
    val v = DoubleArray(9) { reader.s15Fixed16(tag.offset + 8 + it * 4) }
    return Matrix33(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8])
}

/**
 * Reads a version 2 textDescriptionType or a version 4 multiLocalizedUnicodeType, preferring English
 */
private fun readDescription(reader: IccReader, tag: IccTag): String? {
    return when (reader.signature(tag.offset)) {
        "desc" -> {
            val count = reader.u32(tag.offset + 8).toInt()
            require(count in 0..tag.size)
            CharArray(count) { reader.u8(tag.offset + 12 + it).toChar() }.concatToString().trimEnd('\u0000')
        }

        "mluc" -> {
            val records = reader.u32(tag.offset + 8).toInt()
            val recordSize = reader.u32(tag.offset + 12).toInt()
            require(records in 1..1024 && recordSize >= 12)
            val record = (0 until records).map { tag.offset + 16 + it * recordSize }
                .firstOrNull { reader.signature(it).startsWith("en") } ?: (tag.offset + 16)
            val length = reader.u32(record + 4).toInt()
            val offset = reader.u32(record + 8).toInt()
            require(length in 0..tag.size)
            CharArray(length / 2) { reader.u16(tag.offset + offset + it * 2).toChar() }.concatToString()
                .trimEnd('\u0000')
        }

        else -> null
    }
}

private fun readTransfer(reader: IccReader, tag: IccTag): ColorTransfer {
    return when (reader.signature(tag.offset)) {
        "curv" -> {
            val count = reader.u32(tag.offset + 8).toInt()
            require(count in 0..65536)
            when (count) {
                0 -> ColorTransfer.Linear
                // a u8Fixed8Number gamma
                1 -> gamma(reader.u16(tag.offset + 12) / 256.0)
                else -> {
                    val values = DoubleArray(count) { reader.u16(tag.offset + 12 + it * 2) / 65535.0 }
                    if (isSrgbTable(values)) ColorTransfer.SRGB else ColorTransfer.Table(values)
                }
            }
        }

        "para" -> {
            val type = reader.u16(tag.offset + 8)
            fun p(index: Int) = reader.s15Fixed16(tag.offset + 12 + index * 4)
            when (type) {
                0 -> gamma(p(0))
                // y = (ax + b)^g for x >= -b/a, 0 otherwise
                1 -> parametric(p(0), p(1), p(2), 0.0, -p(2) / p(1), 0.0, 0.0)
                // y = (ax + b)^g + c for x >= -b/a, c otherwise
                2 -> parametric(p(0), p(1), p(2), 0.0, -p(2) / p(1), p(3), p(3))
                // y = (ax + b)^g for x >= d, cx otherwise
                3 -> parametric(p(0), p(1), p(2), p(3), p(4), 0.0, 0.0)
                4 -> parametric(p(0), p(1), p(2), p(3), p(4), p(5), p(6))
                else -> throw IllegalArgumentException("unknown parametric curve type $type")
            }
        }

        else -> throw IllegalArgumentException("unknown curve type")
    }
}

private fun gamma(g: Double): ColorTransfer = if (abs(g - 1.0) < 1E-3) ColorTransfer.Linear else ColorTransfer.Gamma(g)

private val SRGB_PARAMETERS = doubleArrayOf(2.4, 1.0 / 1.055, 0.055 / 1.055, 1.0 / 12.92, 0.04045, 0.0, 0.0)

private fun parametric(g: Double, a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): ColorTransfer {
    val parameters = doubleArrayOf(g, a, b, c, d, e, f)
    return if (parameters.indices.all { abs(parameters[it] - SRGB_PARAMETERS[it]) < 1E-3 }) {
        ColorTransfer.SRGB
    } else {
        ColorTransfer.Parametric(g, a, b, c, d, e, f)
    }
}

private fun decodeSrgb(x: Double) = if (x <= 0.04045) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)

private fun isSrgbTable(values: DoubleArray): Boolean =
    values.indices.all { abs(values[it] - decodeSrgb(it / (values.size - 1.0))) < 2E-3 }
