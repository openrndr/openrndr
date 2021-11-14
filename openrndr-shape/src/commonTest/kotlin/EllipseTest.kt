import io.kotest.matchers.shouldBe
import org.openrndr.shape.*
import kotlin.test.Test

class EllipseTest {

    @Test
    fun shouldScaleCorrectly() {

        val ellipse = Ellipse(100.0, 300.0, 10.0, 10.0)
        val scaledMiddleEllipse = ellipse.scaledBy(2.0, 0.5, 0.5)
        val scaledEndEllipse = ellipse.scaledBy(2.0, 1.0, 1.0)

        scaledMiddleEllipse.center shouldBe ellipse.center
        scaledEndEllipse.center shouldBe (ellipse.center + ellipse.scale)
    }
}