package org.openrndr.color

import kotlin.test.*

class ColorHSVaTest {
    @Test
    fun grayscaleToHSVA() {
        val gray = ColorRGBa(0.3, 0.3, 0.3, 1.0, Linearity.SRGB)
        val components = gray.toHSVa().toVector4().toDoubleArray()
        for (component in components) {
            assertTrue(component.isFinite())
        }
    }
}