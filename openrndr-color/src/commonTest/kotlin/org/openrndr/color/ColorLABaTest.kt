package org.openrndr.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorLABaTest {
    @Test
    fun testConversions() {
        val c = ColorRGBa.BLACK.toLABa()
        assertTrue(c.l == c.l && c.a == c.a && c.b == c.b)
        val crgb = c.toRGBa()
        assertTrue(crgb.r == crgb.r && crgb.g == crgb.g && crgb.b == crgb.b)
        assertTrue(crgb.r == 0.0 && crgb.g == 0.0 && crgb.b == 0.0)
    }

    @Test
    fun testNeutralReferencePoint() {
        val labWhite = ColorRGBa.WHITE.toLABa(ref = ColorXYZa.NEUTRAL)
        assertEquals(100.0, labWhite.l, 0.0)
        assertEquals(0.0, labWhite.a, 0.0)
        assertEquals(0.0, labWhite.b, 0.0)

        val labBlack = ColorRGBa.BLACK.toLABa(ref = ColorXYZa.NEUTRAL)
        assertEquals(0.0, labBlack.l, 0.0)
        assertEquals(0.0, labBlack.a, 0.0)
        assertEquals(0.0, labBlack.b, 0.0)

        // the sRGB matrices are derived from the chromaticities of the primaries and of D65 (0.3127, 0.3290),
        // http://www.brucelindbloom.com/ gives (53.2408, 80.0925, 67.2032) for the white point of ASTM E308
        val labRed = ColorRGBa.RED.toLABa(ref = ColorXYZa.NEUTRAL)
        assertEquals(53.23711559542936, labRed.l, 1e-9)
        assertEquals(80.09011352310385, labRed.a, 1e-9)
        assertEquals(67.20326351172214, labRed.b, 1e-9)
    }
}