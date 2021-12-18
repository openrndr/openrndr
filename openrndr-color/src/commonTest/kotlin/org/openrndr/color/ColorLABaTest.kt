package org.openrndr.color

import kotlin.test.Test
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
}