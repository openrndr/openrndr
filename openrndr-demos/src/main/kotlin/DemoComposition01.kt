import org.openrndr.applicationSynchronous
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.shape.Circle
import org.openrndr.shape.Composition
import org.openrndr.shape.GroupNode
import org.openrndr.shape.ShapeNode

fun main() {
    applicationSynchronous {
        program {
            extend {
                val shape = Circle(200.0, 200.0, 100.0).shape
                drawer.isolated {
                    drawer.fill = ColorRGBa.WHITE
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.shape(shape)
                }
                val g = GroupNode()
                g.children.add(ShapeNode(shape).apply {
                    this.fill = ColorRGBa.WHITE
                    this.stroke = ColorRGBa.RED
                })
                drawer.translate(250.0, 0.0)
                val comp = Composition(g)
                drawer.composition(comp)
            }
        }
    }
}