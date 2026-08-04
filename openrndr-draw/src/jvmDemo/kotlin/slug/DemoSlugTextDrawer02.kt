package slug

import FontDriverFreetype
import TextShapingDriverHarfBuzz
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
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            FontDriver.driver = FontDriverFreetype()
            TextShapingDriver.driver = TextShapingDriverHarfBuzz()

            val f0 = loadFace("data/fonts/default.otf", 32.0, 1.0)
            val f1 = loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf", 32.0, 1.0)
            val f2 = loadFace("data/fonts/Platypi-Regular.ttf", 32.0, 1.0)

            val scripts = TextShapingDriver.instance.querySupportedScripts(f2)
            for (script in scripts) {
                println(script)
                println(TextShapingDriver.instance.querySubstitutionFeatures(f2, script))
            }
            val slugTextDrawer = SlugTextDrawer()

            val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. "

            val box = Rectangle(10.0, 10.0, 350.0, 700.0)
            val box2 = box.movedBy(Vector2(360.0, 0.0))

            val spans = mutableListOf<TextSpan>()
            spans.add(TextSpan("Hello ", TextStyle(f2, fill = ColorRGBa.RED)))
            spans.add(TextSpan("World! ", TextStyle(f2, fill = ColorRGBa.TRANSPARENT, strokeWeight = 1.0, stroke = ColorRGBa.RED, strokeMode = StrokeMode.INNER)))
            spans.add(TextSpan(text, TextStyle(f2, fill = ColorRGBa.GRAY.shade(0.5), strokeWeight = 2.0, stroke = ColorRGBa.RED, strokeMode = StrokeMode.OUTER)))
            spans.add(TextSpan("로렘 입숨 돌로르 싯 아멧 ", TextStyle(f1, fill = ColorRGBa.PINK)))
            spans.add(TextSpan("ロレム イプサム ドロール シット アメット", TextStyle(f1, fill = ColorRGBa.PINK)))

            val start = System.currentTimeMillis()
            slugTextDrawer.startBatch()
            slugTextDrawer.addText(spans, listOf(box, box2))
            slugTextDrawer.endBatch()
            val end = System.currentTimeMillis()

            println("setting and font slugging: ${end - start}ms")
            extend {
                drawer.translate(drawer.bounds.center)
                drawer.scale(1.0 + 2.0 * mouse.position.y / height)
                drawer.translate(-drawer.bounds.center)


                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.PINK
                    drawer.rectangle(box)
                }

                var lastFrame = System.currentTimeMillis()
                slugTextDrawer.draw(drawer)
                Driver.instance.finish()
                val now = System.currentTimeMillis()
                //println((now - lastFrame))
                lastFrame = now
            }
        }
    }
}