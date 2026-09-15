import org.openrndr.shape.Circle
import org.openrndr.shape.Shape
import kotlin.test.Test

class TestExpansionDrawerGL3 : AbstractApplicationTestFixture() {
    @Test
    fun testContours() {
        val c = Circle(0.0, 0.0, 100.0).contour
        for (i in 4 until 1000) {
            program.drawer.contour(c.sampleEquidistant(i))
        }
    }
    @Test
    fun testShapes() {
        val c = Circle(0.0, 0.0, 100.0).contour
        val c2 = Circle(0.0, 0.0, 50.0).contour.reversed
        for (i in 4 until 1000) {
            val s = Shape(listOf(c.sampleEquidistant(i), c2.sampleEquidistant(i)))
            program.drawer.shape(s)
        }
    }
}