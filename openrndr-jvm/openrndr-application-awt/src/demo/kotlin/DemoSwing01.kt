import org.openrndr.awt.programPanel
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.shape.Circle
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Two OPENRNDR programs in a Swing user interface: in the halves of a split pane, with Swing controls that change
 * them. The controls are read on the render thread, the values are volatile.
 */
object Controls {
    @Volatile
    var speed = 1.0

    @Volatile
    var count = 12

    @Volatile
    var color: ColorRGBa = ColorRGBa.fromHex("#ff6040")
}

fun main() {
    SwingUtilities.invokeLater {
        val left = programPanel {
            var angle = 0.0
            extend {
                angle += Controls.speed / 60.0
                drawer.clear(ColorRGBa.fromHex("#202830"))
                drawer.fill = Controls.color
                drawer.stroke = null
                val n = Controls.count
                for (i in 0 until n) {
                    val a = angle + i * 2 * Math.PI / n
                    drawer.circle(width / 2.0 + cos(a) * width * 0.3, height / 2.0 + sin(a) * height * 0.3, 16.0)
                }
                drawer.fill = ColorRGBa.WHITE
                drawer.circle(mouse.position, 10.0)
            }
        }
        val right = programPanel {
            val circles = mutableListOf<Circle>()
            mouse.buttonDown.listen { circles.add(Circle(it.position, 20.0)) }
            mouse.dragged.listen { circles.add(Circle(it.position, 8.0)) }
            keyboard.keyDown.listen { if (it.name == "backspace") circles.clear() }
            extend {
                drawer.clear(ColorRGBa.fromHex("#e8e4dc"))
                drawer.fill = Controls.color.opacify(0.6)
                drawer.stroke = ColorRGBa.BLACK
                drawer.circles(circles)
                drawer.isolated {
                    drawer.translate(width / 2.0, height / 2.0)
                    drawer.rotate(seconds * 30.0 * Controls.speed)
                    drawer.fill = null
                    drawer.rectangle(-60.0, -60.0, 120.0, 120.0)
                }
                drawer.fill = ColorRGBa.BLACK
                drawer.text("click and drag to draw, backspace clears", 12.0, 24.0)
            }
        }

        val speed = JSlider(0, 400, 100).apply {
            addChangeListener { Controls.speed = value / 100.0 }
        }
        val count = JSpinner(SpinnerNumberModel(12, 1, 64, 1)).apply {
            addChangeListener { Controls.count = value as Int }
        }
        val color = JButton("color").apply {
            addActionListener {
                Controls.color = ColorRGBa(Random.nextDouble(0.2, 1.0), Random.nextDouble(0.2, 1.0), Random.nextDouble(0.2, 1.0))
            }
        }
        val controls = JPanel().apply {
            add(JLabel("speed"))
            add(speed)
            add(JLabel("count"))
            add(count)
            add(color)
        }

        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right).apply {
            resizeWeight = 0.5
            left.minimumSize = Dimension(100, 100)
            right.minimumSize = Dimension(100, 100)
        }
        JFrame("OPENRNDR in Swing").apply {
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            contentPane.add(split, BorderLayout.CENTER)
            contentPane.add(controls, BorderLayout.SOUTH)
            size = Dimension(1200, 700)
            setLocationRelativeTo(null)
            isVisible = true
        }
    }
}
