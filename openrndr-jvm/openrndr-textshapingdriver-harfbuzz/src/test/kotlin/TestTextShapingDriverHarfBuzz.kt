import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.Script
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import kotlin.test.Test
import kotlin.test.assertEquals

class TestTextShapingDriverHarfBuzz {
    @Test
    fun test() {
//        val fontDriver = FontDriverFreetype()
//        val driver = TextShapingDriverHarfBuzz()
//        val face = fontDriver.loadFace("../../data/fonts/NotoSansKR-VariableFont_wght.ttf").withSize(64.0)
//
//        val features = driver.queryOpenTypeFeatures(face)
//        assert(features.isNotEmpty())
//        assert(features.containsKey("GSUB") || features.containsKey("GPOS"))
//
//        val scripts = driver.querySupportedScripts(face)
//        println("Scripts: $scripts")
//        assert(scripts.isNotEmpty())
//        assert(scripts.contains(Script.HANGUL))
//
//        features.forEach { (table, scripts) ->
//            println("Table: $table")
//            scripts.forEach { (script, features) ->
//                println("  Script: $script, Features: ${features.joinToString(", ")}")
//            }
//        }
    }

    @Test
    fun unsetPropertiesAreGuessed() {
        val driver = TextShapingDriverHarfBuzz()
        val face = FontDriverFreetype().loadFace("../../data/fonts/Platypi-Regular.ttf")
        for (text in listOf("-", "Hello, world!", "fi ff 1-2")) {
            val expected = driver.shape(face, text, direction = Direction.LEFT_TO_RIGHT, script = Script.LATIN, language = "en")
            assertEquals(expected, driver.shape(face, text), "no properties: $text")
            assertEquals(expected, driver.shape(face, text, language = "en"), "language only: $text")
            assertEquals(expected, driver.shape(face, text, script = Script.LATIN), "script only: $text")
            assertEquals(expected, driver.shape(face, text, direction = Direction.LEFT_TO_RIGHT), "direction only: $text")
        }
    }

    @Test
    fun surrogatePairsAreSingleCodePoints() {
        val driver = TextShapingDriverHarfBuzz()
        val face = FontDriverFreetype().loadFace("../../data/fonts/Platypi-Regular.ttf")
        // U+1F600 is outside the BMP and encoded as a surrogate pair, the font has no glyph for it
        val result = driver.shape(face, "a\uD83D\uDE00b", direction = Direction.LEFT_TO_RIGHT, script = Script.LATIN)
        assertEquals(3, result.size)
        assertEquals(driver.shape(face, "a", direction = Direction.LEFT_TO_RIGHT, script = Script.LATIN)[0], result[0])
        assertEquals(driver.shape(face, "b", direction = Direction.LEFT_TO_RIGHT, script = Script.LATIN)[0].glyphIndex, result[2].glyphIndex)
    }
}
