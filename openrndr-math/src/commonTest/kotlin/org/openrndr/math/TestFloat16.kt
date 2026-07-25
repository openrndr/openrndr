package org.openrndr.math

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestFloat16 {
    @Test
    fun testBasic() {
        val values = listOf(0.0f, 1.0f, -1.0f, 2.0f, 0.5f, 65504.0f, -65504.0f)
        for (v in values) {
            val f16 = Float16.fromFloat(v)
            assertEquals(v, f16.toFloat(), "Failed for $v")
        }
    }

    @Test
    fun testRounding() {
        assertEquals(1.0f, Float16.fromFloat(1.0f).toFloat())
        val f2 = 1.0f + 1.0f / 1024.0f
        assertEquals(f2, Float16.fromFloat(f2).toFloat())
    }

    @Test
    fun testInfinity() {
        assertEquals(Float.POSITIVE_INFINITY, Float16.fromFloat(Float.POSITIVE_INFINITY).toFloat())
        assertEquals(Float.NEGATIVE_INFINITY, Float16.fromFloat(Float.NEGATIVE_INFINITY).toFloat())
        assertEquals(Float.POSITIVE_INFINITY, Float16.fromFloat(70000.0f).toFloat())
    }

    @Test
    fun testNaN() {
        assertTrue(Float16.fromFloat(Float.NaN).toFloat().isNaN())
    }

    @Test
    fun testSubnormal() {
        val normalMin = 0.00006103515625f // 2^-14
        val f16 = Float16.fromFloat(normalMin)
        assertTrue(abs(normalMin - f16.toFloat()) < 1e-10f)
    }

    @Test
    fun testConstants() {
        assertEquals(65504.0f, Float16.MAX_VALUE.toFloat())
        assertTrue(Float16.MIN_VALUE.toFloat() > 0.0f)
        assertTrue(Float16.MIN_VALUE.toFloat() < 0.0000001f)
        assertEquals(Float.POSITIVE_INFINITY, Float16.POSITIVE_INFINITY.toFloat())
        assertEquals(Float.NEGATIVE_INFINITY, Float16.NEGATIVE_INFINITY.toFloat())
        assertTrue(Float16.NaN.toFloat().isNaN())
    }
}