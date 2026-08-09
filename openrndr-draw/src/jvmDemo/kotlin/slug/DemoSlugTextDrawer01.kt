package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated
import org.openrndr.draw.slug.SlugTextDrawer
import org.openrndr.draw.slug.TextStyle
import org.openrndr.fontdriver.freetype.FontDriverFreetype
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

            val pface = loadFace("data/fonts/default.otf", 16.0, 1.0)


            val slugTextDrawer = SlugTextDrawer()

            val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum"

            val box = Rectangle(10.0, 360.0, 350.0, 360.0)


            slugTextDrawer.addText(text,
                box, TextStyle(pface, horizontalAlign = 0.5, verticalAlign = 0.5))

            slugTextDrawer.addText(text,
                box.movedBy(Vector2(360.0, 0.0)), style = TextStyle(pface, justify = true, lineHeightInEm = 1.2)
            )
            extend {
                drawer.translate(drawer.bounds.center)
                drawer.scale(1.0 + 2.0 * mouse.position.y / height)
                drawer.translate(-drawer.bounds.center)


                drawer.isolated {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.PINK
                    drawer.rectangle(box)
                }

                slugTextDrawer.draw(drawer)
            }
        }
    }
}