import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineJoin
import org.openrndr.draw.StrokeAlignment
import org.openrndr.draw.loadFont
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.sin

/**
 * Demonstrates [StrokeAlignment]: the stroke of closed geometry is drawn inside, centered on, or outside of its
 * outline. The columns show the three alignments, the rows show circles, rectangles, contours and shapes with a hole.
 * The thin red lines show the outlines, the stroke is translucent to show where it overlaps the fill.
 *
 * The stroke weight animates and the line join changes every 3 seconds. Open geometry, like the line segment at the
 * bottom, has no inside or outside and is always stroked centered.
 */
fun main() = application {
    configure {
        width = 960
        height = 800
    }
    program {
        val font = loadFont(resourceUrl("/fonts/IBMPlexMono-Regular.ttf"), 16.0)

        val star = ShapeContour.fromPoints(
            (0 until 10).map { Polar(it * 36.0 - 90.0, if (it % 2 == 0) 60.0 else 26.0).cartesian },
            closed = true
        )

        // the geometry of each row, centered on the given position
        val rows: List<(Vector2) -> Pair<Drawer.() -> Unit, List<ShapeContour>>> = listOf(
            { p -> Pair({ circle(p, 50.0) }, listOf(Circle(p, 50.0).contour)) },
            { p ->
                val r = Rectangle.fromCenter(p, 110.0, 80.0)
                Pair({ rectangle(r) }, listOf(r.contour))
            },
            { p ->
                val c = star.transform(org.openrndr.math.transforms.transform { translate(p) })
                Pair({ contour(c) }, listOf(c))
            },
            { p ->
                // the hole is wound opposite to the outer contour, an inside stroke goes into the shape
                val s = Shape(listOf(Circle(p, 55.0).contour, Circle(p, 25.0).contour.reversed))
                Pair({ shape(s) }, s.contours)
            },
        )

        extend {
            drawer.clear(ColorRGBa.WHITE)

            val weight = 4.0 + 14.0 * (0.5 + 0.5 * sin(seconds * 1.5))
            val join = LineJoin.entries[(seconds / 3.0).toInt() % LineJoin.entries.size]

            drawer.fontMap = font
            drawer.fill = ColorRGBa.BLACK
            drawer.text("strokeWeight = ${"%.1f".format(weight)}, lineJoin = $join", 40.0, 40.0)

            for ((column, alignment) in StrokeAlignment.entries.withIndex()) {
                val x = 180.0 + column * 300.0
                drawer.fill = ColorRGBa.BLACK
                drawer.text("StrokeAlignment.$alignment", x - 100.0, 90.0)

                for ((row, geometry) in rows.withIndex()) {
                    val (draw, outlines) = geometry(Vector2(x, 180.0 + row * 160.0))

                    drawer.fill = ColorRGBa.PINK
                    drawer.stroke = ColorRGBa.BLACK.opacify(0.5)
                    drawer.strokeWeight = weight
                    drawer.lineJoin = join
                    drawer.strokeAlignment = alignment
                    drawer.draw()

                    // the outline of the geometry
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.RED
                    drawer.strokeWeight = 1.0
                    drawer.strokeAlignment = StrokeAlignment.CENTER
                    drawer.contours(outlines)
                }
            }

            // open geometry ignores the stroke alignment
            drawer.stroke = ColorRGBa.BLACK.opacify(0.5)
            drawer.strokeWeight = weight
            drawer.strokeAlignment = StrokeAlignment.INSIDE
            drawer.lineSegment(80.0, height - 30.0, width - 80.0, height - 30.0)
            drawer.stroke = ColorRGBa.RED
            drawer.strokeWeight = 1.0
            drawer.lineSegment(80.0, height - 30.0, width - 80.0, height - 30.0)
        }
    }
}
