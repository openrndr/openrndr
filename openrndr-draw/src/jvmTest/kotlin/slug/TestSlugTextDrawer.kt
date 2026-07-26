package slug

import AbstractApplicationTestFixture
import FontDriverFreetype
import TextShapingDriverHarfBuzz
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.slug.SlugTextDrawer
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.test.*

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

    @Test
    fun testBoxTextDoesNotExceedLineWidth() {
        val slugTextDrawer = SlugTextDrawer()
        val face = loadFace("../data/fonts/default.otf", 16.0, 1.0)

        val box = Rectangle(10.0, 360.0, 350.0, 720.0)
        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum"

        slugTextDrawer.addText(face, text, box)

        // Verify no glyph exceeds the box right edge
        val maxX = box.x + box.width
        for (cmd in slugTextDrawer.commands) {
            val glyphX = cmd.transform.c3r0
            assertTrue(glyphX <= maxX + 0.01, "Glyph at x=$glyphX exceeds box right edge at $maxX")
        }

        face.close()
    }


}