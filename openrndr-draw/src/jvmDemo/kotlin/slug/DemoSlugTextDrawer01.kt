package slug

import FaceFreetype
import FontDriverFreetype
import TextShapingDriverHarfBuzz
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.draw.slug.SlugMap
import org.openrndr.draw.slug.SlugTextDrawer
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.transform
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

            val pface = loadFace("data/fonts/default.otf", 16.0, 1.0)


            val slugTextDrawer = SlugTextDrawer()

            val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum"

            val box = Rectangle(10.0, 360.0, 350.0, 720.0)


            slugTextDrawer.addText(pface, text,
                box)
            extend {
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