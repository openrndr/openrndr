import org.openrndr.KeyModifier
import org.openrndr.application
import kotlin.math.cos

fun main() {
    application {

        configure {
            windowResizable = true
        }

        program {

            keyboard.character.listen {
                println(it.character)
            }

            keyboard.keyDown.listen {

                if (it.name == "v" &&  KeyModifier.SUPER in it.modifiers) {
                    println("Super+V pressed")
                    println(application.clipboardContents)
                }
            }

            window.drop.listen {
                println("files")
                println(it.files)
            }

            window.dropTexts.listen {
                println("texts")
                println(it.texts)
            }

            extend {
                drawer.circle(mouse.position, 40.0 + cos(seconds) * 20.0)
            }
        }
    }
}