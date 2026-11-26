import org.openrndr.MouseTracker
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

/**
 * An application on the AWT back-end: the program is shown in an AWT frame. Move the mouse, click, drag, scroll and
 * press keys to see the events arrive.
 */
fun main() {
    System.setProperty("org.openrndr.application", "AWT")
    application {
        configure {
            width = 800
            height = 600
            title = "OPENRNDR on AWT"
            windowResizable = true
        }
        program {
            val trail = ArrayDeque<Vector2>()
            val mouseTracker = MouseTracker(mouse)
            var radius = 40.0
            var lastKey = "-"
            var lastCharacter = ' '

            mouse.moved.listen {
                trail.addLast(it.position)
                while (trail.size > 64) trail.removeFirst()
            }
            mouse.scrolled.listen { radius = (radius + it.rotation.y * 4.0).coerceIn(5.0, 200.0) }
            keyboard.keyDown.listen {
                lastKey = it.name
                if (it.name == "escape") application.exit()
            }
            keyboard.character.listen { lastCharacter = it.character }
            window.sized.listen { println("resized to ${it.size}") }

            extend {
                drawer.clear(ColorRGBa.fromHex("#202830"))
                drawer.fill = ColorRGBa.fromHex("#ff6040")
                drawer.stroke = null
                for (i in 0 until 12) {
                    val a = seconds + i * Math.PI / 6
                    drawer.circle(width / 2.0 + cos(a) * 180.0, height / 2.0 + sin(a * 1.5) * 140.0, 18.0)
                }
                drawer.stroke = ColorRGBa.WHITE
                drawer.strokeWeight = 3.0
                drawer.lineJoin = LineJoin.ROUND
                if (trail.size > 1) drawer.lineStrip(trail.toList())
                drawer.fill = if (mouseTracker.pressedButtons.isEmpty()) ColorRGBa.WHITE.opacify(0.5) else ColorRGBa.YELLOW
                drawer.circle(mouse.position, radius)
                drawer.fill = ColorRGBa.WHITE
                drawer.text("key: $lastKey  character: $lastCharacter  ${width}x$height @${window.contentScale}x", 20.0, 30.0)
            }
        }
    }
}
