package org.openrndr.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorLSHaTest {
    val e = 1E-6
    val neutrals = listOf(ColorRGBa.BLACK, ColorRGBa.WHITE, ColorRGBa.GRAY)

    private fun assertRoundTrip(expected: ColorRGBa, actual: ColorRGBa, message: String) {
        for ((a, b) in listOf(expected.r to actual.r, expected.g to actual.g, expected.b to actual.b)) {
            assertTrue(!b.isNaN() && kotlin.math.abs(a - b) < 1E-3, "$message: expected $expected, got $actual")
        }
    }

    /**
     * Neutral colors have a maximum chroma of 0 at L = 0 and L = 100, their saturation must be 0 instead of NaN.
     */
    @Test
    fun neutralsLSHABa() {
        for (color in neutrals) {
            val lsh = ColorLSHABa.fromLCHABa(color.toLCHABa())
            assertEquals(0.0, lsh.s, e, "saturation of $color")
            assertRoundTrip(color, lsh.toRGBa().toSRGB(), "LSHABa round trip")
        }
    }

    @Test
    fun neutralsLSHUVa() {
        for (color in neutrals) {
            val lsh = ColorLSHUVa.fromLCHUVa(color.toLCHUVa())
            assertEquals(0.0, lsh.s, e, "saturation of $color")
            assertRoundTrip(color, lsh.toRGBa().toSRGB(), "LSHUVa round trip")
        }
    }
}
