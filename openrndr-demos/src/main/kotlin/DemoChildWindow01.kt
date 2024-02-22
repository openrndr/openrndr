import org.openrndr.WindowConfiguration
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.math.Vector2
import org.openrndr.window

fun main() {
    application {
        program {
            var w1Position = Vector2.ZERO
            keyboard.character.listen {
                if (it.character == 'w') {
                    window {
                        keyboard.character.listen {

                        }
                        extend {
                            drawer.clear(ColorRGBa.CYAN)
                        }
                    }
                }
            }

            val w1 = window {
                extend(Screenshots())
                mouse.moved.listen {
                    w1Position = it.position
                }
                extend {
                    drawer.clear(ColorRGBa.PINK)
                    drawer.circle(w1Position, 100.0)
                }
            }

            val w2 = window {
                extend(Screenshots())
                extend {
                    drawer.clear(ColorRGBa.GRAY)
                    drawer.circle(w1Position, 100.0)
                }
            }

            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.circle(drawer.bounds.center, 100.0)
            }
        }
    }
}