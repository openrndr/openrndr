import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.test.*

class SegmentTest {

    @Test
    fun shouldCalculateSimplePointAtLength() {
        val curve =
            Segment2D(Vector2(110.0, 150.0), listOf(Vector2(25.0, 190.0), Vector2(210.0, 250.0)), Vector2(210.0, 30.0))
        assertEquals(Vector2(105.53567504882812, 152.2501678466797), curve.pointAtLength(5.0, 0.0001), 0.0005)
        assertEquals(Vector2(162.22564697265625, 170.3757781982422), curve.pointAtLength(120.0, 0.0001), 0.0005)
        assertEquals(curve.start, curve.pointAtLength(-500.0, 0.0001))
        assertEquals(curve.end, curve.pointAtLength(500.0, 0.0001))
    }
}