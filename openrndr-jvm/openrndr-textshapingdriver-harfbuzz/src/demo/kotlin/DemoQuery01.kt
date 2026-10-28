import org.openrndr.application
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz

fun main() {
    application {
        program {
            val fontDriver = FontDriverFreetype()
            FontDriver.driver = fontDriver
            val face = fontDriver.loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf").withSize(64.0)
            val driver = TextShapingDriverHarfBuzz()

            val scripts = driver.querySupportedScripts(face)
            println("Scripts: $scripts")


            for (script in scripts) {
                println("Script: $script")
                val result = driver.queryPositionFeatures(face, script)
                println("Position features: $result")
                val result2 = driver.querySubstitutionFeatures(face, script)
                println("Substitution features: $result2")

            }
            extend {

            }
        }

    }
}