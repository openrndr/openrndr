package org.openrndr.color

import kotlin.test.Test
import kotlin.test.assertTrue

class ColorLCHUVaTest {
    @Test
    fun testConversions() {
        // addresses https://github.com/openrndr/openrndr/issues/275
        val c = ColorRGBa.BLACK.toLCHUVa()
        assertTrue(c.l == c.l && c.c == c.c && c.h == c.h)
        val crgb = c.toRGBa()
        assertTrue(crgb.r == crgb.r && crgb.g == crgb.g && crgb.b == crgb.b)
    }
}