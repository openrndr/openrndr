package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.ShapeFeature
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated
import org.openrndr.draw.slug.SlugTextDrawer
import org.openrndr.draw.slug.StrokeMode
import org.openrndr.draw.slug.TextSpan
import org.openrndr.draw.slug.TextStyle
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import kotlin.math.cos

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            FontDriver.driver = FontDriverFreetype()
            TextShapingDriver.driver = TextShapingDriverHarfBuzz()


            val f2 = loadFace("data/fonts/Platypi-Regular.ttf", 64.0, 1.0)


            val slugTextDrawer = SlugTextDrawer()

            val text = "OPENRNDR"

            val box = Rectangle(10.0, 10.0, 700.0, 700.0)
            val box2 = box.movedBy(Vector2(360.0, 0.0))


            extend {
                drawer.translate(drawer.bounds.center)
                drawer.scale(1.0 + 2.0 * mouse.position.y / height)
                drawer.translate(-drawer.bounds.center)


                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.PINK
                    drawer.rectangle(box)
                }

                val sw = cos(seconds) * 2.0 + 2.0

                val spans = mutableListOf<TextSpan>()
                spans.add(TextSpan("$text\n", TextStyle(f2, fill = ColorRGBa.GRAY)))
                spans.add(TextSpan("$text\n", TextStyle(f2, fill = ColorRGBa.GRAY, strokeWeight = sw, stroke = ColorRGBa.WHITE, strokeMode = StrokeMode.CENTER)))
                spans.add(TextSpan("$text\n", TextStyle(f2, fill = ColorRGBa.GRAY, strokeWeight = sw, stroke = ColorRGBa.WHITE, strokeMode = StrokeMode.INNER)))
                spans.add(TextSpan("$text\n", TextStyle(f2, fill = ColorRGBa.GRAY, strokeWeight = sw, stroke = ColorRGBa.WHITE, strokeMode = StrokeMode.OUTER)))
                spans.add(TextSpan(text, TextStyle(f2, fill = ColorRGBa.GRAY.shade(0.5), strokeWeight = sw, stroke = ColorRGBa.WHITE, strokeMode = StrokeMode.ERODE)))

                slugTextDrawer.clear()
                slugTextDrawer.style = TextStyle.Defaults.copy(verticalAlign = 0.5, horizontalAlign = 0.5)
                slugTextDrawer.startBatch()
                slugTextDrawer.addText(spans, listOf(box))
                slugTextDrawer.endBatch()


                slugTextDrawer.draw(drawer)
            }
        }
    }
}