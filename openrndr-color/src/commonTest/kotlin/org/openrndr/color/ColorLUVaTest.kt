package org.openrndr.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorLUVaTest {
    @Test
    fun testConversions() {
        // addresses https://github.com/openrndr/openrndr/issues/275
        val c = ColorRGBa.BLACK.toLUVa()
        assertTrue(c.l == c.l && c.u == c.u && c.v == c.v)
        val crgb = c.toRGBa()
        assertTrue(crgb.r == crgb.r && crgb.g == crgb.g && crgb.b == crgb.b)
    }


    @Test
    fun testNeutralReferencePoint() {
        val luvWhite = ColorRGBa.WHITE.toLUVa(ref = ColorXYZa.NEUTRAL)
        assertEquals(100.0, luvWhite.l, 0.0)
        assertEquals(0.0, luvWhite.u, 0.0)
        assertEquals(0.0, luvWhite.v, 0.0)

        val luvBlack = ColorRGBa.BLACK.toLUVa(ref = ColorXYZa.NEUTRAL)
        assertEquals(0.0, luvBlack.l, 0.0)
        assertEquals(0.0, luvBlack.u, 0.0)
        assertEquals(0.0, luvBlack.v, 0.0)

        // these values are very close to the conversions provided by http://colormine.org/convert/rgb-to-luv
        // differences are due to differing reference white points
        val luvRed = ColorRGBa.RED.toLUVa(ref = ColorXYZa.NEUTRAL)
        assertEquals(53.23288178584245, luvRed.l, 0.0)
        assertEquals(175.05256160740132, luvRed.u, 0.0)
        assertEquals(37.759612089002886, luvRed.v, 0.0)
    }
}