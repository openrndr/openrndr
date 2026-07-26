package slug

import AbstractApplicationTestFixture
import FontDriverFreetype
import TextShapingDriverHarfBuzz
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.slug.SlugTextDrawer
import org.openrndr.math.Vector2
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestSlugTextDrawer: AbstractApplicationTestFixture() {

    @BeforeTest
    fun setupDrivers() {
        FontDriver.driver = FontDriverFreetype()
        TextShapingDriver.driver = TextShapingDriverHarfBuzz()
    }

    @Test
    fun testBasicDraw() {
        val slugTextDrawer = SlugTextDrawer()
        val face = loadFace("../data/fonts/default.otf", 16.0, 1.0)

        slugTextDrawer.addText(face, "Hello world", Vector2.ZERO)
        slugTextDrawer.draw(program.drawer)
        face.close()
    }


}