package org.openrndr.color

import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorPrecisionTest {
    private fun maxError(a: ColorRGBa, b: ColorRGBa): Double {
        val x = a.toSRGB()
        val y = b.toSRGB()
        return maxOf(abs(x.r - y.r), abs(x.g - y.g), abs(x.b - y.b))
    }

    @Test
    fun roundTripsThroughXyzBasedModelsAreExact() {
        val r = Random(1)
        repeat(1000) {
            val c = ColorRGBa(r.nextDouble(), r.nextDouble(), r.nextDouble(), 1.0, Linearity.SRGB)
            assertTrue(maxError(c, c.toXYZa().toRGBa()) < 1e-12)
            assertTrue(maxError(c, c.toLABa().toRGBa()) < 1e-12)
            assertTrue(maxError(c, c.toLUVa().toRGBa()) < 1e-12)
            assertTrue(maxError(c, c.toLCHABa().toRGBa()) < 1e-12)
            assertTrue(maxError(c, c.toLCHUVa().toRGBa()) < 1e-12)
            assertTrue(maxError(c, ColorYxya.fromXYZa(c.toXYZa()).toRGBa()) < 1e-12)
            assertTrue(maxError(c, c.toP3().toSRGB()) < 1e-12)
        }
    }

    @Test
    fun theYRowIsTheLuminanceOfRec709() {
        val y = ColorRGBa(1.0, 0.0, 0.0, linearity = Linearity.LINEAR).toXYZa().y
        assertEquals(0.2126, y, 1e-4)
        assertEquals(ColorXYZa.NEUTRAL.x, 0.3127 / 0.3290, 1e-12)
    }

    @Test
    fun blackInYxy() {
        val black = ColorYxya.fromXYZa(ColorRGBa.BLACK.toXYZa()).toRGBa()
        assertEquals(0.0, black.r)
        assertEquals(0.0, black.g)
        assertEquals(0.0, black.b)
    }

    @Test
    fun graysHaveNoHueInLch() {
        val gray = ColorRGBa.GRAY.toLCHABa()
        assertEquals(0.0, gray.c)
        assertEquals(0.0, gray.h)
        assertEquals(0.0, ColorRGBa.GRAY.toLCHUVa().c)
    }

    @Test
    fun labIsMonotonicAtItsThreshold() {
        val white = ColorXYZa(1.0, 1.0, 1.0)
        val below = ColorXYZa(0.0, 0.008855999, 0.0).toLABa(white).l
        val above = ColorXYZa(0.0, 0.008856001, 0.0).toLABa(white).l
        assertTrue(above > below)
    }

    @Test
    fun whitePoints() {
        assertEquals(0.96422, ColorXYZa.SO2_D50.x, 1e-12)
        assertEquals(0.94972, ColorXYZa.SO2_D75.x, 1e-12)
        assertEquals(0.95047, ColorXYZa.SO2_D65.x, 1e-12)
    }

    @Test
    fun hslOfHuesFarFromTheirTurn() {
        val near = ColorHSLa(30.0, 0.5, 0.5).toRGBa()
        val far = ColorHSLa(30.0 + 360.0 * 1e7, 0.5, 0.5).toRGBa()
        assertTrue(maxError(near, far) < 1e-6)
    }

    @Test
    fun lowSaturationsInHsv() {
        assertEquals(1.0 - 5e-6, ColorHSVa(120.0, 5e-6, 1.0).toRGBa().r, 1e-15)
    }

    @Test
    fun eightBitColorsDecodeLikeTheFormula() {
        for (k in 0..255) {
            val c = rgb(k, k, k)
            val x = k / 255.0
            val expected = if (x <= 0.04045) x / 12.92 else ((x + 0.055) / (1 + 0.055)).pow(2.4)
            assertEquals(expected, c.toLinear().r)
        }
    }
}
