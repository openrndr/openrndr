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

        val labRed = ColorRGBa.RED.toLABa(ref = ColorXYZa.NEUTRAL)
        assertEquals(53.23288178584245, labRed.l, 0.0)
        assertEquals(80.1053270902018, labRed.a, 0.0)
        assertEquals(67.22278194543621, labRed.b, 0.0)
    }
}