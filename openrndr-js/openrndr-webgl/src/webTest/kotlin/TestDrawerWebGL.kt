import org.openrndr.draw.colorBuffer
import org.openrndr.shape.contour
import kotlin.test.Test

class TestDrawerWebGL: AbstractApplicationTestFixture() {

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

}