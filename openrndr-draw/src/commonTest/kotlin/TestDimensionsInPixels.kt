package org.openrndr.draw

import org.openrndr.math.IntVector2
import kotlin.test.Test
import kotlin.test.assertEquals

class TestDimensionsInPixels {
    @Test
    fun testDimensionsInPixels() {
        assertEquals(IntVector2(720, 720), dimensionsInPixels(720, 720, 1.0, 0))
        assertEquals(IntVector2(360, 360), dimensionsInPixels(720, 720, 1.0, 1))
        assertEquals(IntVector2(180, 180), dimensionsInPixels(720, 720, 1.0, 2))
        assertEquals(IntVector2(90, 90), dimensionsInPixels(720, 720, 1.0, 3))

        assertEquals(IntVector2(1440, 1440), dimensionsInPixels(720, 720, 2.0, 0))
        assertEquals(IntVector2(720, 720), dimensionsInPixels(720, 720, 2.0, 1))
        assertEquals(IntVector2(360, 360), dimensionsInPixels(720, 720, 2.0, 2))
        assertEquals(IntVector2(180, 180), dimensionsInPixels(720, 720, 2.0, 3))
    }
}