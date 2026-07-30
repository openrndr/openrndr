import org.openrndr.application
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.font.internal.FontDriver

fun main() {
    application {
        program {
            val fontDriver = FontDriverFreetype()
            FontDriver.driver = fontDriver
            val face = fontDriver.loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf", 64.0, RenderTarget.active.contentScale)
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