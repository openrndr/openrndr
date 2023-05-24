import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment

const val SIZE = 850.0

fun main() = application {
    configure {
        width = SIZE.toInt()
        height = SIZE.toInt()
    }

    val grid = buildList {
        generateSequence(5.0, 10.0::plus)
            .takeWhile { it < SIZE }
            .forEach {
                add(Segment(Vector2(it, 0.0), Vector2(it, SIZE)))
                add(Segment(Vector2(0.0, it), Vector2(SIZE, it)))
            }
    }

    val rect = Rectangle(0.0, 0.0, 200.0, 200.0)
    val circle = Circle(0.0, 0.0, 100.0)

    fun drawTestShapes(drawer: Drawer) {
        drawer.translate(10.0, 10.0)
        drawer.rectangle(rect)
        drawer.translate(210.0, 0.0)
        drawer.contour(rect.contour)
        drawer.translate(-110.0, 310.0)
        drawer.circle(circle)
        drawer.translate(210.0, 0.0)
        drawer.contour(circle.contour)
    }

    program {
        backgroundColor = ColorRGBa.WHITE

        extend {
            drawer.strokeWeight = 0.1
            drawer.stroke = ColorRGBa.BLACK
            drawer.segments(grid)
        }

        extend {
            drawer.strokeWeight = 10.0

            drawer.isolated {
                drawer.stroke = ColorRGBa.BLACK.opacify(0.5)
                drawer.fill = ColorRGBa.PINK
                drawTestShapes(this)
            }
            drawer.isolated {
                drawer.stroke = ColorRGBa.BLACK.opacify(0.5)
                drawer.fill = ColorRGBa.PINK.opacify(0.5)
                drawer.translate(420.0, 0.0)
                drawTestShapes(this)
            }
            drawer.isolated {
                drawer.stroke = ColorRGBa.fromHex(0x00ffff).opacify(0.5)
                drawer.fill = ColorRGBa.fromHex(0xff00ff)
                drawer.translate(0.0, 420.0)
                drawTestShapes(this)
            }
            drawer.isolated {
                drawer.stroke = ColorRGBa.fromHex(0x00ffff).opacify(0.5)
                drawer.fill = ColorRGBa.fromHex(0xff00ff).opacify(0.5)
                drawer.translate(420.0, 420.0)
                drawTestShapes(this)
            }
        }
    }
}