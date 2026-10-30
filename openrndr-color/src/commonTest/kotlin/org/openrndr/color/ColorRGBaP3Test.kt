package org.openrndr.color

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorRGBaP3Test {
    private val e = 1E-6

    private fun assertColor(expected: ColorRGBa, actual: ColorRGBa, tolerance: Double = e) {
        assertEquals(expected.linearity, actual.linearity, "linearity of $actual")
        assertEquals(expected.r, actual.r, tolerance, "r of $actual")
        assertEquals(expected.g, actual.g, tolerance, "g of $actual")
        assertEquals(expected.b, actual.b, tolerance, "b of $actual")
        assertEquals(expected.alpha, actual.alpha, tolerance, "alpha of $actual")
    }

    @Test
    fun p3PrimariesAreOutsideOfTheSrgbGamut() {
        assertColor(
            ColorRGBa(1.2249402, -0.0420570, -0.0196376, 1.0, Linearity.LINEAR),
            ColorRGBa(1.0, 0.0, 0.0, 1.0, Linearity.DISPLAY_P3).toLinear()
        )
        assertColor(
            ColorRGBa(-0.2249402, 1.0420570, -0.0786360, 1.0, Linearity.LINEAR),
            ColorRGBa(0.0, 1.0, 0.0, 1.0, Linearity.DISPLAY_P3).toLinear()
        )
        assertColor(
            ColorRGBa(0.0, 0.0, 1.0982736, 1.0, Linearity.LINEAR),
            ColorRGBa(0.0, 0.0, 1.0, 1.0, Linearity.DISPLAY_P3).toLinear()
        )
    }

    @Test
    fun srgbRedInP3() {
        // color(display-p3 0.9175 0.2003 0.1386) in CSS
        assertColor(ColorRGBa(0.9175, 0.2003, 0.1386, 1.0, Linearity.DISPLAY_P3), ColorRGBa.RED.toP3(), 1E-4)
    }

    @Test
    fun grayIsTheSameInAllSpaces() {
        for (gray in listOf(0.0, 0.25, 0.5, 1.0)) {
            val srgb = ColorRGBa(gray, gray, gray, 0.5, Linearity.SRGB)
            assertColor(ColorRGBa(gray, gray, gray, 0.5, Linearity.DISPLAY_P3), srgb.toP3())
            assertColor(srgb, srgb.toP3().toSRGB())
        }
    }

    @Test
    fun conversionsRoundTrip() {
        val colors = listOf(
            ColorRGBa(1.0, 0.0, 0.0, 1.0, Linearity.DISPLAY_P3),
            ColorRGBa(0.0, 1.0, 0.0, 0.5, Linearity.DISPLAY_P3),
            ColorRGBa(0.2, 0.4, 0.9, 1.0, Linearity.DISPLAY_P3),
            ColorRGBa(2.0, 0.5, 0.1, 1.0, Linearity.DISPLAY_P3)
        )
        for (color in colors) {
            assertColor(color, color.toLinear().toP3())
            // P3 colors outside of the sRGB gamut have negative sRGB components, which survive the round trip
            assertColor(color, color.toSRGB().toP3())
            assertColor(color, color.toLinearity(Linearity.SRGB).toLinearity(Linearity.DISPLAY_P3))
        }
    }

    @Test
    fun luminanceOfP3Colors() {
        assertEquals(1.0, ColorRGBa(1.0, 1.0, 1.0, 1.0, Linearity.DISPLAY_P3).luminance, e)
        val p3 = ColorRGBa(1.0, 0.0, 0.0, 1.0, Linearity.DISPLAY_P3)
        assertEquals(p3.toLinear().luminance, p3.luminance, e)
    }

    @Test
    fun mixP3Colors() {
        val left = ColorRGBa(1.0, 0.0, 0.0, 1.0, Linearity.DISPLAY_P3)
        val right = ColorRGBa(0.0, 0.0, 1.0, 1.0, Linearity.DISPLAY_P3)
        assertColor(ColorRGBa(0.5, 0.0, 0.5, 1.0, Linearity.DISPLAY_P3), mix(left, right, 0.5))
        // colors of different spaces are mixed in the space of the right color
        assertEquals(Linearity.DISPLAY_P3, mix(ColorRGBa.WHITE, right, 0.5).linearity)
    }
}
