package org.openrndr.color

import kotlin.test.*

class ColorHSLaTest {
    @Test
    fun grayscaleToHSLA() {
        val gray = ColorRGBa(0.3, 0.3, 0.3, 1.0, Linearity.SRGB)
        val components = gray.toHSLa().toVector4().toDoubleArray()
        for (component in components) {
            assertTrue(component.isFinite())
        }
    }
}