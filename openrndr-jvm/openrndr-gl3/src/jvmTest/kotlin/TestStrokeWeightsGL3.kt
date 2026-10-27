import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies that strokes and fills match CSS/SVG rendering for all primitives: the stroke is centered on the outline
 * and covers `strokeWeight` across it, the fill covers the inside of the outline.
 */
class TestStrokeWeightsGL3 : AbstractApplicationTestFixture() {

    private val size = 256
    private val center = 128.0
    private val radius = 60.0

    /** the outline crosses the measured row y = [center] at x = [edge] */
    private val edge = center - radius

    /** renders [draw] and returns the alpha of the row y = [center] (or the column x = [center]) */
    private fun render(
        stroke: Boolean,
        fill: Boolean,
        strokeWeight: Double,
        lineCap: LineCap = LineCap.BUTT,
        strokeAlignment: StrokeAlignment = StrokeAlignment.CENTER,
        column: Boolean = false,
        draw: Drawer.() -> Unit
    ): List<Double> {
        val rt = renderTarget(size, size, contentScale = 1.0) {
            colorBuffer(type = ColorType.FLOAT32)
            depthBuffer(DepthFormat.DEPTH24_STENCIL8)
        }
        val drawer = program.drawer
        drawer.isolatedWithTarget(rt) {
            drawer.ortho(rt)
            drawer.clear(ColorRGBa.TRANSPARENT)
            drawer.stroke = if (stroke) ColorRGBa.WHITE else null
            drawer.fill = if (fill) ColorRGBa.WHITE else null
            drawer.strokeWeight = strokeWeight
            drawer.lineCap = lineCap
            drawer.strokeAlignment = strokeAlignment
            drawer.draw()
        }
        val shadow = rt.colorBuffer(0).shadow
        shadow.download()
        val c = center.toInt()
        val alphas = (0 until size).map { i -> if (column) shadow[c, i].alpha else shadow[i, c].alpha }
        rt.colorBuffer(0).destroy()
        rt.depthBuffer?.destroy()
        rt.destroy()
        return alphas
    }

    private val primitives: List<Pair<String, Drawer.() -> Unit>> = listOf(
        "circle" to { circle(center, center, radius) },
        "rectangle" to { rectangle(edge, edge, radius * 2, radius * 2) },
        "lineSegment" to { lineSegment(edge, 8.0, edge, size - 8.0) },
        "lineSegments" to { lineSegments(listOf(Vector2(edge, 8.0), Vector2(edge, size - 8.0))) },
        "lineStrip" to { lineStrip(listOf(Vector2(edge, 8.0), Vector2(edge, size - 8.0))) },
        "contour(circle)" to { contour(Circle(center, center, radius).contour) },
        "contour(rect)" to { contour(Rectangle(edge, edge, radius * 2, radius * 2).contour) },
        "shape(circle)" to { shape(Circle(center, center, radius).shape) },
        "shape(rect)" to { shape(Rectangle(edge, edge, radius * 2, radius * 2).shape) },
    )

