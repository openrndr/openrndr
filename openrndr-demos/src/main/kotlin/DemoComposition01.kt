import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.shape.Circle
import org.openrndr.shape.Composition
import org.openrndr.shape.GroupNode
import org.openrndr.shape.ShapeNode

/**
 * Draws the same `Circle` using `drawer.shape` and `drawer.composition`
 * side by side to compare both approaches.
 */
fun main() {
    application {
        program {
            extend {
                val shape = Circle(200.0, 200.0, 100.0).shape
                drawer.isolated {
                    drawer.fill = ColorRGBa.WHITE
                    drawer.stroke = null
                    drawer.shape(shape)
                }

                val g = GroupNode()
                g.children.add(ShapeNode(shape).apply {
                    this.fill = ColorRGBa.WHITE
                    this.stroke = null
                })
                drawer.translate(250.0, 0.0)
                val comp = Composition(g)
                drawer.composition(comp)
            }
        }
    }
}