import org.openrndr.ApplicationBuilder
import org.openrndr.annotations.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.boundary.BoundaryCommand
import org.openrndr.draw.boundary.BoundaryGlyphMap
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.ShapeResult
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFaceSuspend
import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Shape

@Program("Text shaped with harfbuzzjs")
fun ApplicationBuilder.webGLDemo03() {
    program {
        // fonts are fetched without blocking, the font driver and text shaping driver are backed by harfbuzzjs
        val face = loadFaceSuspend("default.otf")

        class Line(val face: Face, val y: Double, val glyphs: List<Pair<ShapeResult, Matrix44>>)

        fun line(text: String, size: Double, y: Double): Line {
            val sized = face.withSize(size)
            var x = 20.0
            return Line(sized, y, TextShapingDriver.instance.shape(sized, text).map { result ->
                val transform = buildTransform { translate(x + result.offset.x, y - result.offset.y) }
                x += result.advance.x
                result to transform
            })
        }

        val lines = listOf(
            line("OPENRNDR draws text with windfoil", 48.0, 160.0),
            line("ligatures: office, affine, ffi", 32.0, 220.0),
            line("kerning: AV To Wa Yo", 32.0, 270.0),
            line("12px: The quick brown fox jumps over the lazy dog", 12.0, 310.0)
        )

        // the glyphs drawn by the boundary drawer, a windfoil drawer, from a map of their outlines
        val boundaryDrawer = drawer.boundaryDrawer
        val map = boundaryDrawer.createMap()
        val glyphs = BoundaryGlyphMap(map)
        val commands = lines.flatMap { line ->
            line.glyphs.map { (result, transform) ->
                BoundaryCommand(glyphs.getBoundaryForGlyphIndex(line.face, result.glyphIndex), transform, ColorRGBa.BLACK, null, 0.0)
            }
        }
        val command = boundaryDrawer.prepare(map, commands, boundaryDrawer.createInstanceBuffer(commands.size))

        // the same glyphs drawn as shapes, below
        val shapes: List<Shape> = lines.flatMap { line ->
            line.glyphs.map { (result, transform) ->
                line.face.glyphForIndex(result.glyphIndex).shape()
                    .transform(buildTransform { translate(0.0, 220.0) } * transform)
            }
        }

        extend {
            drawer.clear(ColorRGBa.WHITE)
            boundaryDrawer.draw(drawer, map, command)
            drawer.fill = ColorRGBa.GRAY
            drawer.stroke = null
            drawer.shapes(shapes)
        }
    }
}