    @Test
    fun strokesAreCenteredWithTheirWeight() {
        val failures = mutableListOf<String>()
        for (weight in listOf(1.0, 2.0, 4.0, 10.0, 20.0)) {
            for ((name, draw) in primitives) {
                // only the left half, which contains the left crossing of the outline
                val alphas = render(stroke = true, fill = false, strokeWeight = weight, draw = draw).take(center.toInt())
                val coverage = alphas.sum()
                val centroid = alphas.withIndex().sumOf { (x, a) -> (x + 0.5) * a } / coverage
                if (abs(coverage - weight) > 0.05 || abs(centroid - edge) > 0.05) {
                    failures += "$name, weight $weight: coverage $coverage (expected $weight), centroid $centroid (expected $edge)"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private val lShape = ShapeContour.fromPoints(
        listOf(
            Vector2(edge, edge), Vector2(center + radius, edge), Vector2(center + radius, center + 10.0),
            Vector2(center + 10.0, center + 10.0), Vector2(center + 10.0, center + radius), Vector2(edge, center + radius)
        ), closed = true
    )

    /** a ring with its hole ending at x = 98 on the measured row */
    private val ring = Shape(listOf(Circle(center, center, radius).contour, Circle(center, center, 30.0).contour.reversed))

    @Test
    fun strokeAlignment() {
        val closed = primitives.filter { !it.first.startsWith("line") } + listOf<Pair<String, Drawer.() -> Unit>>(
            "contour(rect, reversed)" to { contour(Rectangle(edge, edge, radius * 2, radius * 2).contour.reversed) },
            "contour(L)" to { contour(lShape) },
            "contour(L, reversed)" to { contour(lShape.reversed) },
            "shape(ring)" to { shape(ring) },
        )
        val open = primitives.filter { it.first.startsWith("line") }
        val failures = mutableListOf<String>()

        fun check(name: String, alphas: List<Double>, from: Int, until: Int, weight: Double, expectedCentroid: Double) {
            val window = alphas.subList(from, until)
            val coverage = window.sum()
            val centroid = window.withIndex().sumOf { (x, a) -> (from + x + 0.5) * a } / coverage
            if (abs(coverage - weight) > 0.05 || abs(centroid - expectedCentroid) > 0.05) {
                failures += "$name, weight $weight: coverage $coverage (expected $weight), centroid $centroid (expected $expectedCentroid)"
            }
        }

        for (alignment in StrokeAlignment.entries) {
            // the inside of all closed primitives lies to the right of x = edge on the measured row
            val shift = (0.5 - alignment.outsideFraction)
            for (weight in listOf(1.0, 4.0, 10.0)) {
                for ((name, draw) in closed) {
                    val alphas = render(stroke = true, fill = false, strokeWeight = weight, strokeAlignment = alignment, draw = draw)
                    if (name == "shape(ring)") {
                        check("$alignment $name (outer)", alphas, 0, 83, weight, edge + shift * weight)
                        // the inside of the hole contour lies to its left
                        check("$alignment $name (hole)", alphas, 83, center.toInt(), weight, 98.0 - shift * weight)
                    } else {
                        check("$alignment $name", alphas, 0, center.toInt(), weight, edge + shift * weight)
                    }
                }
                for ((name, draw) in open) {
                    val alphas = render(stroke = true, fill = false, strokeWeight = weight, strokeAlignment = alignment, draw = draw)
                    check("$alignment $name", alphas, 0, center.toInt(), weight, edge)
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /** the total stroke coverage of the whole render target */
    private fun totalCoverage(strokeWeight: Double, lineJoin: LineJoin, alignment: StrokeAlignment, draw: Drawer.() -> Unit): Double {
        val rt = renderTarget(size, size, contentScale = 1.0) {
            colorBuffer(type = ColorType.FLOAT32)
            depthBuffer(DepthFormat.DEPTH24_STENCIL8)
        }
        val drawer = program.drawer
        drawer.isolatedWithTarget(rt) {
            drawer.ortho(rt)
            drawer.clear(ColorRGBa.TRANSPARENT)
            drawer.stroke = ColorRGBa.WHITE
            drawer.fill = null
            drawer.strokeWeight = strokeWeight
            drawer.lineJoin = lineJoin
            drawer.strokeAlignment = alignment
            drawer.draw()
        }
        val shadow = rt.colorBuffer(0).shadow
        shadow.download()
        var total = 0.0
        for (y in 0 until size) for (x in 0 until size) total += shadow[x, y].alpha
        rt.colorBuffer(0).destroy()
        rt.depthBuffer?.destroy()
        rt.destroy()
        return total
    }

    @Test
    fun rectangleJoinsMatchContour() {
        val failures = mutableListOf<String>()
        val r = Rectangle(edge, edge, radius * 2, radius * 2)
        val weight = 20.0
        for (join in LineJoin.entries) {
            for (alignment in StrokeAlignment.entries) {
                // the area of the stroke band, the joins only change the outer corners
                val outside = weight * alignment.outsideFraction
                val inside = weight - outside
                val band = (2.0 * (radius + outside)).let { it * it } - (2.0 * (radius - inside)).let { it * it }
                val corners = when (join) {
                    LineJoin.MITER -> 0.0
                    LineJoin.BEVEL -> 4.0 * outside * outside / 2.0
                    LineJoin.ROUND -> 4.0 * (outside * outside - PI * outside * outside / 4.0)
                }
                val expected = band - corners
                val rectangle = totalCoverage(weight, join, alignment) { rectangle(r) }
                val contour = totalCoverage(weight, join, alignment) { contour(r.contour) }
                // the rectangle is computed analytically, the contour approximates round joins with segments
                if (abs(rectangle - expected) > 1.0 || abs(contour - expected) > expected * 2E-3) {
                    failures += "$join, $alignment: expected $expected, rectangle $rectangle, contour $contour"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun alignedStrokesDoNotCrossTheOutline() {
        // a star whose arms are narrower than the stroke weight, the stroke overlaps itself
        val star = ShapeContour.fromPoints(
            (0 until 10).map { Polar(it * 36.0 - 90.0, if (it % 2 == 0) 100.0 else 40.0).cartesian + Vector2(center, center) },
            closed = true
        )
        val failures = mutableListOf<String>()
        for (join in LineJoin.entries) {
            for (alignment in listOf(StrokeAlignment.INSIDE, StrokeAlignment.OUTSIDE)) {
                val rt = renderTarget(size, size, contentScale = 1.0) {
                    colorBuffer(type = ColorType.FLOAT32)
                    depthBuffer(DepthFormat.DEPTH24_STENCIL8)
                }
                val drawer = program.drawer
                drawer.isolatedWithTarget(rt) {
                    drawer.ortho(rt)
                    drawer.clear(ColorRGBa.TRANSPARENT)
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.strokeWeight = 26.0
                    drawer.lineJoin = join
                    drawer.strokeAlignment = alignment
                    drawer.contour(star)
                }
                val shadow = rt.colorBuffer(0).shadow
                shadow.download()
                // coverage on the wrong side of the outline, more than a pixel away from it
                var crossing = 0.0
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        val p = Vector2(x + 0.5, y + 0.5)
                        val inside = p in star
                        if (inside != (alignment == StrokeAlignment.INSIDE) && star.nearest(p).position.distanceTo(p) > 1.0) {
                            crossing += shadow[x, y].alpha
                        }
                    }
                }
                rt.colorBuffer(0).destroy()
                rt.depthBuffer?.destroy()
                rt.destroy()
                if (crossing > 0.5) {
                    failures += "$join, $alignment: coverage $crossing on the wrong side of the outline"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun fillsCoverTheInside() {
        val fills = primitives.filter { !it.first.startsWith("line") } + listOf<Pair<String, Drawer.() -> Unit>>(
            "contour(rect, reversed)" to { contour(Rectangle(edge, edge, radius * 2, radius * 2).contour.reversed) },
            "contour(L)" to { contour(lShape) },
            "contour(L, reversed)" to { contour(lShape.reversed) },
            "shape(ring)" to { shape(ring) },
        )
        val failures = mutableListOf<String>()
        val e = edge.toInt()
        for ((name, draw) in fills) {
            val alphas = render(stroke = false, fill = true, strokeWeight = 10.0, draw = draw)
            if (alphas[e - 1] > 0.05 || alphas[e] < 0.95) {
                failures += "$name: alpha left of the edge ${alphas[e - 1]}, right of the edge ${alphas[e]}"
            }
            if (name == "shape(ring)" && (alphas[97] < 0.95 || alphas[98] > 0.05)) {
                failures += "$name: alpha left of the hole ${alphas[97]}, inside the hole ${alphas[98]}"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun capsExtendLikeCss() {
        val failures = mutableListOf<String>()
        for (weight in listOf(2.0, 10.0)) {
            for ((cap, expected) in listOf(LineCap.BUTT to 0.0, LineCap.SQUARE to weight / 2.0)) {
                // a vertical line starting at y = 40, measured along its center column above y = center
                val alphas = render(stroke = true, fill = false, strokeWeight = weight, lineCap = cap, column = true) {
                    lineSegment(center, 40.0, center, size - 40.0)
                }
                val extension = alphas.take(center.toInt()).sum() - (center - 40.0)
                if (abs(extension - expected) > 0.05) {
                    failures += "$cap, weight $weight: extension $extension (expected $expected)"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
