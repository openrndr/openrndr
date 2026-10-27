import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.sin

/**
 * Compares the primitives [Drawer.rectangle] and [Drawer.circle] to drawing the same geometry with [Drawer.contour].
 * Both should produce the same pixels for every stroke weight, [StrokeAlignment] and [LineJoin].
 *
 * The left column shows the primitive, the middle column the contour. The right column overlays both strokes with
 * additive blending: red is drawn by the primitive only, cyan by the contour only, white by both.
 *
 * The geometry rotates slowly to show the anti-aliasing, the stroke weight animates and the stroke alignment and
 * line join change every 3 seconds.
 */
fun main() = application {
    configure {
        width = 960
        height = 640
    }
    program {
        val font = loadFont(resourceUrl("/fonts/IBMPlexMono-Regular.ttf"), 16.0)

        // the geometry of each row, centered on the origin, drawn as primitive and as contour
        val rows = listOf(
            Triple("rectangle", { d: Drawer -> d.rectangle(Rectangle.fromCenter(Vector2.ZERO, 160.0, 110.0)) },
                { d: Drawer -> d.contour(Rectangle.fromCenter(Vector2.ZERO, 160.0, 110.0).contour) }),
            Triple("circle", { d: Drawer -> d.circle(Vector2.ZERO, 70.0) },
                { d: Drawer -> d.contour(Circle(Vector2.ZERO, 70.0).contour) }),
        )

        extend {
            drawer.clear(ColorRGBa.WHITE)

            val weight = 2.0 + 18.0 * (0.5 + 0.5 * sin(seconds * 1.3))
            val step = (seconds / 3.0).toInt()
            val alignment = StrokeAlignment.entries[step % StrokeAlignment.entries.size]
            val join = LineJoin.entries[(step / StrokeAlignment.entries.size) % LineJoin.entries.size]
            val angle = seconds * 6.0

            drawer.fontMap = font
            drawer.fill = ColorRGBa.BLACK
            drawer.text(
                "strokeWeight = ${"%.1f".format(weight)}, strokeAlignment = $alignment, lineJoin = $join",
                40.0, 36.0
            )

            val columns = listOf("drawer.%s", "drawer.contour", "overlay")
            for ((row, geometry) in rows.withIndex()) {
                val (name, primitive, contour) = geometry
                val y = 200.0 + row * 280.0
                for ((column, title) in columns.withIndex()) {
                    val x = 170.0 + column * 310.0
                    drawer.fill = ColorRGBa.BLACK
                    drawer.text(title.format(name), x - 120.0, y - 115.0)

                    if (column == 2) {
                        // a dark backdrop for the additive overlay
                        drawer.fill = ColorRGBa.BLACK
                        drawer.stroke = null
                        drawer.rectangle(Rectangle.fromCenter(Vector2(x, y), 280.0, 210.0))
                    }

                    drawer.isolated {
                        translate(x, y)
                        rotate(angle)
                        strokeWeight = weight
                        strokeAlignment = alignment
                        lineJoin = join
                        if (column < 2) {
                            fill = ColorRGBa.PINK
                            stroke = ColorRGBa.BLACK.opacify(0.5)
                            if (column == 0) primitive(this) else contour(this)
                        } else {
                            fill = null
                            drawStyle.blendMode = BlendMode.ADD
                            stroke = ColorRGBa.RED
                            primitive(this)
                            stroke = ColorRGBa.CYAN
                            contour(this)
                        }
                    }
                }
            }
        }
    }
}
