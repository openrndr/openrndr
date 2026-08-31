import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.shadeStyle
import org.openrndr.shape.Circle
import org.openrndr.shape.contour
import kotlin.test.Test
import kotlin.test.assertEquals

class TestDrawerWebGL : AbstractApplicationTestFixture() {

    @Test
    fun drawCircle() {
        program.drawer.circle(0.0, 0.0, 100.0)
    }

    @Test
    fun drawLineSegment() {
        program.drawer.lineSegment(0.0, 0.0, 100.0, 100.0)
    }

    @Test
    fun drawRectangle() {
        program.drawer.rectangle(0.0, 0.0, 100.0, 100.0)
    }

    @Test
    fun drawImage() {
        val image = colorBuffer(100, 100)
        program.drawer.image(image)
        image.close()
    }

    @Test
    fun drawContour() {
        val contour = contour {
            moveTo(0.0, 0.0)
            lineTo(100.0, 0.0)
            lineTo(100.0, 100.0)
            lineTo(0.0, 100.0)
            close()
        }
        program.drawer.contour(contour)
    }


    @Test
    fun drawContourWithShadeStyle() {
        // https://github.com/openrndr/openrndr/issues/423
        val drawer = program.drawer
        fun createCurve() = Circle(drawer.bounds.center, 100.0).contour
        val curve = createCurve()

        drawer.clear(ColorRGBa.PINK)
        drawer.strokeWeight = 10.0
        drawer.shadeStyle = shadeStyle {
            fragmentTransform = """
                    float lfo = sin(p_seconds) * 0.5 + 0.5;
                    x_stroke = vec4(lfo, lfo, lfo, 1.0);
                """.trimIndent()
            parameter("seconds", program.seconds)
            parameter("someInt", 1)
        }

        assertEquals("int", drawer.shadeStyle?.parameterType("someInt"))
        assertEquals("float", drawer.shadeStyle?.parameterType("seconds"))
        drawer.contour(curve)

        drawer.shadeStyle?.
            parameter("seconds", program.seconds + 1.0)

        drawer.contour(curve)

        drawer.circle(100.0, 100.0, 20.0)
    }

}