package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.ShapeFeature
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated
import org.openrndr.draw.slug.SlugTextDrawer
import org.openrndr.draw.slug.TextSpan
import org.openrndr.draw.slug.TextStyle
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            FontDriver.driver = FontDriverFreetype()
            TextShapingDriver.driver = TextShapingDriverHarfBuzz()

            val f2 = loadFace("data/fonts/Platypi-Regular.ttf", 32.0, 1.0)

            val scripts = TextShapingDriver.instance.querySupportedScripts(f2)
            for (script in scripts) {
                println(script)
                println(TextShapingDriver.instance.querySubstitutionFeatures(f2, script))
            }
            val slugTextDrawer = SlugTextDrawer()

            val box = Rectangle(10.0, 10.0, 350.0, 700.0)
            val box2 = box.movedBy(Vector2(360.0, 0.0))

            val spans = mutableListOf<TextSpan>()

            for (i in 0 until 100) {
                spans.add(TextSpan("NO", TextStyle(f2, fill = ColorRGBa.WHITE)))
                spans.add(TextSpan("x", TextStyle(f2, fill = ColorRGBa.WHITE, sizeInEm = 0.5, baselineShiftInEm = -0.25)))
                spans.add(TextSpan(" CO", TextStyle(f2, fill = ColorRGBa.WHITE)))
                spans.add(TextSpan("2", TextStyle(f2, fill = ColorRGBa.WHITE, sizeInEm = 0.5, baselineShiftInEm = -0.25)))
                spans.add(TextSpan(" H", TextStyle(f2, fill = ColorRGBa.WHITE)))
                spans.add(
                    TextSpan(
                        "2",
                        TextStyle(f2, fill = ColorRGBa.WHITE, sizeInEm = 0.5, baselineShiftInEm = -0.25)
                    )
                )
                spans.add(TextSpan("O ", TextStyle(f2, fill = ColorRGBa.WHITE)))
            }
            slugTextDrawer.addText(spans, listOf(box, box2))

            extend {
                slugTextDrawer.draw(drawer)
                Driver.instance.finish()
                val now = System.currentTimeMillis()
            }
        }
    }
}