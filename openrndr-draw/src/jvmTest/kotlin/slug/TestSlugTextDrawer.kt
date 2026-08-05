package slug

import AbstractApplicationTestFixture
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.slug.SlugTextDrawer
import org.openrndr.draw.slug.TextStyle
import org.openrndr.fontdriver.freetype.FontDriverFreetype
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
    fun testVerticalCenteringSingleLine() {
        val slugTextDrawer = SlugTextDrawer()
        val face = loadFace("../data/fonts/default.otf", 16.0, 1.0)

        val box = Rectangle(0.0, 0.0, 400.0, 400.0)
        slugTextDrawer.addText("Hello", box, TextStyle(face, verticalAlign = 0.5))

        // With vertical centering, the text block should be centered in the box.
        // Text block height = ascent - descent (single line, descent is negative)
        val textBlockHeight = face.ascent - face.descent
        val expectedTopOfText = (box.height - textBlockHeight) / 2.0
        // The baseline y = expectedTopOfText + ascent
        val expectedBaseline = expectedTopOfText + face.ascent

        // All glyphs should be at the same y (single line)
        val glyphYs = slugTextDrawer.commands.map { it.transform.c3r1 }
        println("[DEBUG_LOG] face.ascent=${face.ascent}, face.descent=${face.descent}, face.lineGap=${face.lineGap}")
        println("[DEBUG_LOG] textBlockHeight=$textBlockHeight, expectedTopOfText=$expectedTopOfText, expectedBaseline=$expectedBaseline")
        println("[DEBUG_LOG] actual glyph y values: $glyphYs")
        
        for (cmd in slugTextDrawer.commands) {
            val glyphY = cmd.transform.c3r1
            assertEquals(expectedBaseline, glyphY, 0.01, "Glyph y=$glyphY should be at centered baseline $expectedBaseline")
        }

        face.close()
    }

    @Test
    fun testVerticalCenteringMultiLine() {
        val slugTextDrawer = SlugTextDrawer()
        val face = loadFace("../data/fonts/default.otf", 16.0, 1.0)

        val box = Rectangle(0.0, 0.0, 200.0, 400.0)
        val text = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor"
        slugTextDrawer.addText(text, box, TextStyle(face, verticalAlign = 0.5, lineHeightInEm = 1.5))

        // Find distinct y values (baselines)
        val baselineYs = slugTextDrawer.commands.map { it.transform.c3r1 }.distinct().sorted()
        val totalLines = baselineYs.size
        val lineHeight = (face.ascent - face.descent + face.lineGap) * 1.5

        // The text block top = first baseline - ascent
        // The text block bottom = last baseline - descent (descent is negative, so + |descent|)
        val textBlockTop = baselineYs.first() - face.ascent
        val textBlockBottom = baselineYs.last() - face.descent
        val actualTextBlockHeight = textBlockBottom - textBlockTop
        val expectedTextBlockHeight = face.ascent + (totalLines - 1) * lineHeight - face.descent

        // For centering, the text block should be centered in the box
        val expectedTop = (box.height - expectedTextBlockHeight) / 2.0
        
        println("[DEBUG_LOG] face.ascent=${face.ascent}, face.descent=${face.descent}, face.lineGap=${face.lineGap}")
        println("[DEBUG_LOG] lineHeight=$lineHeight, totalLines=$totalLines")
        println("[DEBUG_LOG] baselineYs=$baselineYs")
        println("[DEBUG_LOG] textBlockTop=$textBlockTop, textBlockBottom=$textBlockBottom")
        println("[DEBUG_LOG] actualTextBlockHeight=$actualTextBlockHeight, expectedTextBlockHeight=$expectedTextBlockHeight")
        println("[DEBUG_LOG] expectedTop=$expectedTop, box.center.y=${box.center.y}")
        println("[DEBUG_LOG] actual center of text block = ${(textBlockTop + textBlockBottom) / 2.0}")

        assertEquals(box.center.y, (textBlockTop + textBlockBottom) / 2.0, 0.01, 
            "Text block center should be at box center")

        face.close()
    }

    @Test
    fun testBoxTextDoesNotExceedLineWidth() {
        val slugTextDrawer = SlugTextDrawer()
        val face = loadFace("../data/fonts/default.otf", 16.0, 1.0)

        val box = Rectangle(10.0, 360.0, 350.0, 720.0)
        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum"

        slugTextDrawer.addText(text, box, TextStyle(face))
        // Verify no glyph exceeds the box right edge
        val maxX = box.x + box.width
        for (cmd in slugTextDrawer.commands) {
            val glyphX = cmd.transform.c3r0
            assertTrue(glyphX <= maxX + 0.01, "Glyph at x=$glyphX exceeds box right edge at $maxX")
        }

        face.close()
    }


}