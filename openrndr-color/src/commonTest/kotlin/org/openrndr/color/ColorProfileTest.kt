package org.openrndr.color

import org.openrndr.math.Matrix33
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Writes RGB matrix/TRC ICC profiles for the tests
 */
private class IccWriter(val version: Int) {
    private val tags = mutableListOf<Pair<String, ByteArray>>()

    private class Bytes {
        val data = mutableListOf<Byte>()
        fun u8(v: Int) = apply { data.add(v.toByte()) }
        fun u16(v: Int) = apply { u8(v shr 8); u8(v) }
        fun u32(v: Long) = apply { u16((v shr 16).toInt()); u16(v.toInt()) }
        fun s15Fixed16(v: Double) = apply { u32((v * 65536.0).roundToLong() and 0xffffffffL) }
        fun signature(s: String) = apply { s.forEach { u8(it.code) } }
        fun pad() = apply { while (data.size % 4 != 0) u8(0) }
        fun bytes() = data.toByteArray()
    }

    fun xyz(signature: String, v: Vector3) = apply {
        tags += signature to Bytes().signature("XYZ ").u32(0).s15Fixed16(v.x).s15Fixed16(v.y).s15Fixed16(v.z).bytes()
    }

    fun chad(m: Matrix33) = apply {
        val values = listOf(m.c0r0, m.c1r0, m.c2r0, m.c0r1, m.c1r1, m.c2r1, m.c0r2, m.c1r2, m.c2r2)
        tags += "chad" to Bytes().signature("sf32").u32(0).apply { values.forEach { s15Fixed16(it) } }.bytes()
    }

    fun para(vararg parameters: Double, type: Int) = apply {
        val curve = Bytes().signature("para").u32(0).u16(type).u16(0).apply { parameters.forEach { s15Fixed16(it) } }
            .pad().bytes()
        listOf("rTRC", "gTRC", "bTRC").forEach { tags += it to curve }
    }

    fun curv(values: List<Int>) = apply {
        val curve = Bytes().signature("curv").u32(0).u32(values.size.toLong()).apply { values.forEach { u16(it) } }
            .pad().bytes()
        listOf("rTRC", "gTRC", "bTRC").forEach { tags += it to curve }
    }

    fun description(name: String) = apply {
        tags += "desc" to if (version >= 4) {
            Bytes().signature("mluc").u32(0).u32(1).u32(12).signature("enUS").u32(name.length * 2L).u32(28)
                .apply { name.forEach { u16(it.code) } }.pad().bytes()
        } else {
            Bytes().signature("desc").u32(0).u32(name.length + 1L).apply { name.forEach { u8(it.code) } }.u8(0)
                .u32(0).u32(0).u16(0).u8(0).apply { repeat(67) { u8(0) } }.pad().bytes()
        }
    }

    fun build(): ByteArray {
        val tableSize = 4 + tags.size * 12
        var offset = 128 + tableSize
        val table = Bytes().u32(tags.size.toLong())
        val data = Bytes()
        for ((signature, bytes) in tags) {
            table.signature(signature).u32(offset.toLong()).u32(bytes.size.toLong())
            bytes.forEach { data.u8(it.toInt()) }
            offset += bytes.size
        }
        val size = offset
        val header = Bytes().u32(size.toLong()).u32(0).u8(version).u8(0x20).u16(0).signature("mntr")
            .signature("RGB ").signature("XYZ ").apply { repeat(12) { u8(0) } }.signature("acsp")
        while (header.data.size < 128) header.u8(0)
        return header.bytes() + table.bytes() + data.bytes()
    }
}

private val D50 = Vector3(0.9642029, 1.0, 0.8249054)

private val BRADFORD = Matrix33(
    0.8951, 0.2664, -0.1614,
    -0.7502, 1.7135, 0.0367,
    0.0389, -0.0685, 1.0296
)

private fun bradford(from: Vector3, to: Vector3): Matrix33 {
    val source = BRADFORD * from
    val target = BRADFORD * to
    return BRADFORD.inversed * Matrix33(c0r0 = target.x / source.x, c1r1 = target.y / source.y, c2r2 = target.z / source.z) * BRADFORD
}

private fun xyz(xy: Vector2, luminance: Double = 1.0) = Vector3(xy.x / xy.y, 1.0, (1.0 - xy.x - xy.y) / xy.y) * luminance

/**
 * The XYZ of the primaries of [profile], scaled so that they add up to its white point
 */
private fun primaries(profile: ColorProfile): Matrix33 {
    val p = Matrix33.fromColumnVectors(xyz(profile.red), xyz(profile.green), xyz(profile.blue))
    val s = p.inversed * xyz(profile.white)
    return Matrix33.fromColumnVectors(xyz(profile.red) * s.x, xyz(profile.green) * s.y, xyz(profile.blue) * s.z)
}

/**
 * A profile with the primaries of [profile], adapted to D50 with chad like version 4 profiles
 */
private fun v4Profile(profile: ColorProfile, name: String): IccWriter {
    val adaptation = bradford(xyz(profile.white), D50)
    val adapted = adaptation * primaries(profile)
    return IccWriter(4).description(name).xyz("wtpt", D50).chad(adaptation)
        .xyz("rXYZ", adapted[0]).xyz("gXYZ", adapted[1]).xyz("bXYZ", adapted[2])
}

/**
 * A profile with the primaries of [profile], adapted to D50 without chad like older version 2 profiles
 */
