import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.resourceUrl
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Texture parameters of shade styles must not replace the textures that primitives bind themselves
 * (https://github.com/openrndr/openrndr/issues/394).
 */
class TestShadeStyleTextureBindingsGL3 : AbstractApplicationTestFixture() {

    private fun render(draw: Drawer.() -> Unit): ColorBuffer {
        val rt = renderTarget(128, 64, contentScale = 1.0) {
            colorBuffer(type = ColorType.FLOAT32)
            depthBuffer(DepthFormat.DEPTH24_STENCIL8)
        }
        program.drawer.isolatedWithTarget(rt) {
            ortho(rt)
            clear(ColorRGBa.TRANSPARENT)
            draw()
        }
        val result = rt.colorBuffer(0)
        rt.detachColorAttachments()
        rt.depthBuffer?.destroy()
        rt.destroy()
        return result
    }

    @Test
    fun imageAndShadeStyleTexturesAreBothBound() {
        val image = colorBuffer(16, 16, type = ColorType.FLOAT32).apply { fill(ColorRGBa.RED) }
        val parameter = colorBuffer(16, 16, type = ColorType.FLOAT32).apply { fill(ColorRGBa.GREEN) }
        val result = render {
            shadeStyle = shadeStyle {
                // red from the image, green from the texture parameter
                fragmentTransform = "x_fill = vec4(x_fill.r, texture(p_texture, vec2(0.5)).g, 0.0, 1.0);"
                parameter("texture", parameter)
            }
            image(image, 0.0, 0.0, 128.0, 64.0)
        }
        result.shadow.download()
        val center = result.shadow[64, 32]
        listOf(image, parameter, result).forEach { it.destroy() }
        assertTrue(
            abs(center.r - 1.0) < 1E-3 && abs(center.g - 1.0) < 1E-3 && center.b < 1E-3,
            "pixel is $center, expected red from the image and green from the texture parameter"
        )
    }

    @Test
    fun fontAndShadeStyleTexturesAreBothBound() {
        val font = program.loadFont(resourceUrl("/fonts/Roboto-Medium.ttf"), 32.0, contentScale = 1.0)
        val parameter = colorBuffer(16, 16, type = ColorType.FLOAT32).apply { fill(ColorRGBa.GREEN) }

        /** the coverage of the text and the color of its most covered pixel */
        fun render(style: ShadeStyle?): Pair<Double, ColorRGBa> {
            val result = render {
                fontMap = font
                fill = ColorRGBa.WHITE
                shadeStyle = style
                text("test", 8.0, 48.0)
            }
            result.shadow.download()
            var total = 0.0
            var brightest = ColorRGBa.TRANSPARENT
            for (y in 0 until result.height) for (x in 0 until result.width) {
                val c = result.shadow[x, y]
                total += c.alpha
                if (c.alpha > brightest.alpha) brightest = c
            }
            result.destroy()
            return Pair(total, brightest)
        }

        val (plain, _) = render(null)
        val (styled, color) = render(shadeStyle {
            // the glyphs are colored with the texture parameter, their coverage comes from the font texture
            fragmentTransform = "x_fill.rgb = texture(p_texture, vec2(0.5)).rgb;"
            parameter("texture", parameter)
        })
        parameter.destroy()
        assertTrue(plain > 50.0, "text should be drawn, coverage $plain")
        assertEquals(plain, styled, plain * 1E-3, "coverage of the text with the shade style")
        assertTrue(color.r < 1E-3 && color.g > 0.99 && color.b < 1E-3, "text color is $color, expected green")
    }
}
