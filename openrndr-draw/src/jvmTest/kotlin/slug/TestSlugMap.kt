package slug

import AbstractApplicationTestFixture
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.slug.SlugMap
import org.openrndr.shape.Circle
import kotlin.test.Test

class TestSlugMap : AbstractApplicationTestFixture() {

    @Test
    fun testAddShape() {

        val curves = colorBuffer(4096, 64, type = ColorType.FLOAT32, format = ColorFormat.RG)
        val bands = colorBuffer(4096, 64, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)

        val slugMap = SlugMap(
            curves,
            bands
        ).apply {
            bands.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
        }

        val circle = Circle(0.0, 0.0, 8.0).shape
        slugMap.addShape(circle)

        // Verify that data was written to the ColorBuffers
        // We can check if shadows were updated or if we can read back from it.
        // For now, if it didn't crash and passed, we at least know flush didn't fail.
    }
}