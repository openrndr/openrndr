package org.openrndr.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorLCHUVaTest {

    @Test
    fun testSaturation() {
        val r = ColorRGBa.RED.toLCHUVa()
        val g = ColorRGBa.GREEN.toLCHUVa()
        val b = ColorRGBa.BLUE.toLCHUVa()

        println(ColorRGBa.YELLOW.toLCHUVa().c)
        println(ColorRGBa.MAGENTA.toLCHUVa().c)
        println(r.c)
        println(g.c)
        println(b.c)
    }

    @Test
    fun testConversions() {
        // addresses https://github.com/openrndr/openrndr/issues/275
        val c = ColorRGBa.BLACK.toLCHUVa()
        assertTrue(c.l == c.l && c.c == c.c && c.h == c.h)
        val crgb = c.toRGBa()
        assertTrue(crgb.r == crgb.r && crgb.g == crgb.g && crgb.b == crgb.b)
    }

    @Suppress("GrazieInspection")
    @Test
    fun testIssue269() {
        // addresses https://github.com/openrndr/openrndr/issues/269
        val hue = 293.0
        val lchuv = ColorLCHUVa(4.0, 20.0, hue)
        val uv = lchuv.toLUVa()

        // here uv is ColorLUVa(l=4.0, u=7.81462256978547, v=-18.41009706904881, alpha=1.0, ref=ColorXYZa(x=0.9505, y=1.0, z=1.089, a=1.0))
        // http://colormine.org/convert/rgb-to-luv converts this to rgb (62.90, 0.0, 84.32)
        // which is very close to the srgb values below (due to difference in reference white point)

        val rgb255 = uv.toRGBa().toSRGB() * 255.0
        assertEquals(rgb255.r, 62.976759000412294, 0.0)
        assertEquals(rgb255.g, -58.03268354243183, 0.0)
        assertEquals(rgb255.b, 84.41559049951348, 0.0)

        // OPENRNDR's green value is negative, this is due to the LCHuv/LUV gamut being larger than the sRGB one
        // to preserve information (and allow to convert back to colorspaces with a supporting gamut) none of the
        // OPENRNDR conversions clamp

    }
}