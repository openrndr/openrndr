package org.openrndr.color

import kotlin.test.*

class ColorRGBaTest {

    @Test
    fun shouldDoEqualsProperly() {
        assertEquals(ColorRGBa.BLACK, ColorRGBa.BLACK)
        assertNotEquals(ColorRGBa.BLACK, ColorRGBa.WHITE)
        assertSame(ColorRGBa.BLACK, ColorRGBa.BLACK)
        assertNotSame(ColorRGBa.BLACK, ColorRGBa.WHITE)
        assertEquals(ColorRGBa.BLACK, ColorRGBa(0.0, 0.0, 0.0, 1.0, Linearity.SRGB))
        assertNotEquals(ColorRGBa.BLACK, ColorRGBa(0.0, 0.0, 0.0, 1.0, Linearity.LINEAR))
        assertEquals(ColorRGBa(0.0, 0.0, 0.0, 1.0, Linearity.SRGB), ColorRGBa(0.0, 0.0, 0.0, 1.0, Linearity.SRGB))
    }

    @Test
    fun mixTwoColors() {
        val mixed = mix(
            ColorRGBa(1.0, 0.0, 0.0, 1.0, Linearity.ASSUMED_LINEAR),
            ColorRGBa(1.0, 1.0, 1.0, 0.0, Linearity.LINEAR),
            0.5
        )
        assertEquals(mixed.linearity, Linearity.ASSUMED_LINEAR)
    }

    @Test
    fun fromHex() {
        assertEquals(ColorRGBa.fromHex(0x7fffff), ColorRGBa.fromHex("#7fffff"))
        assertEquals(ColorRGBa.fromHex(0xffffff), ColorRGBa.WHITE)
        assertEquals(ColorRGBa.fromHex(0x00ffff), ColorRGBa.CYAN)
        assertEquals(ColorRGBa.fromHex("#f0f"), ColorRGBa.MAGENTA)
        assertEquals(ColorRGBa.fromHex("f0f"), ColorRGBa.MAGENTA)
        assertEquals(ColorRGBa.fromHex(0xffff00), ColorRGBa.fromHex("ff0"))
        assertEquals(ColorRGBa.fromHex("#000001"), ColorRGBa(0.0, 0.0, 1.0 / 255.0, 1.0, Linearity.SRGB))
        assertNotEquals(ColorRGBa.fromHex(0xffff00), ColorRGBa.fromHex("ff07"))
        assertEquals(ColorRGBa.fromHex("0xffff0077"), ColorRGBa.fromHex("ff07"))
        assertEquals(ColorRGBa.fromHex("ffff0077"), ColorRGBa.fromHex("#ff07"))
        assertEquals(ColorRGBa.fromHex("0xffAA0077"), ColorRGBa.fromHex("fA07"))
        assertEquals(ColorRGBa.fromHex("0xffff0077"), ColorRGBa(1.0, 1.0, 0.0, 119.0 / 255.0, Linearity.SRGB))
    }
}
