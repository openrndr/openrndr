import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.test.*

class TestCircleDrawerGL3 : AbstractApplicationTestFixture() {
    @Test
    fun `a circle drawer`() {
        program.drawer.circle(Vector2(0.0, 0.0), 40.0)
        program.drawer.circles((0..20000).map {
            Circle(Vector2(Math.random(), Math.random()), Math.random() * 20.0)
        })
        program.drawer.circle(Vector2(0.0, 0.0), 40.0)
    }
}