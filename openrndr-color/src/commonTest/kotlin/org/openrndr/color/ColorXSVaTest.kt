package org.openrndr.color

import kotlin.test.Test
import kotlin.test.assertTrue

class ColorXSVaTest {
    val e = 1E-6
    val colors = listOf(
        ColorRGBa.BLACK,
        ColorRGBa.WHITE,
        ColorRGBa.RED,
        ColorRGBa.GREEN,
        ColorRGBa.BLUE,
        ColorRGBa.MAGENTA,
        ColorRGBa.YELLOW,
        ColorRGBa.CYAN
    )

    /**
     * Verifies the correctness of color conversion functions between `ColorRGBa`, `ColorXSVa`, and `ColorHSVa` types.
     *
     * The method iterates over a predefined set of colors, converts each color through a series of transformations
     * (`ColorRGBa -> ColorXSVa -> ColorHSVa -> ColorXSVa`) and ensures the resulting values (`x`, `s`, `v`) fall within
     * an acceptable precision range (`e`) of the original values.
     *
     * Assertions:
     * - The resulting `x` value after reverse conversion should fall within the tolerance range (`xsva.x - e`, `xsva.x + e`).
     * - The resulting `s` value after reverse conversion should fall within the tolerance range (`xsva.s - e`, `xsva.s + e`).
     * - The resulting `v` value after reverse conversion should fall within the tolerance range (`xsva.v - e`, `xsva.v + e`).
     *
     * This test ensures that the conversion functions maintain the integrity of color data throughout the transformations.
     */
    @Test
    fun testConversions() {
        for (color in colors) {
            val xsva = color.toXSVa()
            val hsva = xsva.toHSVa()
            val xsvap = hsva.toXSVa()
            assertTrue(xsvap.x in xsva.x - e..xsva.x + e)
            assertTrue(xsvap.s in xsva.s - e..xsva.s + e)
            assertTrue(xsvap.v in xsva.v - e..xsva.v + e)
            val colorp = xsvap.toRGBa()
            assertTrue(colorp.r in color.r - e..color.r + e)
            assertTrue(colorp.g in color.g - e..color.g + e)
            assertTrue(colorp.b in color.b - e..color.b + e)
        }
    }
}