package org.openrndr.color

import kotlin.test.Test
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
}