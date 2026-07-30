import org.openrndr.draw.font.internal.Script
import kotlin.test.Test

class TestTextShapingDriverHarfBuzz {
    @Test
    fun test() {
        val fontDriver = FontDriverFreetype()
        val driver = TextShapingDriverHarfBuzz()
        val face = fontDriver.loadFace("../../data/fonts/NotoSansKR-VariableFont_wght.ttf", 64.0, 1.0)

        val features = driver.queryOpenTypeFeatures(face)
        assert(features.isNotEmpty())
        assert(features.containsKey("GSUB") || features.containsKey("GPOS"))

        val scripts = driver.querySupportedScripts(face)
        println("Scripts: $scripts")
        assert(scripts.isNotEmpty())
        assert(scripts.contains(Script.HANGUL))
        
        features.forEach { (table, scripts) ->
            println("Table: $table")
            scripts.forEach { (script, features) ->
                println("  Script: $script, Features: ${features.joinToString(", ")}")
            }
        }
    }
}