private fun v2Profile(profile: ColorProfile, name: String): IccWriter {
    val white = xyz(profile.white)
    val adapted = bradford(white, D50) * primaries(profile)
    return IccWriter(2).description(name).xyz("wtpt", white)
        .xyz("rXYZ", adapted[0]).xyz("gXYZ", adapted[1]).xyz("bXYZ", adapted[2])
}

class ColorProfileTest {
    private val e = 1E-3

    private fun assertChromaticities(expected: ColorProfile, actual: ColorProfile) {
        for ((name, pair) in listOf(
            "red" to (expected.red to actual.red),
            "green" to (expected.green to actual.green),
            "blue" to (expected.blue to actual.blue),
            "white" to (expected.white to actual.white)
        )) {
            assertEquals(pair.first.x, pair.second.x, e, "$name x")
            assertEquals(pair.first.y, pair.second.y, e, "$name y")
        }
    }

    @Test
    fun knownProfilesHaveTheirLinearity() {
        assertEquals(Linearity.SRGB, ColorProfile.SRGB.linearity)
        assertEquals(Linearity.DISPLAY_P3, ColorProfile.DISPLAY_P3.linearity)
    }

    @Test
    fun readsVersion4DisplayP3() {
        val icc = v4Profile(ColorProfile.DISPLAY_P3, "Display P3")
            .para(2.4, 1.0 / 1.055, 0.055 / 1.055, 1.0 / 12.92, 0.04045, type = 3).build()
        val profile = ColorProfile.fromICC(icc)!!
        assertEquals("Display P3", profile.name)
        assertChromaticities(ColorProfile.DISPLAY_P3, profile)
        assertEquals(Linearity.DISPLAY_P3, profile.linearity)
        assertEquals(ColorTransfer.SRGB, profile.transfer)
        assertEquals(icc.toList(), profile.icc?.toList())
    }

    @Test
    fun readsVersion2Srgb() {
        // older sRGB profiles have a sampled curve and no chad
        val table = List(1024) { (decodeSrgb(it / 1023.0) * 65535.0).roundToInt() }
        val profile = ColorProfile.fromICC(v2Profile(ColorProfile.SRGB, "sRGB IEC61966-2.1").curv(table).build())!!
        assertEquals("sRGB IEC61966-2.1", profile.name)
        assertChromaticities(ColorProfile.SRGB, profile)
        assertEquals(Linearity.SRGB, profile.linearity)
        assertEquals(ColorTransfer.SRGB, profile.transfer)
    }

    @Test
    fun otherGamutsHaveNoLinearity() {
        val adobe = ColorProfile(
            "Adobe RGB (1998)", Vector2(0.64, 0.33), Vector2(0.21, 0.71), Vector2(0.15, 0.06),
            Vector2(0.3127, 0.3290), ColorTransfer.Gamma(563 / 256.0)
        )
        val profile = ColorProfile.fromICC(v4Profile(adobe, "Adobe RGB (1998)").curv(listOf(563)).build())!!
        assertChromaticities(adobe, profile)
        assertNull(profile.linearity)
        assertEquals(ColorTransfer.Gamma(563 / 256.0), profile.transfer)
    }

    @Test
    fun readsTransferFunctions() {
        fun transfer(writer: IccWriter.() -> IccWriter) =
            ColorProfile.fromICC(v4Profile(ColorProfile.SRGB, "test").writer().build())!!.transfer

        assertEquals(ColorTransfer.Linear, transfer { curv(emptyList()) })
        assertEquals(ColorTransfer.Linear, transfer { curv(listOf(256)) })
        assertEquals(ColorTransfer.Linear, transfer { para(1.0, type = 0) })
        val gamma = transfer { para(2.2, type = 0) }
        assertIs<ColorTransfer.Gamma>(gamma)
        assertEquals(2.2, gamma.gamma, 1E-4)
        val table = transfer { curv(listOf(0, 1000, 65535)) }
        assertIs<ColorTransfer.Table>(table)
        assertEquals(3, table.values.size)
        val bt709 = transfer { para(1 / 0.45, 1 / 1.099, 0.099 / 1.099, 1 / 4.5, 0.081, type = 3) }
        assertIs<ColorTransfer.Parametric>(bt709)
        assertEquals(((0.5 + 0.099) / 1.099).pow(1 / 0.45), bt709.decode(0.5), 1E-4)
    }

    @Test
    fun otherDataIsNotAProfile() {
        assertNull(ColorProfile.fromICC(ByteArray(0)))
        assertNull(ColorProfile.fromICC(ByteArray(200)))
        val icc = v4Profile(ColorProfile.SRGB, "sRGB").para(2.2, type = 0).build()
        // truncated profiles and profiles without colorants
        assertNull(ColorProfile.fromICC(icc.copyOf(icc.size - 20)))
        assertNull(ColorProfile.fromICC(IccWriter(4).description("no colorants").para(2.2, type = 0).build()))
    }

    @Test
    fun profilesCompareTheirValues() {
        val a = ColorProfile.fromICC(v4Profile(ColorProfile.SRGB, "sRGB").para(2.2, type = 0).build())!!
        val b = ColorProfile.fromICC(v4Profile(ColorProfile.SRGB, "sRGB").para(2.2, type = 0).build())!!
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    private fun decodeSrgb(x: Double) = if (x <= 0.04045) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
}
