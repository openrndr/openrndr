import io.kotest.matchers.shouldBe
import org.openrndr.shape.*
import kotlin.test.Test

class CircleTest {

    @Test
    fun shouldScaleCorrectly() {

        val circle = Circle(100.0, 300.0, 10.0)
        val scaledMiddleCircle = circle.scaledBy(2.0, 0.5, 0.5)
        val scaledEndCircle = circle.scaledBy(2.0, 1.0, 1.0)

        scaledMiddleCircle.center shouldBe circle.center
        scaledEndCircle.center shouldBe (circle.center + circle.radius)
    }
}