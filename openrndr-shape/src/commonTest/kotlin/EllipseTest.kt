import org.openrndr.shape.*
import kotlin.test.*

class EllipseTest {

    @Test
    fun shouldScaleCorrectly() {

        val ellipse = Ellipse(100.0, 300.0, 10.0, 10.0)
        val scaledMiddleEllipse = ellipse.scaledBy(2.0, 0.5, 0.5)
        val scaledEndEllipse = ellipse.scaledBy(2.0, 1.0, 1.0)

        assertTrue {
            scaledMiddleEllipse.center == ellipse.center
        }

        assertTrue {
            scaledEndEllipse.center == (ellipse.center + ellipse.scale)
        }
    }
}